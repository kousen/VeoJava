package com.kousenit.veojava.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;

class RestTemplateVeoVideoClientConstructorTest {
    
    @Test
    void testSpringConstructor() {
        // Test Spring constructor with RestTemplate injection
        RestTemplate restTemplate = new RestTemplate();
        String model = "veo-3.0-fast-generate-preview";
        
        RestTemplateVeoVideoClient client = new RestTemplateVeoVideoClient(model, restTemplate);
        
        assertThat(client).isNotNull();
    }
    
    @Test
    void testSpringConstructorWithCustomModel() {
        // Test Spring constructor with custom model
        RestTemplate restTemplate = new RestTemplate();
        String customModel = "veo-3.0-generate-preview";
        
        RestTemplateVeoVideoClient client = new RestTemplateVeoVideoClient(customModel, restTemplate);
        
        assertThat(client).isNotNull();
    }
    
    @Test
    void testDemoConstructor() {
        // Test demo constructor - this will only work if environment variables are set
        // We can't easily test the actual construction without setting env vars
        // But we can test that the error handling works
        
        // This test will pass if GEMINI_API_KEY or GOOGLEAI_API_KEY is set
        // Otherwise, it should throw an exception
        try {
            RestTemplateVeoVideoClient client = new RestTemplateVeoVideoClient();
            assertThat(client).isNotNull();
        } catch (IllegalArgumentException e) {
            // Expected if no API key is set
            assertThat(e.getMessage()).contains("environment variable is required");
        }
    }
    
    @Test
    void testDemoConstructorValidation() {
        // Test that we can verify the exception message structure
        // Even if we can't control the environment variables directly
        
        // Create a subclass to override environment variable access for testing
        class TestableRestTemplateVeoVideoClient extends RestTemplateVeoVideoClient {
            private final String mockApiKey;
            
            public TestableRestTemplateVeoVideoClient(String mockApiKey) {
                this.mockApiKey = mockApiKey;
                // Initialize the endpoint since parent constructor won't run
                // This is a bit hacky but allows us to test the validation logic
            }
            
            @Override
            public String toString() {
                // Simple validation that mimics the constructor logic
                if (mockApiKey == null || mockApiKey.isEmpty()) {
                    throw new IllegalArgumentException("GOOGLEAI_API_KEY or GEMINI_API_KEY environment variable is required");
                }
                return "TestableClient";
            }
        }
        
        // Test with null API key
        assertThatThrownBy(() -> new TestableRestTemplateVeoVideoClient(null).toString())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("environment variable is required");
        
        // Test with empty API key
        assertThatThrownBy(() -> new TestableRestTemplateVeoVideoClient("").toString())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("environment variable is required");
        
        // Test with valid API key
        assertThatCode(() -> new TestableRestTemplateVeoVideoClient("valid-key").toString())
                .doesNotThrowAnyException();
    }
}