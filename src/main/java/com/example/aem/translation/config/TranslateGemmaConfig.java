package com.example.aem.translation.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
    name = "TranslateGemma Translation Service Configuration",
    description = "Configuration for Google TranslateGemma translation service integration"
)
public @interface TranslateGemmaConfig {

    @AttributeDefinition(
        name = "Google Cloud Project ID",
        description = "Google Cloud Project ID for Vertex AI access"
    )
    String projectId();

    @AttributeDefinition(
        name = "Google Cloud Location",
        description = "Google Cloud location (e.g., us-central1)"
    )
    String location() default "us-central1";

    @AttributeDefinition(
        name = "Service Enabled",
        description = "Enable/disable the TranslateGemma translation service"
    )
    boolean enabled() default true;

    @AttributeDefinition(
        name = "Default Source Language",
        description = "Default source language code (e.g., en)"
    )
    String defaultSourceLanguage() default "en";

    @AttributeDefinition(
        name = "Default Target Language",
        description = "Default target language code (e.g., es)"
    )
    String defaultTargetLanguage() default "es";

    @AttributeDefinition(
        name = "Default Content Category",
        description = "Default content category for translations"
    )
    String defaultContentCategory() default "general";

    @AttributeDefinition(
        name = "Max Translation Length",
        description = "Maximum length of text to translate in one request"
    )
    int maxTranslationLength() default 5000;

    @AttributeDefinition(
        name = "Connection Timeout",
        description = "Connection timeout in seconds"
    )
    int connectionTimeout() default 30;

    @AttributeDefinition(
        name = "Read Timeout",
        description = "Read timeout in seconds"
    )
    int readTimeout() default 60;

    @AttributeDefinition(
        name = "Translation Provider",
        description = "Select the primary translation provider",
        options = {
            @Option(label = "Google TranslateGemma", value = "translategemma"),
            @Option(label = "Google Cloud Translation API", value = "google_translate"),
            @Option(label = "DeepL", value = "deepl"),
            @Option(label = "Microsoft Translator", value = "microsoft"),
            @Option(label = "OpenAI GPT", value = "openai"),
            @Option(label = "Ollama (Local)", value = "ollama")
        }
    )
    String translationProvider() default "translategemma";

    @AttributeDefinition(
        name = "Enable Translation Memory",
        description = "Enable TM lookup before MT translation"
    )
    boolean enableTranslationMemory() default true;

    @AttributeDefinition(
        name = "Translation Memory Min Score",
        description = "Minimum match score for TM suggestions (0.0-1.0)"
    )
    double tmMinScore() default 0.85;

    @AttributeDefinition(
        name = "Store Translations to TM",
        description = "Automatically store translations to TM after completion"
    )
    boolean storeToTranslationMemory() default true;

    @AttributeDefinition(
        name = "Enable Fallback Providers",
        description = "Enable fallback to other providers if primary fails"
    )
    boolean enableFallbackProviders() default true;

    @AttributeDefinition(
        name = "Model Name",
        description = "Translation model name (for TranslateGemma or OpenAI)"
    )
    String modelName() default "google/translategemma-2b-it";

    @AttributeDefinition(
        name = "API Key",
        description = "API key for external providers (DeepL, Microsoft, OpenAI)"
    )
    String apiKey() default "";

    @AttributeDefinition(
        name = "Ollama Endpoint",
        description = "Ollama server endpoint (e.g., http://localhost:11434)"
    )
    String ollamaEndpoint() default "http://localhost:11434";

    @AttributeDefinition(
        name = "Ollama Model",
        description = "Ollama model to use (e.g., llama2, mistral)"
    )
    String ollamaModel() default "llama2";

    @AttributeDefinition(
        name = "Parallel Translations",
        description = "Number of parallel translation requests"
    )
    int parallelTranslations() default 3;

    @AttributeDefinition(
        name = "Retry Attempts",
        description = "Number of retry attempts on failure"
    )
    int retryAttempts() default 2;

    @AttributeDefinition(
        name = "Supported Content Types",
        description = "Comma-separated content types (html,plaintext,json,xml)"
    )
    String supportedContentTypes() default "html,plaintext,json,xml";

    @AttributeDefinition(
        name = "Enable Visual Context",
        description = "Capture visual context for translators"
    )
    boolean enableVisualContext() default false;

    @AttributeDefinition(
        name = "Enable DAM Asset Translation",
        description = "Translate DAM asset metadata"
    )
    boolean enableDamTranslation() default true;

    @AttributeDefinition(
        name = "Enable i18n Dictionary Translation",
        description = "Translate i18n dictionaries"
    )
    boolean enableI18nTranslation() default true;

    @AttributeDefinition(
        name = "Retry Max Attempts",
        description = "Maximum number of retry attempts for failed requests"
    )
    int retryMaxAttempts() default 3;

    @AttributeDefinition(
        name = "Retry Wait Duration (ms)",
        description = "Initial wait duration between retries in milliseconds"
    )
    long retryWaitDurationMs() default 1000;

    @AttributeDefinition(
        name = "Circuit Breaker Failure Rate Threshold",
        description = "Failure rate threshold percentage to open circuit breaker"
    )
    int circuitBreakerFailureRateThreshold() default 50;

    @AttributeDefinition(
        name = "Circuit Breaker Slow Call Rate Threshold",
        description = "Slow call rate threshold percentage to open circuit breaker"
    )
    int circuitBreakerSlowCallRateThreshold() default 100;

    @AttributeDefinition(
        name = "Circuit Breaker Slow Call Duration (ms)",
        description = "Slow call duration threshold in milliseconds"
    )
    int circuitBreakerSlowCallDurationMs() default 5000;

    @AttributeDefinition(
        name = "Circuit Breaker Wait Duration (s)",
        description = "Duration to wait before trying again after circuit opens"
    )
    int circuitBreakerWaitDurationS() default 30;

    @AttributeDefinition(
        name = "Cache Max Size",
        description = "Maximum number of entries in translation cache"
    )
    int cacheMaxSize() default 1000;

    @AttributeDefinition(
        name = "Cache Expire After Minutes",
        description = "Cache entry expiration time in minutes"
    )
    int cacheExpireAfterMinutes() default 60;

    @AttributeDefinition(
        name = "Enable Caching",
        description = "Enable translation caching"
    )
    boolean enableCaching() default true;

    @AttributeDefinition(
        name = "Enable Metrics",
        description = "Enable metrics collection"
    )
    boolean enableMetrics() default true;

    @AttributeDefinition(
        name = "Request Timeout (seconds)",
        description = "Overall request timeout for translation calls"
    )
    int requestTimeoutSeconds() default 120;

    @AttributeDefinition(
        name = "Batch Size",
        description = "Number of texts to translate in a single batch request"
    )
    int batchSize() default 10;

    @AttributeDefinition(
        name = "Metrics Enabled",
        description = "Enable detailed metrics reporting"
    )
    boolean metricsEnabled() default false;
}
