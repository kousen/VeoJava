package com.kousenit.veojava;

import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.service.VideoGenerationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class VeoClientIntegrationTest {
    
    @Autowired
    private VideoGenerationService videoService;
    
    @Test
    void testVideoGenerationRequestCreation() {
        String prompt = "A test video prompt";
        VideoGenerationRequest request = VideoGenerationRequest.of(prompt);
        
        assertNotNull(request);
        assertEquals(1, request.instances().size());
        assertEquals(prompt, request.instances().getFirst().prompt());
        assertEquals("16:9", request.parameters().aspectRatio());
        assertEquals("allow_all", request.parameters().personGeneration());
    }
    
    @Test
    void testServiceInitialization() {
        assertNotNull(videoService);
    }
    
    // Note: These tests require actual API calls and take several minutes
    // Uncomment to test with real API (will consume quota and cost money)
    
    /*
    @Test
    void testRestClientVideoGeneration() throws Exception {
        String prompt = "A simple test animation";
        CompletableFuture<String> future = videoService.generateAndSaveVideo(
                prompt, "restclient", "./test_videos"
        );
        
        String filePath = future.get(15, TimeUnit.MINUTES);
        assertNotNull(filePath);
        assertTrue(Files.exists(Paths.get(filePath)));
    }
    
    @Test
    void testHttpClientVideoGeneration() throws Exception {
        String prompt = "A simple test animation";
        CompletableFuture<String> future = videoService.generateAndSaveVideo(
                prompt, "httpclient", "./test_videos"
        );
        
        String filePath = future.get(15, TimeUnit.MINUTES);
        assertNotNull(filePath);
        assertTrue(Files.exists(Paths.get(filePath)));
    }
    */
}