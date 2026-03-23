package com.example.aem.translation.cfintegration.service;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.example.aem.translation.cfintegration.service.result.ContentFragmentTranslationResult;
import com.adobe.granite.translation.api.TranslationException;

/**
 * A service for translating AEM Content Fragments.
 */
public interface ContentFragmentTranslationService {

    /**
     * Translates a Content Fragment.
     *
     * @param contentFragment The Content Fragment to translate.
     * @param targetLanguage The target language for the translation.
     * @param category The translation category.
     * @return A {@link ContentFragmentTranslationResult} containing the translation details.
     * @throws TranslationException if an error occurs during translation.
     */
    ContentFragmentTranslationResult translateContentFragment(
        ContentFragment contentFragment, String targetLanguage, String category) throws TranslationException;
}
