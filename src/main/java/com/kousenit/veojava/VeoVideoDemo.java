package com.kousenit.veojava;

import com.kousenit.veojava.client.HttpClientVeoVideoClient;
import com.kousenit.veojava.client.RestClientVeoVideoClient;
import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import com.kousenit.veojava.service.SelfSchedulingPollingStrategy;
import com.kousenit.veojava.service.FixedRatePollingStrategy;
import com.kousenit.veojava.service.VirtualThreadPollingStrategy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class VeoVideoDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(VeoVideoDemo.class);
    
    public static void main(String[] args) {
        if (System.getenv("GEMINI_API_KEY") == null) {
            logger.error("GEMINI_API_KEY environment variable is not set!");
            System.exit(1);
        }
        
        try (Scanner scanner = new Scanner(System.in)) {
            logger.info("=== Veo 3 Video Generation Demo ===");
            logger.warn("⚠️  WARNING: Each video costs ~$6.00 (8 seconds × $0.75/second)");
            logger.info("This demo will only generate ONE video at a time.");
            
            while (true) {
                showMenu();
                System.out.print("Choose an option (1-5, or 0 to exit): ");
                
                int choice = getValidChoice(scanner);
                if (choice == 0) {
                    logger.info("Goodbye!");
                    break;
                }
                
                String prompt = getPrompt(scanner);
                logger.info("🎬 Generating video (this will take several minutes)...");
                logger.warn("💰 Cost: ~$6.00");
                logger.info("Prompt: {}", prompt);
                
                try {
                    switch (choice) {
                        case 1 -> demoHttpClientApproach(prompt);
                        case 2 -> demoRestClientApproach(prompt);
                        case 3 -> demoSelfSchedulingStrategy(prompt);
                        case 4 -> demoFixedRateStrategy(prompt);
                        case 5 -> demoVirtualThreadStrategy(prompt);
                    }
                    
                    logger.info("✅ Video generation completed!");
                    
                } catch (Exception e) {
                    logger.error("❌ Video generation failed: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    private static void showMenu() {
        logger.info("Available approaches:");
        logger.info("1. HttpClient (pure Java, no Spring)");
        logger.info("2. RestClient (Spring's modern HTTP client)");
        logger.info("3. SelfScheduling polling strategy");
        logger.info("4. FixedRate polling strategy");
        logger.info("5. VirtualThread polling strategy");
        logger.info("0. Exit");
    }
    
    private static int getValidChoice(Scanner scanner) {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 0 && choice <= 5) {
                    return choice;
                }
                logger.warn("Invalid choice. Please enter 0-5");
            } catch (NumberFormatException e) {
                logger.warn("Please enter a number (0-5)");
            }
        }
    }
    
    private static String getPrompt(Scanner scanner) {
        logger.info("Enter video prompt (or press Enter for default)");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? "A cat playing with a ball of yarn in a sunny garden" : input;
    }
    
    private static void demoHttpClientApproach(String prompt) throws ExecutionException, InterruptedException, IOException {
        logger.info("=== HttpClient Approach ===");
        HttpClientVeoVideoClient client = new HttpClientVeoVideoClient();
        
        CompletableFuture<VideoResult> future = client.generateVideoAsync(
                VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "http_client_");
        logger.info("Video saved: {}", filename);
        logger.info("File size: {} bytes", result.videoBytes().length);
    }
    
    private static void demoRestClientApproach(String prompt) throws ExecutionException, InterruptedException, IOException {
        logger.info("=== RestClient Approach ===");
        
        // Create RestClient manually - works without Spring context for demo
        RestClientVeoVideoClient client = new RestClientVeoVideoClient();
        
        CompletableFuture<VideoResult> future = client.generateVideoAsync(
                VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "rest_client_");
        logger.info("Video saved: {}", filename);
        logger.info("File size: {} bytes", result.videoBytes().length);
    }
    
    private static void demoSelfSchedulingStrategy(String prompt) throws ExecutionException, InterruptedException, IOException {
        logger.info("=== SelfScheduling Strategy ===");
        VeoVideoClient client = new HttpClientVeoVideoClient();
        SelfSchedulingPollingStrategy strategy = new SelfSchedulingPollingStrategy();
        
        CompletableFuture<VideoResult> future = strategy.generateVideo(
                client, VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "self_scheduling_");
        logger.info("Video saved: {}", filename);
        logger.info("Strategy: {}", strategy.getStrategyName());
        logger.info("File size: {} bytes", result.videoBytes().length);
    }
    
    private static void demoFixedRateStrategy(String prompt) throws ExecutionException, InterruptedException, IOException {
        logger.info("=== FixedRate Strategy ===");
        VeoVideoClient client = new HttpClientVeoVideoClient();
        FixedRatePollingStrategy strategy = new FixedRatePollingStrategy();
        
        CompletableFuture<VideoResult> future = strategy.generateVideo(
                client, VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "fixed_rate_");
        logger.info("Video saved: {}", filename);
        logger.info("Strategy: {}", strategy.getStrategyName());
        logger.info("File size: {} bytes", result.videoBytes().length);
    }
    
    private static void demoVirtualThreadStrategy(String prompt) throws ExecutionException, InterruptedException, IOException {
        logger.info("=== VirtualThread Strategy ===");
        VeoVideoClient client = new HttpClientVeoVideoClient();
        VirtualThreadPollingStrategy strategy = new VirtualThreadPollingStrategy();
        
        CompletableFuture<VideoResult> future = strategy.generateVideo(
                client, VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "virtual_thread_");
        logger.info("Video saved: {}", filename);
        logger.info("Strategy: {}", strategy.getStrategyName());
        logger.info("File size: {} bytes", result.videoBytes().length);
    }
    
    private static String saveVideo(VideoResult result, String prefix) throws IOException {
        String outputDir = "./videos";
        Files.createDirectories(Paths.get(outputDir));
        
        String filename = prefix + System.currentTimeMillis() + 
                         (result.mimeType().contains("mp4") ? ".mp4" : ".mov");
        String filepath = outputDir + "/" + filename;
        
        try (FileOutputStream fos = new FileOutputStream(filepath)) {
            fos.write(result.videoBytes());
        }
        
        return filepath;
    }
}