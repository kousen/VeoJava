package com.kousenit.veojava.client;

import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Base64;

@Component("reactiveVeoVideoClient")
public class ReactiveVeoVideoClient {
    
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String GENERATE_ENDPOINT = "/models/veo-3.0-generate-preview:predictLongRunning";
    private static final String OPERATION_ENDPOINT = "/operations/";
    
    private final WebClient webClient;
    
    public ReactiveVeoVideoClient(@Value("${gemini.api.key:#{environment.GEMINI_API_KEY}}") String apiKey) {
        // Configure HTTP client to follow redirects
        HttpClient httpClient = HttpClient.create().followRedirect(true);
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .clientConnector(connector)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB buffer
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
    
    public Mono<VideoGenerationResponse> submitVideoGeneration(VideoGenerationRequest request) {
        return webClient.post()
                .uri(GENERATE_ENDPOINT)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(VideoGenerationResponse.class);
    }
    
    public Mono<OperationStatus> checkOperationStatus(String operationId) {
        // operationId is actually the full operation name like "models/veo-3.0-generate-preview/operations/xyz"
        String uri = operationId.startsWith("models/") ? 
                "/" + operationId : 
                OPERATION_ENDPOINT + operationId;
                
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(OperationStatus.class);
    }
    
    public Mono<VideoResult> downloadVideo(String operationId) {
        return checkOperationStatus(operationId)
                .handle((status, sink) -> {
                    if (!status.done()) {
                        sink.error(new IllegalStateException("Operation not completed yet"));
                        return;
                    }
                    
                    if (status.error() != null) {
                        sink.error(new RuntimeException("Operation failed: " + status.error().message()));
                        return;
                    }
                    
                    // Check the new response structure
                    if (!(status.response() instanceof OperationStatus.OperationResponse response) ||
                        response.generateVideoResponse() == null ||
                        response.generateVideoResponse().generatedSamples() == null ||
                        response.generateVideoResponse().generatedSamples().isEmpty()) {
                        sink.error(new RuntimeException("No video data found in response"));
                        return;
                    }
                    
                    var sample = response.generateVideoResponse().generatedSamples().getFirst();
                    var videoUri = sample.video().uri();
                    
                    sink.next(videoUri);
                })
                .flatMap(videoUri -> 
                    // Download the video from the URI using WebClient
                    webClient.get()
                            .uri((String) videoUri)
                            .retrieve()
                            .bodyToMono(byte[].class)
                            .map(videoBytes -> {
                                var base64Video = Base64.getEncoder().encodeToString(videoBytes);
                                var mimeType = "video/mp4"; // Default since WebClient response headers are complex to access here
                                var filename = "video_" + operationId.replaceAll("[^a-zA-Z0-9]", "_") + ".mp4";
                                
                                return new VideoResult(base64Video, mimeType, filename, videoBytes);
                            })
                );
    }
    
    public Mono<VideoResult> generateVideoReactive(VideoGenerationRequest request) {
        return submitVideoGeneration(request)
                .flatMap(response -> pollUntilComplete(response.operationId()))
                .flatMap(this::downloadVideo);
    }
    
    private Mono<String> pollUntilComplete(String operationId) {
        return checkOperationStatus(operationId)
                .flatMap(status -> {
                    if (status.done()) {
                        if (status.error() != null) {
                            return Mono.error(new RuntimeException("Video generation failed: " + status.error().message()));
                        }
                        return Mono.just(operationId);
                    }
                    return Mono.error(new IllegalStateException("Operation not complete"));
                })
                .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(5))
                        .filter(throwable -> throwable instanceof IllegalStateException))
                .timeout(Duration.ofMinutes(10));
    }
}