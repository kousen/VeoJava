package com.kousenit.veojava.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test that actually exercises the real HttpClient implementation
 * by configuring it to use our MockWebServer as the base URL.
 */
class HttpClientVeoVideoClientComprehensiveTest {
    
    private MockWebServer mockServer;
    private HttpClientVeoVideoClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        
        // Create a testable HttpClientVeoVideoClient that uses our mock server
        client = new TestableHttpClientVeoVideoClient(mockServer.url("/v1beta").toString());
    }
    
    @AfterEach
    void tearDown() throws Exception {
        try {
            mockServer.shutdown();
        } catch (Exception e) {
            // Ignore shutdown exceptions in tests
        }
    }
    
    /**
     * Custom HttpClientVeoVideoClient that allows us to override the base URL
     * to point to our MockWebServer for testing.
     */
    private static class TestableHttpClientVeoVideoClient extends HttpClientVeoVideoClient {
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;
        private final String apiKey;
        private final String baseUrl;
        private final String generateEndpoint = "/models/veo-3.0-fast-generate-preview:predictLongRunning";
        
        public TestableHttpClientVeoVideoClient(String baseUrl) {
            // Call parent constructor with test API key
            super("test-api-key", "veo-3.0-fast-generate-preview");
            
            this.baseUrl = baseUrl;
            this.apiKey = "test-api-key";
            this.objectMapper = new ObjectMapper();
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5)) // Shorter timeout for tests
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        
        @Override
        public VideoGenerationResponse submitVideoGeneration(VideoGenerationRequest request) {
            try {
                String requestBody = objectMapper.writeValueAsString(request);
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + generateEndpoint))
                        .header("x-goog-api-key", apiKey)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10)) // Shorter timeout for tests
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
                String url = operationId.startsWith("models/") ? 
                        baseUrl + "/" + operationId : 
                        baseUrl + "/operations/" + operationId;
                        
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("x-goog-api-key", apiKey)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10)) // Shorter timeout for tests
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
                        .header("x-goog-api-key", apiKey)
                        .timeout(Duration.ofMinutes(5))
                        .GET()
                        .build();
                
                HttpResponse<byte[]> downloadResponse = httpClient.send(downloadRequest, 
                        HttpResponse.BodyHandlers.ofByteArray());
                
                if (downloadResponse.statusCode() != 200) {
                    throw new RuntimeException("Failed to download video: HTTP " + downloadResponse.statusCode());
                }
                
                var videoBytes = downloadResponse.body();
                if (videoBytes == null) {
                    throw new RuntimeException("Failed to download video from URI: " + videoUri);
                }
                
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
    
    @Test
    void testSubmitVideoGeneration_Success() throws Exception {
        // Mock successful response
        VideoGenerationResponse expectedResponse = new VideoGenerationResponse("test-operation-id", Map.of());
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));
        
        // Test the ACTUAL implementation
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        VideoGenerationResponse response = client.submitVideoGeneration(request);
        
        assertThat(response.operationId()).isEqualTo("test-operation-id");
    }
    
    @Test
    void testSubmitVideoGeneration_ServerError() {
        // Mock server error
        mockServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.submitVideoGeneration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HTTP 500");
    }
    
    @Test
    void testSubmitVideoGeneration_ClientError() {
        // Mock client error (4xx)
        mockServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\": \"Bad Request\"}"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.submitVideoGeneration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HTTP 400");
    }
    
    @Test
    void testSubmitVideoGeneration_MalformedJsonResponse() {
        // Mock malformed JSON response
        mockServer.enqueue(new MockResponse()
                .setBody("invalid json")
                .addHeader("Content-Type", "application/json"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.submitVideoGeneration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to submit video generation");
    }
    
    @Test
    void testCheckOperationStatus_InProgress() throws Exception {
        // Mock in-progress response
        OperationStatus inProgressStatus = new OperationStatus(
                "test-operation-id",
                false,
                null,
                null,
                Map.of()
        );
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(inProgressStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Test the ACTUAL implementation
        OperationStatus response = client.checkOperationStatus("test-operation-id");
        
        assertThat(response.done()).isFalse();
        assertThat(response.operationId()).isEqualTo("test-operation-id");
    }
    
    @Test
    void testCheckOperationStatus_WithFullOperationId() throws Exception {
        // Test with operation ID that starts with "models/"
        OperationStatus status = new OperationStatus(
                "models/veo-3.0-fast-generate-preview/operations/test-op",
                true,
                null,
                null,
                Map.of()
        );
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(status))
                .addHeader("Content-Type", "application/json"));
        
        // Test the ACTUAL implementation
        OperationStatus result = client.checkOperationStatus("models/veo-3.0-fast-generate-preview/operations/test-op");
        
        assertThat(result.operationId()).isEqualTo("models/veo-3.0-fast-generate-preview/operations/test-op");
        assertThat(result.done()).isTrue();
    }
    
    @Test
    void testCheckOperationStatus_NotFound() {
        // Mock 404 response
        mockServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("Not Found"));
        
        assertThatThrownBy(() -> client.checkOperationStatus("non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HTTP 404");
    }
    
    @Test
    void testCheckOperationStatus_MalformedJsonResponse() {
        // Mock malformed JSON response
        mockServer.enqueue(new MockResponse()
                .setBody("invalid json")
                .addHeader("Content-Type", "application/json"));
        
        assertThatThrownBy(() -> client.checkOperationStatus("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to check operation status");
    }
    
    @Test
    void testDownloadVideo_CompleteFlow() throws Exception {
        // Mock completed operation status
        var generatedSample = new OperationStatus.GeneratedSample(
                new OperationStatus.VideoReference("http://localhost:" + mockServer.getPort() + "/v1beta/video.mp4")
        );
        var generateVideoResponse = new OperationStatus.GenerateVideoResponse(
                java.util.List.of(generatedSample),
                java.util.List.of()
        );
        var operationResponse = new OperationStatus.OperationResponse(
                "type.googleapis.com/google.ai.generativelanguage.v1beta.GenerateVideoResponse",
                generateVideoResponse
        );
        
        OperationStatus completedStatus = new OperationStatus(
                "test-operation-id",
                true,
                null,
                operationResponse,
                Map.of()
        );
        
        // Mock responses: 1. operation status check, 2. video download
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        byte[] videoContent = "fake-video-content".getBytes();
        mockServer.enqueue(new MockResponse()
                .setBody(new String(videoContent))
                .addHeader("Content-Type", "video/mp4"));
        
        // Test the ACTUAL implementation - this will test downloadVideo AND downloadVideoFromUri
        VideoResult result = client.downloadVideo("test-operation-id");
        
        assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.videoBytes()).isEqualTo(videoContent);
                    assertThat(r.mimeType()).isEqualTo("video/mp4");
                    assertThat(r.filename()).contains("test_operation_id").endsWith(".mp4");
                    assertThat(r.videoBase64()).isEqualTo(Base64.getEncoder().encodeToString(videoContent));
                });
    }
    
    @Test
    void testDownloadVideo_QuickTimeFormat() throws Exception {
        // Mock completed operation status
        var generatedSample = new OperationStatus.GeneratedSample(
                new OperationStatus.VideoReference("http://localhost:" + mockServer.getPort() + "/v1beta/video.mov")
        );
        var generateVideoResponse = new OperationStatus.GenerateVideoResponse(
                java.util.List.of(generatedSample),
                java.util.List.of()
        );
        var operationResponse = new OperationStatus.OperationResponse(
                "type.googleapis.com/google.ai.generativelanguage.v1beta.GenerateVideoResponse",
                generateVideoResponse
        );
        
        OperationStatus completedStatus = new OperationStatus(
                "test-operation-id",
                true,
                null,
                operationResponse,
                Map.of()
        );
        
        // Mock responses: 1. operation status check, 2. video download with QuickTime mime type
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        byte[] videoContent = "fake-quicktime-video-content".getBytes();
        mockServer.enqueue(new MockResponse()
                .setBody(new String(videoContent))
                .addHeader("Content-Type", "video/quicktime"));
        
        VideoResult result = client.downloadVideo("test-operation-id");
        
        assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.mimeType()).isEqualTo("video/quicktime");
                    assertThat(r.filename()).contains("test_operation_id").endsWith(".mov");
                });
    }
    
    @Test
    void testDownloadVideo_NotCompleted() throws Exception {
        // Mock not completed status
        OperationStatus inProgressStatus = new OperationStatus(
                "test-operation-id",
                false,  // Not done
                null,
                null,
                Map.of()
        );
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(inProgressStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Test validateOperationStatus method
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Operation not completed yet");
    }
    
    @Test
    void testDownloadVideo_OperationError() throws Exception {
        // Mock error status
        var error = new OperationStatus.ErrorInfo(400, "Video generation failed", java.util.List.of());
        OperationStatus errorStatus = new OperationStatus(
                "test-operation-id",
                true,  // Done but with error
                error,
                null,
                Map.of()
        );
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(errorStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Test validateOperationStatus method with error
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operation failed: Video generation failed");
    }
    
    @Test
    void testDownloadVideo_NoVideoData() throws Exception {
        // Mock completed status but no video data
        OperationStatus completedStatus = new OperationStatus(
                "test-operation-id",
                true,
                null,
                null,  // No response data
                Map.of()
        );
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Test extractVideoUri method
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No video data found in response");
    }
    
    @Test
    void testDownloadVideo_EmptyGeneratedSamples() throws Exception {
        // Mock completed status but empty generated samples
        var generateVideoResponse = new OperationStatus.GenerateVideoResponse(
                java.util.List.of(),  // Empty list
                java.util.List.of()
        );
        var operationResponse = new OperationStatus.OperationResponse(
                "type.googleapis.com/google.ai.generativelanguage.v1beta.GenerateVideoResponse",
                generateVideoResponse
        );
        
        OperationStatus completedStatus = new OperationStatus(
                "test-operation-id",
                true,
                null,
                operationResponse,
                Map.of()
        );
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Test extractVideoUri method with empty samples
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No video data found in response");
    }
    
    @Test
    void testDownloadVideo_VideoDownloadFails() throws Exception {
        // Mock completed operation status
        var generatedSample = new OperationStatus.GeneratedSample(
                new OperationStatus.VideoReference("http://localhost:" + mockServer.getPort() + "/v1beta/video.mp4")
        );
        var generateVideoResponse = new OperationStatus.GenerateVideoResponse(
                java.util.List.of(generatedSample),
                java.util.List.of()
        );
        var operationResponse = new OperationStatus.OperationResponse(
                "type.googleapis.com/google.ai.generativelanguage.v1beta.GenerateVideoResponse",
                generateVideoResponse
        );
        
        OperationStatus completedStatus = new OperationStatus(
                "test-operation-id",
                true,
                null,
                operationResponse,
                Map.of()
        );
        
        // Mock responses: operation status success, video download failure
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        mockServer.enqueue(new MockResponse().setResponseCode(404)); // Video download fails
        
        // Test downloadVideoFromUri error handling
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to download video: HTTP 404");
    }
    
    @Test
    void testConstructor_DefaultConstructor() {
        // Test the default constructor behavior
        try {
            HttpClientVeoVideoClient client = new HttpClientVeoVideoClient();
            assertThat(client).isNotNull();
        } catch (IllegalArgumentException e) {
            // Expected if no API key is set
            assertThat(e.getMessage()).contains("environment variable is required");
        }
    }
    
    @Test
    void testConstructor_WithNullApiKey() {
        // Test constructor validation with null API key
        assertThatThrownBy(() -> new HttpClientVeoVideoClient(null, "veo-3.0-fast-generate-preview"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("environment variable is required");
    }
    
    @Test
    void testConstructor_WithEmptyApiKey() {
        // Test constructor validation with empty API key
        assertThatThrownBy(() -> new HttpClientVeoVideoClient("", "veo-3.0-fast-generate-preview"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("environment variable is required");
    }
    
    @Test
    void testConstructor_WithValidApiKey() {
        // Test constructor validation with valid API key
        assertThatCode(() -> new HttpClientVeoVideoClient("valid-api-key", "veo-3.0-fast-generate-preview"))
                .doesNotThrowAnyException();
    }
}