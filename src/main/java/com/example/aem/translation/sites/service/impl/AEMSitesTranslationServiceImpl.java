package com.example.aem.translation.sites.service.impl;

import com.example.aem.translation.sites.service.AEMSitesTranslationService;
import com.example.aem.translation.sites.service.result.*;
import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.adobe.granite.translation.api.TranslationResult;
import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationException;
import com.adobe.granite.translation.api.TranslationState;
import com.adobe.granite.comments.Comment;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.dam.api.Asset;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

/**
 * Implementation of AEM Sites Translation Service using TranslateGemma.
 */
@Component(
    service = AEMSitesTranslationService.class,
    immediate = true,
    property = {
        "service.ranking:Integer=100"
    }
)
public class AEMSitesTranslationServiceImpl implements AEMSitesTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(AEMSitesTranslationServiceImpl.class);

    private static final Set<String> TRANSLATABLE_PROPERTIES = Set.of(
        "jcr:title", "jcr:description", "text", "subtitle", "heading", "alt", 
        "title", "label", "caption", "tooltip", "placeholder"
    );

    private static final Set<String> TRANSLATABLE_COMPONENT_TYPES = Set.of(
        "core/wcm/components/text/v2/text",
        "core/wcm/components/title/v2/title",
        "core/wcm/components/button/v1/button",
        "core/wcm/components/teaser/v1/teaser",
        "core/wcm/components/image/v2/image",
        "core/wcm/components/carousel/v1/carousel"
    );

    @Reference
    private TranslateGemmaTranslationService translationService;

    @Override
    public PageTranslationResult translatePage(Page page, String targetLanguage, String category) 
            throws TranslationException {
        
        long startTime = System.currentTimeMillis();
        String sourceLanguage = page.getLanguage(false).getLanguage();
        
        LOG.info("Starting translation for page: {} from {} to {}", 
                page.getPath(), sourceLanguage, targetLanguage);

        try {
            ResourceResolver resourceResolver = page.getContentResource().getResourceResolver();
            Map<String, String> translatedProperties = new HashMap<>();
            Map<String, ComponentTranslationResult> componentResults = new HashMap<>();
            List<Tag> translatedTags = new ArrayList<>();

            // Translate page properties
            translatePageProperties(page, sourceLanguage, targetLanguage, category, translatedProperties);

            // Translate page components
            Resource contentResource = page.getContentResource();
            if (contentResource != null) {
                componentResults = translateComponents(contentResource, sourceLanguage, targetLanguage, category);
            }

            // Translate page tags
            translatedTags = translateResourceTags(page.getContentResource(), sourceLanguage, targetLanguage);

            // Apply translations to page
            applyPageTranslations(page, translatedProperties, componentResults, translatedTags);

            long translationTime = System.currentTimeMillis() - startTime;
            
            LOG.info("Completed translation for page: {} in {}ms", page.getPath(), translationTime);

            return new PageTranslationResult(
                page.getPath(), sourceLanguage, targetLanguage,
                translatedProperties, componentResults, translatedTags, translationTime
            );
        } catch (Exception e) {
            LOG.error("Failed to translate page: {}", page.getPath(), e);
            throw new TranslationException("Page translation failed: " + e.getMessage(),
                TranslationException.ErrorCode.TRANSLATION_FAILED);
        }
    }

    @Override
    public Map<String, ComponentTranslationResult> translateComponents(Resource contentResource, 
                                                                  String sourceLanguage, 
                                                                  String targetLanguage, 
                                                                  String category) throws TranslationException {
        Map<String, ComponentTranslationResult> results = new HashMap<>();

        Iterable<Resource> children = contentResource.getChildren();
        for (Resource componentResource : children) {
            try {
                ComponentTranslationResult result = translateComponent(
                    componentResource, sourceLanguage, targetLanguage, category
                );
                results.put(componentResource.getPath(), result);
            } catch (Exception e) {
                LOG.warn("Failed to translate component: {}", componentResource.getPath(), e);
                results.put(componentResource.getPath(), new ComponentTranslationResult(
                    componentResource.getPath(), 
                    getResourceType(componentResource),
                    "Translation failed: " + e.getMessage()
                ));
            }
        }

        return results;
    }

    @Override
    public AssetTranslationResult translateAssetMetadata(Asset asset, 
                                                   String sourceLanguage, 
                                                   String targetLanguage, 
                                                   String category) throws TranslationException {
        
        LOG.info("Translating metadata for asset: {}", asset.getPath());

        try {
            Map<String, String> translatedMetadata = new HashMap<>();

            // Get asset metadata
            Map<String, Object> metadataMap = asset.getMetadata();

            // Translate standard metadata fields
            translateMetadataField(metadataMap, "dc:title", sourceLanguage, targetLanguage, category, translatedMetadata);
            translateMetadataField(metadataMap, "dc:description", sourceLanguage, targetLanguage, category, translatedMetadata);
            translateMetadataField(metadataMap, "dc:subject", sourceLanguage, targetLanguage, category, translatedMetadata);

            // Translate custom metadata fields
            for (String key : metadataMap.keySet()) {
                if (key.startsWith("custom:") && metadataMap.get(key) instanceof String) {
                    translateMetadataField(metadataMap, key, sourceLanguage, targetLanguage, category, translatedMetadata);
                }
            }

            // Apply translated metadata to asset
            applyAssetMetadataTranslation(asset, translatedMetadata);

            return new AssetTranslationResult(
                asset.getPath(), asset.getMimeType(), translatedMetadata
            );

        } catch (Exception e) {
            LOG.error("Failed to translate asset metadata: {}", asset.getPath(), e);
            return new AssetTranslationResult(
                asset.getPath(), asset.getMimeType(), e.getMessage()
            );
        }
    }

    @Override
    public List<Tag> translateResourceTags(Resource resource, String sourceLanguage, String targetLanguage) 
            throws TranslationException {
        
        List<Tag> translatedTags = new ArrayList<>();
        
        try {
            TagManager tagManager = resource.getResourceResolver().adaptTo(TagManager.class);
            if (tagManager == null) {
                return translatedTags;
            }

            Tag[] tags = tagManager.getTags(resource);
            for (Tag tag : tags) {
                try {
                    // Translate tag title
                    TranslationResult result = translationService.translateString(
                        tag.getTitle(), sourceLanguage, targetLanguage, TranslationConstants.ContentType.PLAIN, "general"
                    );

                    // Create or find translated tag
                    Tag translatedTag = tagManager.resolveByTitle(result.getTranslation(), new Locale(targetLanguage));
                    if (translatedTag == null) {
                        // Create new tag if it doesn't exist
                        translatedTag = createTranslatedTag(tagManager, tag, result.getTranslation(), targetLanguage);
                    }

                    if (translatedTag != null) {
                        translatedTags.add(translatedTag);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to translate tag: {}", tag.getTitle(), e);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to translate tags for resource: {}", resource.getPath(), e);
        }

        return translatedTags;
    }

    @Override
    public String createTranslationJob(List<Page> pages, List<Asset> assets, 
                                   String sourceLanguage, String targetLanguage, 
                                   String jobName) throws TranslationException {
        
        LOG.info("Creating translation job: {} for {} pages and {} assets", 
                jobName, pages.size(), assets.size());

        try {
            String jobDescription = String.format(
                "Translate %d pages and %d assets from %s to %s",
                pages.size(), assets.size(), sourceLanguage, targetLanguage
            );

            TranslationState initialState = new TranslationState() {
                @Override
                public TranslationConstants.TranslationStatus getStatus() {
                    return TranslationConstants.TranslationStatus.DRAFT;
                }

                @Override
                public Comment getComment() {
                    return null;
                }

                @Override
                public void setStatus(TranslationConstants.TranslationStatus status) {
                }

                @Override
                public void setComment(Comment c) {
                }
            };

            return translationService.createTranslationJob(
                jobName, jobDescription, sourceLanguage, targetLanguage,
                new Date(System.currentTimeMillis() + 86400000), // Due in 24 hours
                initialState, null
            );
        } catch (Exception e) {
            LOG.error("Failed to create translation job: {}", jobName, e);
            throw new TranslationException("Failed to create translation job: " + e.getMessage(),
                TranslationException.ErrorCode.TRANSLATION_FAILED);
        }
    }

    @Override
    public TranslationJobStatus getJobStatus(String jobId) throws TranslationException {
        try {
            var status = translationService.getTranslationJobStatus(jobId);

            // Convert to our status format (simplified)
            return new TranslationJobStatus(
                jobId, status.toString(), 0, 0, 0, 0, new Date()
            );
        } catch (Exception e) {
            LOG.error("Failed to get status for job: {}", jobId, e);
            throw new TranslationException("Failed to get job status: " + e.getMessage(),
                TranslationException.ErrorCode.TRANSLATION_FAILED);
        }
    }

    // Private helper methods

    private void translatePageProperties(Page page, String sourceLanguage, String targetLanguage,
                                     String category, Map<String, String> translatedProperties) 
            throws TranslationException {
        
        Resource contentResource = page.getContentResource();
        if (contentResource == null) return;

        ValueMap properties = contentResource.adaptTo(ValueMap.class);
        if (properties == null) return;

        // Translate title
        String title = page.getTitle();
            if (title != null && !title.trim().isEmpty()) {
                TranslationResult result = translationService.translateString(
                    title, sourceLanguage, targetLanguage, TranslationConstants.ContentType.PLAIN, category
                );
                translatedProperties.put("jcr:title", result.getTranslation());
            }

        // Translate description
        String description = page.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            TranslationResult result = translationService.translateString(
                description, sourceLanguage, targetLanguage, TranslationConstants.ContentType.PLAIN, category
            );
            translatedProperties.put("jcr:description", result.getTranslation());
        }

        // Translate additional properties from content resource
        for (String propertyName : TRANSLATABLE_PROPERTIES) {
            String value = properties.get(propertyName, String.class);
            if (value != null && !value.trim().isEmpty() && 
                !translatedProperties.containsKey(propertyName)) {
                TranslationResult result = translationService.translateString(
                    value, sourceLanguage, targetLanguage, TranslationConstants.ContentType.PLAIN, category
                );
                translatedProperties.put(propertyName, result.getTranslation());
            }
        }
    }

    private ComponentTranslationResult translateComponent(Resource componentResource, 
                                                   String sourceLanguage, 
                                                   String targetLanguage, 
                                                   String category) throws TranslationException {
        
        String componentType = getResourceType(componentResource);
        if (!isTranslatableComponent(componentType)) {
            return new ComponentTranslationResult(
                componentResource.getPath(), componentType, new HashMap<>()
            );
        }

        ValueMap properties = componentResource.adaptTo(ValueMap.class);
        if (properties == null) {
            return new ComponentTranslationResult(
                componentResource.getPath(), componentType, new HashMap<>()
            );
        }

        Map<String, String> translatedProperties = new HashMap<>();

        for (String propertyName : TRANSLATABLE_PROPERTIES) {
            String value = properties.get(propertyName, String.class);
            if (value != null && !value.trim().isEmpty()) {
                try {
                    TranslationConstants.ContentType contentType = determineContentType(propertyName, componentType);
                    TranslationResult result = translationService.translateString(
                        value, sourceLanguage, targetLanguage, contentType, category
                    );
                    translatedProperties.put(propertyName, result.getTranslation());
                } catch (Exception e) {
                    LOG.warn("Failed to translate property {} of component {}", 
                             propertyName, componentResource.getPath(), e);
                }
            }
        }

        return new ComponentTranslationResult(
            componentResource.getPath(), componentType, translatedProperties
        );
    }

    private void translateMetadataField(Map<String, Object> metadata, String fieldName, 
                                     String sourceLanguage, String targetLanguage, 
                                     String category, Map<String, String> translatedMetadata) 
            throws TranslationException {
        
        Object value = metadata.get(fieldName);
        if (value instanceof String && value != null && !((String) value).trim().isEmpty()) {
            TranslationResult result = translationService.translateString(
                (String) value, sourceLanguage, targetLanguage, TranslationConstants.ContentType.PLAIN, category
            );
            translatedMetadata.put(fieldName, result.getTranslation());
        }
    }

    private void applyPageTranslations(Page page, Map<String, String> translatedProperties,
                                    Map<String, ComponentTranslationResult> componentResults,
                                    List<Tag> translatedTags) throws RepositoryException {
        
        Resource contentResource = page.getContentResource();
        if (contentResource == null) return;

        ModifiableValueMap properties = contentResource.adaptTo(ModifiableValueMap.class);
        if (properties != null) {
            Map<String, Object> propsMap = properties;
            for (Map.Entry<String, String> entry : translatedProperties.entrySet()) {
                propsMap.put(entry.getKey(), entry.getValue());
            }
        }

        // Apply component translations
        for (ComponentTranslationResult result : componentResults.values()) {
            if (result.isSuccess()) {
                Resource componentResource = contentResource.getResourceResolver().getResource(result.getComponentPath());
                if (componentResource != null) {
                    applyComponentTranslations(componentResource, result.getTranslatedProperties());
                }
            }
        }

        // Apply translated tags
        ResourceResolver resourceResolver = contentResource.getResourceResolver();
        TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
        if (tagManager != null && !translatedTags.isEmpty()) {
            tagManager.setTags(contentResource, translatedTags.toArray(new Tag[0]));
        }
    }

    private void applyComponentTranslations(Resource component, Map<String, String> translations) {
        ModifiableValueMap properties = component.adaptTo(ModifiableValueMap.class);
        if (properties != null) {
            Map<String, Object> propsMap = properties;
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                propsMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private void applyAssetMetadataTranslation(Asset asset, Map<String, String> translatedMetadata) {
        Map<String, Object> metadata = asset.getMetadata();
        if (metadata instanceof ModifiableValueMap) {
            ModifiableValueMap modifiableMetadata = (ModifiableValueMap) metadata;
            Map<String, Object> modMap = modifiableMetadata;
            for (Map.Entry<String, String> entry : translatedMetadata.entrySet()) {
                modMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Tag createTranslatedTag(TagManager tagManager, Tag originalTag, 
                                  String translatedTitle, String language) {
        try {
            // Create new tag namespace structure for the language
            String tagPath = originalTag.getPath().replaceAll("/[^/]+$", "") + "/" + language;
            
            // Create the tag (simplified - actual implementation would need proper namespace handling)
            return tagManager.createTag(tagPath, translatedTitle, translatedTitle);
        } catch (Exception e) {
            LOG.error("Failed to create translated tag: {}", translatedTitle, e);
            return null;
        }
    }

    private boolean isTranslatableComponent(String componentType) {
        return TRANSLATABLE_COMPONENT_TYPES.stream().anyMatch(componentType::contains);
    }

    private String getResourceType(Resource resource) {
        ValueMap properties = resource.adaptTo(ValueMap.class);
        return properties != null ? properties.get("sling:resourceType", String.class) : "";
    }

    private TranslationConstants.ContentType determineContentType(String propertyName, String componentType) {
        // Determine content type based on property name and component type
        if ("text".equals(propertyName) && componentType.contains("text")) {
            return TranslationConstants.ContentType.HTML;
        }
        return TranslationConstants.ContentType.PLAIN;
    }
}