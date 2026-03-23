package com.example.aem.translation.terminology;

import java.util.List;
import java.util.Map;

public interface TerminologyService {

    TerminologyMatch findTerm(String sourceText, String sourceLanguage, String targetLanguage, String domain)
            throws TerminologyException;

    List<TerminologyMatch> findAllTerms(String sourceText, String sourceLanguage, String targetLanguage, 
            String domain, int maxResults) throws TerminologyException;

    void addTerm(TerminologyEntry entry) throws TerminologyException;

    void updateTerm(TerminologyEntry entry) throws TerminologyException;

    void deleteTerm(String termId) throws TerminologyException;

    List<TerminologyEntry> getTerminology(String domain) throws TerminologyException;

    void importTerminology(String filePath, TerminologyFormat format) throws TerminologyException;

    void exportTerminology(String domain, String filePath, TerminologyFormat format) throws TerminologyException;
}
