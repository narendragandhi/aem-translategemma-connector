package com.example.aem.translation.xf;

import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationException;

import java.util.Map;

public interface ExperienceFragmentTranslationService {

    class XFTranslationResult {
        private final String sourcePath;
        private final String targetPath;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final Map<String, String> translatedElements;
        private final long translationTime;
        private final boolean success;
        private final String errorMessage;

        public XFTranslationResult(String sourcePath, String targetPath, String sourceLanguage,
                                  String targetLanguage, Map<String, String> translatedElements,
                                  long translationTime) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.translatedElements = translatedElements;
            this.translationTime = translationTime;
            this.success = true;
            this.errorMessage = null;
        }

        public XFTranslationResult(String sourcePath, String errorMessage) {
            this.sourcePath = sourcePath;
            this.targetPath = null;
            this.sourceLanguage = null;
            this.targetLanguage = null;
            this.translatedElements = null;
            this.translationTime = 0;
            this.success = false;
            this.errorMessage = errorMessage;
        }

        public String getSourcePath() { return sourcePath; }
        public String getTargetPath() { return targetPath; }
        public String getSourceLanguage() { return sourceLanguage; }
        public String getTargetLanguage() { return targetLanguage; }
        public Map<String, String> getTranslatedElements() { return translatedElements; }
        public long getTranslationTime() { return translationTime; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    XFTranslationResult translateExperienceFragment(String fragmentPath, String targetLanguage, 
                                                    String category) throws TranslationException;

    XFTranslationResult translateExperienceFragmentVariation(String fragmentPath, String variationName,
                                                            String targetLanguage, String category) 
                                                            throws TranslationException;
}
