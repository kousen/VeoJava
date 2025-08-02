# Modern Java Features Showcase

This project demonstrates cutting-edge Java features from Java 21+ through Java 24, while maintaining its core educational purpose of showcasing different asynchronous polling strategies.

## 🎯 Project Java Version

- **Target**: Java 24 with preview features enabled
- **Preview Features**: Required for primitive patterns
- **Compatibility**: Will become standard in Java 25 (September 2025)

## 🔥 Java 24 Features (Preview)

### Primitive Patterns with Guards (JEP 455)
**Location**: `VeoVideoDemo.formatFileSize()`

```java
private static String formatFileSize(int bytes) {
    return switch (bytes) {
        case int b when b < 1024 -> b + " bytes";
        case int b when b < 1024 * 1024 -> "%.1f KB".formatted(b / 1024.0);
        case int b when b < 1024 * 1024 * 1024 -> "%.1f MB".formatted(b / (1024.0 * 1024));
        default -> "%.1f GB".formatted(bytes / (1024.0 * 1024 * 1024));
    };
}
```

**Benefits**:
- Pattern matching on primitive types
- Guard conditions with `when` clauses
- Cleaner than traditional if-else chains
- Type-safe variable binding

## 🚀 Java 21+ Features

### Enhanced Switch Expressions
**Location**: `VeoVideoDemo.getValidChoice()`

```java
switch (choice) {
    case 0, 1, 2, 3, 4, 5 -> {
        return choice;
    }
    default -> logger.warn("Invalid choice {}. Please enter 0-5", choice);
}
```

**Benefits**:
- Multiple case values in single line
- Arrow syntax (`->`) for cleaner code
- No fall-through issues

### Pattern Matching for instanceof
**Location**: Throughout HTTP clients

```java
if (!(status.response() instanceof OperationStatus.OperationResponse response) ||
    response.generateVideoResponse() == null) {
    throw new RuntimeException("No video data found in response");
}
```

**Benefits**:
- Automatic type casting
- Reduced boilerplate code
- Compile-time type safety

### Exception Pattern Matching
**Location**: `VeoVideoDemo.getPrompt()`

```java
String errorType = switch (e) {
    case java.nio.file.NoSuchFileException _ -> "File not found";
    case java.nio.file.AccessDeniedException _ -> "Access denied";
    case java.io.FileNotFoundException _ -> "File not found";
    default -> "IO error";
};
```

**Benefits**:
- Type-specific error handling
- Cleaner exception classification
- Unnamed variables with `_`

## 📝 Java 15+ Features

### Text Blocks
**Location**: Throughout the application

```java
String instructions = """
    Enter video prompt (or press Enter for default).
    For multi-line prompts:
      - Type your prompt and press Enter to continue on a new line
      - Type 'END' on a new line when finished
      - Or just press Enter twice to finish
    To load from file:
      - Type 'FILE:' followed by the filename (e.g., FILE:prompt.txt)
      - File path is relative to: %s
    """.formatted(System.getProperty("user.dir"));
```

**Benefits**:
- Multi-line strings without escaping
- Natural indentation handling
- Better readability for JSON, SQL, HTML

### String Formatting
**Location**: Throughout the application

```java
"%.1f KB".formatted(b / 1024.0)
```

**Benefits**:
- Instance method alternative to `String.format()`
- More fluent API
- Better method chaining

## 🏗️ Build Configuration

### Gradle Configuration
**File**: `build.gradle.kts`

```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
}
```

**Required for**:
- Java 24 toolchain
- Preview feature compilation
- Test execution with preview features

## 🧪 Feature Testing

### Compilation Test
```bash
./gradlew compileJava
# Note: uses preview features of Java SE 24
```

### Runtime Test
```bash
./gradlew test
# All tests pass with preview features
```

### Demo Execution
```bash
./gradlew bootRun
# Interactive demo with modern Java features
```

## 🔍 Feature Discovery Notes

### Unnamed Variables Limitations
- ❌ **Cannot use with array parameters**: `String[] _` not supported
- ✅ **Works with exception handling**: `case IOException _ ->`
- ❌ **Main method signature**: Must remain `String[] args`

### Pattern Matching Evolution
- **Java 17**: Pattern matching for instanceof
- **Java 21**: Pattern matching in switch, record patterns
- **Java 22**: Unnamed variables
- **Java 24**: Primitive patterns (preview)
- **Java 25**: Primitive patterns (final - September 2025)

## 🎓 Educational Value

This project demonstrates modern Java features while preserving its core educational purpose:

### ✅ What We Enhanced
- **Input validation**: Modern switch expressions
- **Error handling**: Exception pattern matching
- **File size formatting**: Primitive patterns with guards
- **Multi-line strings**: Text blocks throughout
- **String formatting**: Modern `.formatted()` method

### ✅ What We Preserved
- **Async polling strategies**: Core educational content intact
- **CompletableFuture patterns**: Different async approaches
- **Virtual threads demonstration**: Modern concurrency
- **Reactive programming**: WebFlux integration
- **Spring Boot integration**: Enterprise patterns

## 🔮 Future Roadmap

### Java 25 (September 2025)
- Remove `--enable-preview` flag
- Primitive patterns become standard
- Update documentation to reflect stable features

### Potential Future Features
- **String templates**: When available
- **Value objects**: Project Valhalla
- **Foreign Function & Memory API**: Native integrations
- **Structured concurrency**: Enhanced async patterns

## 📚 References

- [JEP 455: Primitive Types in Patterns, instanceof, and switch (Preview)](https://openjdk.org/jeps/455)
- [JEP 441: Pattern Matching for switch](https://openjdk.org/jeps/441)
- [JEP 456: Unnamed Variables & Patterns](https://openjdk.org/jeps/456)
- [Java 24 Documentation](https://docs.oracle.com/en/java/javase/24/)

---

This project serves as both a practical example of Google Veo 3 integration and a showcase of Java's evolution toward more expressive, concise, and maintainable code patterns.