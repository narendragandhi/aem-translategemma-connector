package com.example.aem.translation.mock;

import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationResult;

public class MockTranslationResult implements TranslationResult {

    private final String sourceLanguage;
    private final String targetLanguage;
    private final String sourceString;
    private final String translation;
    private final TranslationConstants.ContentType contentType;
    private final String category;
    private final int rating;
    private final String userId;

    public MockTranslationResult(String sourceLanguage, String targetLanguage,
                                  String sourceString, String translation,
                                  TranslationConstants.ContentType contentType) {
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.sourceString = sourceString;
        this.translation = translation;
        this.contentType = contentType;
        this.category = "general";
        this.rating = 0;
        this.userId = null;
    }

    @Override
    public String getSourceLanguage() {
        return sourceLanguage;
    }

    @Override
    public String getTargetLanguage() {
        return targetLanguage;
    }

    @Override
    public String getSourceString() {
        return sourceString;
    }

    @Override
    public String getTranslation() {
        return translation;
    }

    @Override
    public TranslationConstants.ContentType getContentType() {
        return contentType;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public int getRating() {
        return rating;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    public static MockTranslationResult create(String sourceLang, String targetLang,
                                                String sourceText, String translatedText) {
        return new MockTranslationResult(sourceLang, targetLang, sourceText, translatedText,
                                          TranslationConstants.ContentType.PLAIN);
    }
}
