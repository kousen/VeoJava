package com.kousenit.veojava;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.service.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Gatherers;

import static org.junit.jupiter.api.Assertions.*;

class ModernJavaFeaturesTest {
    
    @Test
    void testTextBlocksForJsonSerialization() throws Exception {
        var objectMapper = new ObjectMapper();
        
        // Using text blocks for readable JSON
        var expectedJson = """
                {
                  "instances": [
                    {
                      "prompt": "A cat playing with yarn"
                    }
                  ],
                  "parameters": {
                    "aspectRatio": "16:9",
                    "personGeneration": "allow_all"
                  }
                }
                """;
        
        var request = VideoGenerationRequest.of("A cat playing with yarn");
        var actualJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        
        // Parse both to compare structure (ignoring whitespace differences)
        var expectedTree = objectMapper.readTree(expectedJson);
        var actualTree = objectMapper.readTree(actualJson);
        
        assertEquals(expectedTree, actualTree);
    }
    
    @Test
    void testSealedInterfacePatternMatching() {
        // Test with different strategy implementations
        // Note: ReactivePollingStrategy omitted as it requires ReactiveVeoVideoClient dependency
        var strategies = List.of(
                new CompletableFuturePollingStrategy(),
                new ScheduledExecutorPollingStrategy(),
                new VirtualThreadPollingStrategy()
        );
        
        for (PollingStrategy strategy : strategies) {
            var description = getStrategyDescription(strategy);
            assertNotNull(description);
            assertFalse(description.isEmpty());
        }
    }
    
    // Demonstrating pattern matching with sealed interface and unnamed variables (Java 22+)
    private String getStrategyDescription(PollingStrategy strategy) {
        return switch (strategy) {
            case CompletableFuturePollingStrategy _ -> 
                "Uses CompletableFuture with scheduled polling";
            case ScheduledExecutorPollingStrategy _ -> 
                "Uses ScheduledExecutorService for periodic checks";
            case ReactivePollingStrategy _ -> 
                "Uses reactive streams with Flux.interval";
            case VirtualThreadPollingStrategy _ -> 
                "Uses virtual threads for lightweight blocking operations";
        };
    }
    
    @Test
    void testPatternMatchingWithInstanceof() {
        var operationResponse = new OperationStatus.OperationResponse(
                "type.googleapis.com/test",
                List.of(new OperationStatus.PredictionResult(
                        "base64data", "video/mp4", Map.of("duration", "8s")
                ))
        );
        
        var status = new OperationStatus(
                "operations/test-123", 
                true, 
                null, 
                operationResponse, 
                Map.of()
        );
        
        // Using pattern matching with instanceof (Java 16+)
        if (status.response() instanceof OperationStatus.OperationResponse response && 
            !response.predictions().isEmpty()) {
            
            var prediction = response.predictions().getFirst();
            assertEquals("video/mp4", prediction.mimeType());
            assertEquals("base64data", prediction.bytesBase64Encoded());
        } else {
            fail("Expected valid operation response");
        }
    }
    
    @Test
    void testVarUsageInModernCode() {
        // Using var for cleaner code
        var prompt = "A dancing robot";
        var request = VideoGenerationRequest.of(prompt);
        var instances = request.instances();
        var firstInstance = instances.getFirst();
        
        assertEquals(prompt, firstInstance.prompt());
        
        // var with complex types
        var errorInfo = new OperationStatus.ErrorInfo(
                400, "Bad Request", List.of(Map.of("field", "prompt"))
        );
        
        assertEquals(400, errorInfo.code());
        assertEquals("Bad Request", errorInfo.message());
    }
    
    @SuppressWarnings({"ConstantConditions"})
    @Test
    void testRecordPatterns() {
        var request = VideoGenerationRequest.of("Test prompt");
        
        // Record patterns in switch (Java 21)
        String result = switch (request) {
            case VideoGenerationRequest(var instances, var parameters) -> {
                assertEquals(1, instances.size());
                assertEquals("16:9", parameters.aspectRatio());
                yield "Matched VideoGenerationRequest with " + instances.size() + " instances";
            }
        };
        
        assertNotNull(result);
        assertTrue(result.contains("1 instances"));
        
        // Record patterns with instanceof (Java 21)
        if (request instanceof VideoGenerationRequest(var instances, var parameters)) {
            assertEquals(1, instances.size());
            assertEquals("16:9", parameters.aspectRatio());
            
            // Nested record pattern
            var instance = instances.getFirst();
            if (instance instanceof VideoGenerationRequest.Instance(var prompt)) {
                assertEquals("Test prompt", prompt);
            }
        }
    }
    
    @Test
    void testStreamGatherers() {
        // Simulate polling attempt timestamps
        var pollingAttempts = List.of(
                Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T10:00:05Z"),
                Instant.parse("2024-01-01T10:00:10Z"),
                Instant.parse("2024-01-01T10:00:15Z"),
                Instant.parse("2024-01-01T10:00:20Z")
        );
        
        // Use stream gatherers to calculate intervals between polling attempts (Java 22+)
        var intervals = pollingAttempts.stream()
                .gather(Gatherers.windowSliding(2))
                .map(window -> Duration.between(window.getFirst(), window.get(1)))
                .toList();
        
        assertEquals(4, intervals.size()); // 5 attempts = 4 intervals
        intervals.forEach(interval -> 
                assertEquals(Duration.ofSeconds(5), interval)
        );
        
        // Demonstrate gatherer with filtering - only keep intervals over threshold
        var longIntervals = pollingAttempts.stream()
                .gather(Gatherers.windowSliding(2))
                .map(window -> Duration.between(window.getFirst(), window.get(1)))
                .filter(duration -> duration.compareTo(Duration.ofSeconds(3)) > 0)
                .toList();
        
        assertEquals(4, longIntervals.size()); // All our 5-second intervals pass the filter
        
        // Another gatherer example - batch polling attempts into groups
        var batchedAttempts = pollingAttempts.stream()
                .gather(Gatherers.windowFixed(2))
                .toList();
        
        assertEquals(3, batchedAttempts.size()); // 5 items in batches of 2 = 3 batches (last has 1 item)
        assertEquals(2, batchedAttempts.get(0).size());
        assertEquals(2, batchedAttempts.get(1).size());
        assertEquals(1, batchedAttempts.get(2).size()); // Last batch has remainder
    }
}