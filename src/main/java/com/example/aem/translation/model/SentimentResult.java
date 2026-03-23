package com.example.aem.translation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SentimentResult {
    private final String content;
    private final double score;
    private final String label;
    private final String explanation;

    @JsonCreator
    public SentimentResult(
            @JsonProperty("content") String content,
            @JsonProperty("score") double score,
            @JsonProperty("label") String label,
            @JsonProperty("explanation") String explanation) {
        this.content = content;
        this.score = score;
        this.label = label;
        this.explanation = explanation;
    }

    public String getContent() { return content; }
    public double getScore() { return score; }
    public String getLabel() { return label; }
    public String getExplanation() { return explanation; }

    @Override
    public String toString() {
        return String.format("Sentiment: %s (%.2f) - %s", label, score, explanation);
    }
}
