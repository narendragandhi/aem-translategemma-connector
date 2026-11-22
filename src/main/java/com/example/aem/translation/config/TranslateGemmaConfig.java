package com.example.aem.translation.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "TranslateGemma Translation Service Configuration",
    description = "Configuration for the Gemma translation service integration (Vertex AI)"
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
        description = "Enable/disable the Gemma translation service"
    )
    boolean enabled() default true;

    @AttributeDefinition(
        name = "Model Name",
        description = "Vertex AI model ID used for translation"
    )
    String modelName() default "gemma-3-27b-it";

    @AttributeDefinition(
        name = "Default Content Category",
        description = "Default content category for translations"
    )
    String defaultContentCategory() default "general";

    @AttributeDefinition(
        name = "Enable Fallback Providers",
        description = "Fall back to other configured TranslationProvider services when the primary call fails"
    )
    boolean enableFallbackProviders() default true;

    @AttributeDefinition(
        name = "Enable Sentiment Analysis",
        description = "Run an extra LLM sentiment analysis call after every translation (adds cost)"
    )
    boolean enableSentimentAnalysis() default false;

    @AttributeDefinition(
        name = "Enable Compliance Check",
        description = "Run an extra LLM brand-compliance check after every translation (adds cost)"
    )
    boolean enableComplianceCheck() default false;

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
}
