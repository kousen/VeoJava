package com.kousenit.veojava.client;

import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Component("restClientVeoVideoClient")
public class RestClientVeoVideoClient implements VeoVideoClient {
    
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String GENERATE_ENDPOINT = "/models/veo-3.0-generate-preview:predictLongRunning";
    private static final String OPERATION_ENDPOINT = "/operations/";
    
    private final RestClient restClient;
    private final String apiKey;
    
    public RestClientVeoVideoClient(@Value("${gemini.api.key:#{environment.GEMINI_API_KEY}}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
    
    @Override
    public VideoGenerationResponse submitVideoGeneration(VideoGenerationRequest request) {
        return restClient.post()
                .uri(GENERATE_ENDPOINT)
                .body(request)
                .retrieve()
                .body(VideoGenerationResponse.class);
    }
    
    @Override
    public OperationStatus checkOperationStatus(String operationId) {
        return restClient.get()
                .uri(OPERATION_ENDPOINT + operationId)
                .retrieve()
                .body(OperationStatus.class);
    }
    
    @Override
    public VideoResult downloadVideo(String operationId) {
        OperationStatus status = checkOperationStatus(operationId);
        
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
    }
}