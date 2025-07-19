package com.kousenit.veojava;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@EnabledIfEnvironmentVariable(named = "GOOGLEAI_API_KEY", matches = ".+")
public class RedirectTest {
    
    @Test
    void testRestClientRedirects() {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        String videoUri = "https://generativelanguage.googleapis.com/v1beta/files/a936dm659f6e:download?alt=media";
        
        // Configure request factory to follow redirects
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(true);
            }
        };
        
        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        
        try {
            System.out.println("Testing RestClient with redirects...");
            byte[] videoBytes = restClient.get()
                    .uri(videoUri)
                    .retrieve()
                    .body(byte[].class);
            
            System.out.println("✅ RestClient: Successfully downloaded " + videoBytes.length + " bytes");
            if (videoBytes.length < 1000) {
                System.out.println("Content: " + new String(videoBytes));
            }
        } catch (Exception e) {
            System.out.println("❌ RestClient failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    void testWebClientRedirects() {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        String videoUri = "https://generativelanguage.googleapis.com/v1beta/files/a936dm659f6e:download?alt=media";
        
        // Configure HTTP client to follow redirects
        var httpClient = reactor.netty.http.client.HttpClient.create().followRedirect(true);
        var connector = new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient);
        
        WebClient webClient = WebClient.builder()
                .clientConnector(connector)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB buffer
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        
        try {
            System.out.println("Testing WebClient with redirects...");
            byte[] videoBytes = webClient.get()
                    .uri(videoUri)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(Duration.ofMinutes(1));
            
            System.out.println("✅ WebClient: Successfully downloaded " + videoBytes.length + " bytes");
            if (videoBytes.length < 1000) {
                System.out.println("Content: " + new String(videoBytes));
            }
        } catch (Exception e) {
            System.out.println("❌ WebClient failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}