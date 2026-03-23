# API Documentation for AEM TranslateGemma Connector

This document provides detailed API documentation for the TranslateGemma Translation Service implementation.

## Service Interface

### TranslateGemmaTranslationService

The main service interface extending AEM's `TranslationService` with TranslateGemma-specific implementations.

#### Core Methods

##### translateString

```java
TranslationResult translateString(
    String sourceString, 
    String sourceLanguage, 
    String targetLanguage, 
    ContentType contentType, 
    String contentCategory
) throws TranslationException
```

**Parameters:**
- `sourceString`: Text to translate
- `sourceLanguage`: Source language code (ISO 639-1, can be null for auto-detection)
- `targetLanguage`: Target language code (ISO 639-1)
- `contentType`: Content type (`ContentType.PLAIN` or `ContentType.HTML`)
- `contentCategory`: Content category for translation context

**Returns:** `TranslationResult` with translated text and metadata

**Example:**
```java
TranslationResult result = translationService.translateString(
    "Hello world", 
    "en", 
    "es", 
    ContentType.PLAIN, 
    "general"
);
String translated = result.getTranslatedText(); // "Hola mundo"
```

##### translateArray

```java
TranslationResult[] translateArray(
    String[] sourceStringArr, 
    String sourceLanguage, 
    String targetLanguage, 
    ContentType contentType, 
    String contentCategory
) throws TranslationException
```

**Parameters:** Array of texts to translate, same structure as `translateString`

**Returns:** Array of `TranslationResult` objects in the same order

##### detectLanguage

```java
String detectLanguage(
    String detectSource, 
    ContentType contentType
) throws TranslationException
```

**Parameters:**
- `detectSource`: Text to analyze
- `contentType`: Content type of the text

**Returns:** Detected language code (ISO 639-1) or "UNKNOWN"

##### isDirectionSupported

```java
boolean isDirectionSupported(
    String sourceLanguage, 
    String targetLanguage
) throws TranslationException
```

**Returns:** `true` if the language pair is supported, `false` otherwise

#### Asynchronous Methods

##### createTranslationJob

```java
String createTranslationJob(
    String name, 
    String description, 
    String strSourceLanguage, 
    String strTargetLanguage, 
    Date dueDate, 
    TranslationState state, 
    TranslationMetadata jobMetadata
) throws TranslationException
```

**Returns:** Unique job ID for tracking

##### uploadTranslationObject

```java
String uploadTranslationObject(
    String strTranslationJobID, 
    TranslationObject translationObject
) throws TranslationException
```

**Returns:** Unique object ID within the job

##### getTranslationJobStatus

```java
TranslationStatus getTranslationJobStatus(
    String strTranslationJobID
) throws TranslationException
```

**Returns:** Current status of the translation job

## Configuration API

### TranslateGemmaConfig

OSGi configuration interface for the service.

#### Basic Configuration Properties

| Property | Type | Default | Description |
|-----------|------|---------|-------------|
| `projectId()` | String | Required | Google Cloud Project ID |
| `location()` | String | "us-central1" | Google Cloud location |
| `enabled()` | boolean | true | Enable/disable service |
| `defaultSourceLanguage()` | String | "en" | Default source language |
| `defaultTargetLanguage()` | String | "es" | Default target language |
| `maxTranslationLength()` | int | 5000 | Max characters per request |
| `connectionTimeout()` | int | 30 | Connection timeout (seconds) |
| `readTimeout()` | int | 60 | Read timeout (seconds) |

#### Resilience Configuration (v1.2.0+)

| Property | Type | Default | Description |
|-----------|------|---------|-------------|
| `retryMaxAttempts()` | int | 3 | Maximum retry attempts |
| `retryWaitDurationMs()` | long | 1000 | Wait between retries (ms) |
| `circuitBreakerFailureRateThreshold()` | int | 50 | Failure % to open circuit |
| `circuitBreakerSlowCallRateThreshold()` | int | 100 | Slow call % to open circuit |
| `circuitBreakerSlowCallDurationMs()` | int | 5000 | Slow call threshold (ms) |
| `circuitBreakerWaitDurationS()` | int | 30 | Circuit open duration (s) |

#### Caching Configuration (v1.2.0+)

| Property | Type | Default | Description |
|-----------|------|---------|-------------|
| `enableCaching()` | boolean | true | Enable translation cache |
| `cacheMaxSize()` | int | 1000 | Maximum cache entries |
| `cacheExpireAfterMinutes()` | int | 60 | Cache TTL (minutes) |

#### Monitoring Configuration (v1.2.0+)

| Property | Type | Default | Description |
|-----------|------|---------|-------------|
| `enableMetrics()` | boolean | true | Enable metrics collection |
| `metricsEnabled()` | boolean | false | Enable detailed reporting |
| `requestTimeoutSeconds()` | int | 120 | Request timeout (seconds) |
| `batchSize()` | int | 10 | Batch translation size |

## Supported Languages

The connector supports the following languages:

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

## Error Handling

### TranslationException

All methods may throw `TranslationException` with the following common scenarios:

- **Service Unavailable**: Google Cloud credentials not configured
- **Invalid Language**: Unsupported language code
- **Network Issues**: Connection timeout or failure
- **Quota Exceeded**: API rate limits reached
- **Invalid Input**: Empty text, oversized content, invalid content type

### Error Codes

| Error | Description | Resolution |
|-------|-------------|------------|
| `UNKNOWN_LANGUAGE` | Language detection failed | Provide explicit source language |
| `TRANSLATION_FAILED` | Google Cloud API error | Check credentials and connectivity |
| `INVALID_DIRECTION` | Unsupported language pair | Verify language codes |
| `CONTENT_TOO_LARGE` | Exceeds max length | Split content or increase limit |

### TranslateGemmaException (v1.2.0+)

Custom exception with user-friendly error messages for end users:

```java
try {
    TranslationResult result = translationService.translateString(...);
} catch (TranslateGemmaException e) {
    // Get user-friendly message for display
    String userMessage = e.getUserMessage();
    
    // Get technical error type
    TranslateGemmaException.ErrorType errorType = e.getErrorType();
    
    // Handle specific error types
    switch (errorType) {
        case CIRCUIT_BREAKER_OPEN:
            // Service degraded, retry later
            break;
        case RATE_LIMIT_EXCEEDED:
            // Wait and retry
            break;
        case CREDENTIALS_ERROR:
            // Check GCP configuration
            break;
    }
}
```

| Error Type | User Message | Cause |
|------------|--------------|-------|
| `TRANSLATION_FAILED` | "Translation service is temporarily unable to process your request. Please try again later." | API errors |
| `SERVICE_UNAVAILABLE` | "Translation service is currently unavailable. Please contact your administrator." | Service not initialized |
| `CIRCUIT_BREAKER_OPEN` | "Translation service is experiencing high error rates. Please try again later." | Too many failures |
| `RATE_LIMIT_EXCEEDED` | "Too many translation requests. Please wait a moment and try again." | API quota exceeded |
| `INVALID_CONFIGURATION` | "Translation service is misconfigured. Please contact your administrator." | Config errors |
| `CREDENTIALS_ERROR` | "Unable to authenticate with translation service. Please verify credentials." | GCP auth failed |
| `TIMEOUT` | "Translation request timed out. Please try again." | Request timeout |
| `NETWORK_ERROR` | "Unable to connect to translation service. Please check your network connection." | Network issues |

## Usage Examples

### Basic Translation

```java
@Reference
private TranslateGemmaTranslationService translationService;

public void translateContent() {
    try {
        TranslationResult result = translationService.translateString(
            "Welcome to our website", 
            "en", 
            "es", 
            ContentType.PLAIN, 
            "general"
        );
        
        System.out.println("Original: " + result.getOriginalText());
        System.out.println("Translated: " + result.getTranslatedText());
        System.out.println("Source Language: " + result.getSourceLanguage());
        System.out.println("Target Language: " + result.getTargetLanguage());
        
    } catch (TranslationException e) {
        log.error("Translation failed", e);
    }
}
```

### Batch Translation

```java
public void translateMultipleStrings() {
    String[] texts = {
        "Hello", 
        "How are you?", 
        "Good morning"
    };
    
    try {
        TranslationResult[] results = translationService.translateArray(
            texts, 
            "en", 
            "fr", 
            ContentType.PLAIN, 
            "general"
        );
        
        for (TranslationResult result : results) {
            System.out.println(result.getOriginalText() + " -> " + result.getTranslatedText());
        }
        
    } catch (TranslationException e) {
        log.error("Batch translation failed", e);
    }
}
```

### Language Detection

```java
public void detectLanguageOfText(String text) {
    try {
        String detectedLang = translationService.detectLanguage(text, ContentType.PLAIN);
        System.out.println("Detected language: " + detectedLang);
        
        // Verify if the direction is supported
        boolean supported = translationService.isDirectionSupported(detectedLang, "en");
        System.out.println("Translation to English supported: " + supported);
        
    } catch (TranslationException e) {
        log.error("Language detection failed", e);
    }
}
```

### Asynchronous Translation Job

```java
public void createTranslationJob() {
    try {
        String jobId = translationService.createTranslationJob(
            "Product Descriptions Translation",
            "Translate product pages for Spanish market",
            "en",
            "es",
            new Date(System.currentTimeMillis() + 86400000), // Tomorrow
            TranslationState.CREATED,
            null
        );
        
        System.out.println("Created translation job: " + jobId);
        
        // Check status
        TranslationStatus status = translationService.getTranslationJobStatus(jobId);
        System.out.println("Job status: " + status);
        
    } catch (TranslationException e) {
        log.error("Translation job creation failed", e);
    }
}
```

## Performance Considerations

### Rate Limiting
- Google Cloud Vertex AI has rate limits
- Implement exponential backoff for retries
- Monitor API quota usage

### Caching
- Consider caching frequently translated content
- Use translation results with confidence scores
- Implement cache invalidation strategy

### Batch Operations
- Use `translateArray()` for multiple texts
- Group translations by language pair
- Implement parallel processing for large batches

## Monitoring and Metrics

### Key Metrics to Monitor
- Translation request rate
- Average response time
- Error rates by type
- API quota utilization
- Language pair distribution
- Cache hit/miss ratio
- Circuit breaker state

### Health Check API (v1.2.0+)

```java
// Get service health status
TranslateGemmaTranslationServiceImpl service = ...;
String healthStatus = service.getHealthStatus();
System.out.println(healthStatus);

// Sample output:
// TranslateGemma Service Status:
// - Service Available: true
// - Circuit Breaker Open: false
// - Caching Enabled: true
// - Metrics Enabled: true
// - Cache Hit Rate: 45.00%
```

```java
// Get cache statistics
TranslationCache.CacheStats stats = service.getCacheStats();
long hits = stats.getTranslationHitCount();
long misses = stats.getTranslationMissCount();
double hitRate = stats.getTranslationHitRate();
```

```java
// Check circuit breaker state
boolean isOpen = service.isCircuitBreakerOpen();
```

### Logging Configuration
```xml
<Logger name="com.example.aem.translation" level="INFO"/>
<Logger name="com.google.cloud.vertexai" level="WARN"/>
```

### Metrics Available (v1.2.0+)

| Metric | Type | Description |
|--------|------|-------------|
| `translation.requests.total` | Counter | Total translation requests |
| `translation.success.total` | Counter | Successful translations |
| `translation.failures.total` | Counter | Failed translations |
| `translation.cache.hits.total` | Counter | Cache hits |
| `translation.latency` | Timer | Translation latency |
| `translation.retry.total` | Counter | Retry attempts |
| `translation.circuitbreaker.open.total` | Counter | Circuit breaker opens |

## Version History

### Translation Provider Interface

The `TranslationProvider` interface allows implementing custom translation providers as fallbacks:

```java
public interface TranslationProvider {
    enum ProviderType {
        TRANSLATEGEMMA,
        GOOGLE_TRANSLATE,
        DEEPL,
        MICROSOFT_TRANSLATOR,
        OPENAI,
        OLLAMA
    }
    
    String getProviderName();
    ProviderType getProviderType();
    boolean isAvailable();
    
    TranslationResult translate(String sourceText, String sourceLanguage, 
                               String targetLanguage, ContentType contentType,
                               String category) throws Exception;
    
    String detectLanguage(String text) throws Exception;
    boolean supportsLanguagePair(String sourceLanguage, String targetLanguage);
    
    default double getMatchingScore() { return 0.5; }
    default long getAverageResponseTime() { return 0; }
}
```

### Provider Registry

The `ProviderRegistry` manages multiple providers and handles failover:

```java
@Component
public class ProviderRegistry {
    public TranslationProvider getAvailableProvider(ProviderType preferred);
    public TranslationProvider getPrimaryProvider();
    public List<TranslationProvider> getAvailableProviders();
    public TranslationProvider findProviderForLanguagePair(String source, String target);
    public Map<String, Object> getHealthStatus();
}
```

## Version History

- **v1.2.0**: Fallback providers and enhanced integration
  - DeepL provider integration
  - OpenAI provider integration
  - Ollama local provider support
  - ProviderRegistry with automatic failover
  - Enhanced integration tests
  - Service user configuration (repoinit)

- **v1.1.0**: Resilience and monitoring enhancements
  - Retry with exponential backoff
  - Circuit breaker pattern
  - Caffeine caching
  - Micrometer metrics

- **v1.0.0**: Initial release with basic translation functionality
  - Synchronous translation support
  - 20+ language support
  - OSGi configuration
  - AEM Integration Framework compatibility