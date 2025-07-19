package com.kousenit.veojava;

import com.kousenit.veojava.client.HttpClientVeoVideoClient;
import com.kousenit.veojava.client.RestClientVeoVideoClient;
import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import com.kousenit.veojava.service.CompletableFuturePollingStrategy;
import com.kousenit.veojava.service.ScheduledExecutorPollingStrategy;
import com.kousenit.veojava.service.VirtualThreadPollingStrategy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class VeoVideoDemo {
    
    private static final Logger logger = Logger.getLogger(VeoVideoDemo.class.getName());
    
    public static void main(String[] args) {
        if (System.getenv("GEMINI_API_KEY") == null) {
            System.err.println("GEMINI_API_KEY environment variable is not set!");
            System.exit(1);
        }
        
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("=== Veo 3 Video Generation Demo ===");
            System.out.println("⚠️  WARNING: Each video costs ~$6.00 (8 seconds × $0.75/second)");
            System.out.println("This demo will only generate ONE video at a time.\n");
            
            while (true) {
                showMenu();
                System.out.print("Choose an option (1-5, or 0 to exit): ");
                
                int choice = getValidChoice(scanner);
                if (choice == 0) {
                    System.out.println("Goodbye!");
                    break;
                }
                
                String prompt = getPrompt(scanner);
                System.out.println("\n🎬 Generating video (this will take several minutes)...");
                System.out.println("💰 Cost: ~$6.00");
                System.out.println("Prompt: " + prompt + "\n");
                
                try {
                    switch (choice) {
                        case 1 -> demoHttpClientApproach(prompt);
                        case 2 -> demoRestClientApproach(prompt);
                        case 3 -> demoCompletableFutureStrategy(prompt);
                        case 4 -> demoScheduledExecutorStrategy(prompt);
                        case 5 -> demoVirtualThreadStrategy(prompt);
                    }
                    
                    System.out.println("✅ Video generation completed!\n");
                    
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during video generation", e);
                    System.err.println("❌ Video generation failed: " + e.getMessage() + "\n");
                }
            }
        }
    }
    
    private static void showMenu() {
        System.out.println("Available approaches:");
        System.out.println("1. HttpClient (pure Java, no Spring)");
        System.out.println("2. RestClient (Spring's modern HTTP client)");
        System.out.println("3. CompletableFuture polling strategy");
        System.out.println("4. ScheduledExecutor polling strategy");  
        System.out.println("5. VirtualThread polling strategy");
        System.out.println("0. Exit");
    }
    
    private static int getValidChoice(Scanner scanner) {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 0 && choice <= 5) {
                    return choice;
                }
                System.out.print("Invalid choice. Please enter 0-5: ");
            } catch (NumberFormatException e) {
                System.out.print("Please enter a number (0-5): ");
            }
        }
    }
    
    private static String getPrompt(Scanner scanner) {
        System.out.print("Enter video prompt (or press Enter for default): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? "A cat playing with a ball of yarn in a sunny garden" : input;
    }
    
    private static void demoHttpClientApproach(String prompt) throws ExecutionException, InterruptedException, IOException {
        System.out.println("=== HttpClient Approach ===");
        HttpClientVeoVideoClient client = new HttpClientVeoVideoClient();
        
        CompletableFuture<VideoResult> future = client.generateVideoAsync(
                VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "http_client_");
        System.out.println("Video saved: " + filename);
        System.out.println("File size: " + result.videoBytes().length + " bytes\n");
    }
    
    private static void demoRestClientApproach(String prompt) throws ExecutionException, InterruptedException, IOException {
        System.out.println("=== RestClient Approach ===");
        
        // Create RestClient manually - works without Spring context for demo
        String apiKey = System.getenv("GEMINI_API_KEY");
        RestClientVeoVideoClient client = new RestClientVeoVideoClient(apiKey);
        
        CompletableFuture<VideoResult> future = client.generateVideoAsync(
                VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "rest_client_");
        System.out.println("Video saved: " + filename);
        System.out.println("File size: " + result.videoBytes().length + " bytes\n");
    }
    
    private static void demoCompletableFutureStrategy(String prompt) throws ExecutionException, InterruptedException, IOException {
        System.out.println("=== CompletableFuture Strategy ===");
        VeoVideoClient client = new HttpClientVeoVideoClient();
        CompletableFuturePollingStrategy strategy = new CompletableFuturePollingStrategy();
        
        CompletableFuture<VideoResult> future = strategy.generateVideo(
                client, VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "completable_future_");
        System.out.println("Video saved: " + filename);
        System.out.println("Strategy: " + strategy.getStrategyName());
        System.out.println("File size: " + result.videoBytes().length + " bytes\n");
    }
    
    private static void demoScheduledExecutorStrategy(String prompt) throws ExecutionException, InterruptedException, IOException {
        System.out.println("=== ScheduledExecutor Strategy ===");
        VeoVideoClient client = new HttpClientVeoVideoClient();
        ScheduledExecutorPollingStrategy strategy = new ScheduledExecutorPollingStrategy();
        
        CompletableFuture<VideoResult> future = strategy.generateVideo(
                client, VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "scheduled_executor_");
        System.out.println("Video saved: " + filename);
        System.out.println("Strategy: " + strategy.getStrategyName());
        System.out.println("File size: " + result.videoBytes().length + " bytes\n");
    }
    
    private static void demoVirtualThreadStrategy(String prompt) throws ExecutionException, InterruptedException, IOException {
        System.out.println("=== VirtualThread Strategy ===");
        VeoVideoClient client = new HttpClientVeoVideoClient();
        VirtualThreadPollingStrategy strategy = new VirtualThreadPollingStrategy();
        
        CompletableFuture<VideoResult> future = strategy.generateVideo(
                client, VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "virtual_thread_");
        System.out.println("Video saved: " + filename);
        System.out.println("Strategy: " + strategy.getStrategyName());
        System.out.println("File size: " + result.videoBytes().length + " bytes\n");
    }
    
    private static String saveVideo(VideoResult result, String prefix) throws IOException {
        String outputDir = "./demo_videos";
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