package com.kousenit.veojava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "veo")
public class VeoClientConfig {
    
    private Api api = new Api(null, null, 0, 0);
    private Polling polling = new Polling(0, 0, 0);
    private Output output = new Output(null, false);
    
    public record Api(
            String key,
            @DefaultValue("https://generativelanguage.googleapis.com/v1beta") String baseUrl,
            @DefaultValue("30") int connectTimeoutSeconds,
            @DefaultValue("2") int requestTimeoutMinutes
    ) {
        public Api {
            if (baseUrl == null) baseUrl = "https://generativelanguage.googleapis.com/v1beta";
            if (connectTimeoutSeconds == 0) connectTimeoutSeconds = 30;
            if (requestTimeoutMinutes == 0) requestTimeoutMinutes = 2;
        }
    }
    
    public record Polling(
            @DefaultValue("5") int intervalSeconds,
            @DefaultValue("10") int maxTimeoutMinutes,
            @DefaultValue("120") int maxAttempts
    ) {
        public Polling {
            if (intervalSeconds == 0) intervalSeconds = 5;
            if (maxTimeoutMinutes == 0) maxTimeoutMinutes = 10;
            if (maxAttempts == 0) maxAttempts = 120;
        }
    }
    
    public record Output(
            @DefaultValue("./videos") String directory,
            @DefaultValue("true") boolean autoCreateDirectory
    ) {
        public Output {
            if (directory == null) directory = "./videos";
            if (!autoCreateDirectory) autoCreateDirectory = true;
        }
    }
    
    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }
    
    public Polling getPolling() { return polling; }
    public void setPolling(Polling polling) { this.polling = polling; }
    
    public Output getOutput() { return output; }
    public void setOutput(Output output) { this.output = output; }
}