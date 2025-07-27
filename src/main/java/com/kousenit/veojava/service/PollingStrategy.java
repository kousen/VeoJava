package com.kousenit.veojava.service;

import com.kousenit.veojava.client.VeoVideoClient;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("java:S1200")
public sealed interface PollingStrategy 
    permits SelfSchedulingPollingStrategy, FixedRatePollingStrategy, ReactivePollingStrategy, VirtualThreadPollingStrategy {
    
    CompletableFuture<VideoResult> generateVideo(VeoVideoClient client, VideoGenerationRequest request);
    
    String getStrategyName();
}