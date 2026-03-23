package com.example.aem.translation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ComplianceResult {
    private final String content;
    private final List<ComplianceViolation> violations;
    private final boolean isCompliant;

    @JsonCreator
    public ComplianceResult(
            @JsonProperty("content") String content,
            @JsonProperty("violations") List<ComplianceViolation> violations) {
        this.content = content;
        this.violations = violations;
        this.isCompliant = violations == null || violations.isEmpty();
    }

    public String getContent() { return content; }
    public List<ComplianceViolation> getViolations() { return violations; }
    public boolean isCompliant() { return isCompliant; }

    public static class ComplianceViolation {
        private final String flaggedTerm;
        private final String suggestion;
        private final String reason;
        private final Severity severity;

        public enum Severity { LOW, MEDIUM, HIGH }

        @JsonCreator
        public ComplianceViolation(
                @JsonProperty("flaggedTerm") String flaggedTerm,
                @JsonProperty("suggestion") String suggestion,
                @JsonProperty("reason") String reason,
                @JsonProperty("severity") Severity severity) {
            this.flaggedTerm = flaggedTerm;
            this.suggestion = suggestion;
            this.reason = reason;
            this.severity = severity;
        }

        public String getFlaggedTerm() { return flaggedTerm; }
        public String getSuggestion() { return suggestion; }
        public String getReason() { return reason; }
        public Severity getSeverity() { return severity; }
    }
}
