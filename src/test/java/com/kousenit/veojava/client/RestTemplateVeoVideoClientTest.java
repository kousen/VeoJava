package com.kousenit.veojava.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class RestTemplateVeoVideoClientTest {
    
    private MockWebServer mockServer;
    private RestTemplateVeoVideoClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        
        String baseUrl = mockServer.url("/v1beta").toString();
        
        // Create a custom RestTemplate with the mock server base URL
        RestTemplate restTemplate = new RestTemplate();
        
        // Use reflection to override the BASE_URL for testing
        client = new RestTemplateVeoVideoClient() {
            @Override
            public VideoGenerationResponse submitVideoGeneration(VideoGenerationRequest request) {
                String url = baseUrl + "/models/veo-3.0-fast-generate-preview:predictLongRunning";
                return restTemplate.postForObject(url, request, VideoGenerationResponse.class);
            }
            
            @Override
            public OperationStatus checkOperationStatus(String operationId) {
                String uri = operationId.startsWith("models/") ? 
                        "/" + operationId : 
                        "/operations/" + operationId;
                String url = baseUrl + uri;
                return restTemplate.getForObject(url, OperationStatus.class);
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
                
                // Extract video URI and download
                var response = (OperationStatus.OperationResponse) status.response();
                var sample = response.generateVideoResponse().generatedSamples().getFirst();
                var videoUri = sample.video().uri();
                
                // Download video bytes
                byte[] videoBytes = restTemplate.getForObject(videoUri, byte[].class);
                
                if (videoBytes == null) {
                    throw new RuntimeException("Failed to download video from URI: " + videoUri);
                }
                
                var base64Video = java.util.Base64.getEncoder().encodeToString(videoBytes);
                var filename = "video_" + operationId.replaceAll("[^a-zA-Z0-9]", "_") + ".mp4";
                
                return new VideoResult(base64Video, "video/mp4", filename, videoBytes);
            }
        };
    }
    
    @AfterEach
    void tearDown() throws Exception {
        mockServer.shutdown();
    }
    
    @Test
    void testSubmitVideoGeneration() throws Exception {
        // Mock response
        VideoGenerationResponse expectedResponse = new VideoGenerationResponse("test-operation-id", Map.of());
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));
        
        // Test
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        VideoGenerationResponse response = client.submitVideoGeneration(request);
        
        // Verify
        assertThat(response.operationId()).isEqualTo("test-operation-id");
        
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isNotNull().contains("predictLongRunning");
    }
    
    @Test
    void testCheckOperationStatus() throws Exception {
        // Mock response - operation in progress
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
        
        // Test
        OperationStatus response = client.checkOperationStatus("test-operation-id");
        
        // Verify
        assertThat(response.done()).isFalse();
        assertThat(response.operationId()).isEqualTo("test-operation-id");
        
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isNotNull().contains("/operations/test-operation-id");
    }
    
    @Test
    void testDownloadVideoSuccess() throws Exception {
        // Mock operation status response (completed)
        var generatedSample = new OperationStatus.GeneratedSample(
                new OperationStatus.VideoReference("http://localhost:" + mockServer.getPort() + "/video.mp4")
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
        
        // Mock responses: operation status + video download
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Mock video file content
        byte[] videoContent = "fake-video-content".getBytes();
        mockServer.enqueue(new MockResponse()
                .setBody(new String(videoContent))
                .addHeader("Content-Type", "video/mp4"));
        
        // Test
        VideoResult result = client.downloadVideo("test-operation-id");
        
        // Verify
        assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.videoBytes()).isEqualTo(videoContent);
                    assertThat(r.mimeType()).isEqualTo("video/mp4");
                    assertThat(r.filename()).isNotNull().contains("test_operation_id");
                });
        
        // Verify requests
        RecordedRequest statusRequest = mockServer.takeRequest();
        assertThat(statusRequest.getMethod()).isEqualTo("GET");
        
        RecordedRequest videoRequest = mockServer.takeRequest();
        assertThat(videoRequest.getMethod()).isEqualTo("GET");
        assertThat(videoRequest.getPath()).isNotNull().contains("video.mp4");
    }
    
    @Test
    void testDownloadVideoWithRedirect() throws Exception {
        // Mock operation status response (completed)
        var generatedSample = new OperationStatus.GeneratedSample(
                new OperationStatus.VideoReference("http://localhost:" + mockServer.getPort() + "/redirect")
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
        
        // Mock responses: operation status + redirect + final video
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Mock 302 redirect response
        mockServer.enqueue(new MockResponse()
                .setResponseCode(302)
                .addHeader("Location", mockServer.url("/final-video.mp4").toString()));
        
        // Mock final video content
        byte[] videoContent = "actual-video-content".getBytes();
        mockServer.enqueue(new MockResponse()
                .setBody(new String(videoContent))
                .addHeader("Content-Type", "video/mp4"));
        
        // Test - RestTemplate should follow redirects automatically
        VideoResult result = client.downloadVideo("test-operation-id");
        
        // Verify
        assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.videoBytes()).isEqualTo(videoContent);
                    assertThat(r.mimeType()).isEqualTo("video/mp4");
                });
        
        // Verify all requests were made
        assertThat(mockServer.getRequestCount()).isEqualTo(3);
    }
    
    @Test
    void testDownloadVideoOperationNotDone() throws Exception {
        // Mock operation status response (not completed)
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
        
        // Test
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Operation not completed yet");
    }
    
    @Test
    void testDownloadVideoOperationError() throws Exception {
        // Mock operation status response (error)
        var error = new OperationStatus.ErrorInfo(400, "Video generation failed", java.util.List.of());
        OperationStatus errorStatus = new OperationStatus(
                "test-operation-id",
                true,
                error,
                null,
                Map.of()
        );
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(errorStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Test
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operation failed")
                .hasMessageContaining("Video generation failed");
    }
}