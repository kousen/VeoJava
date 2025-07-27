package com.kousenit.veojava.controller;

import com.kousenit.veojava.config.VeoClientConfig;
import com.kousenit.veojava.model.VeoJavaRecords.VideoPromptRequest;
import com.kousenit.veojava.service.VideoGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

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
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithRestClient(@Valid @RequestBody VideoPromptRequest request) {
        String prompt = request.prompt();
        return videoService.generateAndSaveVideo(prompt, "restclient", config.getOutput().directory())
                .thenApply(filePath -> {
                    Map<String, Object> resultMap = Map.of(
                            "strategy", "RestClient",
                            "prompt", prompt,
                            "filePath", filePath,
                            "status", "completed"
                    );
                    return ResponseEntity.ok(resultMap);
                })
                .exceptionally(throwable -> {
                    Map<String, Object> errorMap = Map.of(
                            "strategy", "RestClient",
                            "error", throwable.getMessage(),
                            "status", "failed"
                    );
                    return ResponseEntity.internalServerError().body(errorMap);
                });
    }
    
    @PostMapping("/generate/http-client")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithHttpClient(@Valid @RequestBody VideoPromptRequest request) {
        String prompt = request.prompt();
        return videoService.generateAndSaveVideo(prompt, "httpclient", config.getOutput().directory())
                .thenApply(filePath -> {
                    Map<String, Object> resultMap = Map.of(
                            "strategy", "HttpClient",
                            "prompt", prompt,
                            "filePath", filePath,
                            "status", "completed"
                    );
                    return ResponseEntity.ok(resultMap);
                })
                .exceptionally(throwable -> {
                    Map<String, Object> errorMap = Map.of(
                            "strategy", "HttpClient",
                            "error", throwable.getMessage(),
                            "status", "failed"
                    );
                    return ResponseEntity.internalServerError().body(errorMap);
                });
    }
    
    @PostMapping("/generate/self-scheduling")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithSelfScheduling(@Valid @RequestBody VideoPromptRequest request) {
        String prompt = request.prompt();
        return videoService.generateAndSaveVideo(prompt, "selfscheduling", config.getOutput().directory())
                .thenApply(filePath -> {
                    Map<String, Object> resultMap = Map.of(
                            "strategy", "SelfScheduling",
                            "prompt", prompt,
                            "filePath", filePath,
                            "status", "completed"
                    );
                    return ResponseEntity.ok(resultMap);
                })
                .exceptionally(throwable -> {
                    Map<String, Object> errorMap = Map.of(
                            "strategy", "SelfScheduling",
                            "error", throwable.getMessage(),
                            "status", "failed"
                    );
                    return ResponseEntity.internalServerError().body(errorMap);
                });
    }
    
    @PostMapping("/generate/fixed-rate")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithFixedRate(@Valid @RequestBody VideoPromptRequest request) {
        String prompt = request.prompt();
        return videoService.generateAndSaveVideo(prompt, "fixedrate", config.getOutput().directory())
                .thenApply(filePath -> {
                    Map<String, Object> resultMap = Map.of(
                            "strategy", "FixedRate",
                            "prompt", prompt,
                            "filePath", filePath,
                            "status", "completed"
                    );
                    return ResponseEntity.ok(resultMap);
                })
                .exceptionally(throwable -> {
                    Map<String, Object> errorMap = Map.of(
                            "strategy", "FixedRate",
                            "error", throwable.getMessage(),
                            "status", "failed"
                    );
                    return ResponseEntity.internalServerError().body(errorMap);
                });
    }
    
    @PostMapping("/generate/reactive")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithReactive(@Valid @RequestBody VideoPromptRequest request) {
        String prompt = request.prompt();
        return videoService.generateAndSaveVideo(prompt, "reactive", config.getOutput().directory())
                .thenApply(filePath -> {
                    Map<String, Object> resultMap = Map.of(
                            "strategy", "Reactive",
                            "prompt", prompt,
                            "filePath", filePath,
                            "status", "completed"
                    );
                    return ResponseEntity.ok(resultMap);
                })
                .exceptionally(throwable -> {
                    Map<String, Object> errorMap = Map.of(
                            "strategy", "Reactive",
                            "error", throwable.getMessage(),
                            "status", "failed"
                    );
                    return ResponseEntity.internalServerError().body(errorMap);
                });
    }
    
    @PostMapping("/generate/virtual-thread")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateWithVirtualThread(@Valid @RequestBody VideoPromptRequest request) {
        String prompt = request.prompt();
        return videoService.generateAndSaveVideo(prompt, "virtualthread", config.getOutput().directory())
                .thenApply(filePath -> {
                    Map<String, Object> resultMap = Map.of(
                            "strategy", "VirtualThread",
                            "prompt", prompt,
                            "filePath", filePath,
                            "status", "completed"
                    );
                    return ResponseEntity.ok(resultMap);
                })
                .exceptionally(throwable -> {
                    Map<String, Object> errorMap = Map.of(
                            "strategy", "VirtualThread",
                            "error", throwable.getMessage(),
                            "status", "failed"
                    );
                    return ResponseEntity.internalServerError().body(errorMap);
                });
    }
    
    @GetMapping("/strategies")
    public ResponseEntity<Map<String, Object>> getAvailableStrategies() {
        return ResponseEntity.ok(Map.of(
                "strategies", new String[]{
                        "RestClient", "HttpClient", "SelfScheduling", 
                        "FixedRate", "Reactive", "VirtualThread"
                },
                "endpoints", Map.of(
                        "RestClient", "/api/video/generate/rest-client",
                        "HttpClient", "/api/video/generate/http-client",
                        "SelfScheduling", "/api/video/generate/self-scheduling",
                        "FixedRate", "/api/video/generate/fixed-rate",
                        "Reactive", "/api/video/generate/reactive",
                        "VirtualThread", "/api/video/generate/virtual-thread"
                ),
                "configuration", Map.of(
                        "outputDirectory", config.getOutput().directory(),
                        "pollingInterval", config.getPolling().intervalSeconds() + "s",
                        "maxTimeout", config.getPolling().maxTimeoutMinutes() + "m"
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