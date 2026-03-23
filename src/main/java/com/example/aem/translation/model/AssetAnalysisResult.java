package com.example.aem.translation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AssetAnalysisResult {
    private final String altText;
    private final List<String> keywords;
    private final String suggestedTitle;
    private final String ocrText;

    @JsonCreator
    public AssetAnalysisResult(
            @JsonProperty("altText") String altText,
            @JsonProperty("keywords") List<String> keywords,
            @JsonProperty("suggestedTitle") String suggestedTitle,
            @JsonProperty("ocrText") String ocrText) {
        this.altText = altText;
        this.keywords = keywords;
        this.suggestedTitle = suggestedTitle;
        this.ocrText = ocrText;
    }

    public String getAltText() { return altText; }
    public List<String> getKeywords() { return keywords; }
    public String getSuggestedTitle() { return suggestedTitle; }
    public String getOcrText() { return ocrText; }
}
