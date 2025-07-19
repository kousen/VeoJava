package com.kousenit.veojava.client;

import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Base64;

@Component("reactiveVeoVideoClient")
public class ReactiveVeoVideoClient {
    
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String GENERATE_ENDPOINT = "/models/veo-3.0-generate-preview:predictLongRunning";
    private static final String OPERATION_ENDPOINT = "/operations/";
    
    private final WebClient webClient;
    
    public ReactiveVeoVideoClient(@Value("${gemini.api.key:#{environment.GEMINI_API_KEY}}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
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
        return webClient.get()
                .uri(OPERATION_ENDPOINT + operationId)
                .retrieve()
                .bodyToMono(OperationStatus.class);
    }
    
    public Mono<VideoResult> downloadVideo(String operationId) {
        return checkOperationStatus(operationId)
                .map(status -> {
                    if (!status.done()) {
                        throw new IllegalStateException("Operation not completed yet");
                    }
                    
                    if (status.error() != null) {
                        throw new RuntimeException("Operation failed: " + status.error().message());
                    }
                    
                    // Using pattern matching with instanceof for better null safety
                    if (!(status.response() instanceof OperationStatus.OperationResponse response) ||
                        response.predictions() == null || 
                        response.predictions().isEmpty()) {
                        throw new RuntimeException("No video data found in response");
                    }
                    
                    var prediction = response.predictions().getFirst();
                    var base64Video = prediction.bytesBase64Encoded();
                    var mimeType = prediction.mimeType();
                    
                    var videoBytes = Base64.getDecoder().decode(base64Video);
                    var filename = "video_" + operationId.replaceAll("[^a-zA-Z0-9]", "_") + 
                                  (mimeType.contains("mp4") ? ".mp4" : ".mov");
                    
                    return new VideoResult(base64Video, mimeType, filename, videoBytes);
                });
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