package com.example.aem.translation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TranslationFeedback {
    private final String sourceString;
    private final String originalTranslation;
    private final String humanCorrection;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final String userId;
    private final long timestamp;

    @JsonCreator
    public TranslationFeedback(
            @JsonProperty("sourceString") String sourceString,
            @JsonProperty("originalTranslation") String originalTranslation,
            @JsonProperty("humanCorrection") String humanCorrection,
            @JsonProperty("sourceLanguage") String sourceLanguage,
            @JsonProperty("targetLanguage") String targetLanguage,
            @JsonProperty("userId") String userId,
            @JsonProperty("timestamp") long timestamp) {
        this.sourceString = sourceString;
        this.originalTranslation = originalTranslation;
        this.humanCorrection = humanCorrection;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public String getSourceString() { return sourceString; }
    public String getOriginalTranslation() { return originalTranslation; }
    public String getHumanCorrection() { return humanCorrection; }
    public String getSourceLanguage() { return sourceLanguage; }
    public String getTargetLanguage() { return targetLanguage; }
    public String getUserId() { return userId; }
    public long getTimestamp() { return timestamp; }
}
