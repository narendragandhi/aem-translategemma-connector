package com.example.aem.translation.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TranslationCache {
    private static final Logger LOG = LoggerFactory.getLogger(TranslationCache.class);

    private final Cache<String, CachedTranslation> translationCache;
    private final Cache<String, CachedLanguageDetection> languageDetectionCache;

    public TranslationCache(int maxSize, int expireAfterMinutes) {
        this.translationCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.languageDetectionCache = Caffeine.newBuilder()
                .maximumSize(maxSize / 2)
                .expireAfterWrite(expireAfterMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();

        LOG.info("TranslationCache initialized with maxSize={}, expireAfterMinutes={}", 
                maxSize, expireAfterMinutes);
    }

    public String getTranslation(String sourceText, String sourceLang, String targetLang, 
                                  Function<String, String> loader) {
        String cacheKey = generateTranslationKey(sourceText, sourceLang, targetLang);
        
        CachedTranslation cached = translationCache.getIfPresent(cacheKey);
        if (cached != null) {
            LOG.debug("Cache hit for translation key: {}", cacheKey);
            return cached.getTranslation();
        }

        LOG.debug("Cache miss for translation key: {}", cacheKey);
        String translation = loader.apply(cacheKey);
        
        if (translation != null) {
            translationCache.put(cacheKey, new CachedTranslation(translation));
        }
        
        return translation;
    }

    public String getDetectedLanguage(String text, Function<String, String> loader) {
        String cacheKey = "detect:" + text;
        
        CachedLanguageDetection cached = languageDetectionCache.getIfPresent(cacheKey);
        if (cached != null) {
            LOG.debug("Cache hit for language detection: {}", cacheKey);
            return cached.getLanguage();
        }

        LOG.debug("Cache miss for language detection: {}", cacheKey);
        String language = loader.apply(cacheKey);
        
        if (language != null) {
            languageDetectionCache.put(cacheKey, new CachedLanguageDetection(language));
        }
        
        return language;
    }

    public void invalidateTranslation(String sourceText, String sourceLang, String targetLang) {
        String cacheKey = generateTranslationKey(sourceText, sourceLang, targetLang);
        translationCache.invalidate(cacheKey);
        LOG.info("Invalidated cache for key: {}", cacheKey);
    }

    public void invalidateAll() {
        translationCache.invalidateAll();
        languageDetectionCache.invalidateAll();
        LOG.info("All caches invalidated");
    }

    public CacheStats getStats() {
        return new CacheStats(
            translationCache.stats(),
            languageDetectionCache.stats()
        );
    }

    private String generateTranslationKey(String sourceText, String sourceLang, String targetLang) {
        return sourceLang + ":" + targetLang + ":" + sourceText.hashCode();
    }

    public static class CachedTranslation {
        private final String translation;
        private final long timestamp;

        public CachedTranslation(String translation) {
            this.translation = translation;
            this.timestamp = System.currentTimeMillis();
        }

        public String getTranslation() {
            return translation;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static class CachedLanguageDetection {
        private final String language;
        private final long timestamp;

        public CachedLanguageDetection(String language) {
            this.language = language;
            this.timestamp = System.currentTimeMillis();
        }

        public String getLanguage() {
            return language;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static class CacheStats {
        private final com.github.benmanes.caffeine.cache.stats.CacheStats translationStats;
        private final com.github.benmanes.caffeine.cache.stats.CacheStats languageStats;

        public CacheStats(com.github.benmanes.caffeine.cache.stats.CacheStats translationStats,
                         com.github.benmanes.caffeine.cache.stats.CacheStats languageStats) {
            this.translationStats = translationStats;
            this.languageStats = languageStats;
        }

        public long getTranslationHitCount() {
            return translationStats.hitCount();
        }

        public long getTranslationMissCount() {
            return translationStats.missCount();
        }

        public double getTranslationHitRate() {
            return translationStats.hitRate();
        }

        public long getLanguageDetectionHitCount() {
            return languageStats.hitCount();
        }
    }
}
