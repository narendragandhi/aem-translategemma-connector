package com.example.aem.translation.provider;

import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationResult;

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
                               String targetLanguage, TranslationConstants.ContentType contentType,
                               String category) throws Exception;

    String detectLanguage(String text) throws Exception;

    boolean supportsLanguagePair(String sourceLanguage, String targetLanguage);

    default double getMatchingScore() {
        return 0.5;
    }

    default long getAverageResponseTime() {
        return 0;
    }
}
