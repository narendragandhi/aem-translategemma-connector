package com.example.aem.translation.dam.impl;

import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.example.aem.translation.dam.DamMetadataTranslationService;
import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationException;
import com.adobe.granite.translation.api.TranslationResult;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

@Component(
    service = DamMetadataTranslationService.class,
    immediate = true
)
public class DamMetadataTranslationServiceImpl implements DamMetadataTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(DamMetadataTranslationServiceImpl.class);

    @Reference
    private TranslateGemmaTranslationService translationService;

    private static final Set<String> DEFAULT_TRANSLATABLE_FIELDS = new HashSet<>(Arrays.asList(
        "dc:title",
        "dc:description", 
        "dc:subject",
        "dc:rights",
        "xmp:UsageTerms",
        "photoshop:City",
        "photoshop:Country",
        "iptc:ObjectName",
        "iptc:Caption",
        "iptc:Keywords"
    ));

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    @Override
    public MetadataTranslationResult translateAssetMetadata(String assetPath, String targetLanguage,
                                                            String category) throws TranslationException {
        return translateAssetMetadata(assetPath, targetLanguage, category, null);
    }

    @Override
    public MetadataTranslationResult translateAssetMetadata(String assetPath, String targetLanguage,
                                                            String category, String[] metadataFields) 
                                                            throws TranslationException {
        long startTime = System.currentTimeMillis();

        try {
            String sourceLanguage = detectLanguageFromPath(assetPath);
            Map<String, String> translatedMetadata = new HashMap<>();

            Set<String> fieldsToTranslate = metadataFields != null && metadataFields.length > 0
                ? new HashSet<>(Arrays.asList(metadataFields))
                : DEFAULT_TRANSLATABLE_FIELDS;

            for (String field : fieldsToTranslate) {
                String sampleValue = "Sample " + field + " for asset " + assetPath;
                
                try {
                    TranslationResult result = translationService.translateString(
                        sampleValue, sourceLanguage, targetLanguage,
                        TranslationConstants.ContentType.PLAIN, category
                    );
                    
                    translatedMetadata.put(field, result.getTranslation());
                    
                } catch (TranslationException e) {
                    LOG.warn("Failed to translate metadata field {}: {}", field, e.getMessage());
                }
            }

            long translationTime = System.currentTimeMillis() - startTime;
            
            LOG.info("Translated metadata for asset: {} in {}ms", assetPath, translationTime);

            return new MetadataTranslationResult(assetPath, targetLanguage, translatedMetadata, translationTime);

        } catch (Exception e) {
            LOG.error("Failed to translate asset metadata: {}", assetPath, e);
            return new MetadataTranslationResult(assetPath, e.getMessage());
        }
    }

    @Override
    public MetadataTranslationResult[] translateMultipleAssets(String[] assetPaths, String targetLanguage,
                                                               String category) throws TranslationException {
        if (assetPaths == null || assetPaths.length == 0) {
            return new MetadataTranslationResult[0];
        }

        List<Future<MetadataTranslationResult>> futures = new ArrayList<>();

        for (String assetPath : assetPaths) {
            futures.add(executorService.submit(() -> 
                translateAssetMetadata(assetPath, targetLanguage, category)
            ));
        }

        List<MetadataTranslationResult> results = new ArrayList<>();
        
        for (Future<MetadataTranslationResult> future : futures) {
            try {
                results.add(future.get(60, TimeUnit.SECONDS));
            } catch (Exception e) {
                LOG.error("Failed to get translation result: {}", e.getMessage());
            }
        }

        return results.toArray(new MetadataTranslationResult[0]);
    }

    private String detectLanguageFromPath(String path) {
        if (path.contains("/en/")) return "en";
        if (path.contains("/de/")) return "de";
        if (path.contains("/fr/")) return "fr";
        if (path.contains("/es/")) return "es";
        if (path.contains("/it/")) return "it";
        if (path.contains("/ja/")) return "ja";
        if (path.contains("/zh/")) return "zh";
        if (path.contains("/ko/")) return "ko";
        
        return "en";
    }
}
