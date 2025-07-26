package com.kousenit.veojava;

import com.kousenit.veojava.model.VeoJavaRecords.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VeoRecordsTest {
    
    @Test
    void testVideoGenerationRequestCreation() {
        String prompt = "Test prompt";
        VideoGenerationRequest request = VideoGenerationRequest.of(prompt);
        
        assertNotNull(request);
        assertEquals(1, request.instances().size());
        assertEquals(prompt, request.instances().getFirst().prompt());
        assertEquals("16:9", request.parameters().aspectRatio());
        assertEquals("allow_all", request.parameters().personGeneration());
    }
    
    @Test
    void testVideoGenerationRequestWithCustomParameters() {
        String prompt = "Test prompt";
        var customParams = new VideoGenerationRequest.Parameters("16:9", "allow_all");
        var instance = new VideoGenerationRequest.Instance(prompt);
        
        VideoGenerationRequest request = new VideoGenerationRequest(
                List.of(instance), customParams
        );
        
        assertNotNull(request);
        assertEquals(prompt, request.instances().getFirst().prompt());
        assertEquals("16:9", request.parameters().aspectRatio());
    }
    
    @Test
    void testVideoGenerationResponse() {
        String operationId = "operations/test-123";
        Map<String, Object> metadata = Map.of("key", "value");
        
        VideoGenerationResponse response = new VideoGenerationResponse(operationId, metadata);
        
        assertEquals(operationId, response.operationId());
        assertEquals(metadata, response.metadata());
    }
    
    @Test
    void testOperationStatusCompleted() {
        String operationId = "operations/test-123";
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
        
        OperationStatus status = new OperationStatus(
                operationId, true, null, operationResponse, Map.of()
        );
        
        assertTrue(status.done());
        assertNull(status.error());
        assertNotNull(status.response());
        assertEquals(1, status.response().generateVideoResponse().generatedSamples().size());
        assertEquals("https://example.com/video.mp4", status.response().generateVideoResponse().generatedSamples().getFirst().video().uri());
    }
    
    @Test
    void testOperationStatusWithError() {
        String operationId = "operations/test-123";
        var error = new OperationStatus.ErrorInfo(
                400, "Invalid request", List.of()
        );
        
        OperationStatus status = new OperationStatus(
                operationId, true, error, null, Map.of()
        );
        
        assertTrue(status.done());
        assertNotNull(status.error());
        assertEquals(400, status.error().code());
        assertEquals("Invalid request", status.error().message());
    }
    
    @Test
    void testVideoResult() {
        String base64 = "dGVzdA=="; // "test" in base64
        String mimeType = "video/mp4";
        String filename = "test.mp4";
        byte[] bytes = new byte[]{116, 101, 115, 116}; // "test" as bytes
        
        VideoResult result = new VideoResult(base64, mimeType, filename, bytes);
        
        assertEquals(base64, result.videoBase64());
        assertEquals(mimeType, result.mimeType());
        assertEquals(filename, result.filename());
        assertArrayEquals(bytes, result.videoBytes());
    }
    
    @Test
    void testErrorResponse() {
        ErrorResponse error = new ErrorResponse(500, "Internal error", "INTERNAL");
        
        assertEquals(500, error.code());
        assertEquals("Internal error", error.message());
        assertEquals("INTERNAL", error.status());
    }
}