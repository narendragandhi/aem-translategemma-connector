package com.example.aem.translation.terminology;

public class TerminologyMatch {
    private final String sourceTerm;
    private final String targetTerm;
    private final String domain;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final float confidence;
    private final String termId;

    public TerminologyMatch(String sourceTerm, String targetTerm, String domain,
            String sourceLanguage, String targetLanguage, float confidence, String termId) {
        this.sourceTerm = sourceTerm;
        this.targetTerm = targetTerm;
        this.domain = domain;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.confidence = confidence;
        this.termId = termId;
    }

    public String getSourceTerm() { return sourceTerm; }
    public String getTargetTerm() { return targetTerm; }
    public String getDomain() { return domain; }
    public String getSourceLanguage() { return sourceLanguage; }
    public String getTargetLanguage() { return targetLanguage; }
    public float getConfidence() { return confidence; }
    public String getTermId() { return termId; }
}
