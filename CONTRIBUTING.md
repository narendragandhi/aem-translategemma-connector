# Contributing to AEM TranslateGemma Connector

We welcome contributions to the AEM TranslateGemma Translation Connector! This document provides guidelines for contributors.

## Getting Started

### Prerequisites
- Java 11+ development environment
- Maven 3.6+
- AEM development environment
- Google Cloud Platform account (optional for testing)
- Git

### Development Setup

1. **Fork the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/aem-translategemma-connector.git
   cd aem-translategemma-connector
   ```

2. **Set up development environment**
   ```bash
   mvn clean install
   ```

3. **Run tests**
   ```bash
   mvn test
   ```

## Code Style and Standards

### Java Code Style
- Follow Oracle Java conventions
- Use 4-space indentation (no tabs)
- Maximum line length: 120 characters
- Include Javadoc for public methods
- Use SLF4J for logging

### Example Code Style:
```java
/**
 * Translates content using TranslateGemma model.
 *
 * @param sourceText the text to translate
 * @param sourceLanguage source language code
 * @param targetLanguage target language code
 * @return TranslationResult with translated content
 * @throws TranslationException if translation fails
 */
public TranslationResult translate(String sourceText, String sourceLanguage, 
                               String targetLanguage) throws TranslationException {
    // Implementation
}
```

### Naming Conventions
- Classes: `PascalCase` (e.g., `TranslateGemmaService`)
- Methods: `camelCase` (e.g., `translateString`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_TEXT_LENGTH`)
- Packages: `lowercase.with.dots`

## Development Workflow

### 1. Create a Feature Branch
```bash
git checkout -b feature/your-feature-name
```

### 2. Make Changes
- Implement your feature or fix
- Add tests for new functionality
- Update documentation if needed

### 3. Test Your Changes
- Run unit tests: `mvn test`
- Run integration tests: `mvn verify`
- Check code style: `mvn checkstyle:check`

### 4. Commit Your Changes
```bash
git add .
git commit -m "feat: add support for batch translation optimization"
```

### Commit Message Format
Use conventional commits:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes
- `refactor:` Code refactoring
- `test:` Test additions/changes
- `chore:` Maintenance tasks

Example:
```
fix: handle null input in translateString method

Resolves issue where translateString throws NPE when sourceText is null.
Added input validation and unit test.
Fixes #123
```

## Testing Guidelines

### Unit Tests
- All new features must have unit tests
- Use JUnit 5 for testing
- Mock external dependencies (Google Cloud, AEM services)
- Test both positive and negative scenarios

### Test Structure Example:
```java
@ExtendWith(MockitoExtension.class)
class TranslateGemmaServiceTest {

    @Mock
    private VertexAI mockVertexAI;
    
    @InjectMocks
    private TranslateGemmaTranslationServiceImpl service;
    
    @Test
    @DisplayName("Should translate text successfully")
    void shouldTranslateTextSuccessfully() throws TranslationException {
        // Given
        String sourceText = "Hello";
        String targetLang = "es";
        
        // When
        TranslationResult result = service.translateString(sourceText, "en", targetLang, 
                                                         ContentType.PLAIN, "general");
        
        // Then
        assertNotNull(result);
        assertEquals("es", result.getTargetLanguage());
        assertEquals(sourceText, result.getOriginalText());
        assertNotNull(result.getTranslatedText());
    }
}
```

### Integration Tests
- Test with actual Google Cloud API (when possible)
- Use test credentials and projects
- Clean up test resources

## Code Review Process

### Before Submitting PR
1. Ensure all tests pass
2. Update relevant documentation
3. Run security checks
4. Verify backwards compatibility
5. Check performance implications

### Pull Request Requirements
- Clear title and description
- Link to relevant issues
- Screenshots for UI changes
- Test steps included
- Performance considerations noted

## Release Process

### Version Management
- Use semantic versioning (MAJOR.MINOR.PATCH)
- Update CHANGELOG.md for all releases
- Tag releases in Git

### Release Checklist
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Security review completed
- [ ] Performance tests run
- [ ] Change log updated
- [ ] Version numbers updated
- [ ] Release notes prepared

## Areas of Contribution

### High Priority
1. **Enhanced Language Support**: Add more languages and dialects
2. **Performance Optimization**: Caching, batch processing improvements
3. **Security Enhancements**: Additional security validations
4. **Error Handling**: Better error messages and recovery

### Medium Priority
1. **Testing**: Additional unit and integration tests
2. **Documentation**: API docs, tutorials, examples
3. **Monitoring**: Metrics and health checks
4. **Configuration**: Additional OSGi configuration options

### Low Priority
1. **UI Components**: AEM UI for configuration
2. **Analytics**: Translation usage analytics
3. **Cache Strategies**: Advanced caching mechanisms
4. **Migration Tools**: Tools for migrating from other translators

## Development Guidelines

### Security Considerations
- Never commit credentials or secrets
- Validate all external inputs
- Use HTTPS for all external calls
- Implement rate limiting
- Log security events appropriately

### Performance Guidelines
- Minimize API calls
- Implement proper connection pooling
- Use appropriate timeouts
- Cache frequently accessed data
- Monitor memory usage

### AEM Integration Best Practices
- Follow OSGi development patterns
- Use proper service lifecycle management
- Implement configuration management
- Respect AEM threading models
- Use AEM logging framework

## Community Guidelines

### Code of Conduct
- Be respectful and inclusive
- Provide constructive feedback
- Welcome newcomers
- Focus on what is best for the community
- Show empathy toward other community members

### Getting Help
- Create GitHub issues for bugs
- Use discussions for questions
- Check existing issues before creating new ones
- Provide detailed reproduction steps
- Include environment information

## Documentation Standards

### What to Document
- Public APIs and methods
- Configuration options
- Installation and setup
- Troubleshooting steps
- Security considerations

### Documentation Style
- Use clear, concise language
- Include code examples
- Add screenshots where helpful
- Maintain consistent formatting
- Keep documentation up to date

## Tools and Resources

### Required Tools
- Maven 3.6+
- Java 11+
- Git
- IDE (IntelliJ IDEA recommended)

### Recommended Tools
- SonarQube for code quality
- Checkstyle for code formatting
- JaCoCo for code coverage
- Postman for API testing

### Useful Resources
- [AEM Documentation](https://experienceleague.adobe.com/docs/experience-manager.html)
- [Google Cloud Vertex AI Docs](https://cloud.google.com/vertex-ai/docs)
- [OSGi Development Guide](https://osgi.org/developer/)
- [Maven Best Practices](https://maven.apache.org/guides/)

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

## Questions?

- Create a GitHub issue for bugs or feature requests
- Start a GitHub discussion for questions
- Check existing documentation first
- Review existing issues for similar problems

Thank you for contributing to the AEM TranslateGemma Connector!