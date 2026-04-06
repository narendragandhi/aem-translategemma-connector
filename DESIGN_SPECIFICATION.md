# AEM TranslateGemma Connector - Design Specification
# Extracted from Implementation (Reverse Engineered)

---

## 1. System Overview

### 1.1 Purpose
Machine translation connector for Adobe Experience Manager using Google Cloud Vertex AI (TranslateGemma model).

### 1.2 Architecture Layers
```
┌─────────────────────────────────────────────────────────────┐
│                    AEM Translation Framework                 │
├─────────────────────────────────────────────────────────────┤
│  TranslateGemmaTranslationService (Main Service)           │
├─────────────────────────────────────────────────────────────┤
│  Translation Memory │ Terminology │ DITA │ Continuous Loc │
├─────────────────────────────────────────────────────────────┤
│  Resilience Layer (Retry │ Circuit Breaker │ Cache)       │
├─────────────────────────────────────────────────────────────┤
│              Google Cloud Vertex AI (TranslateGemma)        │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Functional Specifications

### 2.1 Core Translation Service

### 2.1 Core Translation Service

**Interface:** `TranslateGemmaTranslationService extends TranslationService`

| Operation | Input | Output | Behavior |
|-----------|-------|--------|----------|
| `translateString()` | text, sourceLang, targetLang, ContentType, category | TranslationResult | Single string translation |
| `translateArray()` | String[], sourceLang, targetLang, ContentType, category | TranslationResult[] | Batch translation |
| `detectLanguage()` | text, ContentType | String (ISO 639-1) | Auto-detect source language |
| `isDirectionSupported()` | sourceLang, targetLang | boolean | Check language pair support |
| `supportedLanguages()` | void | Map<String, String> | List supported languages |

**Supported Languages:** en, es, fr, de, it, pt, ru, ja, ko, zh, ar, hi, nl, sv, da, no, fi, pl, tr (19 languages)

### 2.2 Translation Job Management

| Operation | Input | Output |
|-----------|-------|--------|
| `createTranslationJob()` | name, desc, sourceLang, targetLang, dueDate, state, metadata | jobId (UUID) |
| `uploadTranslationObject()` | jobId, TranslationObject | objectId |
| `getTranslationJobStatus()` | jobId | TranslationStatus |
| `updateTranslationJobState()` | jobId, state | TranslationStatus |
| `getTranslatedObject()` | jobId, TranslationObject | InputStream |

### 2.3 AEM Sites Translation Service

**Interface:** `AEMSitesTranslationService`

| Operation | Input | Output |
|-----------|-------|--------|
| `translatePage()` | Page, targetLanguage, category | PageTranslationResult |
| `translateContentFragment()` | ContentFragment, targetLanguage, category | ContentFragmentTranslationResult |
| `translateComponents()` | Resource, source/targetLang, category | Map<String, ComponentTranslationResult> |
| `translateAssetMetadata()` | Asset, source/targetLang, category | AssetTranslationResult |
| `translateResourceTags()` | Resource, source/targetLang | List<Tag> |
| `createTranslationJob()` | List<Page>, List<Asset>, source/targetLang, jobName | jobId |
| `getJobStatus()` | jobId | TranslationJobStatus |

### 2.4 Translation Provider (Fallback)

**Interface:** `TranslationProvider`

```java
interface TranslationProvider {
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

### 2.5 Content Fragment Translation

**Interface:** `ContentFragmentTranslationService`

| Operation | Behavior |
|-----------|----------|
| `translateContentFragment()` | Translate structured content fragment |
| `getFragmentStructure()` | Parse CF model elements |

### 2.6 Experience Fragment Translation

**Interface:** `ExperienceFragmentTranslationService`

| Operation | Behavior |
|-----------|----------|
| `translateExperienceFragment()` | Translate XF variations |

### 2.7 DAM Metadata Translation

**Interface:** `DamMetadataTranslationService`

| Operation | Behavior |
|-----------|----------|
| `translateAssetMetadata()` | Translate asset title, description, custom metadata |
| `translateAssetTags()` | Translate DAM tags |

### 2.8 i18n Dictionary Translation

**Interface:** `I18nDictionaryTranslationService`

| Operation | Behavior |
|-----------|----------|
| `translateDictionary()` | Translate i18n XML dictionaries |
| `mergeDictionaries()` | Merge translated dictionaries |

### 2.9 Translation Job Manager

**Interface:** `TranslationJobManager`

| Operation | Behavior |
|-----------|----------|
| `getDashboardStats()` | Get overall translation statistics |
| `getJobInfo(jobId)` | Get detailed job information |
| `getJobsByStatus(status)` | List jobs by status |
| `getAllJobs()` | List all translation jobs |
| `createJob(...)` | Create new translation job |
| `startJob(jobId)` | Start/queue job for translation |
| `pauseJob(jobId)` | Pause running job |
| `cancelJob(jobId)` | Cancel pending/running job |
| `addObjectsToJob(jobId, objects)` | Add content objects to job |

### 2.10 REST Dashboard API

**Servlet:** `TranslationDashboardServlet`  
**Base Path:** `/bin/translationgemma/dashboard`

| Endpoint | Method | Parameters | Description |
|----------|--------|------------|-------------|
| `/` | GET | - | Dashboard stats |
| `/stats` | GET | - | Dashboard stats |
| `/jobs` | GET | - | List all jobs |
| `/jobs` | GET | jobId | Get job by ID |
| `/jobs` | GET | status | List jobs by status |
| `/jobs` | POST | action=create | Create new job |
| `/jobs` | POST | action=start | Start job |
| `/jobs` | POST | action=pause | Pause job |
| `/jobs` | POST | action=cancel | Cancel job |
| `/jobs` | POST | action=addObjects | Add objects to job |

**Interface:** `TranslationMemoryService`

| Operation | Behavior |
|-----------|----------|
| `storeTranslation()` | Persist source→target translation to JCR |
| `findMatches()` | Fuzzy match with configurable threshold (default: 0.85) |
| `getTranslation()` | Retrieve stored translation |
| `deleteTranslation()` | Remove from TM |

**Storage:** JCR `/var/translation-memory/`

### 2.4 Terminology Management

**Interface:** `TerminologyService`

| Operation | Behavior |
|-----------|----------|
| `findTerm()` | Find single term match with confidence score |
| `findAllTerms()` | Find multiple term matches |
| `addTerm()` | Add new terminology entry |
| `updateTerm()` | Update existing term |
| `deleteTerm()` | Remove term |
| `importTerminology()` | Import TBX, CSV, JSON formats |
| `exportTerminology()` | Export to TBX, CSV, JSON |

### 2.5 DITA Support

**Helper:** `DitaTranslationHelper`

| Operation | Behavior |
|-----------|----------|
| `parseDitaContent()` | Parse DITA XML, extract translatable elements |
| `applyTranslations()` | Apply translations back to DITA structure |
| `generateTranslationBatch()` | Create batch prompt for LLM |
| `parseTranslationResponse()` | Parse LLM response into ID→translation map |

**Supported DITA Types:** topic, task, concept, reference, map

### 2.6 Continuous Localization

**Interface:** `ContinuousLocalizationService`

| Operation | Behavior |
|-----------|----------|
| `watchPath()` | Monitor content path for changes |
| `registerWebhook()` | Register callback for translation events |
| `triggerTranslation()` | Async translation on content change |

**Events:** TRANSLATION_STARTED, TRANSLATION_COMPLETED, TRANSLATION_FAILED, CONTENT_CHANGED

### 2.7 Visual Context

**Interface:** `VisualContextService`

| Operation | Behavior |
|-----------|----------|
| `capturePageContext()` | Capture page screenshot for translator reference |
| `captureFragmentContext()` | Capture experience/content fragment in context |
| `captureAssetContext()` | Capture DAM asset preview |
| `captureWithPreview()` | Capture with specific viewport (desktop/tablet/mobile) |

**Viewports:** Desktop (1920x1080), Tablet (1024x768), Mobile (375x812)

---

## 3. Non-Functional Specifications

### 3.1 Resilience

| Feature | Library | Configuration | Default |
|---------|---------|---------------|---------|
| Retry Logic | Resilience4j | `retryMaxAttempts` | 3 |
| Wait Between Retries | Resilience4j | `retryWaitDurationMs` | 1000ms |
| Circuit Breaker | Resilience4j | `circuitBreakerFailureRateThreshold` | 50% |
| Slow Call Threshold | Resilience4j | `circuitBreakerSlowCallDurationMs` | 5000ms |
| Circuit Open Duration | Resilience4j | `circuitBreakerWaitDurationS` | 30s |

### 3.2 Caching

| Feature | Implementation |
|---------|----------------|
| Library | Caffeine (Ben Manes) |
| Cache Enabled | `enableCaching` config |
| Max Entries | `cacheMaxSize` (default: 1000) |
| TTL | `cacheExpireAfterMinutes` (default: 60 min) |

### 3.3 Performance

| Metric | Target |
|--------|--------|
| Translation Latency | < 10s for 5000 chars |
| Concurrent Requests | 3 (configurable) |
| Cache Hit Rate | > 40% (typical) |

### 3.4 Metrics & Observability

| Feature | Library | Description |
|---------|---------|-------------|
| Metrics | Micrometer | Core metrics collection |
| Registry | SimpleMeterRegistry | In-memory metrics (configurable for Prometheus/StatsD) |

### 3.5 Input Sanitization

| Feature | Implementation |
|---------|----------------|
| Input Sanitization | Prompt injection prevention |
| Language Code Validation | ISO 639-1 format check |
| Length Limits | 10,000 char max input |
| Credentials | Environment variable only (no hardcoding) |

---

## 4. Configuration Specification

### 4.1 OSGi Configuration Properties

| Property | Type | Required | Default |
|----------|------|----------|---------|
| `projectId` | String | Yes | - |
| `location` | String | No | us-central1 |
| `enabled` | boolean | No | true |
| `translationProvider` | String | No | translategemma |
| `modelName` | String | No | google/gemma-4-26b-a4b-it |
| `enableTranslationMemory` | boolean | No | true |
| `tmMinScore` | double | No | 0.85 |
| `enableCaching` | boolean | No | true |
| `enableMetrics` | boolean | No | true |
| `retryMaxAttempts` | int | No | 3 |
| `circuitBreakerFailureRateThreshold` | int | No | 50 |

### 4.2 Environment Variables

| Variable | Purpose |
|----------|---------|
| `GCP_PROJECT_ID` | Google Cloud project |
| `GCP_LOCATION` | Vertex AI region |
| `GOOGLE_APPLICATION_CREDENTIALS` | Service account key path |
| `TRANSLATION_API_KEY` | Third-party API keys |

---

## 5. API Contracts

### 5.1 TranslationResult Interface

```java
interface TranslationResult {
    String getSourceLanguage();
    String getTargetLanguage();
    String getSourceString();
    String getTranslation();
    ContentType getContentType();
    String getCategory();
    int getRating();
    String getUserId();
}
```

### 5.2 TranslationJob State Machine

```
DRAFT → COMMITTED_FOR_TRANSLATION → TRANSLATION_IN_PROGRESS 
                                                      ↓
                                              TRANSLATED
                                                      ↓
                                              READY_FOR_REVIEW
                                                      ↓
                                              PUBLISHED

States: DRAFT, COMMITTED_FOR_TRANSLATION, TRANSLATION_IN_PROGRESS, 
        TRANSLATED, READY_FOR_REVIEW, PUBLISHED, ERROR_UPDATE, 
        CANCEL, UNKNOWN_STATE
```

### 5.3 Exception Hierarchy

```
TranslationException (AEM)
    ↑
TranslateGemmaException (Custom)
    ├── TRANSLATION_FAILED
    ├── SERVICE_UNAVAILABLE
    ├── CIRCUIT_BREAKER_OPEN
    ├── RATE_LIMIT_EXCEEDED
    ├── INVALID_CONFIGURATION
    ├── CREDENTIALS_ERROR
    ├── TIMEOUT
    ├── NETWORK_ERROR
    └── UNKNOWN
```

---

## 6. Data Models

### 6.1 Translation Memory Entry (JCR)

```
/var/translation-memory/
├── {sourceLang}/
│   └── {targetLang}/
│       └── {contentType}/
│           └── {hash(source+category)}/
│               ├── sourceText (String)
│               ├── targetText (String)
│               ├── sourceLang (String)
│               ├── targetLang (String)
│               ├── contentType (String)
│               ├── category (String)
│               ├── rating (int)
│               ├── created (Date)
│               └── path (String)
```

### 6.2 Terminology Entry

```
TerminologyEntry {
    termId: String (UUID)
    sourceTerm: String
    targetTerm: String
    sourceLanguage: String (ISO 639-1)
    targetLanguage: String (ISO 639-1)
    domain: String (e.g., "software", "marketing")
    definition: String
    metadata: Map<String, String>
    status: String ("active" | "deprecated")
}
```

---

## 7. Component Dependencies

```
TranslateGemmaTranslationServiceImpl
├── VertexAI (GCP SDK)
├── GenerativeModel (GCP SDK)
├── TranslateGemmaConfig (OSGi)
├── TranslationMemoryService
├── TerminologyService
├── TranslationCache (Caffeine)
├── ResilienceHelper (Resilience4j)
└── TranslationMetrics (Micrometer)
```

---

## 8. Extension Points

### 8.1 Translation Provider Fallback
- Interface: `TranslationProvider`
- Implementations: TranslateGemma, Google Translate API, DeepL, Microsoft, OpenAI, Ollama

### 8.2 Content Type Handlers
- Content Fragments → `ContentFragmentTranslationService`
- Experience Fragments → `ExperienceFragmentTranslationService`
- DAM Metadata → `DamMetadataTranslationService`
- i18n Dictionaries → `I18nDictionaryTranslationService`

---

## 9. Error Handling Strategy

| Error Type | User Message | Action |
|------------|--------------|--------|
| TRANSLATION_FAILED | "Service temporarily unavailable" | Retry with backoff |
| CIRCUIT_BREAKER_OPEN | "High error rates, try later" | Stop requests, wait |
| RATE_LIMIT_EXCEEDED | "Too many requests, wait" | Throttle |
| CREDENTIALS_ERROR | "Auth failed, contact admin" | Alert ops |
| TIMEOUT | "Request timed out, try again" | Retry |

---

## 10. Metrics & Observability

### 10.1 Key Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `translation.requests.total` | Counter | Total requests |
| `translation.success.total` | Counter | Successful translations |
| `translation.failures.total` | Counter | Failed translations |
| `translation.cache.hits.total` | Counter | Cache hits |
| `translation.latency` | Timer | Response time |
| `translation.retry.total` | Counter | Retry attempts |
| `translation.circuitbreaker.open.total` | Counter | CB opens |

### 10.2 Health Check

```java
service.getHealthStatus();
// Output:
// - Service Available: true
// - Circuit Breaker Open: false
// - Caching Enabled: true
// - Cache Hit Rate: 45.00%
```

---

*Document generated by reverse engineering the implementation*
*Version: 1.2.0-SNAPSHOT*
*Date: March 10, 2026*
