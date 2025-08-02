package com.kousenit.veojava.client;

import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Base64;

@Component("restClientVeoVideoClient")
public class RestClientVeoVideoClient implements VeoVideoClient {
    
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private final String generateEndpoint;
    private static final String OPERATION_ENDPOINT = "/operations/";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String API_KEY_HEADER = "x-goog-api-key";
    
    private final RestClient restClient;

    // Constructor for Spring - uses property injection
    @Autowired
    public RestClientVeoVideoClient(
            @SuppressWarnings("SpringElInspection")
            @Value("${gemini.api.key:#{environment.GEMINI_API_KEY}}") String apiKey,
            @Value("${veo.api.model:veo-3.0-fast-generate-preview}") String model) {
        this.generateEndpoint = "/models/" + model + ":predictLongRunning";
        this.restClient = createRestClient(apiKey);
    }
    
    // Constructor for demo usage - checks both environment variables
    public RestClientVeoVideoClient() {
        this.generateEndpoint = "/models/veo-3.0-fast-generate-preview:predictLongRunning";
        // Try both common environment variable names
        String key = System.getenv("GOOGLEAI_API_KEY");
        if (key == null || key.isEmpty()) {
            key = System.getenv("GEMINI_API_KEY");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("GOOGLEAI_API_KEY or GEMINI_API_KEY environment variable is required");
        }
        
        this.restClient = createRestClient(key);
    }
    
    // Helper method to create RestClient with redirect support
    private RestClient createRestClient(String apiKey) {
        // Use modern Java HttpClient with redirect support
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        
        return RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .defaultHeader("Content-Type", CONTENT_TYPE_JSON)
                .build();
    }
    
    @Override
    public VideoGenerationResponse submitVideoGeneration(VideoGenerationRequest request) {
        return restClient.post()
                .uri(generateEndpoint)
                .body(request)
                .retrieve()
                .body(VideoGenerationResponse.class);
    }
    
    @Override
    public OperationStatus checkOperationStatus(String operationId) {
        // operationId is actually the full operation name like "models/veo-3.0-fast-generate-preview/operations/xyz"
        String uri = operationId.startsWith("models/") ? 
                "/" + operationId : 
                OPERATION_ENDPOINT + operationId;
                
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(OperationStatus.class);
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
        byte[] videoBytes = restClient.get()
                .uri(videoUri)
                .retrieve()
                .body(byte[].class);
        
        var base64Video = Base64.getEncoder().encodeToString(videoBytes);
        var mimeType = "video/mp4"; // Default since RestClient doesn't easily expose headers
        var filename = "video_" + operationId.replaceAll("[^a-zA-Z0-9]", "_") + ".mp4";
        
        return new VideoResult(base64Video, mimeType, filename, videoBytes);
    }
}