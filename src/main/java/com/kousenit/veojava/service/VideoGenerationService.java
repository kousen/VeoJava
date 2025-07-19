package com.kousenit.veojava.service;

import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@Service
public class VideoGenerationService {
    
    private final VeoVideoClient restClient;
    private final VeoVideoClient httpClient;
    private final CompletableFuturePollingStrategy completableFutureStrategy;
    private final ScheduledExecutorPollingStrategy scheduledExecutorStrategy;
    private final ReactivePollingStrategy reactiveStrategy;
    private final VirtualThreadPollingStrategy virtualThreadStrategy;
    
    public VideoGenerationService(
            @Qualifier("restClientVeoVideoClient") VeoVideoClient restClient,
            @Qualifier("httpClientVeoVideoClient") VeoVideoClient httpClient,
            CompletableFuturePollingStrategy completableFutureStrategy,
            ScheduledExecutorPollingStrategy scheduledExecutorStrategy,
            ReactivePollingStrategy reactiveStrategy,
            VirtualThreadPollingStrategy virtualThreadStrategy) {
        
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.completableFutureStrategy = completableFutureStrategy;
        this.scheduledExecutorStrategy = scheduledExecutorStrategy;
        this.reactiveStrategy = reactiveStrategy;
        this.virtualThreadStrategy = virtualThreadStrategy;
    }
    
    public CompletableFuture<VideoResult> generateVideoWithRestClient(String prompt) {
        VideoGenerationRequest request = VideoGenerationRequest.of(prompt);
        return restClient.generateVideoAsync(request);
    }
    
    public CompletableFuture<VideoResult> generateVideoWithHttpClient(String prompt) {
        VideoGenerationRequest request = VideoGenerationRequest.of(prompt);
        return httpClient.generateVideoAsync(request);
    }
    
    public CompletableFuture<VideoResult> generateVideoWithCompletableFuture(String prompt) {
        VideoGenerationRequest request = VideoGenerationRequest.of(prompt);
        return completableFutureStrategy.generateVideo(restClient, request);
    }
    
    public CompletableFuture<VideoResult> generateVideoWithScheduledExecutor(String prompt) {
        VideoGenerationRequest request = VideoGenerationRequest.of(prompt);
        return scheduledExecutorStrategy.generateVideo(restClient, request);
    }
    
    public CompletableFuture<VideoResult> generateVideoWithReactive(String prompt) {
        VideoGenerationRequest request = VideoGenerationRequest.of(prompt);
        return reactiveStrategy.generateVideo(restClient, request);
    }
    
    public CompletableFuture<VideoResult> generateVideoWithVirtualThreads(String prompt) {
        VideoGenerationRequest request = VideoGenerationRequest.of(prompt);
        return virtualThreadStrategy.generateVideo(restClient, request);
    }
    
    public String saveVideoToFile(VideoResult videoResult, String outputDirectory) throws IOException {
        Path outputDir = Paths.get(outputDirectory);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        
        Path outputPath = outputDir.resolve(videoResult.filename());
        
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            fos.write(videoResult.videoBytes());
        }
        
        return outputPath.toString();
    }
    
    public CompletableFuture<String> generateAndSaveVideo(String prompt, String strategy, String outputDirectory) {
        CompletableFuture<VideoResult> videoFuture = switch (strategy.toLowerCase()) {
            case "restclient" -> generateVideoWithRestClient(prompt);
            case "httpclient" -> generateVideoWithHttpClient(prompt);
            case "completablefuture" -> generateVideoWithCompletableFuture(prompt);
            case "scheduledexecutor" -> generateVideoWithScheduledExecutor(prompt);
            case "reactive" -> generateVideoWithReactive(prompt);
            case "virtualthread" -> generateVideoWithVirtualThreads(prompt);
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategy);
        };
        
        return videoFuture.thenApply(videoResult -> {
            try {
                return saveVideoToFile(videoResult, outputDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save video file", e);
            }
        });
    }
}