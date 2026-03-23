package com.example.aem.translation.dam;

import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationException;

import java.util.Map;

public interface DamMetadataTranslationService {

    class MetadataTranslationResult {
        private final String assetPath;
        private final String targetLanguage;
        private final Map<String, String> translatedMetadata;
        private final long translationTime;
        private final boolean success;
        private final String errorMessage;

        public MetadataTranslationResult(String assetPath, String targetLanguage,
                                        Map<String, String> translatedMetadata, long translationTime) {
            this.assetPath = assetPath;
            this.targetLanguage = targetLanguage;
            this.translatedMetadata = translatedMetadata;
            this.translationTime = translationTime;
            this.success = true;
            this.errorMessage = null;
        }

        public MetadataTranslationResult(String assetPath, String errorMessage) {
            this.assetPath = assetPath;
            this.targetLanguage = null;
            this.translatedMetadata = null;
            this.translationTime = 0;
            this.success = false;
            this.errorMessage = errorMessage;
        }

        public String getAssetPath() { return assetPath; }
        public String getTargetLanguage() { return targetLanguage; }
        public Map<String, String> getTranslatedMetadata() { return translatedMetadata; }
        public long getTranslationTime() { return translationTime; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    MetadataTranslationResult translateAssetMetadata(String assetPath, String targetLanguage,
                                                     String category) throws TranslationException;

    MetadataTranslationResult translateAssetMetadata(String assetPath, String targetLanguage,
                                                     String category, String[] metadataFields) 
                                                     throws TranslationException;

    MetadataTranslationResult[] translateMultipleAssets(String[] assetPaths, String targetLanguage,
                                                        String category) throws TranslationException;

    class VisualTranslationResult {
        private final String assetPath;
        private final String targetLanguage;
        private final String ocrText;
        private final String translatedOcrText;
        private final String altText;
        private final boolean success;
        private final String errorMessage;

        public VisualTranslationResult(String assetPath, String targetLanguage, String ocrText, 
                                     String translatedOcrText, String altText) {
            this.assetPath = assetPath;
            this.targetLanguage = targetLanguage;
            this.ocrText = ocrText;
            this.translatedOcrText = translatedOcrText;
            this.altText = altText;
            this.success = true;
            this.errorMessage = null;
        }

        public VisualTranslationResult(String assetPath, String errorMessage) {
            this.assetPath = assetPath;
            this.targetLanguage = null;
            this.ocrText = null;
            this.translatedOcrText = null;
            this.altText = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }

        public String getAssetPath() { return assetPath; }
        public String getTargetLanguage() { return targetLanguage; }
        public String getOcrText() { return ocrText; }
        public String getTranslatedOcrText() { return translatedOcrText; }
        public String getAltText() { return altText; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    VisualTranslationResult translateAssetVisualContent(String assetPath, String targetLanguage) 
            throws TranslationException;
}
