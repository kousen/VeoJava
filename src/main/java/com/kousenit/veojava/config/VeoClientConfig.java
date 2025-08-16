package com.kousenit.veojava.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "veo")
public class VeoClientConfig {
    
    private Api api = new Api();
    private Polling polling = new Polling();
    private Output output = new Output();
    
    public record Api(
            String key,
            String baseUrl,
            int connectTimeoutSeconds,
            int requestTimeoutMinutes
    ) {
        public Api() {
            this(null, "https://generativelanguage.googleapis.com/v1beta", 30, 2);
        }
    }
    
    public record Polling(
            int intervalSeconds,
            int maxTimeoutMinutes,
            int maxAttempts
    ) {
        public Polling() {
            this(5, 10, 120);
        }
    }
    
    public record Output(
            String directory,
            boolean autoCreateDirectory
    ) {
        public Output() {
            this("./videos", true);
        }
    }
    
    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }
    
    public Polling getPolling() { return polling; }
    public void setPolling(Polling polling) { this.polling = polling; }
    
    public Output getOutput() { return output; }
    public void setOutput(Output output) { this.output = output; }
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                   @Value("${gemini.api.key}") String apiKey) {
        // RestTemplate follows redirects automatically for GET requests - no special configuration needed!
        // Using RestTemplateBuilder for proper Spring Boot integration and auto-configuration
        return builder
                .rootUri("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("User-Agent", "VeoJava/1.0")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }
}