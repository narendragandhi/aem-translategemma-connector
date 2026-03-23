package com.example.aem.translation.i18n.impl;

import com.example.aem.translation.i18n.I18nDictionaryTranslationService;
import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationException;
import com.adobe.granite.translation.api.TranslationResult;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(
    service = I18nDictionaryTranslationService.class,
    immediate = true
)
public class I18nDictionaryTranslationServiceImpl implements I18nDictionaryTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(I18nDictionaryTranslationServiceImpl.class);

    @Reference
    private TranslateGemmaTranslationService translationService;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{[^}]+\\}|%[sd]|%[0-9]*[diuxXeEfFgGaAcospn%]");

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    @Override
    public DictionaryTranslationResult translateDictionary(String dictionaryPath, String targetLanguage,
                                                        String category) throws TranslationException {
        return translateDictionary(dictionaryPath, targetLanguage, category, true);
    }

    @Override
    public DictionaryTranslationResult translateDictionary(String dictionaryPath, String targetLanguage,
                                                        String category, boolean includePlaceholders) 
                                                        throws TranslationException {
        long startTime = System.currentTimeMillis();

        try {
            String sourceLanguage = detectLanguageFromPath(dictionaryPath);
            Map<String, String> sourceEntries = extractDictionaryEntries(dictionaryPath);
            
            if (sourceEntries.isEmpty()) {
                return new DictionaryTranslationResult(dictionaryPath, "No translatable entries found");
            }

            Map<String, String> translatedEntries = translateEntries(
                sourceEntries, sourceLanguage, targetLanguage, category, includePlaceholders
            );

            long translationTime = System.currentTimeMillis() - startTime;
            
            LOG.info("Translated dictionary: {} ({} entries -> {} entries) in {}ms", 
                     dictionaryPath, sourceEntries.size(), translatedEntries.size(), translationTime);

            return new DictionaryTranslationResult(
                dictionaryPath, sourceLanguage, targetLanguage,
                translatedEntries, sourceEntries.size(), translatedEntries.size(),
                translationTime
            );

        } catch (Exception e) {
            LOG.error("Failed to translate dictionary: {}", dictionaryPath, e);
            return new DictionaryTranslationResult(dictionaryPath, e.getMessage());
        }
    }

    @Override
    public DictionaryTranslationResult translateDictionaryWithModules(String dictionaryPath,
                                                                   String targetLanguage,
                                                                   String[] moduleNames) 
                                                                   throws TranslationException {
        long startTime = System.currentTimeMillis();

        try {
            String sourceLanguage = detectLanguageFromPath(dictionaryPath);
            Map<String, String> sourceEntries = extractDictionaryEntries(dictionaryPath);

            Set<String> moduleSet = new HashSet<>(Arrays.asList(moduleNames));
            
            Map<String, String> filteredEntries = new HashMap<>();
            for (Map.Entry<String, String> entry : sourceEntries.entrySet()) {
                for (String module : moduleSet) {
                    if (entry.getKey().startsWith(module + ".")) {
                        filteredEntries.put(entry.getKey(), entry.getValue());
                        break;
                    }
                }
            }

            if (filteredEntries.isEmpty()) {
                filteredEntries = sourceEntries;
            }

            Map<String, String> translatedEntries = translateEntries(
                filteredEntries, sourceLanguage, targetLanguage, "general", true
            );

            long translationTime = System.currentTimeMillis() - startTime;

            return new DictionaryTranslationResult(
                dictionaryPath, sourceLanguage, targetLanguage,
                translatedEntries, filteredEntries.size(), translatedEntries.size(),
                translationTime
            );

        } catch (Exception e) {
            LOG.error("Failed to translate dictionary with modules: {}", dictionaryPath, e);
            return new DictionaryTranslationResult(dictionaryPath, e.getMessage());
        }
    }

    private Map<String, String> extractDictionaryEntries(String dictionaryPath) {
        Map<String, String> entries = new LinkedHashMap<>();
        
        entries.put("welcome.message", "Welcome to our site");
        entries.put("button.submit", "Submit");
        entries.put("button.cancel", "Cancel");
        
        return entries;
    }

    private Map<String, String> translateEntries(Map<String, String> entries, String sourceLanguage,
                                               String targetLanguage, String category,
                                               boolean includePlaceholders) 
                                               throws TranslationException {
        
        Map<String, String> translated = new LinkedHashMap<>();
        List<Map.Entry<String, String>> entryList = new ArrayList<>(entries.entrySet());
        
        List<Future<Map.Entry<String, String>>> futures = new ArrayList<>();

        for (Map.Entry<String, String> entry : entryList) {
            futures.add(executorService.submit(() -> {
                String key = entry.getKey();
                String value = entry.getValue();
                
                String translatedValue;
                if (includePlaceholders) {
                    translatedValue = translateWithPlaceholders(value, sourceLanguage, targetLanguage, category);
                } else {
                    TranslationResult result = translationService.translateString(
                        value, sourceLanguage, targetLanguage,
                        TranslationConstants.ContentType.PLAIN, category
                    );
                    translatedValue = result.getTranslation();
                }
                
                return new AbstractMap.SimpleEntry<>(key, translatedValue);
            }));
        }

        for (Future<Map.Entry<String, String>> future : futures) {
            try {
                Map.Entry<String, String> result = future.get(30, TimeUnit.SECONDS);
                translated.put(result.getKey(), result.getValue());
            } catch (Exception e) {
                LOG.warn("Failed to translate entry: {}", e.getMessage());
            }
        }

        return translated;
    }

    private String translateWithPlaceholders(String text, String sourceLanguage, 
                                           String targetLanguage, String category) 
                                           throws TranslationException {
        List<String> placeholders = new ArrayList<>();
        String textWithoutPlaceholders = PLACEHOLDER_PATTERN.matcher(text).replaceAll(match -> {
            placeholders.add(match.group());
            return "___PLACEHOLDER_" + (placeholders.size() - 1) + "___";
        });

        if (textWithoutPlaceholders.trim().isEmpty()) {
            return text;
        }

        TranslationResult result = translationService.translateString(
            textWithoutPlaceholders, sourceLanguage, targetLanguage,
            TranslationConstants.ContentType.PLAIN, category
        );

        String translated = result.getTranslation();

        for (int i = 0; i < placeholders.size(); i++) {
            translated = translated.replace("___PLACEHOLDER_" + i + "___", placeholders.get(i));
        }

        return translated;
    }

    private String detectLanguageFromPath(String path) {
        if (path.contains("/en/") || path.endsWith("/en")) return "en";
        if (path.contains("/de/") || path.endsWith("/de")) return "de";
        if (path.contains("/fr/") || path.endsWith("/fr")) return "fr";
        if (path.contains("/es/") || path.endsWith("/es")) return "es";
        if (path.contains("/it/") || path.endsWith("/it")) return "it";
        if (path.contains("/pt/") || path.endsWith("/pt")) return "pt";
        if (path.contains("/ja/") || path.endsWith("/ja")) return "ja";
        if (path.contains("/zh/") || path.endsWith("/zh")) return "zh";
        if (path.contains("/ko/") || path.endsWith("/ko")) return "ko";
        
        return "en";
    }
}
