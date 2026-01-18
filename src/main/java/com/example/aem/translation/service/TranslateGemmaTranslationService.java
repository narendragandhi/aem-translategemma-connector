package com.example.aem.translation.service;

import com.adobe.granite.translation.api.TranslationService;
import com.adobe.granite.translation.api.TranslationResult;
import com.adobe.granite.translation.api.TranslationConstants.ContentType;
import com.adobe.granite.translation.api.TranslationConstants.TranslationStatus;
import com.adobe.granite.translation.api.TranslationObject;
import com.adobe.granite.translation.api.TranslationState;
import com.adobe.granite.translation.api.TranslationMetadata;
import com.adobe.granite.translation.api.TranslationScope;
import com.adobe.granite.translation.api.TranslationException;
import com.adobe.granite.comments.Comment;
import com.adobe.granite.comments.CommentCollection;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

/**
 * TranslateGemma Translation Service interface extending AEM's TranslationService.
 * This service provides translation capabilities using Google's TranslateGemma model.
 */
public interface TranslateGemmaTranslationService extends TranslationService {

    /**
     * Get service information about the TranslateGemma connector.
     * 
     * @return TranslationServiceInfo containing service metadata
     */
    @Override
    TranslationServiceInfo getTranslationServiceInfo();

    /**
     * Check if the TranslateGemma service is available.
     * 
     * @return true if service is available, false otherwise
     */
    boolean isServiceAvailable();

    /**
     * Get supported languages for translation.
     * 
     * @return Map of language codes to language names
     */
    @Override
    Map<String, String> supportedLanguages();

    /**
     * Check if translation direction is supported.
     * 
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @return true if direction is supported, false otherwise
     * @throws TranslationException if error occurs
     */
    @Override
    boolean isDirectionSupported(String sourceLanguage, String targetLanguage) throws TranslationException;

    /**
     * Detect the language of given text.
     * 
     * @param detectSource text to analyze
     * @param contentType type of content (PLAIN or HTML)
     * @return detected language code
     * @throws TranslationException if error occurs
     */
    @Override
    String detectLanguage(String detectSource, ContentType contentType) throws TranslationException;

    /**
     * Translate a single string using TranslateGemma model.
     * 
     * @param sourceString text to translate
     * @param sourceLanguage source language code (can be null for auto-detection)
     * @param targetLanguage target language code
     * @param contentType content type
     * @param contentCategory content category
     * @return TranslationResult with translated text
     * @throws TranslationException if error occurs
     */
    @Override
    TranslationResult translateString(String sourceString, String sourceLanguage, 
                                     String targetLanguage, ContentType contentType, 
                                     String contentCategory) throws TranslationException;

    /**
     * Translate multiple strings using TranslateGemma model.
     * 
     * @param sourceStringArr array of texts to translate
     * @param sourceLanguage source language code (can be null for auto-detection)
     * @param targetLanguage target language code
     * @param contentType content type
     * @param contentCategory content category
     * @return array of TranslationResult objects
     * @throws TranslationException if error occurs
     */
    @Override
    TranslationResult[] translateArray(String[] sourceStringArr, String sourceLanguage,
                                      String targetLanguage, ContentType contentType,
                                      String contentCategory) throws TranslationException;

    // Asynchronous translation methods for Translation Jobs

    /**
     * Create a new translation job.
     */
    @Override
    String createTranslationJob(String name, String description, String strSourceLanguage,
                               String strTargetLanguage, Date dueDate, TranslationState state,
                               TranslationMetadata jobMetadata) throws TranslationException;

    /**
     * Upload a translation object for processing.
     */
    @Override
    String uploadTranslationObject(String strTranslationJobID, TranslationObject translationObject) 
            throws TranslationException;

    /**
     * Get the status of a translation job.
     */
    @Override
    TranslationStatus getTranslationJobStatus(String strTranslationJobID) throws TranslationException;

    /**
     * Update translation job state.
     */
    @Override
    TranslationStatus updateTranslationJobState(String strTranslationJobID, TranslationState state) 
            throws TranslationException;

    /**
     * Get translated object content.
     */
    @Override
    InputStream getTranslatedObject(String strTranslationJobID, TranslationObject translationObject) 
            throws TranslationException;

    // Additional async methods with default implementations for compatibility
    
    @Override
    default TranslationResult[] getAllStoredTranslations(String sourceString, String sourceLanguage,
            String targetLanguage, ContentType contentType, String contentCategory,
            String userId, int maxTranslations) throws TranslationException {
        // TranslateGemma doesn't have a traditional translation memory
        // Return an empty array or current translation
        return new TranslationResult[0];
    }

    @Override
    default void storeTranslation(String originalText, String sourceLanguage, String targetLanguage,
            String updatedTranslation, ContentType contentType, String contentCategory,
            String userId, int rating, String path) throws TranslationException {
        // Optional: Implement storage of approved translations for future reference
    }

    @Override
    default void storeTranslation(String[] originalText, String sourceLanguage, String targetLanguage,
            String[] updatedTranslation, ContentType contentType, String contentCategory,
            String userId, int rating, String path) throws TranslationException {
        // Optional: Implement batch storage of approved translations
    }

    @Override
    default String getDefaultCategory() {
        return "general";
    }

    @Override
    default void setDefaultCategory(String defaultCategory) {
        // Implementation if needed
    }

    // Remaining interface methods with default implementations
    
    @Override
    default void updateTranslationJobMetadata(String strTranslationJobID, TranslationMetadata jobMetadata,
            com.adobe.granite.translation.api.TranslationConstants.TranslationMethod translationMethod) 
            throws TranslationException {
        // Default implementation
    }

    @Override
    default TranslationScope getFinalScope(String strTranslationJobID) throws TranslationException {
        return null;
    }

    @Override
    default CommentCollection<Comment> getTranslationJobCommentCollection(String strTranslationJobID) 
            throws TranslationException {
        return null;
    }

    @Override
    default void addTranslationJobComment(String strTranslationJobID, Comment comment) 
            throws TranslationException {
        // Default implementation
    }

    @Override
    default CommentCollection<Comment> getTranslationObjectCommentCollection(String strTranslationJobID,
            TranslationObject translationObject) throws TranslationException {
        return null;
    }

    @Override
    default void addTranslationObjectComment(String strTranslationJobID, TranslationObject translationObject,
            Comment comment) throws TranslationException {
        // Default implementation
    }

    @Override
    default TranslationStatus[] getTranslationObjectsStatus(String strTranslationJobID,
            TranslationObject[] translationObjects) throws TranslationException {
        return new TranslationStatus[0];
    }

    @Override
    default TranslationStatus getTranslationObjectStatus(String strTranslationJobID,
            TranslationObject translationObject) throws TranslationException {
        return TranslationStatus.PENDING;
    }

    @Override
    default TranslationStatus[] updateTranslationObjectsState(String strTranslationJobID,
            TranslationObject[] translationObjects, TranslationState[] states) throws TranslationException {
        return new TranslationStatus[0];
    }

    @Override
    default TranslationStatus updateTranslationObjectState(String strTranslationJobID,
            TranslationObject translationObject, TranslationState state) throws TranslationException {
        return TranslationStatus.PENDING;
    }

    @Override
    default void updateDueDate(String strTranslationJobID, Date date) throws TranslationException {
        // Default implementation
    }
}