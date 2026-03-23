package com.example.aem.translation.service;

import com.adobe.granite.translation.api.TranslationException;

import java.util.Map;

/**
 * Service for multi-modal translation and analysis (Image + Text).
 * Handles vision capabilities like OCR and visual content description.
 */
public interface MultiModalTranslationService {

    class MultiModalResult {
        private final String rawResponse;
        private final Map<String, String> data;
        private final boolean success;
        private final String errorMessage;

        public MultiModalResult(String rawResponse, Map<String, String> data) {
            this.rawResponse = rawResponse;
            this.data = data;
            this.success = true;
            this.errorMessage = null;
        }

        public MultiModalResult(String errorMessage) {
            this.rawResponse = null;
            this.data = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }

        public String getRawResponse() { return rawResponse; }
        public Map<String, String> getData() { return data; }
        public String getValue(String key) { return data != null ? data.get(key) : null; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Analyze an image with a prompt.
     * 
     * @param imageData Binary image data
     * @param mimeType Image MIME type
     * @param prompt The prompt for the LLM
     * @return Result containing extracted and processed data
     */
    MultiModalResult analyzeImage(byte[] imageData, String mimeType, String prompt) throws TranslationException;
}
