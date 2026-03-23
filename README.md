# AEM TranslateGemma Translation Connector

A custom AEM translation connector that integrates Google's TranslateGemma model for high-quality machine translation. Now with enhanced features comparable to enterprise TMS solutions like Smartling.

## Overview

This connector provides seamless integration between Adobe Experience Manager (AEM) and Google Cloud's TranslateGemma model through Vertex AI. It implements the AEM TranslationService interface to enable automatic content translation within AEM's translation workflow.

## Features

### Core Translation
- **High-Quality Translation**: Leverages Google's TranslateGemma model for accurate translations
- **Multi-Language Support**: Supports 20+ major languages
- **AEM Integration**: Native integration with AEM's Translation Integration Framework
- **Multi-Provider Support**: Configure multiple translation providers (TranslateGemma, Google Translate, DeepL, Microsoft, OpenAI, Ollama)
- **Translation Memory**: Built-in TM with JCR persistence and fuzzy matching
- **Fallback Providers**: Automatic failover to backup providers on failure

### Content Type Support
- **Pages**: Full page translation via AEM workflow
- **Content Fragments**: Translate structured content fragments
- **Experience Fragments**: Translate XF variations
- **DAM Assets**: Translate asset metadata
- **i18n Dictionaries**: Translate language dictionaries for Forms
- **HTML Content**: Preserve HTML structure during translation

### Enterprise Features
- **Translation Memory**: Persistent storage with fuzzy matching (SmartMatch-like functionality)
- **Visual Context**: Capture screenshots for translator reference
- **Job Dashboard**: REST API for translation job management
- **Parallel Translation**: Configurable concurrent translation requests
- **Retry Logic**: Automatic retry with configurable attempts
- **Comprehensive Logging**: DEBUG-level logging for troubleshooting

### Production-Ready Features (v1.2.0)
- **Resilience**: Retry with exponential backoff and circuit breaker pattern
- **Caching**: Caffeine-based translation cache for improved performance
- **Metrics**: Micrometer-based metrics collection
- **Security**: Input sanitization to prevent prompt injection
- **Custom Exceptions**: User-friendly error messages for end users

## Prerequisites

1. **AEM as a Cloud Service** or **AEM 6.5+**
2. **Google Cloud Platform Account** with:
   - Vertex AI API enabled
   - TranslateGemma model access
3. **Java 11+**
4. **Maven 3.6+**

## Setup Instructions

### 1. Google Cloud Setup

1. Create a Google Cloud Project or use an existing one
2. Enable the Vertex AI API:
   ```bash
   gcloud services enable aiplatform.googleapis.com --project=YOUR_PROJECT_ID
   ```
3. Set up authentication (service account key or Application Default Credentials)
4. Ensure you have access to the TranslateGemma model in Vertex AI Model Garden

### 2. Build and Deploy

```bash
mvn clean install
```

Deploy the bundle to AEM:
- Using Package Manager: Upload the generated `.jar` file
- Using Maven: `mvn clean install -PautoInstallPackage`

### 3. Configure the Service

Navigate to AEM Web Console: `/system/console/configMgr`
Find "TranslateGemma Translation Service Configuration"

### 4. Configure AEM Translation Framework

1. Navigate to **Tools > Cloud Services > Translation Cloud Services**
2. Create a new cloud configuration for the TranslateGemma connector
3. Select "TranslateGemma Translation Service" as the Translation Provider

## New Enhanced Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `projectId` | Google Cloud Project ID | Required |
| `location` | Google Cloud location | `us-central1` |
| `enabled` | Enable/disable service | `true` |
| `translationProvider` | Primary provider (translategemma, google_translate, deepl, openai, ollama) | `translategemma` |
| `enableTranslationMemory` | Enable TM lookup | `true` |
| `tmMinScore` | TM minimum match score (0.0-1.0) | `0.85` |
| `storeToTranslationMemory` | Auto-store translations | `true` |
| `enableFallbackProviders` | Enable provider failover | `true` |
| `modelName` | Translation model name | `google/translategemma-2b-it` |
| `parallelTranslations` | Concurrent requests | `3` |
| `retryAttempts` | Retry on failure | `2` |
| `enableVisualContext` | Capture screenshots | `false` |
| `enableDamTranslation` | Translate DAM metadata | `true` |
| `enableI18nTranslation` | Translate i18n dictionaries | `true` |
| **Production Features** |||
| `retryMaxAttempts` | Maximum retry attempts | `3` |
| `retryWaitDurationMs` | Wait between retries (ms) | `1000` |
| `circuitBreakerFailureRateThreshold` | Failure % to open circuit | `50` |
| `circuitBreakerSlowCallRateThreshold` | Slow call % to open circuit | `100` |
| `circuitBreakerSlowCallDurationMs` | Slow call threshold (ms) | `5000` |
| `circuitBreakerWaitDurationS` | Circuit open wait time (s) | `30` |
| `cacheMaxSize` | Cache maximum entries | `1000` |
| `cacheExpireAfterMinutes` | Cache TTL (minutes) | `60` |
| `enableCaching` | Enable translation cache | `true` |
| `enableMetrics` | Enable metrics collection | `true` |
| `requestTimeoutSeconds` | Request timeout (seconds) | `120` |
| `batchSize` | Batch translation size | `10` |

## Usage

### Basic Translation

```java
@Reference
private TranslateGemmaTranslationService translationService;

TranslationResult result = translationService.translateString(
    content, "en", "es", ContentType.HTML, "general"
);
```

### Translation Memory

```java
@Reference
private TranslationMemoryService tmService;

// Find matches
List<TMEntry> matches = tmService.findMatches(
    sourceText, "en", "es", ContentType.HTML, "general", 0.85, 5
);

// Store translation
tmService.storeTranslation(source, target, "en", "es", 
    ContentType.HTML, "general", "/content/page", 5);
```

### Content Fragment Translation

```java
@Reference
private ContentFragmentTranslationService cfService;

ContentFragmentTranslationResult result = cfService.translateContentFragment(
    fragment, "fr", "marketing"
);
```

### DAM Metadata Translation

```java
@Reference
private DamMetadataTranslationService damService;

MetadataTranslationResult result = damService.translateAssetMetadata(
    "/content/dam/image.jpg", "de", "general", 
    new String[]{"dc:title", "dc:description", "dc:subject"}
);
```

### i18n Dictionary Translation

```java
@Reference
private I18nDictionaryTranslationService i18nService;

DictionaryTranslationResult result = i18nService.translateDictionary(
    "/libs/my-app/i18n/en", "ja", "general", true
);
```

### Visual Context

```java
@Reference
private VisualContextService vcService;

ContextCapture capture = vcService.capturePageContext("/content/site/en/home");
byte[] screenshot = capture.getScreenshot();
```

### Job Management Dashboard

```bash
# Get dashboard stats
GET /bin/translationgemma/dashboard/stats

# Get all jobs
GET /bin/translationgemma/dashboard/jobs

# Get job by ID
GET /bin/translationgemma/dashboard/jobs?jobId=xxx

# Get jobs by status
GET /bin/translationgemma/dashboard/jobs?status=TRANSLATION_IN_PROGRESS

# Create job
POST /bin/translationgemma/dashboard/jobs?action=create&name=MyJob&sourceLanguage=en&targetLanguage=fr

# Start job
POST /bin/translationgemma/dashboard/jobs?action=start&jobId=xxx
```

## Supported Languages

| Code | Language | Code | Language |
|------|----------|------|----------|
| en   | English  | fr   | French   |
| es   | Spanish  | de   | German   |
| pt   | Portuguese | it   | Italian  |
| ru   | Russian  | ja   | Japanese |
| ko   | Korean   | zh   | Chinese  |
| ar   | Arabic   | hi   | Hindi    |
| nl   | Dutch    | sv   | Swedish  |
| da   | Danish   | no   | Norwegian |
| fi   | Finnish  | pl   | Polish   |
| tr   | Turkish  |      |          |

## Project Structure

```
src/main/java/com/example/aem/translation/
├── config/
│   └── TranslateGemmaConfig.java           # Enhanced OSGi configuration
├── service/
│   └── TranslateGemmaTranslationService.java  # Main service interface
├── impl/
│   ├── TranslateGemmaTranslationServiceImpl.java  # Main implementation
│   └── TranslateGemmaBundleActivator.java
├── tm/
│   ├── TranslationMemoryService.java         # TM interface
│   └── tm/impl/
│       └── JcrTranslationMemoryServiceImpl.java  # JCR-based TM
├── xf/
│   ├── ExperienceFragmentTranslationService.java
│   └── xf/impl/
│       └── ExperienceFragmentTranslationServiceImpl.java
├── dam/
│   ├── DamMetadataTranslationService.java
│   └── dam/impl/
│       └── DamMetadataTranslationServiceImpl.java
├── i18n/
│   ├── I18nDictionaryTranslationService.java
│   └── i18n/impl/
│       └── I18nDictionaryTranslationServiceImpl.java
├── visualcontext/
│   ├── VisualContextService.java
│   └── visualcontext/impl/
│       └── VisualContextServiceImpl.java
├── job/
│   ├── TranslationJobManager.java
│   └── job/impl/
│       └── TranslationJobManagerImpl.java
├── servlet/
│   └── TranslationDashboardServlet.java      # REST API
└── provider/
    └── TranslationProvider.java               # Multi-provider interface
```

## Comparing to Smartling

| Feature | TranslateGemma | Smartling |
|---------|---------------|-----------|
| Content Fragments | ✅ | ✅ |
| Experience Fragments | ✅ | ✅ |
| DAM Metadata | ✅ | ✅ |
| i18n Dictionaries | ✅ | ✅ |
| Translation Memory | ✅ (Basic) | ✅ (Advanced) |
| Visual Context | ✅ (HTTP) | ✅ (Full) |
| Multi-Provider | ✅ (6 providers) | ✅ (20+ MT) |
| Job Dashboard | ✅ REST API | ✅ Full UI |
| Human Translation | ❌ | ✅ (4K+ linguists) |
| LQA Tools | ❌ | ✅ |
| Instant Translation | ❌ | ✅ |

## Building

```bash
mvn clean install
```

## Troubleshooting

### Enable Debug Logging

```xml
<Logger name="com.example.aem.translation" level="DEBUG"/>
<Logger name="com.google.cloud.vertexai" level="DEBUG"/>
```

### Common Issues

#### 1. Service Not Available
- Check Google Cloud authentication
- Verify Vertex AI API is enabled
- Confirm project ID and location are correct
- Check service account has required permissions

#### 2. Translation Fails
- Verify source/target language codes
- Check content length limits (default: 5000 chars)
- Review Google Cloud quotas
- Check circuit breaker status via health check

#### 3. Configuration Issues
- Ensure OSGi configuration is saved
- Check service ranking if multiple translators exist (use ranking=100)
- Verify bundle is active in /system/console/bundles

#### 4. High Error Rates
- Check circuit breaker is not open: `service.isCircuitBreakerOpen()`
- Review health status: `service.getHealthStatus()`
- Check GCP quotas in Google Cloud Console

#### 5. Performance Issues
- Enable caching: `enableCaching=true`
- Check cache hit rate in health status
- Increase cache size: `cacheMaxSize=5000`
- Adjust batch size: `batchSize=20`

#### 6. AEM Cloud Service Issues
- Verify environment variables in Cloud Manager
- Check Cloud Manager logs for errors
- Ensure secrets are configured as secrets (not variables)
- Verify GCP service account has Vertex AI access

### Diagnostic Commands

```bash
# Check bundle status
curl -u admin:admin http://localhost:4502/system/console/bundles/com.example.aem.translation

# Test translation directly
curl -X POST -u admin:admin http://localhost:4502/bin/translationgemma/dashboard/stats

# Check configuration
curl -u admin:admin http://localhost:4502/system/console/configMgr/com.example.aem.translation.config.TranslateGemmaConfig
```

### Health Check (v1.2.0+)

```java
// In a JSP or servlet
TranslateGemmaTranslationServiceImpl service = ...;
out.println(service.getHealthStatus());
```

Output example:
```
TranslateGemma Service Status:
- Service Available: true
- Circuit Breaker Open: false
- Caching Enabled: true
- Metrics Enabled: true
- Cache Hit Rate: 45.00%
```

## Version History

- **1.2.0**: Production-ready release
  - Resilience: Retry with exponential backoff
  - Resilience: Circuit breaker pattern
  - Performance: Caffeine-based translation cache
  - Monitoring: Micrometer metrics
  - Security: Input sanitization
  - Security: Custom exceptions with user-friendly messages
  - Health check endpoint

- **1.1.0**: Enhanced feature release
  - Translation Memory with JCR persistence
  - Multi-provider support (DeepL, Microsoft, OpenAI, Ollama)
  - Experience Fragment translation
  - DAM metadata translation
  - i18n dictionary translation
  - Visual context capture
  - Job management dashboard with REST API

- **1.0.0**: Initial release
  - Basic translation functionality
  - OSGi configuration
  - AEM Translation Framework integration
  - Support for 20+ languages
