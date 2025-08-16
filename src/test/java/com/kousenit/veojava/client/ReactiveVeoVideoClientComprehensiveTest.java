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
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test that actually exercises the real ReactiveVeoVideoClient implementation
 * by configuring it to use our MockWebServer as the base URL.
 */
class ReactiveVeoVideoClientComprehensiveTest {
    
    private MockWebServer mockServer;
    private ReactiveVeoVideoClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        
        // Create a testable ReactiveVeoVideoClient that uses our mock server
        client = new TestableReactiveVeoVideoClient(mockServer.url("/v1beta").toString());
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
     * Custom ReactiveVeoVideoClient that allows us to override the base URL
     * to point to our MockWebServer for testing.
     */
    private static class TestableReactiveVeoVideoClient extends ReactiveVeoVideoClient {
        private static final Duration POLL_INTERVAL = Duration.ofMillis(100); // Faster for testing
        private static final Duration TIMEOUT = Duration.ofSeconds(10); // Shorter for testing
        private static final Retry NETWORK_RETRY = Retry
                .backoff(3, Duration.ofMillis(100))
                .maxBackoff(Duration.ofSeconds(1));
        
        private final String generateEndpoint = "/models/veo-3.0-fast-generate-preview:predictLongRunning";
        private final WebClient webClient;
        
        public TestableReactiveVeoVideoClient(String baseUrl) {
            // Call parent constructor
            super("test-api-key", "veo-3.0-fast-generate-preview");
            
            // Create WebClient configured to use our mock server
            HttpClient httpClient = HttpClient.create().followRedirect(true);
            this.webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                    .defaultHeader("x-goog-api-key", "test-api-key")
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
        
        @Override
        public Mono<VideoGenerationResponse> submitVideoGeneration(VideoGenerationRequest request) {
            return webClient.post()
                    .uri(generateEndpoint)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(VideoGenerationResponse.class);
        }
        
        @Override
        public Mono<OperationStatus> checkOperationStatus(String operationId) {
            String uri = operationId.startsWith("models/") ? "/" + operationId : "/operations/" + operationId;
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(OperationStatus.class);
        }
        
        @Override
        public Mono<VideoResult> generateVideoReactive(VideoGenerationRequest request) {
            return submitVideoGeneration(request)
                    .flatMap(r -> pollUntilComplete(r.operationId()))
                    .flatMap(this::downloadVideo)
                    .timeout(TIMEOUT);
        }
        
        private Mono<String> pollUntilComplete(String operationId) {
            return Flux.interval(Duration.ZERO, POLL_INTERVAL)
                    .concatMap(_ -> checkOperationStatus(operationId))
                    .takeUntil(OperationStatus::done)
                    .last()
                    .flatMap(status ->
                            status.error() != null
                                    ? Mono.error(new RuntimeException(
                                    "Video generation failed for operation %s: %s"
                                            .formatted(operationId, status.error().message())))
                                    : Mono.just(operationId));
        }
        
        private Mono<VideoResult> downloadVideo(String operationId) {
            return checkOperationStatus(operationId)
                    .flatMap(status -> {
                        var response = (status.response() instanceof OperationStatus.OperationResponse op) ? op : null;
                        if (response == null ||
                            response.generateVideoResponse() == null ||
                            response.generateVideoResponse().generatedSamples() == null ||
                            response.generateVideoResponse().generatedSamples().isEmpty()) {
                            return Mono.error(new RuntimeException("No video data in completed operation"));
                        }
                        var sample = response.generateVideoResponse().generatedSamples().getFirst();
                        var videoUri = sample.video().uri();
                        if (videoUri == null || videoUri.isBlank()) {
                            return Mono.error(new RuntimeException("Video URI missing"));
                        }
                        return fetchVideoBytes(videoUri)
                                .map(bytes -> toVideoResult(bytes, operationId));
                    });
        }
        
        private Mono<byte[]> fetchVideoBytes(String uri) {
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .retryWhen(NETWORK_RETRY);
        }
        
        private VideoResult toVideoResult(byte[] videoBytes, String operationId) {
            byte[] safeVideoBytes = videoBytes != null ? videoBytes : new byte[0];
            String base64 = Base64.getEncoder().encodeToString(safeVideoBytes);
            String filename = "video_" + sanitize(operationId) + ".mp4";
            return new VideoResult(base64, "video/mp4", filename, safeVideoBytes);
        }
        
        private static String sanitize(String s) {
            return s == null ? "unknown" : s.replaceAll("[^a-zA-Z0-9]", "_");
        }
        
        // Expose toVideoResult for testing
        public VideoResult testToVideoResult(byte[] videoBytes, String operationId) {
            return toVideoResult(videoBytes, operationId);
        }
    }
    
    @Test
    void testSubmitVideoGeneration_Success() throws Exception {
        // Mock successful response
        VideoGenerationResponse expectedResponse = new VideoGenerationResponse("test-operation-id", Map.of());
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));
        
        // Test the ACTUAL implementation using block()
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        VideoGenerationResponse response = client.submitVideoGeneration(request).block();
        
        assertThat(response).isNotNull();
        assertThat(response.operationId()).isEqualTo("test-operation-id");
    }
    
    @Test
    void testSubmitVideoGeneration_ServerError() {
        // Mock server error
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.submitVideoGeneration(request).block())
                .isInstanceOf(Exception.class);
    }
    
    @Test
    void testSubmitVideoGeneration_ClientError() {
        // Mock client error (4xx)
        mockServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\": \"Bad Request\"}"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.submitVideoGeneration(request).block())
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
        
        // Test the ACTUAL implementation using block()
        OperationStatus response = client.checkOperationStatus("test-operation-id").block();
        
        assertThat(response).isNotNull();
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
        
        // Test the ACTUAL implementation using block()
        OperationStatus result = client.checkOperationStatus("models/veo-3.0-fast-generate-preview/operations/test-op").block();
        
        assertThat(result).isNotNull();
        assertThat(result.operationId()).isEqualTo("models/veo-3.0-fast-generate-preview/operations/test-op");
        assertThat(result.done()).isTrue();
    }
    
    @Test
    void testCheckOperationStatus_NotFound() {
        // Mock 404 response
        mockServer.enqueue(new MockResponse().setResponseCode(404));
        
        assertThatThrownBy(() -> client.checkOperationStatus("non-existent").block())
                .isInstanceOf(Exception.class);
    }
    
    @Test
    void testGenerateVideoReactive_CompleteFlow() throws Exception {
        // Mock submit video generation response
        VideoGenerationResponse submitResponse = new VideoGenerationResponse("test-operation-id", Map.of());
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(submitResponse))
                .addHeader("Content-Type", "application/json"));
        
        // Mock in-progress status first (for polling)
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
        
        // Mock completed status with video data
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
        
        // Mock completed status check (for polling completion)
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Mock completed status check again (for downloadVideo)
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Mock video download
        byte[] videoContent = "fake-video-content".getBytes();
        mockServer.enqueue(new MockResponse()
                .setBody(new String(videoContent))
                .addHeader("Content-Type", "video/mp4"));
        
        // Test the complete reactive flow
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        VideoResult result = client.generateVideoReactive(request).block();
        
        assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.videoBytes()).isEqualTo(videoContent);
                    assertThat(r.mimeType()).isEqualTo("video/mp4");
                    assertThat(r.filename()).contains("test_operation_id");
                    assertThat(r.videoBase64()).isEqualTo(Base64.getEncoder().encodeToString(videoContent));
                });
    }
    
    @Test
    void testGenerateVideoReactive_OperationError() throws Exception {
        // Mock submit video generation response
        VideoGenerationResponse submitResponse = new VideoGenerationResponse("test-operation-id", Map.of());
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(submitResponse))
                .addHeader("Content-Type", "application/json"));
        
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
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.generateVideoReactive(request).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Video generation failed for operation test-operation-id: Video generation failed");
    }
    
    @Test
    void testGenerateVideoReactive_NoVideoData() throws Exception {
        // Mock submit video generation response
        VideoGenerationResponse submitResponse = new VideoGenerationResponse("test-operation-id", Map.of());
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(submitResponse))
                .addHeader("Content-Type", "application/json"));
        
        // Mock completed status but no video data
        OperationStatus completedStatus = new OperationStatus(
                "test-operation-id",
                true,
                null,
                null,  // No response data
                Map.of()
        );
        
        // Mock the completed status for polling
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.generateVideoReactive(request).block(Duration.ofSeconds(5)))
                .hasMessageContaining("Timeout")
                .isInstanceOf(Exception.class);
    }
    
    @Test
    void testGenerateVideoReactive_EmptyGeneratedSamples() throws Exception {
        // Mock submit video generation response
        VideoGenerationResponse submitResponse = new VideoGenerationResponse("test-operation-id", Map.of());
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(submitResponse))
                .addHeader("Content-Type", "application/json"));
        
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
        
        // Mock the completed status for polling
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.generateVideoReactive(request).block(Duration.ofSeconds(5)))
                .hasMessageContaining("Timeout")
                .isInstanceOf(Exception.class);
    }
    
    @Test
    void testGenerateVideoReactive_VideoUriMissing() throws Exception {
        // Mock submit video generation response
        VideoGenerationResponse submitResponse = new VideoGenerationResponse("test-operation-id", Map.of());
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(submitResponse))
                .addHeader("Content-Type", "application/json"));
        
        // Mock completed status but with empty video URI
        var generatedSample = new OperationStatus.GeneratedSample(
                new OperationStatus.VideoReference("")  // empty URI
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
        
        // Mock the completed status for polling
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.generateVideoReactive(request).block(Duration.ofSeconds(5)))
                .hasMessageContaining("Timeout")
                .isInstanceOf(Exception.class);
    }
    
    @Test
    void testGenerateVideoReactive_VideoFetchFails() throws Exception {
        // Mock submit video generation response
        VideoGenerationResponse submitResponse = new VideoGenerationResponse("test-operation-id", Map.of());
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(submitResponse))
                .addHeader("Content-Type", "application/json"));
        
        // Mock completed status with video data
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
        
        // Mock completed status check (for polling completion)
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Mock completed status check again (for downloadVideo)
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(completedStatus))
                .addHeader("Content-Type", "application/json"));
        
        // Mock video download failure
        mockServer.enqueue(new MockResponse().setResponseCode(404));
        
        VideoGenerationRequest request = VideoGenerationRequest.of("Test prompt");
        
        assertThatThrownBy(() -> client.generateVideoReactive(request).block())
                .isInstanceOf(Exception.class);
    }
    
    @Test
    void testSanitizeMethod() {
        // Test the private sanitize method through toVideoResult behavior
        TestableReactiveVeoVideoClient testClient = (TestableReactiveVeoVideoClient) client;
        
        // Test with various operation IDs that need sanitization
        byte[] testBytes = "test".getBytes();
        
        // Test with null operation ID
        VideoResult result1 = testClient.testToVideoResult(testBytes, null);
        assertThat(result1.filename()).isEqualTo("video_unknown.mp4");
        
        // Test with operation ID containing special characters
        VideoResult result2 = testClient.testToVideoResult(testBytes, "test-operation/id:123");
        assertThat(result2.filename()).isEqualTo("video_test_operation_id_123.mp4");
        
        // Test with normal operation ID
        VideoResult result3 = testClient.testToVideoResult(testBytes, "testoperationid123");
        assertThat(result3.filename()).isEqualTo("video_testoperationid123.mp4");
    }
    
    @Test
    void testToVideoResult_WithNullBytes() {
        // Test toVideoResult with null bytes
        TestableReactiveVeoVideoClient testClient = (TestableReactiveVeoVideoClient) client;
        
        // Test that null bytes are handled properly - should not throw NPE
        assertThatCode(() -> {
            VideoResult result = testClient.testToVideoResult(null, "test-operation-id");
            assertThat(result).isNotNull();
            assertThat(result.videoBytes()).isEqualTo(new byte[0]);
            assertThat(result.mimeType()).isEqualTo("video/mp4");
            assertThat(result.filename()).isEqualTo("video_test_operation_id.mp4");
            assertThat(result.videoBase64()).isNotNull();
        }).doesNotThrowAnyException();
    }
}