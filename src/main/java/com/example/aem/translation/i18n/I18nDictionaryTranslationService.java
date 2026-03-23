package com.example.aem.translation.i18n;

import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationException;

import java.util.Map;

public interface I18nDictionaryTranslationService {

    class DictionaryTranslationResult {
        private final String dictionaryPath;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final Map<String, String> translatedEntries;
        private final int totalEntries;
        private final int translatedCount;
        private final long translationTime;
        private final boolean success;
        private final String errorMessage;

        public DictionaryTranslationResult(String dictionaryPath, String sourceLanguage,
                                           String targetLanguage, Map<String, String> translatedEntries,
                                           int totalEntries, int translatedCount, long translationTime) {
            this.dictionaryPath = dictionaryPath;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.translatedEntries = translatedEntries;
            this.totalEntries = totalEntries;
            this.translatedCount = translatedCount;
            this.translationTime = translationTime;
            this.success = true;
            this.errorMessage = null;
        }

        public DictionaryTranslationResult(String dictionaryPath, String errorMessage) {
            this.dictionaryPath = dictionaryPath;
            this.sourceLanguage = null;
            this.targetLanguage = null;
            this.translatedEntries = null;
            this.totalEntries = 0;
            this.translatedCount = 0;
            this.translationTime = 0;
            this.success = false;
            this.errorMessage = errorMessage;
        }

        public String getDictionaryPath() { return dictionaryPath; }
        public String getSourceLanguage() { return sourceLanguage; }
        public String getTargetLanguage() { return targetLanguage; }
        public Map<String, String> getTranslatedEntries() { return translatedEntries; }
        public int getTotalEntries() { return totalEntries; }
        public int getTranslatedCount() { return translatedCount; }
        public long getTranslationTime() { return translationTime; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    DictionaryTranslationResult translateDictionary(String dictionaryPath, String targetLanguage,
                                                   String category) throws TranslationException;

    DictionaryTranslationResult translateDictionary(String dictionaryPath, String targetLanguage,
                                                   String category, boolean includePlaceholders)
                                                   throws TranslationException;

    DictionaryTranslationResult translateDictionaryWithModules(String dictionaryPath,
                                                               String targetLanguage, 
                                                               String[] moduleNames)
                                                               throws TranslationException;
}
