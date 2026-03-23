package com.adobe.granite.translation.api;

import com.adobe.granite.comments.Comment;
import com.adobe.granite.comments.CommentCollection;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

public interface TranslationService {

    interface TranslationServiceInfo {
        String getTranslationServiceName();
        String getTranslationServiceLabel();
        String getTranslationServiceAttribution();
        TranslationConstants.TranslationMethod getSupportedTranslationMethod();
        String getServiceCloudConfigRootPath();
    }

    boolean isDirectionSupported(String sourceLanguage, String targetLanguage) throws TranslationException;
    Map<String, String> supportedLanguages() throws TranslationException;
    TranslationResult translateString(String sourceString, String sourceLanguage, String targetLanguage, TranslationConstants.ContentType contentType, String category) throws TranslationException;
    TranslationResult[] translateArray(String[] sourceArray, String sourceLanguage, String targetLanguage, TranslationConstants.ContentType contentType, String category) throws TranslationException;
    String detectLanguage(String sourceString, TranslationConstants.ContentType contentType) throws TranslationException;
    
    String createTranslationJob(String name, String description, String sourceLanguage, String targetLanguage, Date dueDate, TranslationState state, TranslationMetadata metadata) throws TranslationException;
    String uploadTranslationObject(String jobId, TranslationObject translationObject) throws TranslationException;
    TranslationConstants.TranslationStatus getTranslationJobStatus(String jobId) throws TranslationException;
    TranslationConstants.TranslationStatus updateTranslationJobState(String jobId, TranslationState state) throws TranslationException;
    InputStream getTranslatedObject(String jobId, TranslationObject translationObject) throws TranslationException;
    
    // Additional methods commonly present in AEM SDK or TranslateGemma implementation
    void updateDueDate(String jobId, Date date) throws TranslationException;
    void updateTranslationJobMetadata(String jobId, TranslationMetadata jobMetadata, TranslationConstants.TranslationMethod translationMethod) throws TranslationException;
    TranslationConstants.TranslationStatus updateTranslationObjectState(String jobId, TranslationObject translationObject, TranslationState state) throws TranslationException;
    TranslationConstants.TranslationStatus[] updateTranslationObjectsState(String jobId, TranslationObject[] translationObjects, TranslationState[] states) throws TranslationException;
    TranslationConstants.TranslationStatus getTranslationObjectStatus(String jobId, TranslationObject translationObject) throws TranslationException;
    TranslationConstants.TranslationStatus[] getTranslationObjectsStatus(String jobId, TranslationObject[] translationObjects) throws TranslationException;
    TranslationScope getFinalScope(String jobId) throws TranslationException;
    void addTranslationJobComment(String jobId, Comment comment) throws TranslationException;
    void addTranslationObjectComment(String jobId, TranslationObject translationObject, Comment comment) throws TranslationException;
    CommentCollection<Comment> getTranslationJobCommentCollection(String jobId) throws TranslationException;
    CommentCollection<Comment> getTranslationObjectCommentCollection(String jobId, TranslationObject translationObject) throws TranslationException;
    String getDefaultCategory();
    void setDefaultCategory(String defaultCategory);
    TranslationResult[] getAllStoredTranslations(String sourceString, String sourceLanguage, String targetLanguage, TranslationConstants.ContentType contentType, String contentCategory, String userId, int maxTranslations) throws TranslationException;
    void storeTranslation(String[] originalText, String sourceLanguage, String targetLanguage, String[] updatedTranslation, TranslationConstants.ContentType contentType, String contentCategory, String userId, int rating, String path) throws TranslationException;
    void storeTranslation(String originalText, String sourceLanguage, String targetLanguage, String updatedTranslation, TranslationConstants.ContentType contentType, String contentCategory, String userId, int rating, String path) throws TranslationException;
    TranslationService.TranslationServiceInfo getTranslationServiceInfo();
}
