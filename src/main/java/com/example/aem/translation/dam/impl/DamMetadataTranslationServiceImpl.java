package com.example.aem.translation.dam.impl;

import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.example.aem.translation.service.PromptTemplateService;
import com.example.aem.translation.service.MultiModalTranslationService;
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

    @Reference
    private PromptTemplateService promptTemplateService;

    @Reference
    private MultiModalTranslationService multiModalTranslationService;

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

    @Override
    public VisualTranslationResult translateAssetVisualContent(String assetPath, String targetLanguage) 
            throws TranslationException {
        
        LOG.info("Starting visual translation for asset: {} to language: {}", assetPath, targetLanguage);

        try {
            // In a real AEM environment, we would fetch the asset binary here.
            // For now, we simulate or use a mock.
            byte[] imageData = getAssetBinaryMock(assetPath);
            String mimeType = "image/jpeg"; // Default mock type

            Map<String, Object> vars = new HashMap<>();
            vars.put("targetLanguage", targetLanguage);

            String prompt = promptTemplateService.renderPrompt("ocr", vars);

            MultiModalTranslationService.MultiModalResult result = 
                multiModalTranslationService.analyzeImage(imageData, mimeType, prompt);

            if (result.isSuccess()) {
                return new VisualTranslationResult(
                    assetPath,
                    targetLanguage,
                    result.getValue("ocrText"),
                    result.getValue("translatedOcrText"),
                    result.getValue("altText")
                );
            } else {
                return new VisualTranslationResult(assetPath, "Analysis failed: " + result.getRawResponse());
            }

        } catch (Exception e) {
            LOG.error("Error in visual content translation for {}", assetPath, e);
            throw new TranslationException("Visual translation failed: " + e.getMessage(), e);
        }
    }

    private byte[] getAssetBinaryMock(String path) {
        // Mock binary data for sandbox environment
        return new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xff, (byte)0xff, (byte)0xff, 0x21, (byte)0xf9, 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3b};
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
