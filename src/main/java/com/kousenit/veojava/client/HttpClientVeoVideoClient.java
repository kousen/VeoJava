package com.kousenit.veojava.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Component("httpClientVeoVideoClient")
public class HttpClientVeoVideoClient implements VeoVideoClient {
    
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String GENERATE_ENDPOINT = "/models/veo-3.0-generate-preview:predictLongRunning";
    private static final String OPERATION_ENDPOINT = "/operations/";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    
    public HttpClientVeoVideoClient() {
        this.apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("GEMINI_API_KEY environment variable is required");
        }
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public VideoGenerationResponse submitVideoGeneration(VideoGenerationRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + GENERATE_ENDPOINT))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(2))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }
            
            return objectMapper.readValue(response.body(), VideoGenerationResponse.class);
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to submit video generation", e);
        }
    }
    
    @Override
    public OperationStatus checkOperationStatus(String operationId) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + OPERATION_ENDPOINT + operationId))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }
            
            return objectMapper.readValue(response.body(), OperationStatus.class);
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to check operation status", e);
        }
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