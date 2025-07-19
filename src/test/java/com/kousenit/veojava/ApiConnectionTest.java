package com.kousenit.veojava;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diagnostic test for basic API connectivity to Google's Veo 3 video generation endpoint.
 * Useful for troubleshooting when the main application fails to connect.
 */
@EnabledIfEnvironmentVariable(named = "GOOGLEAI_API_KEY", matches = ".+")
public class ApiConnectionTest {
    
    @Test
    void testApiConnection() throws IOException, InterruptedException {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        assertNotNull(apiKey, "GOOGLEAI_API_KEY environment variable must be set");
        
        String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        String endpoint = "/models/veo-3.0-generate-preview:predictLongRunning";
        
        String requestBody = """
            {
              "instances": [{
                "prompt": "Basic connectivity test video"
              }],
              "parameters": {
                "aspectRatio": "16:9",
                "personGeneration": "allow_all"
              }
            }
            """;
        
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(2))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            System.out.println("🔗 Testing connectivity to: " + baseUrl + endpoint);
            System.out.println("🔑 API Key present: " + (apiKey.length() > 10 ? "Yes" : "No"));
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("📡 Response Status: " + response.statusCode());
            System.out.println("📋 Response Body: " + response.body());
            
            // Basic assertions
            assertTrue(response.statusCode() >= 200 && response.statusCode() < 300, 
                    "Expected successful response, got " + response.statusCode());
            assertNotNull(response.body(), "Response body should not be null");
            assertTrue(response.body().contains("name"), 
                    "Response should contain operation name");
        }
    }
}