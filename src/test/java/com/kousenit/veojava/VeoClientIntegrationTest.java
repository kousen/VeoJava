package com.kousenit.veojava;

import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.service.VideoGenerationService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    // They are disabled by default to prevent accidental quota/cost consumption
    
    @Disabled("Requires actual API calls - costs ~$6 per test and takes several minutes")
    @Test
    void testRestClientVideoGeneration() throws Exception {
        String prompt = """
                A warrior cat flies into battle on the back
                of a dragon. They roar in harmony as they
                fly by.
                """;
        CompletableFuture<String> future = videoService.generateAndSaveVideo(
                prompt, "restclient", "./videos"
        );
        
        String filePath = future.get(15, TimeUnit.MINUTES);
        assertNotNull(filePath);
        assertTrue(Files.exists(Paths.get(filePath)));
    }
    
    @Disabled("Requires actual API calls - costs ~$6 per test and takes several minutes")
    @Test
    void testHttpClientVideoGeneration() throws Exception {
        String prompt = "A simple test animation";
        CompletableFuture<String> future = videoService.generateAndSaveVideo(
                prompt, "httpclient", "./videos"
        );
        
        String filePath = future.get(15, TimeUnit.MINUTES);
        assertNotNull(filePath);
        assertTrue(Files.exists(Paths.get(filePath)));
    }
}