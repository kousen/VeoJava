# Veo Java - Google Gemini Veo 3 Video Generator Java Client

This Spring Boot application demonstrates multiple approaches to integrate with Google's Gemini Veo 3 video generation API from Java. The implementation is based on the REST API documented at https://ai.google.dev/gemini-api/docs/video (note that the official examples only show Python, JavaScript, and Go, but this project demonstrates Java integration patterns).

## Features

- **Multiple Client Implementations**:
  - Spring `RestClient`
  - Java `HttpClient` (standalone)
  - Reactive `WebClient`

- **Multiple Polling Strategies**:
  - `CompletableFuture`-based polling
  - `ScheduledExecutor`-based polling
  - `VirtualThread`-based polling (Java 21+)
  - Reactive `Flux`-based polling

- **REST API Endpoints** for testing all approaches
- **Configuration** via `application.properties`
- **Video File Saving** with `Base64` decoding

## Prerequisites

1. **Java 24** or later
2. **Gradle** (wrapper included)
3. **Gemini API Key** — Set the `GEMINI_API_KEY` environment variable

## Getting Started

### 1. Set up your API key
```bash
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
- **POST** `/api/video/generate/completable-future`
- **POST** `/api/video/generate/scheduled-executor`
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
# API Configuration
gemini.api.key=${GEMINI_API_KEY}

# Polling Configuration
veo.polling.interval-seconds=5
veo.polling.max-timeout-minutes=10

# Output Configuration
veo.output.directory=./videos
```

## Implementation Details

### Client Approaches

1. **`RestClient`** — Uses Spring's `RestClient` with autoconfigured Jackson
2. **`HttpClient`** — Uses Java 11+ `HttpClient` with explicit `ObjectMapper`
3. **`ReactiveClient`** — Uses `WebClient` with `Mono`/`Flux` patterns

### Polling Strategies

1. **`CompletableFuture`** — Chains futures with scheduled delays
2. **`ScheduledExecutor`** — Uses `ScheduledExecutorService` for periodic checks
3. **`VirtualThread`** — Uses virtual threads for lightweight blocking operations
4. **`Reactive`** — Uses `Flux.interval` with retry logic

### Video Generation Process

1. Submit video generation request
2. Poll operation status every 5 seconds
3. Download completed video (`Base64` decoded)
4. Save to local file system

## Important Notes

- **Paid Feature**: Veo 3 requires billing to be enabled
- **8-second videos**: Current limitation of the API
- **720p @ 24 fps**: Fixed resolution and frame rate
- **2-day storage**: Videos are stored server-side for 2 days
- **English prompts**: Only English language supported

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

Note: Integration tests require a valid API key and will make actual API calls.

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
- **Async Patterns**: `CompletableFuture`, `ScheduledExecutor`, and Reactive streams
- **Record-Based Data Models**: All DTOs in a single `VeoJavaRecords` class for easy static imports
- **Modern Java Features**: Sealed interfaces, pattern matching, virtual threads, unnamed variables, stream gatherers (Java 22+)
- **Configuration Properties**: Type-safe configuration with `@ConfigurationProperties`
- **Polling Strategies**: Different approaches to handle long-running operations
- **Error Handling**: Comprehensive error handling across sync and async flows

## Contributing

This is a demonstration project showing different Java approaches to REST API integration with async polling patterns. Feel free to explore and adapt the patterns for your own use cases.