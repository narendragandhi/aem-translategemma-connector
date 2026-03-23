package com.example.aem.translation.cfintegration.service.result;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents the result of a Content Fragment translation.
 */
public class ContentFragmentTranslationResult {

    private final String fragmentPath;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final Map<String, String> translatedElements;
    private final long translationTime;
    private final String errorMessage;

    /**
     * Constructor for a successful translation.
     *
     * @param fragmentPath The path of the translated Content Fragment.
     * @param sourceLanguage The source language of the translation.
     * @param targetLanguage The target language of the translation.
     * @param translatedElements A map of translated element names and their translated content.
     * @param translationTime The time taken for the translation in milliseconds.
     */
    public ContentFragmentTranslationResult(String fragmentPath, String sourceLanguage, String targetLanguage,
                                            Map<String, String> translatedElements, long translationTime) {
        this.fragmentPath = fragmentPath;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.translatedElements = translatedElements;
        this.translationTime = translationTime;
        this.errorMessage = null;
    }

    /**
     * Constructor for a failed translation.
     *
     * @param fragmentPath The path of the Content Fragment that failed to translate.
     * @param errorMessage The error message.
     */
    public ContentFragmentTranslationResult(String fragmentPath, String errorMessage) {
        this.fragmentPath = fragmentPath;
        this.sourceLanguage = null;
        this.targetLanguage = null;
        this.translatedElements = new HashMap<>();
        this.translationTime = 0;
        this.errorMessage = errorMessage;
    }

    /**
     * @return true if the translation was successful, false otherwise.
     */
    public boolean isSuccess() {
        return errorMessage == null;
    }

    public String getFragmentPath() {
        return fragmentPath;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public Map<String, String> getTranslatedElements() {
        return translatedElements;
    }

    public long getTranslationTime() {
        return translationTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
