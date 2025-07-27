package com.kousenit.veojava.service;

import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public final class FixedRatePollingStrategy implements PollingStrategy {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    
    @Override
    public CompletableFuture<VideoResult> generateVideo(VeoVideoClient client, VideoGenerationRequest request) {
        CompletableFuture<VideoResult> future = new CompletableFuture<>();
        
        // Submit initial request
        CompletableFuture.supplyAsync(() -> client.submitVideoGeneration(request))
                .thenAccept(response -> startPolling(client, response.operationId(), future))
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });
        
        return future;
    }
    
    private void startPolling(VeoVideoClient client, String operationId, CompletableFuture<VideoResult> future) {
        AtomicInteger attempts = new AtomicInteger(0);
        int maxAttempts = 120; // 10 minutes with 5-second intervals
        
        ScheduledFuture<?> pollingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (attempts.incrementAndGet() > maxAttempts) {
                    future.completeExceptionally(new TimeoutException("Video generation timed out"));
                    return;
                }
                
                OperationStatus status = client.checkOperationStatus(operationId);
                
                if (status.done()) {
                    if (status.error() != null) {
                        future.completeExceptionally(
                            new RuntimeException("Video generation failed: " + status.error().message())
                        );
                    } else {
                        VideoResult result = client.downloadVideo(operationId);
                        future.complete(result);
                    }
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        // Cancel polling when future completes
        future.whenComplete((_, _) -> pollingTask.cancel(false));
    }
    
    @Override
    public String getStrategyName() {
        return "FixedRate";
    }
    
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException _) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}