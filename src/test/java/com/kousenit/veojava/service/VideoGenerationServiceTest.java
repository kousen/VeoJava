package com.kousenit.veojava.service;

import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SpringBootTest
class VideoGenerationServiceTest {

    @MockitoBean
    @Qualifier("restClientVeoVideoClient")
    private VeoVideoClient mockRestClient;

    @MockitoBean
    @Qualifier("httpClientVeoVideoClient")
    private VeoVideoClient mockHttpClient;

    @MockitoBean
    private SelfSchedulingPollingStrategy mockSelfSchedulingStrategy;

    @MockitoBean
    private FixedRatePollingStrategy mockFixedRateStrategy;

    @MockitoBean
    private ReactivePollingStrategy mockReactiveStrategy;

    @MockitoBean
    private VirtualThreadPollingStrategy mockVirtualThreadStrategy;

    @Autowired
    private VideoGenerationService videoService;

    @TempDir
    Path tempDir;

    private VideoResult mockVideoResult;
    private VideoGenerationResponse mockResponse;
    private OperationStatus mockCompletedStatus;

    @BeforeEach
    void setUp() {
        // Create mock video data
        byte[] videoBytes = "mock video content".getBytes();
        mockVideoResult = new VideoResult(
                "dGVzdA==", // base64 for "test"
                "video/mp4",
                "test_video_123.mp4",
                videoBytes
        );

        // Create mock response
        mockResponse = new VideoGenerationResponse(
                "operations/test-operation-123",
                Map.of("status", "submitted")
        );

        // Create mock completed status
        var videoRef = new OperationStatus.VideoReference("https://example.com/video.mp4");
        var sample = new OperationStatus.GeneratedSample(videoRef);
        var videoResponse = new OperationStatus.GenerateVideoResponse(
                List.of(sample),
                List.of()
        );
        var operationResponse = new OperationStatus.OperationResponse(
                "type.googleapis.com/google.ai.generativelanguage.v1beta.PredictLongRunningResponse",
                videoResponse
        );
        
        mockCompletedStatus = new OperationStatus(
                "operations/test-operation-123",
                true,
                null,
                operationResponse,
                Map.of()
        );
    }

    @Test
    void testGenerateVideoWithRestClient() throws Exception {
        // Given
        String prompt = "Test video prompt";
        given(mockRestClient.generateVideoAsync(any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // When
        CompletableFuture<VideoResult> future = videoService.generateVideoWithRestClient(prompt);
        VideoResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertEquals("video/mp4", result.mimeType());
        assertEquals("test_video_123.mp4", result.filename());
        assertArrayEquals("mock video content".getBytes(), result.videoBytes());
        
        verify(mockRestClient).generateVideoAsync(any(VideoGenerationRequest.class));
    }

    @Test
    void testGenerateVideoWithHttpClient() throws Exception {
        // Given
        String prompt = "Test video prompt";
        given(mockHttpClient.generateVideoAsync(any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // When
        CompletableFuture<VideoResult> future = videoService.generateVideoWithHttpClient(prompt);
        VideoResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertEquals("video/mp4", result.mimeType());
        assertEquals("test_video_123.mp4", result.filename());
        
        verify(mockHttpClient).generateVideoAsync(any(VideoGenerationRequest.class));
    }

    @Test
    void testGenerateVideoWithSelfScheduling() throws Exception {
        // Given
        String prompt = "Test video prompt";
        given(mockSelfSchedulingStrategy.generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // When
        CompletableFuture<VideoResult> future = videoService.generateVideoWithSelfScheduling(prompt);
        VideoResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertEquals("test_video_123.mp4", result.filename());
        
        verify(mockSelfSchedulingStrategy).generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class));
    }

    @Test
    void testGenerateVideoWithFixedRate() throws Exception {
        // Given
        String prompt = "Test video prompt";
        given(mockFixedRateStrategy.generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // When
        CompletableFuture<VideoResult> future = videoService.generateVideoWithFixedRate(prompt);
        VideoResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertEquals("test_video_123.mp4", result.filename());
        
        verify(mockFixedRateStrategy).generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class));
    }

    @Test
    void testGenerateVideoWithReactive() throws Exception {
        // Given
        String prompt = "Test video prompt";
        given(mockReactiveStrategy.generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // When
        CompletableFuture<VideoResult> future = videoService.generateVideoWithReactive(prompt);
        VideoResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertEquals("test_video_123.mp4", result.filename());
        
        verify(mockReactiveStrategy).generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class));
    }

    @Test
    void testGenerateVideoWithVirtualThreads() throws Exception {
        // Given
        String prompt = "Test video prompt";
        given(mockVirtualThreadStrategy.generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // When
        CompletableFuture<VideoResult> future = videoService.generateVideoWithVirtualThreads(prompt);
        VideoResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertEquals("test_video_123.mp4", result.filename());
        
        verify(mockVirtualThreadStrategy).generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class));
    }

    @Test
    void testSaveVideoToFile() throws IOException {
        // Given
        String outputDirectory = tempDir.toString();

        // When
        String filePath = videoService.saveVideoToFile(mockVideoResult, outputDirectory);

        // Then
        assertNotNull(filePath);
        assertTrue(filePath.endsWith("test_video_123.mp4"));
        
        Path savedFile = Path.of(filePath);
        assertTrue(Files.exists(savedFile));
        
        byte[] savedContent = Files.readAllBytes(savedFile);
        assertArrayEquals("mock video content".getBytes(), savedContent);
    }

    @Test
    void testSaveVideoToFileCreatesDirectory() throws IOException {
        // Given
        Path nonExistentDir = tempDir.resolve("new/directory");
        String outputDirectory = nonExistentDir.toString();

        // When
        String filePath = videoService.saveVideoToFile(mockVideoResult, outputDirectory);

        // Then
        assertTrue(Files.exists(nonExistentDir));
        assertTrue(Files.exists(Path.of(filePath)));
    }

    @Test
    void testGenerateAndSaveVideoWithRestClient() throws Exception {
        // Given
        String prompt = "Test video prompt";
        String strategy = "restclient";
        String outputDirectory = tempDir.toString();
        
        given(mockRestClient.generateVideoAsync(any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // When
        CompletableFuture<String> future = videoService.generateAndSaveVideo(prompt, strategy, outputDirectory);
        String filePath = future.get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(filePath);
        assertTrue(filePath.endsWith("test_video_123.mp4"));
        assertTrue(Files.exists(Path.of(filePath)));
        
        verify(mockRestClient).generateVideoAsync(any(VideoGenerationRequest.class));
    }

    @Test
    void testGenerateAndSaveVideoWithHttpClient() throws Exception {
        // Given
        String prompt = "Test video prompt";
        String strategy = "httpclient";
        String outputDirectory = tempDir.toString();
        
        given(mockHttpClient.generateVideoAsync(any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // When
        CompletableFuture<String> future = videoService.generateAndSaveVideo(prompt, strategy, outputDirectory);
        String filePath = future.get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(filePath);
        assertTrue(filePath.endsWith("test_video_123.mp4"));
        assertTrue(Files.exists(Path.of(filePath)));
        
        verify(mockHttpClient).generateVideoAsync(any(VideoGenerationRequest.class));
    }

    @Test
    void testGenerateAndSaveVideoWithAllPollingStrategies() throws Exception {
        // Given
        String prompt = "Test video prompt";
        String outputDirectory = tempDir.toString();
        
        // Setup mocks for all strategies
        given(mockSelfSchedulingStrategy.generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));
        given(mockFixedRateStrategy.generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));
        given(mockReactiveStrategy.generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));
        given(mockVirtualThreadStrategy.generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // Test all polling strategies
        String[] strategies = {"selfscheduling", "fixedrate", "reactive", "virtualthread"};
        
        for (String strategy : strategies) {
            // When
            CompletableFuture<String> future = videoService.generateAndSaveVideo(prompt, strategy, outputDirectory);
            String filePath = future.get(5, TimeUnit.SECONDS);

            // Then
            assertNotNull(filePath, "File path should not be null for strategy: " + strategy);
            assertTrue(filePath.endsWith("test_video_123.mp4"), 
                    "File path should end with filename for strategy: " + strategy);
            assertTrue(Files.exists(Path.of(filePath)), 
                    "File should exist for strategy: " + strategy);
        }

        // Verify all strategies were called
        verify(mockSelfSchedulingStrategy).generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class));
        verify(mockFixedRateStrategy).generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class));
        verify(mockReactiveStrategy).generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class));
        verify(mockVirtualThreadStrategy).generateVideo(any(VeoVideoClient.class), any(VideoGenerationRequest.class));
    }

    @Test
    void testGenerateAndSaveVideoWithInvalidStrategy() {
        // Given
        String prompt = "Test video prompt";
        String invalidStrategy = "invalid";
        String outputDirectory = tempDir.toString();

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                videoService.generateAndSaveVideo(prompt, invalidStrategy, outputDirectory)
        );
    }

    @Test
    void testGenerateAndSaveVideoHandlesIOException() {
        // Given
        String prompt = "Test video prompt";
        String strategy = "restclient";
        String invalidOutputDirectory = "/invalid/directory/that/cannot/be/created";
        
        given(mockRestClient.generateVideoAsync(any(VideoGenerationRequest.class)))
                .willReturn(CompletableFuture.completedFuture(mockVideoResult));

        // When
        CompletableFuture<String> future = videoService.generateAndSaveVideo(prompt, strategy, invalidOutputDirectory);

        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, () ->
                future.get(5, TimeUnit.SECONDS)
        );
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertTrue(exception.getCause().getMessage().contains("Failed to save video file"));
    }
}