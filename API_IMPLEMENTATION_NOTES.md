# Google Veo 3 API Implementation Notes

## Overview

This document explains the implementation details and architectural decisions for the Java client library that interfaces with Google's Veo 3 video generation API. It covers the key findings discovered during development, including the actual API behavior versus expected patterns.

## Available Models

Google Veo 3 offers multiple model variants:

1. **veo-3.0-fast-generate-preview** (Default)
   - Cost: $0.40/second
   - Faster generation time
   - Suitable for rapid prototyping

2. **veo-3.0-generate-preview**
   - Cost: $0.75/second
   - Higher quality output
   - Suitable for production use

The model can be configured via the `veo.api.model` property in `application.properties`.

## API Response Structure Discovery

### Expected vs. Actual Behavior

**What we initially expected** (based on typical AI APIs):
```json
{
  "response": {
    "predictions": [{
      "bytesBase64Encoded": "data:video/mp4;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEA...",
      "mimeType": "video/mp4"
    }]
  }
}
```

**What Veo 3 actually returns**:
```json
{
  "name": "models/{model}/operations/abc123",
  "done": true,
  "response": {
    "@type": "type.googleapis.com/google.ai.generativelanguage.v1beta.PredictLongRunningResponse",
    "generateVideoResponse": {
      "generatedSamples": [{
        "video": {
          "uri": "https://generativelanguage.googleapis.com/v1beta/files/a936dm659f6e:download?alt=media"
        }
      }],
      "raiMediaFilteredReasons": [
        "The video was created, but the requested audio was blocked for violating our content policies..."
      ]
    }
  }
}
```

### Key Differences

1. **URL-based delivery**: Instead of embedding video data as base64, Veo 3 provides download URLs
2. **Separate download step**: Requires an additional HTTP request to fetch the actual video file
3. **RAI filtering**: Audio can be filtered while video succeeds, with detailed explanations
4. **File service integration**: Uses Google's file service with redirect-based downloads

## Redirect Handling Requirements

### The Problem

When requesting video files from the provided URI, Google's file service returns a **302 redirect** response:

```http
GET https://generativelanguage.googleapis.com/v1beta/files/a936dm659f6e:download?alt=media
HTTP/1.1 302 Found
Location: https://actual-storage-url.googleapis.com/video-data
```

Without proper redirect handling, clients receive a 95-byte JSON error instead of the 635KB video file:

```json
{
  "error": {
    "code": 302,
    "message": "Unknown Error.",
    "status": "UNKNOWN"
  }
}
```

### Solution Implementation

Different HTTP clients require different approaches:

**Java HttpClient** (Built-in redirect support):
```java
HttpClient client = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();
```

**Spring RestClient** (Custom request factory):
```java
SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        super.prepareConnection(connection, httpMethod);
        connection.setInstanceFollowRedirects(true);
    }
};
```

**Spring WebClient** (Reactor Netty configuration):
```java
HttpClient httpClient = HttpClient.create().followRedirect(true);
ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

WebClient webClient = WebClient.builder()
    .clientConnector(connector)
    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
    .build();
```

## Complete Video Generation Flow

### 1. Initial Request
```http
POST https://generativelanguage.googleapis.com/v1beta/models/{model}:predictLongRunning
x-goog-api-key: YOUR_API_KEY
Content-Type: application/json

{
  "instances": [{
    "prompt": "A cat playing with a ball of yarn in a sunny garden"
  }],
  "parameters": {
    "aspectRatio": "16:9",
    "personGeneration": "allow_all"
  }
}
```

**Response**: Operation ID for polling
```json
{
  "name": "models/{model}/operations/xyz123"
}
```

### 2. Status Polling
```http
GET https://generativelanguage.googleapis.com/v1beta/models/{model}/operations/xyz123
x-goog-api-key: YOUR_API_KEY
```

**Response (in progress)**:
```json
{
  "name": "models/{model}/operations/xyz123",
  "done": false,
  "metadata": { ... }
}
```

**Response (completed)**:
```json
{
  "name": "models/{model}/operations/xyz123",
  "done": true,
  "response": {
    "generateVideoResponse": {
      "generatedSamples": [{
        "video": {
          "uri": "https://generativelanguage.googleapis.com/v1beta/files/file-id:download?alt=media"
        }
      }]
    }
  }
}
```

### 3. Video Download
```http
GET https://generativelanguage.googleapis.com/v1beta/files/file-id:download?alt=media
x-goog-api-key: YOUR_API_KEY
```

**Response**: Raw video bytes (with redirect handling)

### 4. File Storage
Save the downloaded bytes to a local file:
```java
Files.write(Paths.get("video.mp4"), videoBytes);
```

## Authentication

### API Key Format
The Veo 3 API uses Google AI API keys with the `x-goog-api-key` header:

```http
x-goog-api-key: AIzaSyC7-8X9YZ_example_key_12345
```

### Environment Variable Support
Our implementation checks multiple common environment variable names:
1. `GOOGLEAI_API_KEY` (primary)
2. `GEMINI_API_KEY` (fallback)

## Error Handling and Content Filtering

### Responsible AI (RAI) Filtering
Google applies content filtering that can affect audio while preserving video:

```json
{
  "raiMediaFilteredReasons": [
    "The video was created, but the requested audio was blocked for violating our content policies. You have only been charged for the video; please modify your request and resubmit."
  ]
}
```

**Key Points**:
- Video generation succeeds even if audio is filtered
- Billing applies only to successfully generated content
- Filtered content provides detailed explanations
- Generic prompts are more likely to trigger filters

### Common Error Scenarios

1. **Invalid API Key**:
   ```json
   {
     "error": {
       "code": 400,
       "message": "API key not valid. Please pass a valid API key."
     }
   }
   ```

2. **Operation Not Found** (wrong URL format):
   ```http
   HTTP/1.1 404 Not Found
   ```

3. **Buffer Limit Exceeded** (WebClient without configuration):
   ```
   DataBufferLimitException: Exceeded limit on max bytes to buffer : 262144
   ```

## Consistency with Python/REST Documentation

### Alignment with Official Patterns

The implementation follows Google's standard patterns for long-running operations:

1. **Submit operation** → Get operation name
2. **Poll operation status** → Check `done` field
3. **Extract results** → Process response when complete

### Differences from Documentation Examples

Most documentation shows base64-embedded responses, but Veo 3's actual implementation uses:
- **File service URLs** instead of embedded data
- **Redirect-based downloads** instead of direct responses
- **RAI filtering metadata** not typically shown in examples

This appears to be an optimization for large media files, similar to how Google Cloud Storage handles file downloads.

## Performance Considerations

### Video File Characteristics
- **Size**: ~75-80 KB per second of video content
- **8-second videos**: ~600-635 KB
- **Format**: MP4 with H.264 encoding
- **Resolution**: 720p (may vary)

### Buffer Configuration
- **Default limits**: 256 KB (insufficient for videos)
- **Recommended**: 2 MB for WebClient implementations
- **Memory usage**: Videos are loaded entirely into memory during download

### Polling Strategy
- **Interval**: 5 seconds (recommended)
- **Timeout**: 10 minutes maximum
- **Generation time**: Typically 2-4 minutes for 8-second videos

## Architecture Benefits

### Multiple Client Implementations
The system provides several HTTP client options:

1. **HttpClient**: Pure Java, no dependencies
2. **RestClient**: Spring's modern HTTP client
3. **WebClient**: Reactive streams support

### Async Patterns
Multiple polling strategies demonstrate different concurrency approaches:
- **CompletableFuture**: Standard async programming
- **ScheduledExecutor**: Traditional thread pool management
- **Reactive Streams**: Backpressure-aware processing
- **Virtual Threads**: Lightweight blocking operations (Java 21+)

### Production Readiness
- Comprehensive error handling
- Configurable timeouts and retry logic
- Environment-based configuration
- Proper resource cleanup

## Cost Considerations

### Pricing Model
- **Rate**: $0.75 per second of generated video
- **8-second video**: ~$6.00 per generation
- **Audio filtering**: No additional charge for blocked audio

### Quota Management
- **Rate limits**: 10 requests per minute per project
- **Concurrent operations**: Multiple videos can generate simultaneously
- **Failed operations**: No charge for unsuccessful generations

## Conclusion

The Veo 3 API implementation revealed several important patterns:

1. **Large media content** is delivered via URLs, not embedded data
2. **Redirect handling** is essential for file downloads
3. **Content filtering** can be selective (audio vs. video)
4. **Buffer limits** must accommodate large files
5. **Authentication** follows Google AI patterns, not Cloud patterns

These findings suggest that Google is optimizing for large media delivery while maintaining the familiar long-running operation pattern for AI services. The URL-based approach likely provides better scalability, caching, and bandwidth management compared to embedding large binary data in JSON responses.

The implementation successfully demonstrates how to handle these requirements across multiple Java HTTP client libraries, providing a robust foundation for production video generation applications.