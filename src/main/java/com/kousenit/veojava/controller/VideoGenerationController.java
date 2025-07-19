package com.kousenit.veojava.controller;

import com.kousenit.veojava.config.VeoClientConfig;
import com.kousenit.veojava.service.VideoGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/video")
public class VideoGenerationController {
    
    private final VideoGenerationService videoService;
    private final VeoClientConfig config;
    
    public VideoGenerationController(VideoGenerationService videoService, VeoClientConfig config) {
        this.videoService = videoService;
        this.config = config;
    }
    
    @PostMapping("/generate/rest-client")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithRestClient(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        return videoService.generateAndSaveVideo(prompt, "restclient", config.getOutput().getDirectory())
                .thenApply(filePath -> ResponseEntity.ok(Map.of(
                        "strategy", "RestClient",
                        "prompt", prompt,
                        "filePath", filePath,
                        "status", "completed"
                )))
                .exceptionally(throwable -> ResponseEntity.internalServerError().body(Map.of(
                        "strategy", "RestClient",
                        "error", throwable.getMessage(),
                        "status", "failed"
                )));
    }
    
    @PostMapping("/generate/http-client")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithHttpClient(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        return videoService.generateAndSaveVideo(prompt, "httpclient", config.getOutput().getDirectory())
                .thenApply(filePath -> ResponseEntity.ok(Map.of(
                        "strategy", "HttpClient",
                        "prompt", prompt,
                        "filePath", filePath,
                        "status", "completed"
                )))
                .exceptionally(throwable -> ResponseEntity.internalServerError().body(Map.of(
                        "strategy", "HttpClient",
                        "error", throwable.getMessage(),
                        "status", "failed"
                )));
    }
    
    @PostMapping("/generate/completable-future")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithCompletableFuture(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        return videoService.generateAndSaveVideo(prompt, "completablefuture", config.getOutput().getDirectory())
                .thenApply(filePath -> ResponseEntity.ok(Map.of(
                        "strategy", "CompletableFuture",
                        "prompt", prompt,
                        "filePath", filePath,
                        "status", "completed"
                )))
                .exceptionally(throwable -> ResponseEntity.internalServerError().body(Map.of(
                        "strategy", "CompletableFuture",
                        "error", throwable.getMessage(),
                        "status", "failed"
                )));
    }
    
    @PostMapping("/generate/scheduled-executor")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithScheduledExecutor(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        return videoService.generateAndSaveVideo(prompt, "scheduledexecutor", config.getOutput().getDirectory())
                .thenApply(filePath -> ResponseEntity.ok(Map.of(
                        "strategy", "ScheduledExecutor",
                        "prompt", prompt,
                        "filePath", filePath,
                        "status", "completed"
                )))
                .exceptionally(throwable -> ResponseEntity.internalServerError().body(Map.of(
                        "strategy", "ScheduledExecutor",
                        "error", throwable.getMessage(),
                        "status", "failed"
                )));
    }
    
    @PostMapping("/generate/reactive")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithReactive(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        return videoService.generateAndSaveVideo(prompt, "reactive", config.getOutput().getDirectory())
                .thenApply(filePath -> ResponseEntity.ok(Map.of(
                        "strategy", "Reactive",
                        "prompt", prompt,
                        "filePath", filePath,
                        "status", "completed"
                )))
                .exceptionally(throwable -> ResponseEntity.internalServerError().body(Map.of(
                        "strategy", "Reactive",
                        "error", throwable.getMessage(),
                        "status", "failed"
                )));
    }
    
    @PostMapping("/generate/virtual-thread")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithVirtualThread(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        return videoService.generateAndSaveVideo(prompt, "virtualthread", config.getOutput().getDirectory())
                .thenApply(filePath -> ResponseEntity.ok(Map.of(
                        "strategy", "VirtualThread",
                        "prompt", prompt,
                        "filePath", filePath,
                        "status", "completed"
                )))
                .exceptionally(throwable -> ResponseEntity.internalServerError().body(Map.of(
                        "strategy", "VirtualThread",
                        "error", throwable.getMessage(),
                        "status", "failed"
                )));
    }
    
    @GetMapping("/strategies")
    public ResponseEntity<Map<String, Object>> getAvailableStrategies() {
        return ResponseEntity.ok(Map.of(
                "strategies", new String[]{
                        "RestClient", "HttpClient", "CompletableFuture", 
                        "ScheduledExecutor", "Reactive", "VirtualThread"
                },
                "endpoints", Map.of(
                        "RestClient", "/api/video/generate/rest-client",
                        "HttpClient", "/api/video/generate/http-client",
                        "CompletableFuture", "/api/video/generate/completable-future",
                        "ScheduledExecutor", "/api/video/generate/scheduled-executor",
                        "Reactive", "/api/video/generate/reactive",
                        "VirtualThread", "/api/video/generate/virtual-thread"
                ),
                "configuration", Map.of(
                        "outputDirectory", config.getOutput().getDirectory(),
                        "pollingInterval", config.getPolling().getIntervalSeconds() + "s",
                        "maxTimeout", config.getPolling().getMaxTimeoutMinutes() + "m"
                )
        ));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Veo Video Generation",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}