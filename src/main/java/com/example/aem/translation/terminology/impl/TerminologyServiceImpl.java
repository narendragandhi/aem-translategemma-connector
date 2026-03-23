package com.example.aem.translation.terminology.impl;

import com.example.aem.translation.terminology.TerminologyService;
import com.example.aem.translation.terminology.TerminologyMatch;
import com.example.aem.translation.terminology.TerminologyEntry;
import com.example.aem.translation.terminology.TerminologyException;
import com.example.aem.translation.terminology.TerminologyFormat;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component(service = TerminologyService.class)
public class TerminologyServiceImpl implements TerminologyService {
    
    private static final Logger LOG = LoggerFactory.getLogger(TerminologyServiceImpl.class);
    
    private final Map<String, TerminologyEntry> terminologyStore = new ConcurrentHashMap<>();
    private final Map<String, List<TerminologyMatch>> cache = new ConcurrentHashMap<>();

    public TerminologyServiceImpl() {
        initializeSampleTerminology();
    }

    private void initializeSampleTerminology() {
        try {
            addTerm(new TerminologyEntry("Adobe Experience Manager", "Adobe Experience Manager", "en", "de", "software"));
            addTerm(new TerminologyEntry("Adobe Experience Manager", "Adobe Experience Manager", "en", "fr", "software"));
            addTerm(new TerminologyEntry("content fragment", "fragmento de contenido", "en", "es", "content"));
            addTerm(new TerminologyEntry("experience fragment", "fragmento de experiencia", "en", "es", "content"));
            addTerm(new TerminologyEntry("translation memory", "memoria de traducción", "en", "es", "localization"));
        } catch (TerminologyException e) {
            LOG.warn("Could not initialize sample terminology", e);
        }
        LOG.info("Sample terminology loaded");
    }

    @Override
    public TerminologyMatch findTerm(String sourceText, String sourceLanguage, String targetLanguage, String domain) 
            throws TerminologyException {
        List<TerminologyMatch> matches = findAllTerms(sourceText, sourceLanguage, targetLanguage, domain, 1);
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Override
    public List<TerminologyMatch> findAllTerms(String sourceText, String sourceLanguage, String targetLanguage,
            String domain, int maxResults) throws TerminologyException {
        
        String cacheKey = generateCacheKey(sourceText, sourceLanguage, targetLanguage, domain);
        
        if (cache.containsKey(cacheKey)) {
            LOG.debug("Returning cached terminology matches for: {}", cacheKey);
            return cache.get(cacheKey).stream().limit(maxResults).collect(Collectors.toList());
        }

        List<TerminologyMatch> matches = new ArrayList<>();
        String searchTerm = sourceText.toLowerCase();

        for (TerminologyEntry entry : terminologyStore.values()) {
            if (matches.size() >= maxResults) break;
            
            if (matchesCriteria(entry, searchTerm, sourceLanguage, targetLanguage, domain)) {
                TerminologyMatch match = new TerminologyMatch(
                    entry.getSourceTerm(),
                    entry.getTargetTerm(),
                    entry.getDomain(),
                    entry.getSourceLanguage(),
                    entry.getTargetLanguage(),
                    calculateConfidence(entry, searchTerm),
                    entry.getTermId()
                );
                matches.add(match);
            }
        }

        cache.put(cacheKey, matches);
        LOG.debug("Found {} terminology matches for: {}", matches.size(), cacheKey);
        
        return matches;
    }

    @Override
    public void addTerm(TerminologyEntry entry) throws TerminologyException {
        if (entry.getTermId() == null) {
            entry.setTermId(UUID.randomUUID().toString());
        }
        
        String key = generateEntryKey(entry);
        terminologyStore.put(key, entry);
        
        clearCacheForLanguagePair(entry.getSourceLanguage(), entry.getTargetLanguage());
        
        LOG.info("Added terminology entry: {} -> {}", entry.getSourceTerm(), entry.getTargetTerm());
    }

    @Override
    public void updateTerm(TerminologyEntry entry) throws TerminologyException {
        if (entry.getTermId() == null) {
            throw new TerminologyException("Term ID is required for update", TerminologyException.ErrorCode.STORAGE_ERROR);
        }
        
        String key = findKeyByTermId(entry.getTermId());
        if (key == null) {
            throw new TerminologyException("Term not found: " + entry.getTermId(), 
                    TerminologyException.ErrorCode.TERM_NOT_FOUND);
        }
        
        terminologyStore.put(key, entry);
        clearCacheForLanguagePair(entry.getSourceLanguage(), entry.getTargetLanguage());
        
        LOG.info("Updated terminology entry: {}", entry.getTermId());
    }

    @Override
    public void deleteTerm(String termId) throws TerminologyException {
        String key = findKeyByTermId(termId);
        if (key == null) {
            throw new TerminologyException("Term not found: " + termId, 
                    TerminologyException.ErrorCode.TERM_NOT_FOUND);
        }
        
        TerminologyEntry entry = terminologyStore.remove(key);
        clearCacheForLanguagePair(entry.getSourceLanguage(), entry.getTargetLanguage());
        
        LOG.info("Deleted terminology entry: {}", termId);
    }

    @Override
    public List<TerminologyEntry> getTerminology(String domain) throws TerminologyException {
        if (domain == null || domain.isEmpty()) {
            return new ArrayList<>(terminologyStore.values());
        }
        
        return terminologyStore.values().stream()
                .filter(e -> domain.equalsIgnoreCase(e.getDomain()))
                .collect(Collectors.toList());
    }

    @Override
    public void importTerminology(String filePath, TerminologyFormat format) throws TerminologyException {
        LOG.info("Importing terminology from: {} format: {}", filePath, format);
        
        switch (format) {
            case TBX:
                importFromTBX(filePath);
                break;
            case CSV:
                importFromCSV(filePath);
                break;
            case JSON:
                importFromJSON(filePath);
                break;
            default:
                throw new TerminologyException("Unsupported format: " + format, 
                        TerminologyException.ErrorCode.INVALID_FORMAT);
        }
        
        clearAllCache();
    }

    @Override
    public void exportTerminology(String domain, String filePath, TerminologyFormat format) 
            throws TerminologyException {
        LOG.info("Exporting terminology to: {} format: {}", filePath, format);
        
        List<TerminologyEntry> entries = getTerminology(domain);
        
        switch (format) {
            case TBX:
                exportToTBX(entries, filePath);
                break;
            case CSV:
                exportToCSV(entries, filePath);
                break;
            case JSON:
                exportToJSON(entries, filePath);
                break;
            default:
                throw new TerminologyException("Unsupported format: " + format, 
                        TerminologyException.ErrorCode.INVALID_FORMAT);
        }
    }

    private boolean matchesCriteria(TerminologyEntry entry, String searchTerm, 
            String sourceLanguage, String targetLanguage, String domain) {
        
        boolean langMatch = (sourceLanguage == null || entry.getSourceLanguage().equalsIgnoreCase(sourceLanguage))
                && (targetLanguage == null || entry.getTargetLanguage().equalsIgnoreCase(targetLanguage));
        
        boolean domainMatch = domain == null || entry.getDomain().equalsIgnoreCase(domain);
        
        boolean termMatch = entry.getSourceTerm().toLowerCase().contains(searchTerm)
                || searchTerm.contains(entry.getSourceTerm().toLowerCase());
        
        return langMatch && domainMatch && termMatch;
    }

    private float calculateConfidence(TerminologyEntry entry, String searchTerm) {
        if (entry.getSourceTerm().toLowerCase().equals(searchTerm)) {
            return 1.0f;
        } else if (entry.getSourceTerm().toLowerCase().startsWith(searchTerm)) {
            return 0.9f;
        } else if (entry.getSourceTerm().toLowerCase().contains(searchTerm)) {
            return 0.8f;
        }
        return 0.5f;
    }

    private String generateCacheKey(String text, String sourceLang, String targetLang, String domain) {
        return text + ":" + sourceLang + ":" + targetLang + ":" + domain;
    }

    private String generateEntryKey(TerminologyEntry entry) {
        return entry.getSourceLanguage() + ":" + entry.getTargetLanguage() + ":" 
                + entry.getDomain() + ":" + entry.getSourceTerm().toLowerCase();
    }

    private String findKeyByTermId(String termId) {
        for (Map.Entry<String, TerminologyEntry> entry : terminologyStore.entrySet()) {
            if (termId.equals(entry.getValue().getTermId())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void clearCacheForLanguagePair(String sourceLang, String targetLang) {
        cache.entrySet().removeIf(e -> e.getKey().contains(sourceLang + ":" + targetLang));
    }

    private void clearAllCache() {
        cache.clear();
    }

    private void importFromTBX(String filePath) throws TerminologyException {
        LOG.info("TBX import not fully implemented - sample data used");
    }

    private void importFromCSV(String filePath) throws TerminologyException {
        LOG.info("CSV import not fully implemented - sample data used");
    }

    private void importFromJSON(String filePath) throws TerminologyException {
        LOG.info("JSON import not fully implemented - sample data used");
    }

    private void exportToTBX(List<TerminologyEntry> entries, String filePath) {
        LOG.info("TBX export to: {}", filePath);
    }

    private void exportToCSV(List<TerminologyEntry> entries, String filePath) {
        LOG.info("CSV export to: {}", filePath);
    }

    private void exportToJSON(List<TerminologyEntry> entries, String filePath) {
        LOG.info("JSON export to: {}", filePath);
    }
}
