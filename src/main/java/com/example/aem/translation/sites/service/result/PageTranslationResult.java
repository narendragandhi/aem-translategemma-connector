package com.example.aem.translation.sites.service.result;

import com.day.cq.tagging.Tag;
import java.util.Map;

public class PageTranslationResult {
    private final String pagePath;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final Map<String, String> translatedProperties;
    private final Map<String, ComponentTranslationResult> componentResults;
    private final java.util.List<Tag> translatedTags;
    private final long translationTime;

    public PageTranslationResult(String pagePath, String sourceLanguage, String targetLanguage,
                               Map<String, String> translatedProperties,
                               Map<String, ComponentTranslationResult> componentResults,
                               java.util.List<Tag> translatedTags, long translationTime) {
        this.pagePath = pagePath;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.translatedProperties = translatedProperties;
        this.componentResults = componentResults;
        this.translatedTags = translatedTags;
        this.translationTime = translationTime;
    }

    public String getPagePath() { return pagePath; }
    public String getSourceLanguage() { return sourceLanguage; }
    public String getTargetLanguage() { return targetLanguage; }
    public Map<String, String> getTranslatedProperties() { return translatedProperties; }
    public Map<String, ComponentTranslationResult> getComponentResults() { return componentResults; }
    public java.util.List<Tag> getTranslatedTags() { return translatedTags; }
    public long getTranslationTime() { return translationTime; }
}