package com.kousenit.veojava.client;

import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Component("restTemplateVeoVideoClient")
public class RestTemplateVeoVideoClient implements VeoVideoClient {
    
    private final String generateEndpoint;
    private static final String OPERATION_ENDPOINT = "/operations/";
    
    private final RestTemplate restTemplate;

    // Constructor for Spring - uses property injection
    @Autowired
    public RestTemplateVeoVideoClient(
            @Value("${veo.api.model:veo-3.0-fast-generate-preview}") String model,
            RestTemplate restTemplate) {
        this.generateEndpoint = "/models/" + model + ":predictLongRunning";
        this.restTemplate = restTemplate;
    }
    
    // Constructor for demo usage - creates basic RestTemplate
    public RestTemplateVeoVideoClient() {
        this.generateEndpoint = "/models/veo-3.0-fast-generate-preview:predictLongRunning";
        this.restTemplate = new RestTemplate(); // Redirects work automatically!
        // Note: For demo usage, API key needs to be configured separately
    }
    
    
    @Override
    public VideoGenerationResponse submitVideoGeneration(VideoGenerationRequest request) {
        // All headers (including API key) and base URL configured in RestTemplate
        return restTemplate.postForObject(generateEndpoint, request, VideoGenerationResponse.class);
    }
    
    @Override
    public OperationStatus checkOperationStatus(String operationId) {
        // operationId is actually the full operation name like "models/veo-3.0-fast-generate-preview/operations/xyz"
        String uri = operationId.startsWith("models/") ? 
                "/" + operationId : 
                OPERATION_ENDPOINT + operationId;
        
        // All headers (including API key) and base URL configured in RestTemplate
        return restTemplate.getForObject(uri, OperationStatus.class);
    }
    
    @Override
    public VideoResult downloadVideo(String operationId) {
        OperationStatus status = checkOperationStatus(operationId);
        validateOperationStatus(status);
        
        var videoUri = extractVideoUri(status);
        return downloadVideoFromUri(videoUri, operationId);
    }
    
    private void validateOperationStatus(OperationStatus status) {
        if (!status.done()) {
            throw new IllegalStateException("Operation not completed yet");
        }
        
        if (status.error() != null) {
            throw new RuntimeException("Operation failed: " + status.error().message());
        }
    }
    
    private String extractVideoUri(OperationStatus status) {
        if (!(status.response() instanceof OperationStatus.OperationResponse response) ||
            response.generateVideoResponse() == null ||
            response.generateVideoResponse().generatedSamples() == null ||
            response.generateVideoResponse().generatedSamples().isEmpty()) {
            throw new RuntimeException("No video data found in response");
        }
        
        var sample = response.generateVideoResponse().generatedSamples().getFirst();
        return sample.video().uri();
    }
    
    private VideoResult downloadVideoFromUri(String videoUri, String operationId) {
        // No special setup needed - RestTemplate follows redirects automatically for GET requests!
        byte[] videoBytes = restTemplate.getForObject(videoUri, byte[].class);
        
        if (videoBytes == null) {
            throw new RuntimeException("Failed to download video from URI: " + videoUri);
        }
        
        var base64Video = Base64.getEncoder().encodeToString(videoBytes);
        var mimeType = "video/mp4";
        var filename = "video_" + operationId.replaceAll("[^a-zA-Z0-9]", "_") + ".mp4";
        
        return new VideoResult(base64Video, mimeType, filename, videoBytes);
    }
}