package com.kousenit.veojava.service;

import com.kousenit.veojava.client.ReactiveVeoVideoClient;
import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Component
public final class ReactivePollingStrategy implements PollingStrategy {
    
    @Autowired
    private ReactiveVeoVideoClient reactiveClient;
    
    @Override
    public CompletableFuture<VideoResult> generateVideo(VeoVideoClient client, VideoGenerationRequest request) {
        return reactiveClient.generateVideoReactive(request).toFuture();
    }
    
    public Mono<VideoResult> generateVideoReactive(VideoGenerationRequest request) {
        return reactiveClient.submitVideoGeneration(request)
                .flatMap(response -> pollUntilCompleteReactive(response.operationId()))
                .flatMap(reactiveClient::downloadVideo);
    }
    
    private Mono<String> pollUntilCompleteReactive(String operationId) {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(5))
                .flatMap(tick -> reactiveClient.checkOperationStatus(operationId))
                .filter(VeoJavaRecords.OperationStatus::done)
                .next()
                .map(status -> {
                    if (status.error() != null) {
                        throw new RuntimeException("Video generation failed: " + status.error().message());
                    }
                    return operationId;
                })
                .timeout(Duration.ofMinutes(10));
    }
    
    @Override
    public String getStrategyName() {
        return "Reactive";
    }
}