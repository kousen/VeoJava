# Veo Java - Google Gemini Veo 3 Video Generator Java Client

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-24+-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3+-green.svg)](https://spring.io/projects/spring-boot)

This Spring Boot application demonstrates multiple approaches to integrate with Google's Gemini Veo 3 video generation API from Java. The implementation is based on the REST API documented at https://ai.google.dev/gemini-api/docs/video (note that the official examples only show Python, JavaScript, and Go, but this project demonstrates Java integration patterns).

> **⚠️ Important**: This is a demonstration project showing different Java polling strategies for long-running operations. Each video generation costs ~$6.00 and requires Google API access approval.

## Features

- **Multiple Client Implementations**:
  - Spring `RestClient`
  - Java `HttpClient` (standalone)
  - Reactive `WebClient`

- **Multiple Polling Strategies**:
  - `SelfScheduling`-based polling (dynamic intervals)
  - `FixedRate`-based polling (fixed intervals)
  - `VirtualThread`-based polling (Java 21+)
  - Reactive `Flux`-based polling

- **REST API Endpoints** for testing all approaches
- **Configuration** via `application.properties`
- **Video File Saving** with `Base64` decoding

## Prerequisites

1. **Java 24** or later
2. **Gradle** (wrapper included)  
3. **Gemini API Key** with **Veo 3 Access** — Set the `GOOGLEAI_API_KEY` or `GEMINI_API_KEY` environment variable
   - Note: Veo 3 is in controlled access and may require approval from Google

## Getting Started

### 1. Set up your API key
```bash
export GOOGLEAI_API_KEY="your-api-key-here"
# OR
export GEMINI_API_KEY="your-api-key-here"
```

### 2. Build the project
```bash
./gradlew build
```

### 3. Run the application
```bash
./gradlew bootRun
```

### 4. Test the endpoints
The application starts on `http://localhost:8080`

## API Endpoints

### Generate Video (different approaches)
- **POST** `/api/video/generate/rest-client`
- **POST** `/api/video/generate/http-client`
- **POST** `/api/video/generate/self-scheduling`
- **POST** `/api/video/generate/fixed-rate`
- **POST** `/api/video/generate/virtual-thread`
- **POST** `/api/video/generate/reactive`

Request body:
```json
{
  "prompt": "A cat playing with a ball of yarn in a sunny garden"
}
```

### Utility Endpoints
- **GET** `/api/video/strategies` - List available strategies
- **GET** `/api/video/health` - Health check

## Example Usage

### Using cURL
```bash
curl -X POST http://localhost:8080/api/video/generate/rest-client \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A beautiful sunset over mountains"}'
```

### Interactive Demo
⚠️ **Cost Warning**: Each video costs ~$6.00 (8 seconds × $0.75/second)

Run the interactive demo:
```bash
java -cp "build/classes/java/main:$(./gradlew -q printClasspath)" com.kousenit.veojava.VeoVideoDemo
```

Or use Gradle:
```bash
./gradlew run
```

The demo will present an interactive menu where you can:
- Choose which approach to test (one at a time)
- Enter custom prompts or use the default
- See cost warnings before generating videos
- Compare different implementations safely

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# API Configuration - checks GOOGLEAI_API_KEY first, then GEMINI_API_KEY
gemini.api.key=${GOOGLEAI_API_KEY:${GEMINI_API_KEY}}

# Polling Configuration
veo.polling.interval-seconds=5
veo.polling.max-timeout-minutes=10

# Output Configuration
veo.output.directory=./videos

# Async Configuration - video generation takes 2-4 minutes
spring.mvc.async.request-timeout=600000
```

## Implementation Details

### Client Approaches

1. **`RestClient`** — Uses Spring's `RestClient` with autoconfigured Jackson
2. **`HttpClient`** — Uses Java 11+ `HttpClient` with explicit `ObjectMapper`
3. **`ReactiveClient`** — Uses `WebClient` with `Mono`/`Flux` patterns

### Polling Strategies

1. **`SelfScheduling`** — Reschedules after each check completes (dynamic timing)
2. **`FixedRate`** — Uses `ScheduledExecutorService` for fixed periodic checks
3. **`VirtualThread`** — Uses virtual threads for lightweight blocking operations
4. **`Reactive`** — Uses `Flux.interval` with clean polling patterns

### Video Generation Process

1. **Submit** video generation request → Get operation ID
2. **Poll** operation status every 5 seconds → Wait for `done: true`
3. **Extract** download URL from response → Get file URI with redirect
4. **Download** video file → Follow 302 redirects to actual content
5. **Save** to local file system → Write binary data to MP4 file

**Key Discovery**: Veo 3 returns download URLs instead of base64-encoded data, requiring redirect handling for proper file retrieval.

## Important Notes

- **Paid Feature**: Veo 3 costs $0.75/second (~$6 for 8-second videos)
- **8-second videos**: Current limitation of the API
- **720p @ 24 fps**: Fixed resolution and frame rate
- **Redirect handling**: All HTTP clients must follow 302 redirects for video downloads
- **Buffer limits**: WebClient requires 2MB+ buffer for video files (default 256KB insufficient)
- **Content filtering**: Audio may be blocked while video succeeds
- **Controlled access**: Veo 3 requires special API access approval
- **English prompts**: Only English language supported
- **Async timeout**: REST endpoints require 10-minute timeout due to 2-4 minute generation time

## Project Structure

```
src/main/java/com/kousenit/veojava/
├── client/           # HTTP client implementations
├── config/           # Spring configuration
├── controller/       # REST endpoints
├── model/            # Data models (records)
├── service/          # Business logic and polling strategies
└── VeoVideoDemo.java # Standalone demo
```

## Testing

Run tests:
```bash
./gradlew test
```

**Note**: Integration tests are disabled by default (using `@Disabled`) to prevent accidental API costs. Each test video costs ~$6 and takes several minutes to generate.

## Build and Run

Build the project:
```bash
./gradlew build
```

Run all checks including tests:
```bash
./gradlew check
```

Create a distribution:
```bash
./gradlew bootJar
java -jar build/libs/VeoJava-0.0.1-SNAPSHOT.jar
```

## Architecture Highlights

This project demonstrates several key Java/Spring patterns:

- **Multiple HTTP Client Approaches**: Compare Spring `RestClient`, Java `HttpClient`, and `WebClient`
- **Async Patterns**: `SelfScheduling`, `FixedRate`, `VirtualThread`, and Reactive streams
- **Polling Strategy Analysis**: Comprehensive comparison of non-busy-waiting approaches
- **Record-Based Data Models**: All DTOs in a single `VeoJavaRecords` class for easy static imports
- **Modern Java Features**: Sealed interfaces, pattern matching, virtual threads, unnamed variables, stream gatherers (Java 22+)
- **Configuration Properties**: Type-safe configuration with `@ConfigurationProperties`
- **Polling Strategies**: Different approaches to handle long-running operations
- **Error Handling**: Comprehensive error handling across sync and async flows

## YouTube Tutorial

This project includes comprehensive educational content for Java developers:

- **[script.md](script.md)** - Complete YouTube video script explaining all 5 polling strategies
- **[polling-strategy-comparison.md](polling-strategy-comparison.md)** - Detailed comparison matrix with performance metrics and decision guidelines
- **[mermaid-examples.md](mermaid-examples.md)** - Visual diagrams for presentations and documentation

The tutorial covers the journey from naive busy-waiting to enterprise-grade async patterns, perfect for developers learning modern Java concurrency patterns.

## Additional Documentation

- **[NETWORKING_STRATEGY_ANALYSIS.md](NETWORKING_STRATEGY_ANALYSIS.md)** - Comprehensive analysis of HTTP clients and polling strategies with recommendations
- **[API Implementation Notes](API_IMPLEMENTATION_NOTES.md)** - Detailed technical documentation about redirect handling, response structures, and API behavior discoveries
- **[CLAUDE.md](CLAUDE.md)** - Development context and implementation decisions

## Educational Value

This project serves multiple purposes:

1. **Production Example**: Real-world integration with Google's Veo 3 API including proper error handling, timeouts, and redirect management
2. **Learning Resource**: Comprehensive comparison of 5 different polling strategies from naive blocking to advanced async patterns
3. **Modern Java Showcase**: Demonstrates records, sealed interfaces, virtual threads (Java 21+), reactive streams, and other contemporary features
4. **Performance Analysis**: Includes detailed metrics and recommendations for different concurrency approaches

## Contributing

This is a demonstration project showing different Java approaches to REST API integration with async polling patterns. The implementation includes important discoveries about Google's Veo 3 API behavior, particularly around redirect handling and URL-based video delivery. Feel free to explore and adapt the patterns for your own use cases.

The educational materials (script.md, comparison tables, diagrams) are designed to help other developers understand the trade-offs between different async programming approaches in Java.