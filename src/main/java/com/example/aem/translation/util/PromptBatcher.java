package com.example.aem.translation.util;

import com.adobe.granite.translation.api.TranslationConstants.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Enterprise Utility for Gemma 4 Batch Prompting.
 * Consolidates multiple strings into a single LLM request to respect GCP quotas.
 */
public class PromptBatcher {
    private static final Logger LOG = LoggerFactory.getLogger(PromptBatcher.class);
    
    public static class BatchRequest {
        private final List<String> texts = new ArrayList<>();
        private final String sourceLang;
        private final String targetLang;
        private final ContentType contentType;

        public BatchRequest(String sourceLang, String targetLang, ContentType contentType) {
            this.sourceLang = sourceLang;
            this.targetLang = targetLang;
            this.contentType = contentType;
        }

        public void add(String text) {
            texts.add(text);
        }

        public String createPrompt() {
            StringBuilder sb = new StringBuilder();
            sb.append("Translate the following list of texts from ").append(sourceLang)
              .append(" to ").append(targetLang).append(". ");
            sb.append("Respond ONLY with a JSON array of strings in the same order.\n\n");
            
            for (int i = 0; i < texts.size(); i++) {
                sb.append(i).append(": ").append(texts.get(i)).append("\n");
            }
            
            return sb.toString();
        }

        public List<String> getTexts() {
            return texts;
        }
    }

    /**
     * Groups an array of strings into manageable batches for Gemma 4.
     */
    public static List<BatchRequest> batch(String[] strings, String sourceLang, String targetLang, 
                                          ContentType contentType, int batchSize) {
        List<BatchRequest> batches = new ArrayList<>();
        BatchRequest currentBatch = new BatchRequest(sourceLang, targetLang, contentType);
        
        for (String s : strings) {
            if (currentBatch.getTexts().size() >= batchSize) {
                batches.add(currentBatch);
                currentBatch = new BatchRequest(sourceLang, targetLang, contentType);
            }
            currentBatch.add(s);
        }
        
        if (!currentBatch.getTexts().isEmpty()) {
            batches.add(currentBatch);
        }
        
        return batches;
    }
}
