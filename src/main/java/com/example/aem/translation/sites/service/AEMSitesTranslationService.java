package com.example.aem.translation.sites.service;

import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.example.aem.translation.sites.service.result.*;
import com.adobe.granite.translation.api.TranslationResult;
import com.adobe.granite.translation.api.TranslationConstants.ContentType;
import com.adobe.granite.translation.api.TranslationException;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.dam.api.Asset;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.ModifiableValueMap;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service for translating AEM Sites pages, components, and assets using TranslateGemma.
 */
public interface AEMSitesTranslationService {

    /**
     * Translates a complete page including title, description, and content components.
     *
     * @param page the page to translate
     * @param targetLanguage target language code
     * @param category content category for translation context
     * @return TranslationResult containing summary of translated content
     * @throws TranslationException if translation fails
     */
    PageTranslationResult translatePage(Page page, String targetLanguage, String category) throws TranslationException;

    /**
     * Translates page components within a content resource.
     *
     * @param contentResource the content resource containing components
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @param category content category for translation context
     * @return Map of component paths to their translation results
     * @throws TranslationException if translation fails
     */
    Map<String, ComponentTranslationResult> translateComponents(Resource contentResource,
                                                            String sourceLanguage,
                                                            String targetLanguage,
                                                            String category) throws TranslationException;

    /**
     * Translates asset metadata including title, description, and custom metadata.
     *
     * @param asset the asset to translate
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @param category content category for translation context
     * @return AssetTranslationResult with translated metadata
     * @throws TranslationException if translation fails
     */
    AssetTranslationResult translateAssetMetadata(Asset asset,
                                               String sourceLanguage,
                                               String targetLanguage,
                                               String category) throws TranslationException;

    /**
     * Translates tags associated with a resource.
     *
     * @param resource the resource with tags
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @return List of translated tags
     * @throws TranslationException if translation fails
     */
    List<Tag> translateResourceTags(Resource resource, String sourceLanguage, String targetLanguage) throws TranslationException;

    /**
     * Creates a translation job for multiple pages and assets.
     *
     * @param pages list of pages to translate
     * @param assets list of assets to translate
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @param jobName name for the translation job
     * @return job ID for tracking translation progress
     * @throws TranslationException if job creation fails
     */
    String createTranslationJob(List<Page> pages, List<Asset> assets,
                             String sourceLanguage, String targetLanguage,
                             String jobName) throws TranslationException;

    /**
     * Gets the status of a translation job.
     *
     * @param jobId the translation job ID
     * @return current status of the translation job
     * @throws TranslationException if status check fails
     */
    TranslationJobStatus getJobStatus(String jobId) throws TranslationException;
}
