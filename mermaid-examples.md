# Mermaid Diagram Examples for VeoJava YouTube Script

## Video Generation Flow

```mermaid
sequenceDiagram
    participant User
    participant JavaApp as Java Application
    participant VeoAPI as Veo 3 API
    participant Storage as File Storage
    
    User->>JavaApp: Submit prompt
    JavaApp->>VeoAPI: POST /predictLongRunning
    VeoAPI-->>JavaApp: Return operation ID
    
    loop Poll every 5 seconds
        JavaApp->>VeoAPI: GET /operations/{id}
        VeoAPI-->>JavaApp: Status (done: false)
    end
    
    VeoAPI-->>JavaApp: Status (done: true) + Video URL
    JavaApp->>VeoAPI: GET video URL (302 redirect)
    VeoAPI-->>JavaApp: Actual video content
    JavaApp->>Storage: Save MP4 file
    JavaApp-->>User: Return video path
```

## Polling Strategy Decision Tree

```mermaid
graph TD
    A[Need to Poll API] --> B{Java Version?}
    B -->|Java 8-20| C{Existing Stack?}
    B -->|Java 21+| D[Use Virtual Threads]
    
    C -->|Traditional| E[ScheduledExecutor]
    C -->|Reactive| F[Spring WebFlux]
    
    D --> G[Simple & Scalable]
    E --> H[Enterprise Ready]
    F --> I[High Concurrency]
    
    style D fill:#90EE90
    style G fill:#90EE90
```

## Thread Usage Comparison

```mermaid
graph LR
    subgraph "Busy Wait"
        BW1[Thread 1 - Blocked]
        BW2[Thread 2 - Blocked]
        BW3[Thread 3 - Blocked]
        BWN[Thread N - Blocked]
    end
    
    subgraph "ScheduledExecutor"
        SE1[Thread Pool]
        SE2[Task Queue]
        SE1 --> SE2
    end
    
    subgraph "Virtual Threads"
        VT1[Virtual Thread 1]
        VT2[Virtual Thread 2]
        VTN[Virtual Thread N]
        VTP[Platform Thread Pool]
        VT1 -.-> VTP
        VT2 -.-> VTP
        VTN -.-> VTP
    end
```

## Reactive Streams Flow

```mermaid
graph TB
    A[Flux.interval] --> B[Emit tick every 5s]
    B --> C[flatMap: Check Status]
    C --> D{Done?}
    D -->|No| E[Continue stream]
    D -->|Yes| F[filter & next]
    F --> G[Convert to Future]
    E --> B
    
    style A fill:#FFE4B5
    style G fill:#90EE90
```

## Performance Scaling Visualization

```mermaid
graph BT
    subgraph "Concurrent Operations Supported"
        A[Busy Wait: 200] --> B[ScheduledExecutor: 10,000]
        B --> C[Reactive: 100,000+]
        C --> D[Virtual Threads: 1,000,000+]
    end
    
    style A fill:#FF6B6B
    style B fill:#FFD93D
    style C fill:#6BCF7F
    style D fill:#4ECDC4
```

## API Architecture Overview

```mermaid
graph TB
    subgraph "VeoJava Application"
        Controller[REST Controller]
        Service[Video Generation Service]
        
        subgraph "HTTP Clients"
            RestClient[Spring RestClient]
            HttpClient[Java HttpClient]
            WebClient[Spring WebClient]
        end
        
        subgraph "Polling Strategies"
            PS1[SelfScheduling]
            PS2[FixedRate]
            PS3[VirtualThread]
            PS4[Reactive]
        end
    end
    
    Controller --> Service
    Service --> HttpClients
    Service --> PS1
    Service --> PS2
    Service --> PS3
    Service --> PS4
    
    HttpClients --> API[Google Veo 3 API]
    
    style Controller fill:#B19CD9
    style Service fill:#87CEEB
    style API fill:#FFB6C1
```

## Code Complexity Comparison

```mermaid
graph LR
    subgraph "Lines of Code"
        A[Busy Wait: 5] --> B[Virtual Threads: 10]
        B --> C[Reactive: 15]
        C --> D[ScheduledExecutor: 30]
    end
    
    subgraph "Cognitive Load"
        E[😊 Low] -.-> A
        E -.-> B
        F[🤔 Medium] -.-> D
        G[😰 High] -.-> C
    end
```