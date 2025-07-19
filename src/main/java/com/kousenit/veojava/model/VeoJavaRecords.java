package com.kousenit.veojava.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class VeoJavaRecords {

    // Simple request DTO for REST endpoints
    public record VideoPromptRequest(String prompt) {}

    public record VideoGenerationRequest(
            List<Instance> instances,
            Parameters parameters
    ) {
        public record Instance(String prompt) {}
        
        public record Parameters(
                String aspectRatio,
                String personGeneration
        ) {
            public Parameters() {
                this("16:9", "allow_all");
            }
        }
        
        public static VideoGenerationRequest of(String prompt) {
            return new VideoGenerationRequest(
                    List.of(new Instance(prompt)),
                    new Parameters()
            );
        }
    }

    public record VideoGenerationResponse(
            @JsonProperty("name") String operationId,
            @JsonProperty("metadata") Map<String, Object> metadata
    ) {}

    public record OperationStatus(
            @JsonProperty("name") String operationId,
            @JsonProperty("done") boolean done,
            @JsonProperty("error") ErrorInfo error,
            @JsonProperty("response") OperationResponse response,
            @JsonProperty("metadata") Map<String, Object> metadata
    ) {
        public record ErrorInfo(
                int code,
                String message,
                List<Map<String, Object>> details
        ) {}
        
        public record OperationResponse(
                @JsonProperty("@type") String type,
                @JsonProperty("generateVideoResponse") GenerateVideoResponse generateVideoResponse
        ) {}
        
        public record GenerateVideoResponse(
                @JsonProperty("generatedSamples") List<GeneratedSample> generatedSamples,
                @JsonProperty("raiMediaFilteredReasons") List<String> raiMediaFilteredReasons
        ) {}
        
        public record GeneratedSample(
                @JsonProperty("video") VideoReference video
        ) {}
        
        public record VideoReference(
                @JsonProperty("uri") String uri
        ) {}
        
    }

    public record VideoResult(
            String videoBase64,
            String mimeType,
            String filename,
            byte[] videoBytes
    ) {}

    public record ErrorResponse(
            int code,
            String message,
            String status
    ) {}
}