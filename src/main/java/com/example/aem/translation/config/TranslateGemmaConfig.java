package com.example.aem.translation.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi configuration for TranslateGemma Translation Service.
 */
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
}