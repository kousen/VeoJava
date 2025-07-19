package com.kousenit.veojava.client;

import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationRequest;
import com.kousenit.veojava.model.VeoJavaRecords.VideoGenerationResponse;
import com.kousenit.veojava.model.VeoJavaRecords.OperationStatus;
import com.kousenit.veojava.model.VeoJavaRecords.VideoResult;

import java.util.concurrent.CompletableFuture;

public interface VeoVideoClient {
    
    VideoGenerationResponse submitVideoGeneration(VideoGenerationRequest request);
    
    OperationStatus checkOperationStatus(String operationId);
    
    VideoResult downloadVideo(String operationId);
    
    default CompletableFuture<VideoResult> generateVideoAsync(VideoGenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            VideoGenerationResponse response = submitVideoGeneration(request);
            String operationId = response.operationId();
            
            OperationStatus status;
            do {
                try {
                    // This is "busy waiting", blocking a platform thread
                    // That's why we show all the other alternatives in this project
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for video generation", e);
                }
                status = checkOperationStatus(operationId);
            } while (!status.done());
            
            if (status.error() != null) {
                throw new RuntimeException("Video generation failed: " + status.error().message());
            }
            
            return downloadVideo(operationId);
        });
    }
}