package com.kousenit.veojava.service;

import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public final class VirtualThreadPollingStrategy implements PollingStrategy {
    
    // Virtual thread executor - extremely lightweight for I/O bound tasks
    private static final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    @Override
    public CompletableFuture<VideoResult> generateVideo(VeoVideoClient client, VideoGenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Submit initial request
                VideoGenerationResponse response = client.submitVideoGeneration(request);
                String operationId = response.operationId();
                
                // Poll for completion using virtual threads - no need to worry about thread pool size!
                OperationStatus status;
                do {
                    // Virtual threads can block without issues - no need for complex async patterns
                    Thread.sleep(Duration.ofSeconds(5));
                    status = client.checkOperationStatus(operationId);
                } while (!status.done());
                
                if (status.error() != null) {
                    throw new RuntimeException("Video generation failed: " + status.error().message());
                }
                
                return client.downloadVideo(operationId);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for video generation", e);
            }
        }, virtualExecutor);
    }
    
    @Override
    public String getStrategyName() {
        return "VirtualThread";
    }
    
    @PreDestroy
    public void shutdown() {
        virtualExecutor.shutdown();
        try {
            if (!virtualExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                virtualExecutor.shutdownNow();
            }
        } catch (InterruptedException _) {
            virtualExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}