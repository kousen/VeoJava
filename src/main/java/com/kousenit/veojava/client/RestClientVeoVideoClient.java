package com.kousenit.veojava.client;

import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Base64;

@Component("restClientVeoVideoClient")
public class RestClientVeoVideoClient implements VeoVideoClient {
    
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String GENERATE_ENDPOINT = "/models/veo-3.0-generate-preview:predictLongRunning";
    private static final String OPERATION_ENDPOINT = "/operations/";
    
    private final RestClient restClient;

    // Constructor for Spring - uses property injection
    public RestClientVeoVideoClient(@Value("${gemini.api.key:#{environment.GEMINI_API_KEY}}") String apiKey) {
        this.restClient = createRestClient(apiKey);
    }
    
    // Constructor for demo usage - checks both environment variables
    public RestClientVeoVideoClient() {
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
        // Configure request factory to follow redirects
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(true);
            }
        };
        
        return RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
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
        // operationId is actually the full operation name like "models/veo-3.0-generate-preview/operations/xyz"
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
        
        if (!status.done()) {
            throw new IllegalStateException("Operation not completed yet");
        }
        
        if (status.error() != null) {
            throw new RuntimeException("Operation failed: " + status.error().message());
        }
        
        // Check the new response structure
        if (!(status.response() instanceof OperationStatus.OperationResponse response) ||
            response.generateVideoResponse() == null ||
            response.generateVideoResponse().generatedSamples() == null ||
            response.generateVideoResponse().generatedSamples().isEmpty()) {
            throw new RuntimeException("No video data found in response");
        }
        
        var sample = response.generateVideoResponse().generatedSamples().getFirst();
        var videoUri = sample.video().uri();
        
        // Download the video from the URI using RestClient
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