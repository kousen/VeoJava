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
public class OperationStatusTest {
    
    @Test
    void testOperationStatusUrl() throws IOException, InterruptedException {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        
        // Use the operation name from our previous test
        String operationName = "models/veo-3.0-generate-preview/operations/tglsvrvwxxft";
        String url = baseUrl + "/" + operationName;
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        
        System.out.println("Testing operation status URL: " + url);
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}