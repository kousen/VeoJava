package com.kousenit.veojava;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import org.junit.jupiter.api.Test;

public class DebugApiTest {
    
    @Test
    void testJsonSerialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        VideoGenerationRequest request = VideoGenerationRequest.of("A cat playing with a ball of yarn");
        
        String json = objectMapper.writeValueAsString(request);
        System.out.println("Generated JSON:");
        System.out.println(json);
        
        // Pretty print for easier reading
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        System.out.println("\nPretty JSON:");
        System.out.println(prettyJson);
    }
}