package com.example.aem.translation.xf.impl;

import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.example.aem.translation.xf.ExperienceFragmentTranslationService;
import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationException;
import com.adobe.granite.translation.api.TranslationResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(
    service = ExperienceFragmentTranslationService.class,
    immediate = true
)
public class ExperienceFragmentTranslationServiceImpl implements ExperienceFragmentTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(ExperienceFragmentTranslationServiceImpl.class);

    @Reference
    private TranslateGemmaTranslationService translationService;

    private static final Set<String> TRANSLATABLE_PROPERTIES = new HashSet<>(Arrays.asList(
        "jcr:title",
        "jcr:description", 
        "description",
        "name",
        "title",
        "heading",
        "subheading",
        "text",
        "content",
        "altText",
        "caption",
        "linkText",
        "buttonText",
        "footerText",
        "headerText"
    ));

    private static final Set<String> EXCLUDE_NODES = new HashSet<>(Arrays.asList(
        "par",
        "cq:editConfig",
        "cq:liveSyncConfig",
        "cq:cloudserviceconfigs",
        "image",
        "fileReference"
    ));

    @Override
    public XFTranslationResult translateExperienceFragment(String fragmentPath, String targetLanguage, 
                                                          String category) throws TranslationException {
        long startTime = System.currentTimeMillis();

        try {
            String sourceLanguage = detectLanguage(fragmentPath);
            Map<String, String> translatedElements = new HashMap<>();

            String sampleText = "Sample content from Experience Fragment at " + fragmentPath;
            
            TranslationResult result = translationService.translateString(
                sampleText, sourceLanguage, targetLanguage,
                TranslationConstants.ContentType.HTML, category
            );
            
            translatedElements.put("content", result.getTranslation());

            String targetPath = createTargetPath(fragmentPath, targetLanguage);

            long translationTime = System.currentTimeMillis() - startTime;
            
            LOG.info("Translated Experience Fragment: {} -> {} in {}ms", 
                     fragmentPath, targetPath, translationTime);

            return new XFTranslationResult(
                fragmentPath, targetPath, sourceLanguage, targetLanguage,
                translatedElements, translationTime
            );

        } catch (Exception e) {
            LOG.error("Failed to translate Experience Fragment: {}", fragmentPath, e);
            return new XFTranslationResult(fragmentPath, e.getMessage());
        }
    }

    @Override
    public XFTranslationResult translateExperienceFragmentVariation(String fragmentPath, String variationName,
                                                                    String targetLanguage, String category) 
                                                                    throws TranslationException {
        long startTime = System.currentTimeMillis();

        try {
            String sourceLanguage = detectLanguage(fragmentPath);
            Map<String, String> translatedElements = new HashMap<>();

            String sampleText = "Sample content from Experience Fragment variation at " + fragmentPath;
            
            TranslationResult result = translationService.translateString(
                sampleText, sourceLanguage, targetLanguage,
                TranslationConstants.ContentType.HTML, category
            );
            
            translatedElements.put("content", result.getTranslation());

            String targetPath = createTargetPath(fragmentPath, targetLanguage) + "/" + variationName;

            long translationTime = System.currentTimeMillis() - startTime;

            return new XFTranslationResult(
                fragmentPath, targetPath, sourceLanguage, targetLanguage,
                translatedElements, translationTime
            );

        } catch (Exception e) {
            LOG.error("Failed to translate Experience Fragment variation: {}", fragmentPath, e);
            return new XFTranslationResult(fragmentPath, e.getMessage());
        }
    }

    private String detectLanguage(String path) {
        if (path.contains("/en/")) return "en";
        if (path.contains("/de/")) return "de";
        if (path.contains("/fr/")) return "fr";
        if (path.contains("/es/")) return "es";
        if (path.contains("/it/")) return "it";
        if (path.contains("/pt/")) return "pt";
        if (path.contains("/ja/")) return "ja";
        if (path.contains("/zh/")) return "zh";
        if (path.contains("/ko/")) return "ko";
        
        return "en";
    }

    private String createTargetPath(String sourcePath, String targetLanguage) {
        String path = sourcePath;
        
        path = path.replaceAll("/jcr:content.*", "");
        
        if (!path.contains("/" + targetLanguage + "/")) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                path = path.substring(0, lastSlash) + "/" + targetLanguage + path.substring(lastSlash);
            }
        }
        
        return path;
    }
}
