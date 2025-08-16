package com.kousenit.veojava.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class RestTemplateVeoVideoClientCoverageTest {
    
    private MockWebServer mockServer;
    private RestTemplateVeoVideoClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        
        String baseUrl = mockServer.url("/v1beta").toString();
        RestTemplate restTemplate = new RestTemplate();
        
        // Create client with mock server URLs
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
        };
    }
    
    @AfterEach
    void tearDown() throws Exception {
        mockServer.shutdown();
    }
    
    @Test
    void testSubmitVideoGeneration_ServerError() {
        // Mock 500 server error
        mockServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.submitVideoGeneration(request))
                .isInstanceOf(HttpServerErrorException.class);
    }
    
    @Test
    void testSubmitVideoGeneration_ClientError() {
        // Mock 400 client error
        mockServer.enqueue(new MockResponse().setResponseCode(400).setBody("Bad Request"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.submitVideoGeneration(request))
                .isInstanceOf(HttpClientErrorException.class);
    }
    
    @Test
    void testSubmitVideoGeneration_MalformedResponse() {
        // Mock malformed JSON response
        mockServer.enqueue(new MockResponse()
                .setBody("invalid-json")
                .addHeader("Content-Type", "application/json"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.submitVideoGeneration(request))
                .isInstanceOf(Exception.class);
    }
    
    @Test
    void testCheckOperationStatus_NotFound() {
        // Mock 404 not found
        mockServer.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));
        
        assertThatThrownBy(() -> client.checkOperationStatus("non-existent-operation"))
                .isInstanceOf(HttpClientErrorException.class);
    }
    
    @Test
    void testCheckOperationStatus_WithFullOperationId() throws Exception {
        // Test with operation ID that starts with "models/"
        OperationStatus status = new OperationStatus(
                "models/veo-3.0-fast-generate-preview/operations/test-op",
                false,
                null,
                null,
                Map.of()
        );
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(status))
                .addHeader("Content-Type", "application/json"));
        
        OperationStatus result = client.checkOperationStatus("models/veo-3.0-fast-generate-preview/operations/test-op");
        
        assertThat(result.operationId()).isEqualTo("models/veo-3.0-fast-generate-preview/operations/test-op");
    }
    
    @Test
    void testDownloadVideo_EmptyVideoResponse() throws Exception {
        // Mock operation status with empty generated samples
        var generateVideoResponse = new OperationStatus.GenerateVideoResponse(
                java.util.List.of(), // Empty samples
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
        
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No video data found in response");
    }
    
    @Test
    void testDownloadVideo_NullVideoUri() throws Exception {
        // Mock operation status with null video URI
        var generatedSample = new OperationStatus.GeneratedSample(
                new OperationStatus.VideoReference(null) // Null URI
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
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class);
    }
    
    @Test
    void testDownloadVideo_EmptyVideoUri() throws Exception {
        // Mock operation status with empty video URI
        var generatedSample = new OperationStatus.GeneratedSample(
                new OperationStatus.VideoReference("") // Empty URI
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
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class);
    }
    
    @Test
    void testDownloadVideo_VideoDownloadFails() throws Exception {
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
        
        // Mock operation status success, then video download failure
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        mockServer.enqueue(new MockResponse().setResponseCode(404)); // Video not found
        
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(HttpClientErrorException.class);
    }
    
    @Test 
    void testDownloadVideo_NullOperationResponse() throws Exception {
        // Mock operation status with null operation response
        OperationStatus completedStatus = new OperationStatus(
                "test-operation-id",
                true,
                null,
                null, // Null response
                Map.of()
        );
        
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No video data found in response");
    }
    
    @Test
    void testDownloadVideo_NullGenerateVideoResponse() throws Exception {
        // Mock operation status with null generateVideoResponse
        var operationResponse = new OperationStatus.OperationResponse(
                "type.googleapis.com/google.ai.generativelanguage.v1beta.GenerateVideoResponse",
                null // Null generateVideoResponse
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
        
        assertThatThrownBy(() -> client.downloadVideo("test-operation-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No video data found in response");
    }
}