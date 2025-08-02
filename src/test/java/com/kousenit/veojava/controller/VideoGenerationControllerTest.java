package com.kousenit.veojava.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kousenit.veojava.config.VeoClientConfig;
import com.kousenit.veojava.model.VeoJavaRecords.VideoPromptRequest;
import com.kousenit.veojava.service.VideoGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(VideoGenerationController.class)
class VideoGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VideoGenerationService mockVideoService;

    @MockitoBean
    private VeoClientConfig mockConfig;

    @MockitoBean
    private VeoClientConfig.Output mockOutput;

    @MockitoBean
    private VeoClientConfig.Polling mockPolling;

    @Test
    void testGenerateWithRestClient() throws Exception {
        // Given
        String prompt = "A test video prompt";
        String expectedFilePath = "/videos/test_video_123.mp4";
        VideoPromptRequest request = new VideoPromptRequest(prompt);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(prompt, "restclient", "./videos"))
                .willReturn(CompletableFuture.completedFuture(expectedFilePath));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/video/generate/rest-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        // Complete the async request and verify the response
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("RestClient")))
                .andExpect(jsonPath("$.prompt", is(prompt)))
                .andExpect(jsonPath("$.filePath", is(expectedFilePath)))
                .andExpect(jsonPath("$.status", is("completed")));
    }

    @Test
    void testGenerateWithHttpClient() throws Exception {
        // Given
        String prompt = "Another test video";
        String expectedFilePath = "/videos/test_video_456.mp4";
        VideoPromptRequest request = new VideoPromptRequest(prompt);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(prompt, "httpclient", "./videos"))
                .willReturn(CompletableFuture.completedFuture(expectedFilePath));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/video/generate/http-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("HttpClient")))
                .andExpect(jsonPath("$.prompt", is(prompt)))
                .andExpect(jsonPath("$.filePath", is(expectedFilePath)))
                .andExpect(jsonPath("$.status", is("completed")));
    }

    @Test
    void testGenerateWithSelfScheduling() throws Exception {
        // Given
        String prompt = "Self scheduling test";
        String expectedFilePath = "/videos/test_video_789.mp4";
        VideoPromptRequest request = new VideoPromptRequest(prompt);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(prompt, "selfscheduling", "./videos"))
                .willReturn(CompletableFuture.completedFuture(expectedFilePath));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/video/generate/self-scheduling")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("SelfScheduling")))
                .andExpect(jsonPath("$.prompt", is(prompt)))
                .andExpect(jsonPath("$.filePath", is(expectedFilePath)))
                .andExpect(jsonPath("$.status", is("completed")));
    }

    @Test
    void testGenerateWithFixedRate() throws Exception {
        // Given
        String prompt = "Fixed rate test";
        String expectedFilePath = "/videos/test_video_fixed.mp4";
        VideoPromptRequest request = new VideoPromptRequest(prompt);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(prompt, "fixedrate", "./videos"))
                .willReturn(CompletableFuture.completedFuture(expectedFilePath));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/video/generate/fixed-rate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("FixedRate")))
                .andExpect(jsonPath("$.prompt", is(prompt)))
                .andExpect(jsonPath("$.filePath", is(expectedFilePath)))
                .andExpect(jsonPath("$.status", is("completed")));
    }

    @Test
    void testGenerateWithReactive() throws Exception {
        // Given
        String prompt = "Reactive test";
        String expectedFilePath = "/videos/test_video_reactive.mp4";
        VideoPromptRequest request = new VideoPromptRequest(prompt);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(prompt, "reactive", "./videos"))
                .willReturn(CompletableFuture.completedFuture(expectedFilePath));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/video/generate/reactive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("Reactive")))
                .andExpect(jsonPath("$.prompt", is(prompt)))
                .andExpect(jsonPath("$.filePath", is(expectedFilePath)))
                .andExpect(jsonPath("$.status", is("completed")));
    }

    @Test
    void testGenerateWithVirtualThread() throws Exception {
        // Given
        String prompt = "Virtual thread test";
        String expectedFilePath = "/videos/test_video_virtual.mp4";
        VideoPromptRequest request = new VideoPromptRequest(prompt);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(prompt, "virtualthread", "./videos"))
                .willReturn(CompletableFuture.completedFuture(expectedFilePath));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/video/generate/virtual-thread")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("VirtualThread")))
                .andExpect(jsonPath("$.prompt", is(prompt)))
                .andExpect(jsonPath("$.filePath", is(expectedFilePath)))
                .andExpect(jsonPath("$.status", is("completed")));
    }

    @Test
    void testGenerateWithRestClientError() throws Exception {
        // Given
        String prompt = "Error test prompt";
        VideoPromptRequest request = new VideoPromptRequest(prompt);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(prompt, "restclient", "./videos"))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("Video generation failed")));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/video/generate/rest-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.strategy", is("RestClient")))
                .andExpect(jsonPath("$.error", containsString("Video generation failed")))
                .andExpect(jsonPath("$.status", is("failed")));
    }

    @Test
    void testGenerateWithHttpClientError() throws Exception {
        // Given
        String prompt = "Error test prompt";
        VideoPromptRequest request = new VideoPromptRequest(prompt);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(prompt, "httpclient", "./videos"))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("HTTP client error")));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/video/generate/http-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.strategy", is("HttpClient")))
                .andExpect(jsonPath("$.error", containsString("HTTP client error")))
                .andExpect(jsonPath("$.status", is("failed")));
    }

    @Test
    void testGetAvailableStrategies() throws Exception {
        // Given
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockConfig.getPolling()).willReturn(mockPolling);
        given(mockOutput.directory()).willReturn("./test-videos");
        given(mockPolling.intervalSeconds()).willReturn(5);
        given(mockPolling.maxTimeoutMinutes()).willReturn(10);

        // When & Then
        mockMvc.perform(get("/api/video/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategies", hasSize(6)))
                .andExpect(jsonPath("$.strategies", hasItems("RestClient", "HttpClient", "SelfScheduling", "FixedRate", "Reactive", "VirtualThread")))
                .andExpect(jsonPath("$.endpoints['RestClient']", is("/api/video/generate/rest-client")))
                .andExpect(jsonPath("$.endpoints['HttpClient']", is("/api/video/generate/http-client")))
                .andExpect(jsonPath("$.endpoints['SelfScheduling']", is("/api/video/generate/self-scheduling")))
                .andExpect(jsonPath("$.endpoints['FixedRate']", is("/api/video/generate/fixed-rate")))
                .andExpect(jsonPath("$.endpoints['Reactive']", is("/api/video/generate/reactive")))
                .andExpect(jsonPath("$.endpoints['VirtualThread']", is("/api/video/generate/virtual-thread")))
                .andExpect(jsonPath("$.configuration.outputDirectory", is("./test-videos")))
                .andExpect(jsonPath("$.configuration.pollingInterval", is("5s")))
                .andExpect(jsonPath("$.configuration.maxTimeout", is("10m")));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/video/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("Veo Video Generation")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @SuppressWarnings("JsonStandardCompliance")
    @Test
    void testInvalidRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/video/generate/rest-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/video/generate/rest-client")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEmptyPrompt() throws Exception {
        // Given
        VideoPromptRequest request = new VideoPromptRequest("");

        // When & Then - empty prompts should be rejected by validation
        mockMvc.perform(post("/api/video/generate/rest-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testNullPrompt() throws Exception {
        // Given
        String requestJson = "{\"prompt\": null}";

        // When & Then
        mockMvc.perform(post("/api/video/generate/rest-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLongPrompt() throws Exception {
        // Given - create a very long prompt
        String longPrompt = "A very long prompt that contains ".repeat(100) + "lots of text";
        VideoPromptRequest request = new VideoPromptRequest(longPrompt);
        String expectedFilePath = "/videos/long_prompt_video.mp4";
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(longPrompt, "restclient", "./videos"))
                .willReturn(CompletableFuture.completedFuture(expectedFilePath));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/video/generate/rest-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt", is(longPrompt)))
                .andExpect(jsonPath("$.filePath", is(expectedFilePath)));
    }

    @Test
    void testConcurrentRequests() throws Exception {
        // Given
        String prompt1 = "First video prompt";
        String prompt2 = "Second video prompt";
        VideoPromptRequest request1 = new VideoPromptRequest(prompt1);
        VideoPromptRequest request2 = new VideoPromptRequest(prompt2);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        given(mockVideoService.generateAndSaveVideo(prompt1, "restclient", "./videos"))
                .willReturn(CompletableFuture.completedFuture("/videos/video1.mp4"));
        given(mockVideoService.generateAndSaveVideo(prompt2, "httpclient", "./videos"))
                .willReturn(CompletableFuture.completedFuture("/videos/video2.mp4"));

        // When & Then - both requests should succeed
        MvcResult result1 = mockMvc.perform(post("/api/video/generate/rest-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt", is(prompt1)));

        MvcResult result2 = mockMvc.perform(post("/api/video/generate/http-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt", is(prompt2)));
    }

    @Test
    void testUnsupportedHttpMethod() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/video/generate/rest-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"prompt": "test"}
                    """))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testInvalidContentType() throws Exception {
        // Given
        VideoPromptRequest request = new VideoPromptRequest("test prompt");

        // When & Then
        mockMvc.perform(post("/api/video/generate/rest-client")
                .contentType(MediaType.TEXT_PLAIN)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testAllStrategiesWithSamePrompt() throws Exception {
        // Given
        String prompt = "Universal test prompt";
        VideoPromptRequest request = new VideoPromptRequest(prompt);
        
        given(mockConfig.getOutput()).willReturn(mockOutput);
        given(mockOutput.directory()).willReturn("./videos");
        
        // Mock all strategies
        given(mockVideoService.generateAndSaveVideo(prompt, "restclient", "./videos"))
                .willReturn(CompletableFuture.completedFuture("/videos/restclient.mp4"));
        given(mockVideoService.generateAndSaveVideo(prompt, "httpclient", "./videos"))
                .willReturn(CompletableFuture.completedFuture("/videos/httpclient.mp4"));
        given(mockVideoService.generateAndSaveVideo(prompt, "selfscheduling", "./videos"))
                .willReturn(CompletableFuture.completedFuture("/videos/selfscheduling.mp4"));
        given(mockVideoService.generateAndSaveVideo(prompt, "fixedrate", "./videos"))
                .willReturn(CompletableFuture.completedFuture("/videos/fixedrate.mp4"));
        given(mockVideoService.generateAndSaveVideo(prompt, "reactive", "./videos"))
                .willReturn(CompletableFuture.completedFuture("/videos/reactive.mp4"));
        given(mockVideoService.generateAndSaveVideo(prompt, "virtualthread", "./videos"))
                .willReturn(CompletableFuture.completedFuture("/videos/virtualthread.mp4"));

        // Test all endpoints
        String[] endpoints = {
                "/api/video/generate/rest-client",
                "/api/video/generate/http-client",
                "/api/video/generate/self-scheduling",
                "/api/video/generate/fixed-rate",
                "/api/video/generate/reactive",
                "/api/video/generate/virtual-thread"
        };

        String[] expectedStrategies = {
                "RestClient", "HttpClient", "SelfScheduling", 
                "FixedRate", "Reactive", "VirtualThread"
        };

        for (int i = 0; i < endpoints.length; i++) {
            MvcResult result = mockMvc.perform(post(endpoints[i])
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted())
                    .andReturn();
            
            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategy", is(expectedStrategies[i])))
                    .andExpect(jsonPath("$.prompt", is(prompt)))
                    .andExpect(jsonPath("$.status", is("completed")));
        }
    }
}