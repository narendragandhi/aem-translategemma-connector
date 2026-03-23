package com.example.aem.translation.cfintegration.service.impl;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.example.aem.translation.cfintegration.service.ContentFragmentTranslationService;
import com.example.aem.translation.cfintegration.service.result.ContentFragmentTranslationResult;
import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.adobe.granite.translation.api.TranslationResult;
import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component(
    service = ContentFragmentTranslationService.class,
    immediate = true
)
public class ContentFragmentTranslationServiceImpl implements ContentFragmentTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(ContentFragmentTranslationServiceImpl.class);

    @Reference
    private TranslateGemmaTranslationService translationService;

    @Override
    public ContentFragmentTranslationResult translateContentFragment(
            ContentFragment contentFragment, String targetLanguage, String category) throws TranslationException {

        long startTime = System.currentTimeMillis();
        Resource fragmentResource = contentFragment.adaptTo(Resource.class);
        ResourceResolver resourceResolver = fragmentResource.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page containingPage = pageManager.getContainingPage(fragmentResource);
        String sourceLanguage = containingPage.getLanguage(false).getLanguage();

        Map<String, String> translatedElements = new HashMap<>();

        try {
            Iterator<ContentElement> elements = contentFragment.getElements();
            while (elements.hasNext()) {
                ContentElement element = elements.next();
                if (isTranslatable(element)) {
                    String originalValue = element.getContent();
                    TranslationResult result = translationService.translateString(
                            originalValue, sourceLanguage, targetLanguage,
                            TranslationConstants.ContentType.HTML, category);
                    translatedElements.put(element.getName(), result.getTranslation());
                }
            }

            // In a real implementation, you would create a new Content Fragment
            // or update an existing one with the translated content.
            // For this example, we'll just log the translated elements.
            LOG.info("Translated Content Fragment elements: {}", translatedElements);

            long translationTime = System.currentTimeMillis() - startTime;
            return new ContentFragmentTranslationResult(
                    fragmentResource.getPath(), sourceLanguage, targetLanguage,
                    translatedElements, translationTime);

        } catch (Exception e) {
            LOG.error("Failed to translate Content Fragment: {}", fragmentResource.getPath(), e);
            return new ContentFragmentTranslationResult(fragmentResource.getPath(), e.getMessage());
        }
    }

    private boolean isTranslatable(ContentElement element) {
        String contentType = element.getValue().getContentType();
        return contentType != null && (contentType.startsWith("text/") || contentType.equals("application/json"));
    }
}
