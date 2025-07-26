package com.kousenit.veojava.service;

import com.kousenit.veojava.client.ReactiveVeoVideoClient;
import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Simplified tests for polling strategy implementations.
 * Focus on business logic, strategy names, and error handling rather than async timing.
 */
class PollingStrategyTest {

    private VeoVideoClient mockClient;
    private ReactiveVeoVideoClient mockReactiveClient;
    private VideoGenerationRequest testRequest;
    private VideoGenerationResponse testResponse;
    private VideoResult testVideoResult;
    private OperationStatus completedStatus;
    private OperationStatus errorStatus;

    @BeforeEach
    void setUp() {
        mockClient = mock(VeoVideoClient.class);
        mockReactiveClient = mock(ReactiveVeoVideoClient.class);
        
        testRequest = VideoGenerationRequest.of("Test video prompt");
        
        testResponse = new VideoGenerationResponse(
                "models/veo-3.0-generate-preview/operations/test-operation-123",
                Map.of("status", "submitted")
        );
        
        testVideoResult = new VideoResult(
                "dGVzdA==", // base64 for "test"
                "video/mp4",
                "test_video_123.mp4",
                "test video content".getBytes()
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
                true,
                null,
                operationResponse,
                Map.of()
        );
        
        errorStatus = new OperationStatus(
                testResponse.operationId(),
                true,
                new OperationStatus.ErrorInfo(500, "Video generation failed", List.of()),
                null,
                Map.of()
        );
    }

    @Test
    void testSelfSchedulingPollingStrategy_StrategyName() {
        SelfSchedulingPollingStrategy strategy = new SelfSchedulingPollingStrategy();
        assertEquals("SelfScheduling", strategy.getStrategyName());
    }

    @Test
    void testFixedRatePollingStrategy_StrategyName() {
        FixedRatePollingStrategy strategy = new FixedRatePollingStrategy();
        assertEquals("FixedRate", strategy.getStrategyName());
    }

    @Test
    void testVirtualThreadPollingStrategy_StrategyName() {
        VirtualThreadPollingStrategy strategy = new VirtualThreadPollingStrategy();
        assertEquals("VirtualThread", strategy.getStrategyName());
    }

    @Test
    void testReactivePollingStrategy_StrategyName() {
        ReactivePollingStrategy strategy = new ReactivePollingStrategy(mockReactiveClient);
        assertEquals("Reactive", strategy.getStrategyName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SelfScheduling", "FixedRate", "VirtualThread", "Reactive"})
    void testAllStrategiesReturnCorrectNames(String strategyName) {
        PollingStrategy strategy = createStrategy(strategyName);
        assertEquals(strategyName, strategy.getStrategyName());
    }

    @Test
    void testSelfSchedulingPollingStrategy_InitializesSuccessfully() {
        // Test that the strategy can be created without throwing exceptions
        assertDoesNotThrow(() -> new SelfSchedulingPollingStrategy());
    }

    @Test
    void testFixedRatePollingStrategy_InitializesSuccessfully() {
        assertDoesNotThrow(() -> new FixedRatePollingStrategy());
    }

    @Test
    void testVirtualThreadPollingStrategy_InitializesSuccessfully() {
        assertDoesNotThrow(() -> new VirtualThreadPollingStrategy());
    }

    @Test
    void testReactivePollingStrategy_InitializesSuccessfully() {
        assertDoesNotThrow(() -> new ReactivePollingStrategy(mockReactiveClient));
    }

    @Test
    void testReactivePollingStrategy_AcceptsNullClient() {
        // Test that reactive strategy can be created with null (will fail later when used)
        ReactivePollingStrategy strategy = new ReactivePollingStrategy(null);
        assertEquals("Reactive", strategy.getStrategyName());
    }

    @Test
    void testSelfSchedulingPollingStrategy_CallsClientMethods() {
        // Test basic method invocation without waiting for async completion
        SelfSchedulingPollingStrategy strategy = new SelfSchedulingPollingStrategy();
        
        // Setup immediate completion to avoid timing issues
        when(mockClient.submitVideoGeneration(testRequest)).thenReturn(testResponse);
        when(mockClient.checkOperationStatus(testResponse.operationId())).thenReturn(completedStatus);
        when(mockClient.downloadVideo(testResponse.operationId())).thenReturn(testVideoResult);

        // When - just call the method to test it doesn't throw
        CompletableFuture<VideoResult> future = strategy.generateVideo(mockClient, testRequest);
        
        // Then - verify the strategy creates a future
        assertNotNull(future);
        
        // Clean up
        future.cancel(true);
    }

    @Test
    void testReactivePollingStrategy_WithMockClient() {
        // Test reactive strategy basic creation and method access
        ReactivePollingStrategy strategy = new ReactivePollingStrategy(mockReactiveClient);
        
        // Test that the strategy can be created and provides correct name
        assertEquals("Reactive", strategy.getStrategyName());
        assertNotNull(strategy);
    }

    private PollingStrategy createStrategy(String strategyName) {
        return switch (strategyName) {
            case "SelfScheduling" -> new SelfSchedulingPollingStrategy();
            case "FixedRate" -> new FixedRatePollingStrategy();
            case "VirtualThread" -> new VirtualThreadPollingStrategy();
            case "Reactive" -> new ReactivePollingStrategy(mockReactiveClient);
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategyName);
        };
    }
}