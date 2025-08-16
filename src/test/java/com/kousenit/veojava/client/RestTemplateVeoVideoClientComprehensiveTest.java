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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test that actually exercises the real RestTemplate implementation
 * by configuring it to use our MockWebServer as the base URL.
 */
class RestTemplateVeoVideoClientComprehensiveTest {
    
    private MockWebServer mockServer;
    private RestTemplateVeoVideoClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        
        // Create RestTemplate configured to use our mock server
        String baseUrl = mockServer.url("/v1beta").toString();
        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-goog-api-key", "test-api-key")
                .build();
        
        // Create client with our configured RestTemplate - this will test the ACTUAL implementation
        client = new RestTemplateVeoVideoClient("veo-3.0-fast-generate-preview", restTemplate);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        mockServer.shutdown();
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
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.submitVideoGeneration(request))
                .isInstanceOf(Exception.class);
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
        mockServer.enqueue(new MockResponse().setResponseCode(404));
        
        assertThatThrownBy(() -> client.checkOperationStatus("non-existent"))
                .isInstanceOf(HttpClientErrorException.class);
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
                    assertThat(r.filename()).contains("test_operation_id");
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
                .isInstanceOf(HttpClientErrorException.class);
    }
    
    @Test
    void testDownloadVideo_NullVideoDownload() throws Exception {
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
        
        // Mock responses: operation status success, but video returns null (empty response)
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        mockServer.enqueue(new MockResponse()
                .setBody("")  // Empty body - RestTemplate may return null for empty responses
                .addHeader("Content-Type", "video/mp4"));
        
        // Test downloadVideoFromUri null handling
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to download video from URI");
    }
}