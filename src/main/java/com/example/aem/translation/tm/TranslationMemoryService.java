package com.example.aem.translation.tm;

import com.adobe.granite.translation.api.TranslationConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.Resource;

import java.util.List;
import java.util.Map;

public interface TranslationMemoryService {

    class TMEntry {
        private final String sourceText;
        private final String targetText;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final TranslationConstants.ContentType contentType;
        private final String category;
        private final String path;
        private final int rating;
        private final long createdAt;
        private final long lastUsedAt;
        private final int useCount;

        public TMEntry(String sourceText, String targetText, String sourceLanguage, 
                      String targetLanguage, TranslationConstants.ContentType contentType,
                      String category, String path, int rating) {
            this.sourceText = sourceText;
            this.targetText = targetText;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.contentType = contentType;
            this.category = category;
            this.path = path;
            this.rating = rating;
            this.createdAt = System.currentTimeMillis();
            this.lastUsedAt = System.currentTimeMillis();
            this.useCount = 1;
        }

        public String getSourceText() { return sourceText; }
        public String getTargetText() { return targetText; }
        public String getSourceLanguage() { return sourceLanguage; }
        public String getTargetLanguage() { return targetLanguage; }
        public TranslationConstants.ContentType getContentType() { return contentType; }
        public String getCategory() { return category; }
        public String getPath() { return path; }
        public int getRating() { return rating; }
        public long getCreatedAt() { return createdAt; }
        public long getLastUsedAt() { return lastUsedAt; }
        public int getUseCount() { return useCount; }
    }

    void storeTranslation(String sourceText, String targetText, String sourceLanguage,
                        String targetLanguage, TranslationConstants.ContentType contentType,
                        String category, String path, int rating) throws Exception;

    List<TMEntry> findMatches(String sourceText, String sourceLanguage, String targetLanguage,
                             TranslationConstants.ContentType contentType, String category,
                             double minScore, int maxResults);

    Map<String, Integer> getStatistics();

    void clearMemory();

    String getStoragePath();
}
