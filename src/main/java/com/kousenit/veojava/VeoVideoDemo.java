package com.kousenit.veojava;

import com.kousenit.veojava.client.HttpClientVeoVideoClient;
import com.kousenit.veojava.client.ReactiveVeoVideoClient;
import com.kousenit.veojava.client.RestClientVeoVideoClient;
import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import com.kousenit.veojava.service.SelfSchedulingPollingStrategy;
import com.kousenit.veojava.service.FixedRatePollingStrategy;
import com.kousenit.veojava.service.ReactivePollingStrategy;
import com.kousenit.veojava.service.VirtualThreadPollingStrategy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class VeoVideoDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(VeoVideoDemo.class);
    private static final String DEFAULT_MODEL = "veo-3.0-fast-generate-preview";
    
    @SuppressWarnings({"unused", "UnnecessaryModifier"})
    public static void main(String[] args) {
        if (System.getenv("GEMINI_API_KEY") == null) {
            logger.error("GEMINI_API_KEY environment variable is not set!");
            System.exit(1);
        }
        
        try (Scanner scanner = new Scanner(System.in)) {
            String header = """
                === Veo 3 Video Generation Demo ===
                ⚠️  WARNING: Each video costs ~$3.20 with fast preview model (8 seconds × $0.40/second)
                This demo will only generate ONE video at a time.
                """;
            logger.info(header);
            
            while (true) {
                showMenu();
                System.out.print("Choose an option (1-6, or 0 to exit): ");
                
                int choice = getValidChoice(scanner);
                if (choice == 0) {
                    logger.info("Goodbye!");
                    break;
                }
                
                String prompt = getPrompt(scanner);
                String generationInfo = """
                    🎬 Generating video (this will take several minutes)...
                    💰 Cost: ~$3.20 with fast preview model
                    Prompt: %s
                    """.formatted(prompt);
                logger.info(generationInfo);
                
                try {
                    switch (choice) {
                        case 1 -> demoHttpClientApproach(prompt);
                        case 2 -> demoRestClientApproach(prompt);
                        case 3 -> demoSelfSchedulingStrategy(prompt);
                        case 4 -> demoFixedRateStrategy(prompt);
                        case 5 -> demoReactiveStrategy(prompt);
                        case 6 -> demoVirtualThreadStrategy(prompt);
                    }
                    
                    logger.info("✅ Video generation completed!");
                    
                } catch (Exception e) {
                    logger.error("❌ Video generation failed: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    private static void showMenu() {
        String menu = """
            Available approaches:
            1. HttpClient (pure Java, no Spring)
            2. RestClient (Spring's modern HTTP client)
            3. SelfScheduling polling strategy
            4. FixedRate polling strategy
            5. Reactive polling strategy
            6. VirtualThread polling strategy
            0. Exit
            """;
        logger.info(menu);
    }
    
    private static int getValidChoice(Scanner scanner) {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                switch (choice) {
                    case 0, 1, 2, 3, 4, 5, 6 -> {
                        return choice;
                    }
                    default -> logger.warn("Invalid choice {}. Please enter 0-6", choice);
                }
            } catch (NumberFormatException e) {
                logger.warn("Please enter a number (0-6)");
            }
        }
    }
    
    private static String getPrompt(Scanner scanner) {
        String instructions = """
            Enter video prompt (or press Enter for default).
            For multi-line prompts:
              - Type your prompt and press Enter to continue on a new line
              - Type 'END' on a new line when finished
              - Or just press Enter twice to finish
            To load from file:
              - Type 'FILE:' followed by the filename (e.g., FILE:prompt.txt)
              - File path is relative to: %s
            """.formatted(System.getProperty("user.dir"));
        logger.info(instructions);
        
        StringBuilder promptBuilder = new StringBuilder();
        String line;
        boolean firstLine = true;
        int emptyLineCount = 0;
        
        while ((line = scanner.nextLine()) != null) {
            // Check for file input
            if (firstLine && line.trim().toUpperCase().startsWith("FILE:")) {
                String filename = line.trim().substring(5).trim();
                try {
                    Path filePath = Paths.get(filename);
                    // Show absolute path for clarity
                    Path absolutePath = filePath.toAbsolutePath();
                    logger.info("Looking for file at: {}", absolutePath);
                    
                    if (!Files.exists(filePath)) {
                        String fileError = """
                            File not found: %s
                            Make sure the file exists at that location.
                            You can use:
                              - Relative paths: FILE:prompts/my-prompt.txt
                              - Absolute paths: FILE:/Users/name/prompts/my-prompt.txt
                            Please enter prompt manually:
                            """.formatted(absolutePath);
                        logger.error(fileError);
                        continue;
                    }
                    
                    String fileContent = Files.readString(filePath);
                    logger.info("Successfully loaded prompt from: {}", filename);
                    logger.info("Prompt preview: {}", 
                        fileContent.length() > 100 ? 
                        fileContent.substring(0, 100) + "..." : 
                        fileContent);
                    return fileContent.trim();
                } catch (IOException e) {
                    String ioError = formatFileReadError(e, filename);
                    logger.error(ioError);
                    continue;
                }
            }
            
            // Check for explicit END marker
            if (line.trim().equalsIgnoreCase("END")) {
                break;
            }
            
            // Check for double Enter (two consecutive empty lines)
            if (line.trim().isEmpty()) {
                emptyLineCount++;
                // For first line, single Enter means use default
                if (firstLine) {
                    return "A cat playing with a ball of yarn in a sunny garden";
                }
                // Two consecutive empty lines means end of input
                if (emptyLineCount >= 2) {
                    break;
                }
                // Single empty line - just add a space to separate paragraphs
                if (!promptBuilder.isEmpty()) {
                    promptBuilder.append(" ");
                }
            } else {
                emptyLineCount = 0;
                // Add line to prompt
                if (!firstLine && !promptBuilder.isEmpty()) {
                    promptBuilder.append(" ");
                }
                promptBuilder.append(line.trim());
            }
            firstLine = false;
        }
        
        String prompt = promptBuilder.toString().trim();
        return prompt.isEmpty() ? "A cat playing with a ball of yarn in a sunny garden" : prompt;
    }
    
    private static void demoHttpClientApproach(String prompt) throws ExecutionException, InterruptedException, IOException {
        logger.info("=== HttpClient Approach ===");
        HttpClientVeoVideoClient client = new HttpClientVeoVideoClient();
        
        CompletableFuture<VideoResult> future = client.generateVideoAsync(
                VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "http_client_");
        logVideoSaved(filename, result.videoBytes().length);
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
        logVideoSaved(filename, result.videoBytes().length);
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
        logVideoSavedWithStrategy(filename, result.videoBytes().length, strategy.getStrategyName());
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
        logVideoSavedWithStrategy(filename, result.videoBytes().length, strategy.getStrategyName());
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
        logVideoSavedWithStrategy(filename, result.videoBytes().length, strategy.getStrategyName());
    }
    
    private static void demoReactiveStrategy(String prompt) throws ExecutionException, InterruptedException, IOException {
        logger.info("=== Reactive Strategy ===");
        String apiKey = System.getenv("GEMINI_API_KEY");
        String model = "veo-3.0-fast-generate-preview"; // Use same default as HttpClient for consistency
        logger.info("Using model: {}", model);
        ReactiveVeoVideoClient reactiveClient = new ReactiveVeoVideoClient(apiKey, model);
        ReactivePollingStrategy strategy = new ReactivePollingStrategy(reactiveClient);
        VeoVideoClient client = new HttpClientVeoVideoClient(); // Passed but not used by reactive strategy
        
        CompletableFuture<VideoResult> future = strategy.generateVideo(
                client, VideoGenerationRequest.of(prompt)
        );
        
        VideoResult result = future.get();
        String filename = saveVideo(result, "reactive_");
        logVideoSavedWithStrategy(filename, result.videoBytes().length, strategy.getStrategyName());
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
    
    private static void logVideoSaved(String filename, int fileSize) {
        String formattedSize = formatFileSize(fileSize);
        logger.info("Video saved: {} (size: {})", filename, formattedSize);
    }
    
    private static void logVideoSavedWithStrategy(String filename, int fileSize, String strategyName) {
        String formattedSize = formatFileSize(fileSize);
        logger.info("Video saved: {} (size: {}, strategy: {})", filename, formattedSize, strategyName);
    }
    
    private static String formatFileSize(int bytes) {
        return switch (bytes) {
            case int b when b < 1024 -> b + " bytes";
            case int b when b < 1024 * 1024 -> "%.1f KB".formatted(b / 1024.0);
            case int b when b < 1024 * 1024 * 1024 -> "%.1f MB".formatted(b / (1024.0 * 1024));
            default -> "%.1f GB".formatted(bytes / (1024.0 * 1024 * 1024));
        };
    }
    
    private static String formatFileReadError(IOException e, String filename) {
        String errorType = switch (e) {
            case java.nio.file.NoSuchFileException _ -> "File not found";
            case java.nio.file.AccessDeniedException _ -> "Access denied";
            case java.io.FileNotFoundException _ -> "File not found";
            default -> "IO error";
        };
        return """
            %s reading file '%s': %s
            Please enter prompt manually:
            """.formatted(errorType, filename, e.getMessage());
    }
    
    private static String getConfiguredModel() {
        try (InputStream input = VeoVideoDemo.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.warn("Could not find application.properties, using default model: {}", DEFAULT_MODEL);
                return DEFAULT_MODEL;
            }
            
            Properties properties = new Properties();
            properties.load(input);
            String model = properties.getProperty("veo.api.model", DEFAULT_MODEL);
            logger.info("Loaded model from application.properties: {}", model);
            return model;
            
        } catch (IOException e) {
            logger.warn("Error reading application.properties, using default model: {} (error: {})", DEFAULT_MODEL, e.getMessage());
            return DEFAULT_MODEL;
        }
    }
}