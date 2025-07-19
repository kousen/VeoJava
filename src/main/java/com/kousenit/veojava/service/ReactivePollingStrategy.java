package com.kousenit.veojava.service;

import com.kousenit.veojava.client.ReactiveVeoVideoClient;
import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public final class ReactivePollingStrategy implements PollingStrategy {
    
    private final ReactiveVeoVideoClient reactiveClient;
    
    public ReactivePollingStrategy(ReactiveVeoVideoClient reactiveClient) {
        this.reactiveClient = reactiveClient;
    }
    
    @Override
    public CompletableFuture<VideoResult> generateVideo(VeoVideoClient client, VideoGenerationRequest request) {
        return reactiveClient.generateVideoReactive(request).toFuture();
    }
    
    @Override
    public String getStrategyName() {
        return "Reactive";
    }
}