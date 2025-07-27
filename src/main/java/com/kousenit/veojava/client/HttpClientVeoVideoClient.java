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
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String API_KEY_HEADER = "x-goog-api-key";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    
    public HttpClientVeoVideoClient() {
        // Try both common environment variable names
        String key = System.getenv("GOOGLEAI_API_KEY");
        if (key == null || key.isEmpty()) {
            key = System.getenv("GEMINI_API_KEY");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("GOOGLEAI_API_KEY or GEMINI_API_KEY environment variable is required");
        }
        this.apiKey = key;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public VideoGenerationResponse submitVideoGeneration(VideoGenerationRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + GENERATE_ENDPOINT))
                    .header(API_KEY_HEADER, apiKey)
                    .header("Content-Type", CONTENT_TYPE_JSON)
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
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to submit video generation", e);
        }
    }
    
    @Override
    public OperationStatus checkOperationStatus(String operationId) {
        try {
            // operationId is actually the full operation name like "models/veo-3.0-generate-preview/operations/xyz"
            String url = operationId.startsWith("models/") ? 
                    BASE_URL + "/" + operationId : 
                    BASE_URL + OPERATION_ENDPOINT + operationId;
                    
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header(API_KEY_HEADER, apiKey)
                    .header("Content-Type", CONTENT_TYPE_JSON)
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
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to check operation status", e);
        }
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
        try {
            HttpRequest downloadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(videoUri))
                    .header(API_KEY_HEADER, apiKey)
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();
            
            HttpResponse<byte[]> downloadResponse = httpClient.send(downloadRequest, 
                    HttpResponse.BodyHandlers.ofByteArray());
            
            if (downloadResponse.statusCode() != 200) {
                throw new RuntimeException("Failed to download video: HTTP " + downloadResponse.statusCode());
            }
            
            var videoBytes = downloadResponse.body();
            var base64Video = Base64.getEncoder().encodeToString(videoBytes);
            var mimeType = downloadResponse.headers().firstValue("content-type").orElse("video/mp4");
            var filename = "video_" + operationId.replaceAll("[^a-zA-Z0-9]", "_") + 
                          (mimeType.contains("mp4") ? ".mp4" : ".mov");
            
            return new VideoResult(base64Video, mimeType, filename, videoBytes);
            
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to download video", e);
        }
    }
}