package com.kousenit.veojava;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@EnabledIfEnvironmentVariable(named = "GOOGLEAI_API_KEY", matches = ".+")
public class RetrieveTestVideoTest {
    
    @Test
    void downloadTestVideo() throws IOException, InterruptedException {
        String apiKey = System.getenv("GOOGLEAI_API_KEY");
        
        // Video URI from our previous test
        String videoUri = "https://generativelanguage.googleapis.com/v1beta/files/a936dm659f6e:download?alt=media";
        String operationId = "tglsvrvwxxft";
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(videoUri))
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        
        System.out.println("Downloading video from: " + videoUri);
        
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Content-Type: " + response.headers().firstValue("content-type").orElse("unknown"));
        System.out.println("Content-Length: " + response.body().length + " bytes");
        
        if (response.statusCode() == 200) {
            // Save the video
            Path videosDir = Paths.get("./videos");
            if (!Files.exists(videosDir)) {
                Files.createDirectories(videosDir);
            }
            
            String filename = "test_video_" + operationId + ".mp4";
            Path videoPath = videosDir.resolve(filename);
            
            Files.write(videoPath, response.body());
            
            System.out.println("✅ Video saved to: " + videoPath.toAbsolutePath());
            System.out.println("🎬 Video size: " + Files.size(videoPath) + " bytes");
        } else {
            System.out.println("❌ Failed to download video: " + response.statusCode());
        }
    }
}