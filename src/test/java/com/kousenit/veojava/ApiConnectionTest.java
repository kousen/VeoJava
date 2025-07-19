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
public class ApiConnectionTest {
    
    @Test
    void testApiConnection() throws IOException, InterruptedException {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        String endpoint = "/models/veo-3.0-generate-preview:predictLongRunning";
        
        String requestBody = """
            {
              "instances": [{
                "prompt": "A simple test video"
              }],
              "parameters": {
                "aspectRatio": "16:9",
                "personGeneration": "allow_all"
              }
            }
            """;
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        System.out.println("Request URL: " + baseUrl + endpoint);
        System.out.println("Request Body: " + requestBody);
        System.out.println("API Key present: " + (apiKey != null && !apiKey.isEmpty()));
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response Headers: " + response.headers().map());
        System.out.println("Response Body: " + response.body());
    }
}