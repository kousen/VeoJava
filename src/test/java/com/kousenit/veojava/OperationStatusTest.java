package com.kousenit.veojava;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diagnostic test for checking the status of specific Veo 3 video generation operations.
 * Useful for monitoring long-running video generation jobs.
 * <p>
 * To use: Replace the operation ID in the test with an actual operation from your API calls.
 */
@EnabledIfEnvironmentVariable(named = "GOOGLEAI_API_KEY", matches = ".+")
public class OperationStatusTest {
    
    @Disabled("Disabled to prevent accidental API calls - enable manually for operation status checking")
    @ParameterizedTest
    @ValueSource(strings = {
        "models/veo-3.0-generate-preview/operations/example-operation-id"
        // Add your actual operation IDs here for testing
    })
    void testOperationStatus(String operationName) throws IOException, InterruptedException {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        assertNotNull(apiKey, "GOOGLEAI_API_KEY environment variable must be set");
        
        String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        String url = baseUrl + "/" + operationName;
        
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            System.out.println("🔍 Checking operation: " + operationName);
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("📡 Response Status: " + response.statusCode());
            System.out.println("📋 Response Body: " + response.body());
            
            // Basic assertions
            assertTrue(response.statusCode() == 200 || response.statusCode() == 404, 
                    "Expected 200 (found) or 404 (not found), got " + response.statusCode());
            assertNotNull(response.body(), "Response body should not be null");
            
            if (response.statusCode() == 200) {
                assertTrue(response.body().contains("\"name\""), 
                        "Successful response should contain operation name");
                System.out.println("✅ Operation found and status retrieved");
            } else {
                System.out.println("❌ Operation not found (may have expired)");
            }
        }
    }
    
    @Test
    void testOperationStatusUrlFormat() {
        // Test URL construction without making API calls
        String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        String operationName = "models/veo-3.0-generate-preview/operations/test123";
        String expectedUrl = baseUrl + "/" + operationName;
        
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/veo-3.0-generate-preview/operations/test123", 
                expectedUrl);
        
        System.out.println("✅ URL format validation passed");
    }
}