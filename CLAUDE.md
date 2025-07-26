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
- **Async Patterns**: SelfScheduling, FixedRate, VirtualThread, and Reactive polling strategies

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
│   ├── PollingStrategy.java (sealed interface)
│   ├── SelfSchedulingPollingStrategy.java
│   ├── FixedRatePollingStrategy.java
│   ├── VirtualThreadPollingStrategy.java
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
1. **SelfSchedulingPollingStrategy** — Reschedules after each check (dynamic timing)
2. **FixedRatePollingStrategy** — Fixed-rate periodic checks with ScheduledExecutorService
3. **VirtualThreadPollingStrategy** — Uses virtual threads for lightweight blocking operations (Java 21+)
4. **ReactivePollingStrategy** — Flux.interval with clean polling patterns (no exception-based control flow)

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
7. **Educational content**: When updating implementations, also update corresponding sections in tutorial materials
8. **Cost awareness**: Remember each test video costs ~$6, design tests accordingly

## API Endpoints

REST endpoints for testing all approaches:
- POST `/api/video/generate/rest-client`
- POST `/api/video/generate/http-client`
- POST `/api/video/generate/self-scheduling`
- POST `/api/video/generate/fixed-rate`
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

## Key Insights

### Polling Strategy Comparison
This project demonstrates **four production-ready alternatives to busy waiting** for long-running operations:

1. **ScheduledExecutorService + CompletableFuture** (2 variants):
   - `SelfSchedulingPollingStrategy`: Dynamic rate - reschedules after each check
   - `FixedRatePollingStrategy`: Fixed rate - checks every 5 seconds regardless

2. **Reactive Approach**: Uses `Flux.interval` for time-based polling with backpressure

3. **Virtual Threads**: Simple blocking code that scales efficiently (Java 21+)

4. **Basic Async**: HttpClient/RestClient with CompletableFuture (good for simple cases)

All approaches avoid CPU-wasting busy loops while providing different trade-offs in complexity, resource usage, and debugging ease.

### Decision Matrix for Developers
| If You Have... | Use This Strategy |
|---|---|
| Java 8-20 + Simple needs | ScheduledExecutor |
| Java 8-20 + High concurrency | Reactive Streams |
| Java 21+ | Virtual Threads |
| Existing reactive codebase | Reactive Streams |
| Need simplest code | Virtual Threads |
| Legacy constraints | ScheduledExecutor |

### Educational Purpose: From Naive to Enterprise
This project demonstrates the evolution from simple to sophisticated:

**Naive Approach (Don't Use in Production)**:
```java
while (!status.done()) {
    Thread.sleep(5000); // Blocks precious threads!
    status = checkStatus(id);
}
```

**Enterprise Solutions**:
1. **ScheduledExecutorService** - Non-blocking, resource-efficient
2. **Reactive Streams** - Declarative, composable, high-concurrency
3. **Virtual Threads** - Simple code, massive scalability (Java 21+)

The Java implementations showcase enterprise-grade patterns suitable for:
- Production web applications with concurrent requests
- Resource-efficient handling of multiple simultaneous operations
- Integration with Spring's ecosystem and observability tools
- Library code for different deployment contexts

### Performance Characteristics
- **Busy Wait**: ~200 concurrent operations max
- **ScheduledExecutor**: ~10,000 concurrent operations
- **Reactive Streams**: ~100,000+ concurrent operations
- **Virtual Threads**: ~1,000,000+ concurrent operations

## Educational Content

This project includes comprehensive tutorial materials for teaching Java async patterns:

### YouTube Tutorial Materials
- **[script.md](script.md)** - Complete 10-12 minute YouTube video script covering all 5 polling strategies
- **[polling-strategy-comparison.md](polling-strategy-comparison.md)** - Detailed comparison matrix with performance metrics, complexity analysis, and decision guidelines
- **[mermaid-examples.md](mermaid-examples.md)** - Professional diagrams including sequence flows, decision trees, and architecture overviews

### Technical Documentation
- **[NETWORKING_STRATEGY_ANALYSIS.md](NETWORKING_STRATEGY_ANALYSIS.md)** - Comprehensive analysis and recommendations for different approaches
- **[API Implementation Notes](API_IMPLEMENTATION_NOTES.md)** - Comprehensive technical documentation of API behavior, redirect handling, and response structure discoveries

This project serves as both a working example of Google Veo 3 integration and a comprehensive educational resource for Java developers learning modern async patterns, from naive busy-waiting to enterprise-grade solutions using virtual threads and reactive streams.