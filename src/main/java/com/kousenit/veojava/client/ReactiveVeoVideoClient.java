package com.kousenit.veojava.client;

import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Base64;

@Component("reactiveVeoVideoClient")
public class ReactiveVeoVideoClient {
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration TIMEOUT = Duration.ofMinutes(10);
    private static final Retry NETWORK_RETRY = Retry
            .backoff(3, Duration.ofSeconds(2))
            .maxBackoff(Duration.ofSeconds(10));

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String OPERATION_ENDPOINT = "/operations/";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final String generateEndpoint;
    private final WebClient webClient;

    public ReactiveVeoVideoClient(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${veo.api.model:veo-3.0-fast-generate-preview}") String model) {

        this.generateEndpoint = "/models/" + model + ":predictLongRunning";
        HttpClient httpClient = HttpClient.create().followRedirect(true);
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .defaultHeader(API_KEY_HEADER, apiKey)
                .defaultHeader("Content-Type", CONTENT_TYPE_JSON)
                .build();
    }

    public Mono<VideoGenerationResponse> submitVideoGeneration(VideoGenerationRequest request) {
        return webClient.post()
                .uri(generateEndpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(VideoGenerationResponse.class);
    }

    public Mono<OperationStatus> checkOperationStatus(String operationId) {
        String uri = operationId.startsWith("models/") ? "/" + operationId : OPERATION_ENDPOINT + operationId;
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(OperationStatus.class);
    }

    public Mono<VideoResult> generateVideoReactive(VideoGenerationRequest request) {
        return submitVideoGeneration(request)
                .flatMap(r -> pollUntilComplete(r.operationId()))
                .flatMap(this::downloadVideo)
                .timeout(TIMEOUT);
    }

    private Mono<String> pollUntilComplete(String operationId) {
        return Flux.interval(Duration.ZERO, POLL_INTERVAL)
                .concatMap(_ -> checkOperationStatus(operationId))
                .takeUntil(OperationStatus::done)
                .last()
                .flatMap(status ->
                        status.error() != null
                                ? Mono.error(new RuntimeException(
                                "Video generation failed for operation %s: %s"
                                        .formatted(operationId, status.error().message())))
                                : Mono.just(operationId));
    }

    private Mono<VideoResult> downloadVideo(String operationId) {
        return checkOperationStatus(operationId)
                .flatMap(status -> {
                    var response = (status.response() instanceof OperationStatus.OperationResponse op) ? op : null;
                    if (response == null ||
                        response.generateVideoResponse() == null ||
                        response.generateVideoResponse().generatedSamples() == null ||
                        response.generateVideoResponse().generatedSamples().isEmpty()) {
                        return Mono.error(new RuntimeException("No video data in completed operation"));
                    }
                    var sample = response.generateVideoResponse().generatedSamples().getFirst();
                    var videoUri = sample.video().uri();
                    if (videoUri == null || videoUri.isBlank()) {
                        return Mono.error(new RuntimeException("Video URI missing"));
                    }
                    return fetchVideoBytes(videoUri)
                            .map(bytes -> toVideoResult(bytes, operationId));
                });
    }


    private Mono<byte[]> fetchVideoBytes(String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(byte[].class)
                .retryWhen(NETWORK_RETRY);
    }

    private VideoResult toVideoResult(byte[] videoBytes, String operationId) {
        String base64 = Base64.getEncoder().encodeToString(videoBytes);
        String filename = "video_" + sanitize(operationId) + ".mp4";
        return new VideoResult(base64, "video/mp4", filename, videoBytes != null ? videoBytes : new byte[0]);
    }

    private static String sanitize(String s) {
        return s == null ? "unknown" : s.replaceAll("[^a-zA-Z0-9]", "_");
    }

}