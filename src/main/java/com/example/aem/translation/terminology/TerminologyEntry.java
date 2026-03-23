package com.example.aem.translation.terminology;

import java.util.Map;

public class TerminologyEntry {
    private String termId;
    private String sourceTerm;
    private String targetTerm;
    private String sourceLanguage;
    private String targetLanguage;
    private String domain;
    private String definition;
    private Map<String, String> metadata;
    private String status;

    public TerminologyEntry(String sourceTerm, String targetTerm, String sourceLanguage,
            String targetLanguage, String domain) {
        this.sourceTerm = sourceTerm;
        this.targetTerm = targetTerm;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.domain = domain;
        this.status = "active";
    }

    public String getTermId() { return termId; }
    public void setTermId(String termId) { this.termId = termId; }
    public String getSourceTerm() { return sourceTerm; }
    public void setSourceTerm(String sourceTerm) { this.sourceTerm = sourceTerm; }
    public String getTargetTerm() { return targetTerm; }
    public void setTargetTerm(String targetTerm) { this.targetTerm = targetTerm; }
    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
