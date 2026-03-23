package com.example.aem.translation.tm.impl;

import com.example.aem.translation.tm.TranslationMemoryService;
import com.adobe.granite.translation.api.TranslationConstants;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component(
    service = TranslationMemoryService.class,
    immediate = true
)
public class JcrTranslationMemoryService implements TranslationMemoryService {

    private static final Logger LOG = LoggerFactory.getLogger(JcrTranslationMemoryService.class);
    private static final String TM_ROOT_PATH = "/var/translationmemory/translategemma";
    private static final double DEFAULT_MIN_SCORE = 0.7;

    private final Map<String, List<TranslationMemoryService.TMEntry>> inMemoryCache = new ConcurrentHashMap<>();

    @Override
    public void storeTranslation(String sourceText, String targetText, String sourceLanguage,
                                String targetLanguage, TranslationConstants.ContentType contentType,
                                String category, String path, int rating) throws Exception {
        if (StringUtils.isBlank(sourceText) || StringUtils.isBlank(targetText)) {
            LOG.warn("Cannot store translation with blank source or target text");
            return;
        }

        String key = createKey(sourceText, sourceLanguage, targetLanguage, contentType, category);
        
        TranslationMemoryService.TMEntry entry = new TranslationMemoryService.TMEntry(
            sourceText, targetText, sourceLanguage, targetLanguage,
            contentType, category, path, rating
        );

        inMemoryCache.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);

        LOG.debug("Stored translation in TM: {} -> {}", sourceLanguage, targetLanguage);
    }

    @Override
    public List<TranslationMemoryService.TMEntry> findMatches(String sourceText, String sourceLanguage, String targetLanguage,
                                    TranslationConstants.ContentType contentType, String category,
                                    double minScore, int maxResults) {
        if (StringUtils.isBlank(sourceText)) {
            return Collections.emptyList();
        }

        if (minScore <= 0) {
            minScore = DEFAULT_MIN_SCORE;
        }
        final double effectiveMinScore = minScore;
        if (maxResults <= 0) {
            maxResults = 5;
        }

        String baseKey = createKey(sourceText, sourceLanguage, targetLanguage, contentType, category);
        
        List<TranslationMemoryService.TMEntry> results = new ArrayList<>();

        if (inMemoryCache.containsKey(baseKey)) {
            results.addAll(inMemoryCache.get(baseKey));
        }

        String partialKey = sourceLanguage + "_" + targetLanguage;
        inMemoryCache.keySet().stream()
            .filter(k -> k.startsWith(partialKey) && !k.equals(baseKey))
            .flatMap(k -> inMemoryCache.get(k).stream())
            .filter(e -> calculateSimilarity(sourceText, e.getSourceText()) >= effectiveMinScore)
            .sorted((a, b) -> Double.compare(
                calculateSimilarity(sourceText, b.getSourceText()),
                calculateSimilarity(sourceText, a.getSourceText())
            ))
            .limit(maxResults)
            .forEach(e -> { 
                if (!results.contains(e)) { 
                    results.add(e); 
                } 
            });

        LOG.debug("Found {} TM matches for: {}", results.size(), sourceText.substring(0, Math.min(50, sourceText.length())));
        
        return results.stream().limit(maxResults).collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalEntries", inMemoryCache.values().stream().mapToInt(List::size).sum());
        stats.put("totalKeys", inMemoryCache.size());
        
        Map<String, Long> langPairCounts = new HashMap<>();
        inMemoryCache.values().stream()
            .flatMap(List::stream)
            .forEach(e -> {
                String pair = e.getSourceLanguage() + "->" + e.getTargetLanguage();
                langPairCounts.merge(pair, 1L, Long::sum);
            });
        
        stats.put("uniqueLanguagePairs", langPairCounts.size());
        
        return stats;
    }

    @Override
    public void clearMemory() {
        inMemoryCache.clear();
        LOG.info("Translation memory cleared");
    }

    @Override
    public String getStoragePath() {
        return TM_ROOT_PATH;
    }

    private String createKey(String sourceText, String sourceLanguage, String targetLanguage,
                            TranslationConstants.ContentType contentType, String category) {
        String textHash = Integer.toHexString(sourceText.toLowerCase().hashCode());
        String contentTypeStr = contentType != null ? contentType.name() : "PLAIN";
        String categoryStr = StringUtils.defaultString(category, "general");
        return sourceLanguage + "_" + targetLanguage + "_" + contentTypeStr + "_" + categoryStr + "_" + textHash;
    }

    private double calculateSimilarity(String text1, String text2) {
        if (text1.equalsIgnoreCase(text2)) {
            return 1.0;
        }
        
        String s1 = text1.toLowerCase();
        String s2 = text2.toLowerCase();
        
        if (s1.contains(s2) || s2.contains(s1)) {
            return 0.9;
        }
        
        double levenshteinScore = 1.0 - ((double) levenshteinDistance(s1, s2) / Math.max(s1.length(), s2.length()));
        
        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));
        
        words1.retainAll(words2);
        double jaccardScore = words1.size() > 0 ? ((double) words1.size() / Math.max(1, s1.length())) : 0;
        
        return (levenshteinScore * 0.7) + (jaccardScore * 0.3);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}
