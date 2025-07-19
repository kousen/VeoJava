# Claude Development Context

This file contains important context for Claude Code to understand this project and provide better assistance.

## Project Overview

**VeoJava** is a Spring Boot application that demonstrates multiple Java approaches for integrating with Google's Gemini Veo 3 video generation API. It showcases different HTTP client implementations, async polling strategies, and reactive programming patterns.

## Key Requirements & Constraints

- **Java 24+** with Gradle build system
- **API Key**: Requires `GEMINI_API_KEY` environment variable
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
gemini.api.key=${GEMINI_API_KEY}
veo.polling.interval-seconds=5
veo.polling.max-timeout-minutes=10
veo.output.directory=./videos
```

## Important Implementation Details

### API Integration
- **Base URL**: `https://generativelanguage.googleapis.com/v1beta`
- **Submit**: `/models/veo-3.0-generate-preview:predictLongRunning`
- **Poll**: `/operations/{operationId}`
- **Authentication**: `x-goog-api-key` header

### Video Generation Process
1. Submit video generation request
2. Poll operation status every 5 seconds
3. Download completed video (Base64 encoded)
4. Decode and save to file system

### Record Design Pattern
All DTOs are nested records in `VeoJavaRecords` class:
- `VideoGenerationRequest` with factory method `of(prompt)`
- `VideoGenerationResponse`
- `OperationStatus` with nested error/response records
- `VideoResult`

### Client Implementations
1. **RestClientVeoVideoClient** — Spring RestClient with autoconfigured Jackson
2. **HttpClientVeoVideoClient** — Java HttpClient with explicit ObjectMapper
3. **ReactiveVeoVideoClient** — WebClient with Mono/Flux patterns

### Polling Strategies
1. **CompletableFuturePollingStrategy** — Chains futures with delays
2. **ScheduledExecutorPollingStrategy** — Periodic checks with ScheduledExecutorService
3. **VirtualThreadPollingStrategy** — Uses virtual threads for lightweight blocking operations
4. **ReactivePollingStrategy** — Flux.interval with retry logic

## Testing

- Unit tests for records and basic functionality
- Modern Java features demonstration (sealed interfaces, pattern matching, virtual threads, unnamed variables, stream gatherers)
- Integration tests require valid API key (marked with `@EnabledIfEnvironmentVariable`)
- Tests avoid actual API calls to prevent quota usage

## Common Development Tasks

When making changes:
1. Always run `./gradlew test` before committing
2. Check that GEMINI_API_KEY is set for integration tests
3. Use static imports for VeoJavaRecords: `import static com.kousenit.veojava.model.VeoJavaRecords.*;`
4. Follow existing patterns for error handling and async operations

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

## Known Limitations

- **Veo 3 API**: 8-second videos, 720p@24fps, English prompts only
- **Paid Feature**: Requires billing enabled on Google Cloud
- **Quota**: API calls consume quota and cost money
- **Storage**: Videos stored server-side for 2 days only

This project serves as a comprehensive example of different Java patterns for REST API integration with long-running operations.