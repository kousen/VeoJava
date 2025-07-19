package com.kousenit.veojava;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@EnabledIfEnvironmentVariable(named = "GOOGLEAI_API_KEY", matches = ".+")
public class AuthHeaderTest {
    
    @Test
    void testXGoogApiKeyHeader() throws IOException, InterruptedException {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        
        // Test with a simple Gemini model first to verify API key works
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent";
        
        String requestBody = """
            {
              "contents": [{
                "parts": [{
                  "text": "Hello, just testing API key"
                }]
              }]
            }
            """;
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        System.out.println("Testing with x-goog-api-key header...");
        System.out.println("Endpoint: " + endpoint);
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
    
    @Test
    void testAuthorizationBearerHeader() throws IOException, InterruptedException {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        
        // Test with Authorization Bearer header
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent";
        
        String requestBody = """
            {
              "contents": [{
                "parts": [{
                  "text": "Hello, just testing API key"
                }]
              }]
            }
            """;
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        System.out.println("Testing with Authorization Bearer header...");
        System.out.println("Endpoint: " + endpoint);
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}