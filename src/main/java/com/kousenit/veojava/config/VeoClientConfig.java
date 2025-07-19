package com.kousenit.veojava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "veo")
public class VeoClientConfig {
    
    private Api api = new Api();
    private Polling polling = new Polling();
    private Output output = new Output();
    
    public static class Api {
        private String key;
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private int connectTimeoutSeconds = 30;
        private int requestTimeoutMinutes = 2;
        
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
        
        public int getRequestTimeoutMinutes() { return requestTimeoutMinutes; }
        public void setRequestTimeoutMinutes(int requestTimeoutMinutes) { this.requestTimeoutMinutes = requestTimeoutMinutes; }
    }
    
    public static class Polling {
        private int intervalSeconds = 5;
        private int maxTimeoutMinutes = 10;
        private int maxAttempts = 120;
        
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        
        public int getMaxTimeoutMinutes() { return maxTimeoutMinutes; }
        public void setMaxTimeoutMinutes(int maxTimeoutMinutes) { this.maxTimeoutMinutes = maxTimeoutMinutes; }
        
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    }
    
    public static class Output {
        private String directory = "./videos";
        private boolean autoCreateDirectory = true;
        
        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }
        
        public boolean isAutoCreateDirectory() { return autoCreateDirectory; }
        public void setAutoCreateDirectory(boolean autoCreateDirectory) { this.autoCreateDirectory = autoCreateDirectory; }
    }
    
    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }
    
    public Polling getPolling() { return polling; }
    public void setPolling(Polling polling) { this.polling = polling; }
    
    public Output getOutput() { return output; }
    public void setOutput(Output output) { this.output = output; }
}