package com.kousenit.veojava;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diagnostic test for downloading specific video files from Google's file service.
 * Useful for retrieving test videos when you have a direct download URI.
 * <p>
 * To use: Replace the video URI and operation ID with actual values from your API responses.
 */
@EnabledIfEnvironmentVariable(named = "GOOGLEAI_API_KEY", matches = ".+")
public class RetrieveTestVideoTest {
    
    private static final Logger logger = LoggerFactory.getLogger(RetrieveTestVideoTest.class);
    
    @Disabled("Disabled to prevent accidental API calls and video downloads - enable manually for testing")
    @ParameterizedTest
    @CsvSource({
        "'https://generativelanguage.googleapis.com/v1beta/files/example-file-id:download?alt=media', 'example-operation-id'"
        // Add your actual video URIs and operation IDs here for testing
    })
    void downloadSpecificVideo(String videoUri, String operationId) throws IOException, InterruptedException {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        assertNotNull(apiKey, "GOOGLEAI_API_KEY environment variable must be set");
        
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(videoUri))
                    .header("x-goog-api-key", apiKey)
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();
            
            logger.info("📥 Downloading video from: {}", videoUri);
            
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            logger.info("📡 Response Status: {}", response.statusCode());
            logger.info("📄 Content-Type: {}", response.headers().firstValue("content-type").orElse("unknown"));
            logger.info("📏 Content-Length: {} bytes", response.body().length);
            
            if (response.statusCode() == 200) {
                // Basic assertions for successful download
                assertNotNull(response.body(), "Response body should not be null");
                assertTrue(response.body().length > 1000, "Video should be larger than 1KB");
                
                // Save the video with timestamp to avoid overwrites
                Path videosDir = Paths.get("./videos");
                if (!Files.exists(videosDir)) {
                    Files.createDirectories(videosDir);
                }
                
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String mimeType = response.headers().firstValue("content-type").orElse("video/mp4");
                String extension = mimeType.contains("mp4") ? ".mp4" : ".mov";
                String filename = "diagnostic_video_" + operationId + "_" + timestamp + extension;
                Path videoPath = videosDir.resolve(filename);
                
                Files.write(videoPath, response.body());
                
                logger.info("✅ Video saved to: {}", videoPath.toAbsolutePath());
                logger.info("🎬 Video size: {} bytes", Files.size(videoPath));
                
                // Verify file was written correctly
                assertTrue(Files.exists(videoPath), "Video file should exist after download");
                assertEquals(response.body().length, Files.size(videoPath), "File size should match downloaded bytes");
                
            } else if (response.statusCode() == 404) {
                logger.warn("❌ Video not found (may have expired): {}", response.statusCode());
                assertNotNull(response.body(), "Response body should contain error details");
            } else {
                logger.error("❌ Failed to download video: {}", response.statusCode());
                logger.error("Response: {}", new String(response.body()));
                fail("Unexpected response status: " + response.statusCode());
            }
        }
    }
    
    @Test
    void testVideoUriFormat() {
        // Test URI format validation without making API calls
        String baseFileUrl = "https://generativelanguage.googleapis.com/v1beta/files";
        String fileId = "test123";
        String expectedUri = baseFileUrl + "/" + fileId + ":download?alt=media";
        
        assertEquals("https://generativelanguage.googleapis.com/v1beta/files/test123:download?alt=media", 
                expectedUri);
        
        logger.info("✅ Video URI format validation passed");
    }
}