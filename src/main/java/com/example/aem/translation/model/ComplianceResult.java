package com.example.aem.translation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ComplianceResult {
    private final boolean compliant;
    private final String feedback;
    private final double confidence;
    private final String reasoning;

    @JsonCreator
    public ComplianceResult(
            @JsonProperty("compliant") boolean compliant,
            @JsonProperty("feedback") String feedback,
            @JsonProperty("confidence") double confidence,
            @JsonProperty("reasoning") String reasoning) {
        this.compliant = compliant;
        this.feedback = feedback;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }

    public boolean isCompliant() { return compliant; }
    public String getFeedback() { return feedback; }
    public double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
}
