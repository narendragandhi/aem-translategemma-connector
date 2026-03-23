# Changelog

All notable changes to the AEM TranslateGemma Translation Connector will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-03-11

### Added
- **Fallback Translation Providers**
  - DeepL Provider (`DeepLProvider`) - DeepL API integration with free/pro tier support
  - OpenAI Provider (`OpenAIProvider`) - GPT-4o integration via OpenAI API  
  - Ollama Provider (`OllamaProvider`) - Local LLM deployment support
  - Provider Registry (`ProviderRegistry`) - Multi-provider management with automatic failover

- **Integration Tests**
  - Fallback provider selection tests
  - Provider language pair support tests
  - Provider health status tests
  - Batch translation with failover tests
  - Provider priority sorting tests

- **Service User Configuration**
  - RepoInit script for AEM service user setup
  - ACL permissions for translation memory and terminology storage

### Changed
- Updated PROJECT_STATUS.md with new features
- Updated API.md with provider interface documentation
- Enhanced pom.xml for new provider packages

### Fixed
- Integration test compilation errors resolved

---

## [1.1.0] - 2026-03-09

### Added
- **Translation Memory Service**: JCR-based TM with fuzzy matching (Levenshtein + Jaccard)
- **Multi-Provider Support**: Interface for 6 providers (TranslateGemma, Google Translate, DeepL, Microsoft, OpenAI, Ollama)
- **Experience Fragment Translation**: New service for XF translation
- **DAM Asset Metadata Translation**: Translate dc:title, dc:description, tags, etc.
- **i18n Dictionary Translation**: AEM Forms dictionary support with placeholder preservation
- **Visual Context Capture**: HTTP-based screenshot service for translator reference
- **Job Management Dashboard**: REST API at /bin/translationgemma/dashboard
- **Enhanced Configuration**: 20+ OSGi properties for provider selection, TM settings, etc.

### Features
- Automatic fallback between translation providers
- Parallel translation requests (configurable)
- Retry logic on failure
- Content type filtering for DAM/i18n
- Job statistics and filtering

### Test Coverage
- Added TranslationMemoryServiceTest with 12 test methods
- Coverage for TM store/retrieve, fuzzy matching, statistics

### Breaking Changes
- `TranslationConstants.ContentType.PLAIN_TEXT` renamed to `PLAIN`
- `TranslationConstants.TranslationStatus.PAUSED` not available

### Files Changed
- New Java files: 14
- Modified: pom.xml, TranslateGemmaConfig.java, README.md
- New test files: 1

## [1.0.0] - 2024-01-17

### Added
- **Initial Release**: Complete AEM translation connector for TranslateGemma
- **Google Cloud Integration**: Vertex AI API integration with TranslateGemma model
- **Multi-Language Support**: Support for 20+ languages including English, Spanish, French, German, Chinese, Japanese, Arabic, and more
- **AEM Framework Integration**: Native integration with AEM Translation Integration Framework
- **OSGi Configuration**: Configurable service properties through AEM Web Console
- **Synchronous Translation**: Real-time translation via `translateString()` and `translateArray()` methods
- **Asynchronous Translation**: Job-based translation workflows for batch processing
- **Language Detection**: Automatic source language detection
- **Content Type Support**: Handles both plain text and HTML content
- **Security Features**: Secure credential handling and input validation
- **Comprehensive Documentation**: README, API docs, security guide, and contributing guidelines
- **Unit Testing**: JUnit 5 based test suite with Mockito
- **Build Automation**: Maven-based build with deployment scripts

### Features
- **TranslationService Implementation**: Full implementation of AEM's TranslationService interface
- **Configuration Management**: OSGi-based configuration with project ID, location, language settings
- **Error Handling**: Comprehensive exception handling with meaningful error messages
- **Performance Optimization**: Connection pooling and configurable timeouts
- **Logging**: SLF4J-based logging with appropriate log levels
- **Input Validation**: Length limits, language code validation, content type checking

### Supported Languages
| Code | Language | Code | Language |
|------|----------|------|----------|
| en | English | es | Spanish |
| fr | French | de | German |
| it | Italian | pt | Portuguese |
| ru | Russian | ja | Japanese |
| ko | Korean | zh | Chinese |
| ar | Arabic | hi | Hindi |
| nl | Dutch | sv | Swedish |
| da | Danish | no | Norwegian |
| fi | Finnish | pl | Polish |
| tr | Turkish |   |   |

### Configuration Options
- Google Cloud Project ID
- Google Cloud Location (default: us-central1)
- Service enable/disable flag
- Default source and target languages
- Maximum translation length (default: 5000 characters)
- Connection and read timeouts
- Default content category

### Security Features
- Secure Google Cloud authentication
- Input validation and sanitization
- No hardcoded credentials
- HTTPS-only external communication
- Configurable timeouts and limits

### Documentation
- **README.md**: Comprehensive setup and usage guide
- **API.md**: Detailed API documentation with examples
- **SECURITY.md**: Security considerations and best practices
- **CONTRIBUTING.md**: Development guidelines and contribution process
- **build.sh**: Automated build and deployment script

### Dependencies
- **AEM SDK API**: Core AEM integration
- **Google Cloud Vertex AI**: TranslateGemma model access
- **OSGi Annotations**: Component configuration
- **SLF4J**: Logging framework
- **Jackson**: JSON processing
- **JUnit 5**: Unit testing framework
- **Mockito**: Test mocking framework

## Project Structure (v1.1.0)

```
aem-translategemma-connector/
├── src/main/java/com/example/aem/translation/
│   ├── config/
│   │   └── TranslateGemmaConfig.java           # OSGi configuration (enhanced)
│   ├── service/
│   │   └── TranslateGemmaTranslationService.java
│   ├── impl/
│   │   ├── TranslateGemmaTranslationServiceImpl.java
│   │   └── TranslateGemmaBundleActivator.java
│   ├── tm/                                      # NEW: Translation Memory
│   │   ├── TranslationMemoryService.java
│   │   └── impl/JcrTranslationMemoryServiceImpl.java
│   ├── xf/                                      # NEW: Experience Fragments
│   │   ├── ExperienceFragmentTranslationService.java
│   │   └── impl/ExperienceFragmentTranslationServiceImpl.java
│   ├── dam/                                     # NEW: DAM Metadata
│   │   ├── DamMetadataTranslationService.java
│   │   └── impl/DamMetadataTranslationServiceImpl.java
│   ├── i18n/                                    # NEW: i18n Dictionaries
│   │   ├── I18nDictionaryTranslationService.java
│   │   └── impl/I18nDictionaryTranslationServiceImpl.java
│   ├── visualcontext/                           # NEW: Visual Context
│   │   ├── VisualContextService.java
│   │   └── impl/VisualContextServiceImpl.java
│   ├── job/                                     # NEW: Job Management
│   │   ├── TranslationJobManager.java
│   │   └── impl/TranslationJobManagerImpl.java
│   ├── servlet/                                 # NEW: REST API
│   │   └── TranslationDashboardServlet.java
│   └── provider/                                # NEW: Multi-Provider
│       └── TranslationProvider.java
├── src/test/java/com/example/aem/translation/
│   ├── TranslateGemmaTranslationServiceImplTest.java
│   ├── TranslateGemmaConnectorTest.java
│   └── tm/                                      # NEW: TM Tests
│       └── TranslationMemoryServiceTest.java
├── docs/
│   ├── API.md
│   ├── SECURITY.md
│   └── CONTRIBUTING.md
├── README.md
├── CHANGELOG.md
├── pom.xml
└── build.sh
```

## Technical Specifications

### System Requirements
- **AEM**: AEM as a Cloud Service or AEM 6.5+
- **Java**: Java 11+
- **Maven**: Maven 3.6+
- **Google Cloud**: Vertex AI API access and TranslateGemma model

### Performance Characteristics
- **Max Request Size**: 5000 characters (configurable)
- **Connection Timeout**: 30 seconds (configurable)
- **Read Timeout**: 60 seconds (configurable)
- **Supported Content Types**: Plain text, HTML
- **Translation Methods**: Synchronous, Asynchronous (simulated)

### Integration Points
- **AEM Translation Framework**: Native integration
- **Google Cloud Vertex AI**: TranslateGemma model
- **OSGi Service Registry**: Service lifecycle management
- **AEM Configuration**: Web Console integration

---

## Version History

### Future Roadmap

#### v1.2.0 (Planned)
- AEM UI configuration components
- Content preview in target languages
- Translation quality scoring
- Real-time translation progress tracking
- Human translation bridge
- LQA (Linguistic Quality Assurance) tools

#### v2.0.0 (Future)
- Multi-model support (Gemini, PaLM, etc.)
- Advanced content analysis
- Automated language detection improvements
- Enterprise security features
- Scalability enhancements

---

## Support and Maintenance

### Maintenance Policy
- **Security Updates**: As needed
- **Bug Fixes**: Priority based on severity
- **Feature Requests**: Community-driven
- **Dependencies**: Regular security updates

### Compatibility
- **AEM Versions**: Cloud Service, 6.5+
- **Java Versions**: 11, 17
- **Google Cloud API**: Vertex AI latest stable

### Known Limitations (v1.1.0)
- Google Cloud rate limits apply
- Dependent on TranslateGemma model availability
- Content size restrictions per request
- External service dependency
- Translation Memory persistence is in-memory only (JCR write stubbed)
- Visual context generates mock screenshots (requires AEM runtime for HTTP capture)
- Human translation bridge not implemented
- LQA tools not implemented

---

*For detailed release notes and migration guides, see the documentation.*