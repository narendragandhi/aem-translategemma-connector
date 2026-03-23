package com.example.aem.translation.service;

import java.util.Map;

/**
 * Bead for protecting DITA/XML tags and attributes from AI hallucinations.
 * Uses a placeholder-injection strategy.
 */
public interface DitaTagProtectionService {
    
    class ProtectedContent {
        private final String maskedContent;
        private final Map<String, String> placeholders;

        public ProtectedContent(String maskedContent, Map<String, String> placeholders) {
            this.maskedContent = maskedContent;
            this.placeholders = placeholders;
        }

        public String getMaskedContent() { return maskedContent; }
        public Map<String, String> getPlaceholders() { return placeholders; }
    }

    /**
     * Identifies DITA tags/attributes and replaces them with inert placeholders.
     */
    ProtectedContent protect(String rawXml);

    /**
     * Re-injects the original DITA tags/attributes into the translated content.
     */
    String restore(String translatedXml, Map<String, String> placeholders);
}
