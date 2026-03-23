package com.example.aem.translation.service;

import com.example.aem.translation.model.ComplianceResult;
import com.example.aem.translation.model.SentimentResult;
import java.util.List;
import java.util.Map;

/**
 * Service for auditing translation and analysis events to ensure transparency.
 */
public interface TranslationAuditService {

    class AuditEntry {
        private final String path;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final SentimentResult sentiment;
        private final ComplianceResult compliance;
        private final long timestamp;
        private final String userId;

        public AuditEntry(String path, String sourceLanguage, String targetLanguage,
                         SentimentResult sentiment, ComplianceResult compliance,
                         long timestamp, String userId) {
            this.path = path;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.sentiment = sentiment;
            this.compliance = compliance;
            this.timestamp = timestamp;
            this.userId = userId;
        }

        public String getPath() { return path; }
        public String getSourceLanguage() { return sourceLanguage; }
        public String getTargetLanguage() { return targetLanguage; }
        public SentimentResult getSentiment() { return sentiment; }
        public ComplianceResult getCompliance() { return compliance; }
        public long getTimestamp() { return timestamp; }
        public String getUserId() { return userId; }
    }

    /**
     * Log a translation event with analysis results.
     */
    void logEvent(String path, String sourceLanguage, String targetLanguage,
                  SentimentResult sentiment, ComplianceResult compliance, String userId);

    /**
     * Get recent audit entries.
     */
    List<AuditEntry> getRecentEntries(int limit);
}
