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
import java.util.concurrent.TimeUnit;

@Component
public final class SelfSchedulingPollingStrategy implements PollingStrategy {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    @Override
    public CompletableFuture<VideoResult> generateVideo(VeoVideoClient client, VideoGenerationRequest request) {
        return CompletableFuture
                .supplyAsync(() -> client.submitVideoGeneration(request))
                .thenCompose(response -> pollForCompletion(client, response.operationId()))
                .thenApply(client::downloadVideo);
    }

    private CompletableFuture<String> pollForCompletion(VeoVideoClient client, String operationId) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Runnable pollTask = new Runnable() {
            @Override
            public void run() {
                if (future.isDone()) return;
                try {
                    OperationStatus status = client.checkOperationStatus(operationId);

                    if (!status.done()) {
                        scheduler.schedule(this, 5, TimeUnit.SECONDS);
                        return;
                    }

                    if (status.error() != null) {
                        future.completeExceptionally(
                                new RuntimeException(
                                        "Video generation failed for operation %s: %s"
                                                .formatted(operationId, status.error().message())));
                        return;
                    }

                    future.complete(operationId);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        };

        scheduler.schedule(pollTask, 0, TimeUnit.SECONDS);
        return future.orTimeout(10, TimeUnit.MINUTES);
    }
    
    @Override
    public String getStrategyName() {
        return "SelfScheduling";
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