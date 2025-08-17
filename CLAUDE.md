# Claude Development Context

This file contains important context for Claude Code to understand this project and provide better assistance.

## Project Overview

**VeoJava** is a Spring Boot application that demonstrates multiple Java approaches for integrating with Google's Gemini Veo 3 video generation API. It showcases different HTTP client implementations, async polling strategies, and reactive programming patterns.

## Key Requirements & Constraints

- **Java 24** with Gradle build system and preview features enabled
- **Modern Java Features**: Primitive patterns, enhanced switch expressions, text blocks, pattern matching
- **API Key**: Requires `GOOGLEAI_API_KEY` or `GEMINI_API_KEY` environment variable
- **Veo 3 Access**: Controlled access API requiring approval from Google
- **Record Organization**: All DTOs are in a single `VeoJavaRecords` class for easy static imports
- **Multiple Client Approaches**: RestClient, HttpClient, and WebClient implementations
- **Async Patterns**: SelfScheduling, FixedRate, VirtualThread, and Reactive polling strategies
- **Application Plugin**: Provides `./gradlew run` task for standalone demo execution

## Build & Test Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application (Spring Boot context)
./gradlew bootRun

# Run the standalone demo (lightweight, no Spring context)
./gradlew run

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
- `spring-boot-starter-validation` - For Bean Validation (@Valid, @NotBlank)
- `mockwebserver` (test) - For HTTP client behavior testing
- Jackson is already included via web starter (no need to add explicitly)

## Configuration

Main configuration in `application.properties`:

```properties
# API Configuration - checks GOOGLEAI_API_KEY first, then GEMINI_API_KEY
gemini.api.key=${GOOGLEAI_API_KEY:${GEMINI_API_KEY}}
veo.api.model=veo-3.0-fast-generate-preview
veo.polling.interval-seconds=5
veo.polling.max-timeout-minutes=10
veo.output.directory=./videos

# Async Configuration - video generation takes 2-4 minutes
spring.mvc.async.request-timeout=600000
```

## Important Implementation Details

### API Integration
- **Base URL**: `https://generativelanguage.googleapis.com/v1beta`
- **Submit**: `/models/{model}:predictLongRunning` (configurable via `veo.api.model`, defaults to `veo-3.0-fast-generate-preview`)
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

### Model Selection
- Default model: `veo-3.0-fast-generate-preview` (cheaper at $0.40/sec)
- Alternative: `veo-3.0-generate-preview` (higher quality at $0.75/sec)
- Configurable via `veo.api.model` property in `application.properties`

### Client Implementations
1. **RestClientVeoVideoClient** — Spring RestClient with custom redirect handling via request factory
2. **HttpClientVeoVideoClient** — Java HttpClient with `.followRedirects(NORMAL)`
3. **ReactiveVeoVideoClient** — WebClient with Reactor Netty redirect support + 2MB buffer ⚠️ (compatibility issue)

**All clients updated** to handle 302 redirects for video file downloads and increased buffer limits for large files.

**⚠️ WebClient Compatibility Issue**: ReactiveVeoVideoClient experiences consistent 400 Bad Request errors despite sending identical JSON payloads and using the same URLs as the working HttpClient implementations. This appears to be a deep incompatibility between Reactor Netty and Google's Veo 3 API infrastructure, possibly related to HTTP protocol negotiation, TLS handshake differences, or client fingerprinting.

### Polling Strategies
1. **SelfSchedulingPollingStrategy** — Reschedules after each check (dynamic timing)
2. **FixedRatePollingStrategy** — Fixed-rate periodic checks with ScheduledExecutorService
3. **VirtualThreadPollingStrategy** — Uses virtual threads for lightweight blocking operations (Java 21+) ✅ **Recommended**
4. **ReactivePollingStrategy** — Flux.interval with clean polling patterns ⚠️ (affected by WebClient compatibility issue)

**Current Status**: 4 out of 5 polling strategies work correctly. The ReactivePollingStrategy demonstrates excellent reactive programming patterns but is affected by the underlying WebClient compatibility issue with Google's API.

## Testing

- **Comprehensive test suite** with 69+ tests covering all components
- **Service layer tests** using `@MockitoBean` (Spring Boot 3.4+) for dependency injection
- **Controller tests** with proper async endpoint testing using `MockMvc.asyncDispatch()`
- **Polling strategy tests** focused on business logic rather than timing
- **Input validation tests** using Bean Validation (`@Valid`, `@NotBlank`)
- **Resource cleanup tests** ensuring `@PreDestroy` methods prevent memory leaks
- Unit tests for records and basic functionality
- Modern Java features demonstration (sealed interfaces, pattern matching, virtual threads, unnamed variables, stream gatherers)
- Integration tests marked `@Disabled` by default to prevent accidental $6 charges
- Tests require valid API key with Veo 3 access (marked with `@EnabledIfEnvironmentVariable`)
- **Test execution**: 0.73s for all tests (originally 1m 21s with 23 failures)

## Common Development Tasks

When making changes:
1. Always run `./gradlew test` before committing
2. Check that `GOOGLEAI_API_KEY` or `GEMINI_API_KEY` is set for integration tests
3. Use static imports for VeoJavaRecords: `import static com.kousenit.veojava.model.VeoJavaRecords.*;`
4. Follow existing patterns for error handling and async operations
5. **Redirect handling**: Ensure all HTTP clients follow 302 redirects for video downloads
6. **Buffer limits**: Configure adequate buffer sizes for video files (2MB+ for WebClient)
7. **Resource cleanup**: Add `@PreDestroy` methods for any `ScheduledExecutorService` instances
8. **Testing patterns**: Use `@MockitoBean` for Spring Boot 3.4+, `MockMvc.asyncDispatch()` for async endpoints
9. **Input validation**: Use `@Valid` with `@NotBlank` for prompt validation
10. **Educational content**: When updating implementations, also update corresponding sections in tutorial materials
11. **Cost awareness**: Remember each test video costs ~$3.20 with fast preview model, design tests accordingly

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
- **Cost**: $0.40/second for fast preview model (~$3.20 per 8-second video), $0.75/second for standard model (~$6 per 8-second video)
- **Access**: Controlled access requiring Google approval
- **Redirect handling**: All HTTP clients must follow 302 redirects
- **Buffer limits**: Videos (~635KB) exceed default WebClient buffers (256KB)
- **Content filtering**: Audio may be blocked while video succeeds
- **RAI policies**: Generic prompts more likely to trigger content filters
- **Async timeout**: REST endpoints require 10-minute timeout due to 2-4 minute generation time
- **Constructor patterns**: Multiple constructors needed for Spring DI vs standalone demo usage
- **⚠️ WebClient Compatibility Issue**: ReactiveVeoVideoClient experiences 400 Bad Request errors with Google's Veo 3 API despite sending identical requests to HttpClient. Wire-level analysis shows identical JSON payloads and URLs, but Google's API consistently rejects WebClient/Reactor Netty requests with empty error responses. This appears to be a low-level protocol incompatibility. The reactive polling strategy is included for educational purposes but should not be used in production. Use VirtualThread approach for similar simplicity with high performance.

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
| If You Have... | Use This Strategy | Status |
|---|---|---|
| Java 8-20 + Simple needs | ScheduledExecutor | ✅ Working |
| Java 8-20 + High concurrency | ScheduledExecutor (FixedRate) | ✅ Working |
| Java 21+ | Virtual Threads | ✅ **Recommended** |
| Existing reactive codebase | Virtual Threads (simpler than Reactive) | ✅ Working |
| Need simplest code | Virtual Threads | ✅ **Recommended** |
| Legacy constraints | ScheduledExecutor | ✅ Working |
| Learning reactive patterns | ReactivePollingStrategy | ⚠️ Educational only |

**Note**: Due to the WebClient compatibility issue, VirtualThread approach is now the recommended solution for all modern Java applications, providing simple code with excellent performance.

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
2. **Virtual Threads** - Simple code, massive scalability (Java 21+) ✅ **Recommended**
3. **Reactive Streams** - Declarative, composable patterns ⚠️ (WebClient compatibility issue with Google API)

The Java implementations showcase enterprise-grade patterns suitable for:
- Production web applications with concurrent requests
- Resource-efficient handling of multiple simultaneous operations
- Integration with Spring's ecosystem and observability tools
- Library code for different deployment contexts

**Current Recommendation**: VirtualThread approach provides the best balance of simplicity, performance, and reliability for this specific Google API integration.

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

This project serves as both a working example of Google Veo 3 integration and a comprehensive educational resource for Java developers learning:
- Modern async patterns, from naive busy-waiting to enterprise-grade solutions
- Java 24 preview features in real-world applications
- Multiple HTTP client approaches and their trade-offs
- Gradle toolchain configuration for modern Java development
- Interactive console applications with proper input handling

## Modern Java Features

This project showcases cutting-edge Java 24 features including:

- **Primitive Patterns with Guards**: Pattern matching on primitive types with `when` clauses
- **Text Blocks**: Multi-line strings throughout the application for better readability
- **Exception Pattern Matching**: Clean error handling with switch expressions on exception types
- **Enhanced Switch Expressions**: Multiple case values and arrow syntax
- **Pattern Matching for instanceof**: Automatic type casting and variable binding

See **[MODERN_JAVA_FEATURES.md](MODERN_JAVA_FEATURES.md)** for a comprehensive guide to all modern Java features used in this project.

## Build Configuration for Java 24

The project uses Gradle toolchains and preview features:

```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

application {
    mainClass.set("com.kousenit.veojava.VeoVideoDemo")
}

// Enable preview features for all tasks
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
}

// Enable preview features for both Spring Boot and application plugin
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("--enable-preview")
}

tasks.named<JavaExec>("run") {
    jvmArgs("--enable-preview")
    standardInput = System.`in`  // Enable console input for interactive mode
}
```

**Key Points**:
- Gradle toolchain ensures Java 24 is used for compilation regardless of system Java version
- Preview features are enabled for compilation, testing, and runtime
- Application plugin provides `./gradlew run` as an alternative to IDE main method execution
- Console input is configured for interactive demo usage

## VeoVideoDemo Interactive Menu

The standalone demo (`./gradlew run`) presents an interactive menu with 6 options:

1. HttpClient (pure Java, no Spring) ✅
2. RestClient (Spring's modern HTTP client) ✅
3. SelfScheduling polling strategy ✅
4. FixedRate polling strategy ✅
5. Reactive polling strategy ⚠️ (WebClient compatibility issue)
6. VirtualThread polling strategy ✅ **Recommended**

Each option demonstrates a different approach to the same video generation task, allowing direct comparison of implementation styles and performance characteristics. Option 6 (VirtualThread) is recommended for new projects due to its simplicity and excellent performance.

## Recent Updates

### Added Reactive Strategy to Demo Menu
- Added missing reactive polling strategy option (5) to VeoVideoDemo menu
- Reordered menu so VirtualThread strategy is last (option 6)
- Fixed model consistency across all approaches (using fast preview model)
- Added interactive menu improvements with file-based prompt loading

### WebClient Compatibility Investigation
Conducted comprehensive investigation into ReactiveVeoVideoClient 400 Bad Request errors:
- **Wire-level analysis**: Confirmed identical JSON payloads and URLs between HttpClient and WebClient
- **Header comparison**: Tested User-Agent and other header variations
- **Protocol analysis**: Both use same TLS handshake and HTTP/1.1
- **Buffer configuration**: Ensured adequate buffer sizes for video files
- **Model consistency**: Both use same API model (veo-3.0-fast-generate-preview)
- **Result**: Identified as fundamental incompatibility between Reactor Netty and Google's API infrastructure

The issue appears to be at the HTTP client implementation level, possibly related to:
- Client fingerprinting by Google's API servers
- TLS/HTTP protocol negotiation differences
- Low-level request encoding variations that are invisible at the application layer

**Recommendation**: Use VirtualThread approach (option 6) for production applications requiring reactive-style simplicity with high performance.