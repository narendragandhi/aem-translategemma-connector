package com.example.aem.translation.service;

import com.adobe.granite.translation.api.TranslationService;
import com.adobe.granite.translation.api.TranslationResult;
import com.adobe.granite.translation.api.TranslationConstants.ContentType;
import com.adobe.granite.translation.api.TranslationConstants.TranslationStatus;
import com.adobe.granite.translation.api.TranslationConstants.TranslationMethod;
import com.adobe.granite.translation.api.TranslationObject;
import com.adobe.granite.translation.api.TranslationState;
import com.adobe.granite.translation.api.TranslationMetadata;
import com.adobe.granite.translation.api.TranslationException;
import com.adobe.granite.translation.api.TranslationScope;
import com.adobe.granite.comments.Comment;
import com.adobe.granite.comments.CommentCollection;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

public interface TranslateGemmaTranslationService extends TranslationService {

    boolean isServiceAvailable();

    @Override
    boolean isDirectionSupported(String sourceLanguage, String targetLanguage) throws TranslationException;

    @Override
    String detectLanguage(String detectSource, ContentType contentType) throws TranslationException;

    @Override
    TranslationResult translateString(String sourceString, String sourceLanguage, 
                                     String targetLanguage, ContentType contentType, 
                                     String contentCategory) throws TranslationException;

    @Override
    TranslationResult[] translateArray(String[] sourceStringArr, String sourceLanguage,
                                      String targetLanguage, ContentType contentType,
                                      String contentCategory) throws TranslationException;

    @Override
    String createTranslationJob(String name, String description, String strSourceLanguage,
                               String strTargetLanguage, Date dueDate, TranslationState state,
                               TranslationMetadata jobMetadata) throws TranslationException;

    @Override
    String uploadTranslationObject(String strTranslationJobID, TranslationObject translationObject) 
            throws TranslationException;

    @Override
    TranslationStatus getTranslationJobStatus(String strTranslationJobID) throws TranslationException;

    @Override
    TranslationStatus updateTranslationJobState(String strTranslationJobID, TranslationState state) 
            throws TranslationException;

    @Override
    InputStream getTranslatedObject(String strTranslationJobID, TranslationObject translationObject) 
            throws TranslationException;

    @Override
    Map<String, String> supportedLanguages();

    @Override
    TranslationService.TranslationServiceInfo getTranslationServiceInfo();

    @Override
    void updateDueDate(String strTranslationJobID, Date date) throws TranslationException;

    @Override
    void updateTranslationJobMetadata(String strTranslationJobID, TranslationMetadata jobMetadata,
            TranslationMethod translationMethod) throws TranslationException;

    @Override
    TranslationStatus updateTranslationObjectState(String strTranslationJobID,
            TranslationObject translationObject, TranslationState state) throws TranslationException;

    @Override
    TranslationStatus[] updateTranslationObjectsState(String strTranslationJobID,
            TranslationObject[] translationObjects, TranslationState[] states) throws TranslationException;

    @Override
    TranslationStatus getTranslationObjectStatus(String strTranslationJobID,
            TranslationObject translationObject) throws TranslationException;

    @Override
    TranslationStatus[] getTranslationObjectsStatus(String strTranslationJobID,
            TranslationObject[] translationObjects) throws TranslationException;

    @Override
    TranslationScope getFinalScope(String strTranslationJobID) throws TranslationException;

    @Override
    void addTranslationJobComment(String strTranslationJobID, Comment comment) throws TranslationException;

    @Override
    void addTranslationObjectComment(String strTranslationJobID, TranslationObject translationObject,
            Comment comment) throws TranslationException;

    @Override
    CommentCollection<Comment> getTranslationJobCommentCollection(String strTranslationJobID)
            throws TranslationException;

    @Override
    CommentCollection<Comment> getTranslationObjectCommentCollection(String strTranslationJobID,
            TranslationObject translationObject) throws TranslationException;

    @Override
    String getDefaultCategory();

    @Override
    void setDefaultCategory(String defaultCategory);

    @Override
    TranslationResult[] getAllStoredTranslations(String sourceString, String sourceLanguage,
            String targetLanguage, ContentType contentType, String contentCategory,
            String userId, int maxTranslations) throws TranslationException;

    @Override
    void storeTranslation(String[] originalText, String sourceLanguage, String targetLanguage,
            String[] updatedTranslation, ContentType contentType, String contentCategory,
            String userId, int rating, String path) throws TranslationException;

    @Override
    void storeTranslation(String originalText, String sourceLanguage, String targetLanguage,
            String updatedTranslation, ContentType contentType, String contentCategory,
            String userId, int rating, String path) throws TranslationException;
}
