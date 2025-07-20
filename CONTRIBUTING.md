# Contributing to VeoJava

Thank you for your interest in contributing to VeoJava! This project demonstrates different Java approaches to handling long-running API operations.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally
3. **Set up your development environment**:
   ```bash
   ./gradlew build
   ```
4. **Set up API access** (optional, for testing):
   ```bash
   export GOOGLEAI_API_KEY="your-api-key"
   ```

## Development Guidelines

### Code Style
- Follow existing Java code conventions
- Use modern Java features (records, sealed interfaces, etc.)
- Keep methods focused and well-documented
- Prefer composition over inheritance

### Testing
- Run tests before submitting: `./gradlew test`
- Integration tests are disabled by default (they cost money!)
- Add unit tests for new functionality

### Commit Messages
- Use clear, descriptive commit messages
- Reference issues when applicable
- Keep commits focused on single changes

## Types of Contributions

### 🐛 Bug Reports
- Use GitHub Issues with a clear description
- Include reproduction steps
- Mention your Java version and OS

### ✨ Feature Requests
- Discuss major changes in an issue first
- Focus on polling strategies and HTTP client patterns
- Consider backwards compatibility

### 📝 Documentation
- Improve README clarity
- Add code examples
- Fix typos and grammar

### 🔧 Code Contributions
- **New Polling Strategies**: Always welcome!
- **HTTP Client Improvements**: Show different approaches
- **Performance Optimizations**: With benchmarks
- **Modern Java Features**: Demonstrate new language features

## What We're Looking For

This project aims to demonstrate:
- Different approaches to avoid busy waiting
- HTTP client best practices
- Async programming patterns
- Modern Java features in real-world scenarios

## What We're Not Looking For

- Adding more dependencies unless absolutely necessary
- Changing the core demonstration purpose
- Adding UI frameworks (this is a backend demo)

## Questions?

Feel free to open an issue for questions about contributing!

## License

By contributing, you agree that your contributions will be licensed under the MIT License.