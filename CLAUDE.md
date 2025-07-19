# Claude Development Context

This file contains important context for Claude Code to understand this project and provide better assistance.

## Project Overview

**VeoJava** is a Spring Boot application that demonstrates multiple Java approaches for integrating with Google's Gemini Veo 3 video generation API. It showcases different HTTP client implementations, async polling strategies, and reactive programming patterns.

## Key Requirements & Constraints

- **Java 24+** with Gradle build system
- **API Key**: Requires `GOOGLEAI_API_KEY` or `GEMINI_API_KEY` environment variable
- **Veo 3 Access**: Controlled access API requiring approval from Google
- **Record Organization**: All DTOs are in a single `VeoJavaRecords` class for easy static imports
- **Multiple Client Approaches**: RestClient, HttpClient, and WebClient implementations
- **Async Patterns**: CompletableFuture, ScheduledExecutor, and Reactive polling strategies

## Build & Test Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application
./gradlew bootRun

# Run checks (includes tests, formatting, etc.)
./gradlew check

# Create JAR
./gradlew bootJar
```

## Project Structure

```
src/main/java/com/kousenit/veojava/
├── client/           # HTTP client implementations
│   ├── VeoVideoClient.java (interface)
│   ├── RestClientVeoVideoClient.java
│   ├── HttpClientVeoVideoClient.java
│   └── ReactiveVeoVideoClient.java
├── config/           # Spring configuration
│   └── VeoClientConfig.java
├── controller/       # REST endpoints
│   └── VideoGenerationController.java
├── model/            # Data models
│   └── VeoJavaRecords.java (all DTOs here)
├── service/          # Business logic and polling strategies
│   ├── VideoGenerationService.java
│   ├── PollingStrategy.java (interface)
│   ├── CompletableFuturePollingStrategy.java
│   ├── ScheduledExecutorPollingStrategy.java
│   └── ReactivePollingStrategy.java
└── VeoVideoDemo.java # Standalone demo
```

## Key Dependencies

- `spring-boot-starter-web` - Includes RestClient and Jackson
- `spring-boot-starter-webflux` - For reactive WebClient
- Jackson is already included via web starter (no need to add explicitly)

## Configuration

Main configuration in `application.properties`:

```properties
# API Configuration - checks GOOGLEAI_API_KEY first, then GEMINI_API_KEY
gemini.api.key=${GOOGLEAI_API_KEY:${GEMINI_API_KEY}}
veo.polling.interval-seconds=5
veo.polling.max-timeout-minutes=10
veo.output.directory=./videos

# Async Configuration - video generation takes 2-4 minutes
spring.mvc.async.request-timeout=600000
```

## Important Implementation Details

### API Integration
- **Base URL**: `https://generativelanguage.googleapis.com/v1beta`
- **Submit**: `/models/veo-3.0-generate-preview:predictLongRunning`
- **Poll**: `/operations/{operationId}`
- **Authentication**: `x-goog-api-key` header

### Video Generation Process
1. **Submit** video generation request → Get operation ID  
2. **Poll** operation status every 5 seconds → Wait for `done: true`
3. **Extract** download URL from response → Get file URI with redirect
4. **Download** video file → Follow 302 redirects to actual content
5. **Save** to local file system → Write binary data to MP4 file

**Critical Discovery**: Veo 3 API returns download URLs instead of base64-encoded data, requiring proper redirect handling in all HTTP clients.

### Record Design Pattern
All DTOs are nested records in `VeoJavaRecords` class:
- `VideoGenerationRequest` with factory method `of(prompt)`
- `VideoGenerationResponse`
- `OperationStatus` with `GenerateVideoResponse` → `GeneratedSample` → `VideoReference` structure
- `VideoResult`

**Updated**: Response structure changed to match actual Veo 3 API format with `generateVideoResponse.generatedSamples[].video.uri`

### Client Implementations
1. **RestClientVeoVideoClient** — Spring RestClient with custom redirect handling via request factory
2. **HttpClientVeoVideoClient** — Java HttpClient with `.followRedirects(NORMAL)`
3. **ReactiveVeoVideoClient** — WebClient with Reactor Netty redirect support + 2MB buffer

**All clients updated** to handle 302 redirects for video file downloads and increased buffer limits for large files.

### Polling Strategies
1. **CompletableFuturePollingStrategy** — Chains futures with delays
2. **ScheduledExecutorPollingStrategy** — Periodic checks with ScheduledExecutorService
3. **VirtualThreadPollingStrategy** — Uses virtual threads for lightweight blocking operations
4. **ReactivePollingStrategy** — Flux.interval with retry logic

## Testing

- Unit tests for records and basic functionality
- Modern Java features demonstration (sealed interfaces, pattern matching, virtual threads, unnamed variables, stream gatherers)
- Integration tests marked `@Disabled` by default to prevent accidental $6 charges
- Tests require valid API key with Veo 3 access (marked with `@EnabledIfEnvironmentVariable`)
- Redirect handling tests verify all HTTP clients work with Google's file service

## Common Development Tasks

When making changes:
1. Always run `./gradlew test` before committing
2. Check that `GOOGLEAI_API_KEY` or `GEMINI_API_KEY` is set for integration tests
3. Use static imports for VeoJavaRecords: `import static com.kousenit.veojava.model.VeoJavaRecords.*;`
4. Follow existing patterns for error handling and async operations
5. **Redirect handling**: Ensure all HTTP clients follow 302 redirects for video downloads
6. **Buffer limits**: Configure adequate buffer sizes for video files (2MB+ for WebClient)

## API Endpoints

REST endpoints for testing all approaches:
- POST `/api/video/generate/rest-client`
- POST `/api/video/generate/http-client`
- POST `/api/video/generate/completable-future`
- POST `/api/video/generate/scheduled-executor`
- POST `/api/video/generate/virtual-thread`
- POST `/api/video/generate/reactive`
- GET `/api/video/strategies`
- GET `/api/video/health`

## Known Limitations & Requirements

- **Veo 3 API**: 8-second videos, 720p@24fps, English prompts only
- **Cost**: $0.75/second (~$6 per 8-second video)
- **Access**: Controlled access requiring Google approval
- **Redirect handling**: All HTTP clients must follow 302 redirects
- **Buffer limits**: Videos (~635KB) exceed default WebClient buffers (256KB)
- **Content filtering**: Audio may be blocked while video succeeds
- **RAI policies**: Generic prompts more likely to trigger content filters
- **Async timeout**: REST endpoints require 10-minute timeout due to 2-4 minute generation time
- **Constructor patterns**: Multiple constructors needed for Spring DI vs standalone demo usage

## Additional Documentation

- **[API Implementation Notes](API_IMPLEMENTATION_NOTES.md)** - Comprehensive technical documentation of API behavior, redirect handling, and response structure discoveries

This project serves as a comprehensive example of different Java patterns for REST API integration with long-running operations, including important lessons about handling large media file delivery via URL-based downloads with redirect requirements.