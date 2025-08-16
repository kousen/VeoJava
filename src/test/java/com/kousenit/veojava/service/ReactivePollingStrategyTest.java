package com.kousenit.veojava.service;

import com.kousenit.veojava.client.ReactiveVeoVideoClient;
import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for ReactivePollingStrategy business logic.
 * Tests the delegation to ReactiveVeoVideoClient and proper reactive behavior.
 */
class ReactivePollingStrategyTest {

    private ReactiveVeoVideoClient mockReactiveClient;
    private VeoVideoClient mockClient; // This is passed but not used in current implementation
    private ReactivePollingStrategy strategy;
    private VideoGenerationRequest testRequest;
    private VideoResult testVideoResult;

    @BeforeEach
    void setUp() {
        mockReactiveClient = mock(ReactiveVeoVideoClient.class);
        mockClient = mock(VeoVideoClient.class);
        strategy = new ReactivePollingStrategy(mockReactiveClient);
        
        testRequest = VideoGenerationRequest.of("Test video prompt");
        
        testVideoResult = new VideoResult(
                "dGVzdA==", // base64 for "test"
                "video/mp4",
                "test_video_123.mp4",
                "test video content".getBytes()
        );
    }

    @Test
    void testGenerateVideo_SuccessfulFlow() throws Exception {
        // Given - setup successful reactive flow
        Mono<VideoResult> successfulMono = Mono.just(testVideoResult);
        when(mockReactiveClient.generateVideoReactive(testRequest)).thenReturn(successfulMono);

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);
        VideoResult result = future.get(5, TimeUnit.SECONDS);

        // Then - verify delegation and result
        assertNotNull(result);
        assertEquals("video/mp4", result.mimeType());
        assertEquals("test_video_123.mp4", result.filename());
        assertArrayEquals("test video content".getBytes(), result.videoBytes());
        
        verify(mockReactiveClient).generateVideoReactive(testRequest);
        // mockClient should not be used in current implementation
        verifyNoInteractions(mockClient);
    }

    @Test
    void testGenerateVideo_ReactiveClientFailure() {
        // Given - reactive client fails
        RuntimeException reactiveError = new RuntimeException("Reactive processing failed");
        Mono<VideoResult> failingMono = Mono.error(reactiveError);
        when(mockReactiveClient.generateVideoReactive(testRequest)).thenReturn(failingMono);

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);

        // Then - future should complete exceptionally
        ExecutionException exception = assertThrows(ExecutionException.class, 
                () -> future.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Reactive processing failed", exception.getCause().getMessage());
        assertTrue(future.isCompletedExceptionally());
        
        verify(mockReactiveClient).generateVideoReactive(testRequest);
        verifyNoInteractions(mockClient);
    }

    @Test
    void testGenerateVideo_EmptyMono() {
        // Given - reactive client returns empty
        Mono<VideoResult> emptyMono = Mono.empty();
        when(mockReactiveClient.generateVideoReactive(testRequest)).thenReturn(emptyMono);

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);

        // Then - should complete with null (empty Mono behavior)
        assertDoesNotThrow(() -> {
            VideoResult result = future.get(5, TimeUnit.SECONDS);
            assertNull(result); // Empty Mono converts to null in CompletableFuture
        });
        
        verify(mockReactiveClient).generateVideoReactive(testRequest);
        verifyNoInteractions(mockClient);
    }

    @Test
    void testGenerateVideo_MultipleConcurrentRequests() throws Exception {
        // Given - multiple requests with different results
        VideoGenerationRequest request1 = VideoGenerationRequest.of("Prompt 1");
        VideoGenerationRequest request2 = VideoGenerationRequest.of("Prompt 2");
        
        VideoResult result1 = new VideoResult("data1", "video/mp4", "video1.mp4", "video1".getBytes());
        VideoResult result2 = new VideoResult("data2", "video/mp4", "video2.mp4", "video2".getBytes());
        
        when(mockReactiveClient.generateVideoReactive(request1)).thenReturn(Mono.just(result1));
        when(mockReactiveClient.generateVideoReactive(request2)).thenReturn(Mono.just(result2));

        // When - submit multiple requests concurrently
        CompletableFuture<VideoResult> future1 = strategy.generateVideo(mockClient, request1);
        CompletableFuture<VideoResult> future2 = strategy.generateVideo(mockClient, request2);

        // Then - both should complete successfully
        VideoResult actualResult1 = future1.get(5, TimeUnit.SECONDS);
        VideoResult actualResult2 = future2.get(5, TimeUnit.SECONDS);
        
        assertEquals("video1.mp4", actualResult1.filename());
        assertEquals("video2.mp4", actualResult2.filename());
        
        verify(mockReactiveClient).generateVideoReactive(request1);
        verify(mockReactiveClient).generateVideoReactive(request2);
        verifyNoInteractions(mockClient);
    }

    @Test
    void testGetStrategyName() {
        assertEquals("Reactive", strategy.getStrategyName());
    }

    @Test
    void testConstructor_WithNullClient() {
        // Given - null reactive client
        ReactivePollingStrategy strategyWithNull = new ReactivePollingStrategy(null);
        
        // When/Then - should be created but fail when used
        assertEquals("Reactive", strategyWithNull.getStrategyName());
        
        // Using it should fail
        assertThrows(NullPointerException.class, () -> {
            strategyWithNull.generateVideo(mockClient, testRequest);
        });
    }

    @Test
    void testDelegationPattern() throws Exception {
        // This test verifies that the strategy properly delegates to the reactive client
        // and converts the Mono to CompletableFuture correctly
        
        // Given - a delayed reactive response to test async behavior
        Mono<VideoResult> delayedMono = Mono.just(testVideoResult)
                .delayElement(java.time.Duration.ofMillis(100)); // Small delay
        when(mockReactiveClient.generateVideoReactive(testRequest)).thenReturn(delayedMono);

        // When - call generateVideo
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);
        
        // Verify it's not completed immediately (due to delay)
        assertFalse(future.isDone());
        
        // Then - should complete after the delay
        VideoResult result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(testVideoResult.filename(), result.filename());
        
        verify(mockReactiveClient).generateVideoReactive(testRequest);
    }

    @Test
    void testReactiveClientInteraction() {
        // This test specifically verifies the interaction with ReactiveVeoVideoClient
        
        // Given
        when(mockReactiveClient.generateVideoReactive(testRequest))
                .thenReturn(Mono.just(testVideoResult));

        // When
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);

        // Then - verify the exact method called on reactive client
        verify(mockReactiveClient, times(1)).generateVideoReactive(testRequest);
        verify(mockReactiveClient, times(1)).generateVideoReactive(any(VideoGenerationRequest.class));
        
        // Verify no other methods called
        verifyNoMoreInteractions(mockReactiveClient);
        verifyNoInteractions(mockClient);
        
        assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
    }
}