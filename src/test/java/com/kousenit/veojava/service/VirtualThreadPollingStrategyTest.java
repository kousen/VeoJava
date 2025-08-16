package com.kousenit.veojava.service;

import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for VirtualThreadPollingStrategy business logic.
 * Tests the blocking polling behavior with virtual threads, error handling, and completion scenarios.
 */
class VirtualThreadPollingStrategyTest {

    private VeoVideoClient mockClient;
    private VirtualThreadPollingStrategy strategy;
    private VideoGenerationRequest testRequest;
    private VideoGenerationResponse testResponse;
    private VideoResult testVideoResult;
    private OperationStatus inProgressStatus;
    private OperationStatus completedStatus;
    private OperationStatus errorStatus;

    @BeforeEach
    void setUp() {
        mockClient = mock(VeoVideoClient.class);
        strategy = new VirtualThreadPollingStrategy();
        
        testRequest = VideoGenerationRequest.of("Test video prompt");
        
        testResponse = new VideoGenerationResponse(
                "models/veo-3.0-fast-generate-preview/operations/test-operation-123",
                Map.of("status", "submitted")
        );
        
        testVideoResult = new VideoResult(
                "dGVzdA==", // base64 for "test"
                "video/mp4",
                "test_video_123.mp4",
                "test video content".getBytes()
        );
        
        // Create in-progress status
        inProgressStatus = new OperationStatus(
                testResponse.operationId(),
                false, // not done
                null,
                null,
                Map.of()
        );
        
        // Create completed status with proper response structure
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
        
        completedStatus = new OperationStatus(
                testResponse.operationId(),
                true, // done
                null,
                operationResponse,
                Map.of()
        );
        
        // Create error status
        var errorInfo = new OperationStatus.ErrorInfo(400, "Invalid request", List.of());
        errorStatus = new OperationStatus(
                testResponse.operationId(),
                true, // done but with error
                errorInfo,
                null,
                Map.of()
        );
    }
    
    @AfterEach
    void tearDown() {
        strategy.shutdown();
    }

    @Test
    void testGenerateVideo_SuccessfulFlow() throws Exception {
        // Given - setup successful flow
        when(mockClient.submitVideoGeneration(testRequest)).thenReturn(testResponse);
        when(mockClient.checkOperationStatus(testResponse.operationId())).thenReturn(completedStatus);
        when(mockClient.downloadVideo(testResponse.operationId())).thenReturn(testVideoResult);

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);
        VideoResult result = future.get(10, TimeUnit.SECONDS);

        // Then - verify complete flow
        assertNotNull(result);
        assertEquals("video/mp4", result.mimeType());
        assertEquals("test_video_123.mp4", result.filename());
        assertArrayEquals("test video content".getBytes(), result.videoBytes());
        
        verify(mockClient).submitVideoGeneration(testRequest);
        verify(mockClient).checkOperationStatus(testResponse.operationId());
        verify(mockClient).downloadVideo(testResponse.operationId());
    }

    @Test
    void testGenerateVideo_SubmissionFailure() {
        // Given - submission fails
        RuntimeException submissionError = new RuntimeException("API submission failed");
        when(mockClient.submitVideoGeneration(testRequest)).thenThrow(submissionError);

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);

        // Then - future should complete exceptionally
        ExecutionException exception = assertThrows(ExecutionException.class, 
                () -> future.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("API submission failed", exception.getCause().getMessage());
        assertTrue(future.isCompletedExceptionally());
        
        verify(mockClient).submitVideoGeneration(testRequest);
        verify(mockClient, never()).checkOperationStatus(any());
        verify(mockClient, never()).downloadVideo(any());
    }

    @Test
    void testGenerateVideo_PollingWithDelayedCompletion() throws Exception {
        // Given - operation completes after a few polling attempts
        AtomicInteger callCount = new AtomicInteger(0);
        
        when(mockClient.submitVideoGeneration(testRequest)).thenReturn(testResponse);
        when(mockClient.checkOperationStatus(testResponse.operationId())).thenAnswer(invocation -> {
            int count = callCount.incrementAndGet();
            if (count < 3) {
                return inProgressStatus; // Still in progress
            } else {
                return completedStatus; // Finally complete
            }
        });
        when(mockClient.downloadVideo(testResponse.operationId())).thenReturn(testVideoResult);

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);
        VideoResult result = future.get(20, TimeUnit.SECONDS); // Allow more time for polling delays

        // Then - should complete successfully after multiple polls
        assertNotNull(result);
        assertEquals("test_video_123.mp4", result.filename());
        
        verify(mockClient).submitVideoGeneration(testRequest);
        verify(mockClient, atLeast(3)).checkOperationStatus(testResponse.operationId());
        verify(mockClient).downloadVideo(testResponse.operationId());
    }

    @Test
    void testGenerateVideo_OperationError() {
        // Given - operation completes with error
        when(mockClient.submitVideoGeneration(testRequest)).thenReturn(testResponse);
        when(mockClient.checkOperationStatus(testResponse.operationId())).thenReturn(errorStatus);

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);

        // Then - should complete exceptionally with operation error
        ExecutionException exception = assertThrows(ExecutionException.class, 
                () -> future.get(10, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof RuntimeException);
        
        assertTrue(exception.getCause().getMessage().contains("Video generation failed"));
        assertTrue(exception.getCause().getMessage().contains("Invalid request"));
        
        verify(mockClient).submitVideoGeneration(testRequest);
        verify(mockClient).checkOperationStatus(testResponse.operationId());
        verify(mockClient, never()).downloadVideo(any());
    }

    @Test
    void testGenerateVideo_PollingException() {
        // Given - polling throws exception
        when(mockClient.submitVideoGeneration(testRequest)).thenReturn(testResponse);
        when(mockClient.checkOperationStatus(testResponse.operationId()))
                .thenThrow(new RuntimeException("Network error during polling"));

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);

        // Then - should complete exceptionally
        ExecutionException exception = assertThrows(ExecutionException.class, 
                () -> future.get(10, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof RuntimeException);
        
        assertEquals("Network error during polling", exception.getCause().getMessage());
        
        verify(mockClient).submitVideoGeneration(testRequest);
        verify(mockClient).checkOperationStatus(testResponse.operationId());
        verify(mockClient, never()).downloadVideo(any());
    }

    @Test
    void testGenerateVideo_DownloadFailure() {
        // Given - download fails after successful polling
        when(mockClient.submitVideoGeneration(testRequest)).thenReturn(testResponse);
        when(mockClient.checkOperationStatus(testResponse.operationId())).thenReturn(completedStatus);
        when(mockClient.downloadVideo(testResponse.operationId()))
                .thenThrow(new RuntimeException("Download failed"));

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);

        // Then - should complete exceptionally
        ExecutionException exception = assertThrows(ExecutionException.class, 
                () -> future.get(10, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof RuntimeException);
        
        assertEquals("Download failed", exception.getCause().getMessage());
        
        verify(mockClient).submitVideoGeneration(testRequest);
        verify(mockClient).checkOperationStatus(testResponse.operationId());
        verify(mockClient).downloadVideo(testResponse.operationId());
    }

    @Test
    void testGenerateVideo_InterruptedException() throws Exception {
        // Given - setup a scenario that could be interrupted
        when(mockClient.submitVideoGeneration(testRequest)).thenReturn(testResponse);
        when(mockClient.checkOperationStatus(testResponse.operationId())).thenReturn(inProgressStatus);

        // When - call generateVideo and then interrupt
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);
        
        // Allow some time for submission to happen
        Thread.sleep(50);
        
        // Interrupt the future
        future.cancel(true);

        // Then - should handle cancellation gracefully
        assertTrue(future.isCancelled());
        
        verify(mockClient, atLeastOnce()).submitVideoGeneration(testRequest);
    }

    @Test
    void testGetStrategyName() {
        assertEquals("VirtualThread", strategy.getStrategyName());
    }

    @Test
    void testShutdown_GracefulShutdown() throws Exception {
        // Given - strategy is running
        when(mockClient.submitVideoGeneration(testRequest)).thenReturn(testResponse);
        when(mockClient.checkOperationStatus(testResponse.operationId())).thenReturn(inProgressStatus);
        
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);
        
        // When - shutdown is called
        strategy.shutdown();
        
        // Then - should shutdown gracefully
        // The future might be cancelled or continue running briefly
        // Main thing is shutdown() doesn't throw exceptions
        
        future.cancel(true); // Clean up
    }

    @Test
    void testMultipleConcurrentRequests() throws Exception {
        // Given - multiple requests (virtual threads handle high concurrency well)
        VideoGenerationRequest request1 = VideoGenerationRequest.of("Prompt 1");
        VideoGenerationRequest request2 = VideoGenerationRequest.of("Prompt 2");
        VideoGenerationRequest request3 = VideoGenerationRequest.of("Prompt 3");
        
        VideoGenerationResponse response1 = new VideoGenerationResponse("op1", Map.of());
        VideoGenerationResponse response2 = new VideoGenerationResponse("op2", Map.of());
        VideoGenerationResponse response3 = new VideoGenerationResponse("op3", Map.of());
        
        OperationStatus completed1 = new OperationStatus("op1", true, null, 
                completedStatus.response(), Map.of());
        OperationStatus completed2 = new OperationStatus("op2", true, null, 
                completedStatus.response(), Map.of());
        OperationStatus completed3 = new OperationStatus("op3", true, null, 
                completedStatus.response(), Map.of());
        
        VideoResult result1 = new VideoResult("data1", "video/mp4", "video1.mp4", "video1".getBytes());
        VideoResult result2 = new VideoResult("data2", "video/mp4", "video2.mp4", "video2".getBytes());
        VideoResult result3 = new VideoResult("data3", "video/mp4", "video3.mp4", "video3".getBytes());

        // Setup mocks for all requests
        when(mockClient.submitVideoGeneration(request1)).thenReturn(response1);
        when(mockClient.submitVideoGeneration(request2)).thenReturn(response2);
        when(mockClient.submitVideoGeneration(request3)).thenReturn(response3);
        when(mockClient.checkOperationStatus("op1")).thenReturn(completed1);
        when(mockClient.checkOperationStatus("op2")).thenReturn(completed2);
        when(mockClient.checkOperationStatus("op3")).thenReturn(completed3);
        when(mockClient.downloadVideo("op1")).thenReturn(result1);
        when(mockClient.downloadVideo("op2")).thenReturn(result2);
        when(mockClient.downloadVideo("op3")).thenReturn(result3);

        // When - submit multiple requests concurrently (virtual threads scale well)
        CompletableFuture<VideoResult> future1 = strategy.generateVideo(mockClient, request1);
        CompletableFuture<VideoResult> future2 = strategy.generateVideo(mockClient, request2);
        CompletableFuture<VideoResult> future3 = strategy.generateVideo(mockClient, request3);

        // Then - all should complete successfully
        VideoResult actualResult1 = future1.get(10, TimeUnit.SECONDS);
        VideoResult actualResult2 = future2.get(10, TimeUnit.SECONDS);
        VideoResult actualResult3 = future3.get(10, TimeUnit.SECONDS);
        
        assertEquals("video1.mp4", actualResult1.filename());
        assertEquals("video2.mp4", actualResult2.filename());
        assertEquals("video3.mp4", actualResult3.filename());
        
        verify(mockClient).submitVideoGeneration(request1);
        verify(mockClient).submitVideoGeneration(request2);
        verify(mockClient).submitVideoGeneration(request3);
    }

    @Test
    void testVirtualThreadExecutorUsage() throws Exception {
        // Given - a request that completes immediately
        when(mockClient.submitVideoGeneration(testRequest)).thenReturn(testResponse);
        when(mockClient.checkOperationStatus(testResponse.operationId())).thenReturn(completedStatus);
        when(mockClient.downloadVideo(testResponse.operationId())).thenReturn(testVideoResult);

        // When - call generateVideo multiple times to test executor usage
        CompletableFuture<VideoResult> future1 = strategy.generateVideo(mockClient, testRequest);
        CompletableFuture<VideoResult> future2 = strategy.generateVideo(mockClient, testRequest);

        // Then - both should complete using the virtual thread executor
        VideoResult result1 = future1.get(10, TimeUnit.SECONDS);
        VideoResult result2 = future2.get(10, TimeUnit.SECONDS);
        
        assertNotNull(result1);
        assertNotNull(result2);
        
        // Virtual threads should handle this efficiently
        verify(mockClient, times(2)).submitVideoGeneration(testRequest);
        verify(mockClient, times(2)).checkOperationStatus(testResponse.operationId());
        verify(mockClient, times(2)).downloadVideo(testResponse.operationId());
    }
}