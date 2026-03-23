package com.example.aem.translation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SentimentResult {
    private final String sentiment; // POSITIVE, NEGATIVE, NEUTRAL
    private final double confidence;
    private final String reasoning;

    @JsonCreator
    public SentimentResult(
            @JsonProperty("sentiment") String sentiment,
            @JsonProperty("confidence") double confidence,
            @JsonProperty("reasoning") String reasoning) {
        this.sentiment = sentiment;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }

    public String getSentiment() { return sentiment; }
    public double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
}
