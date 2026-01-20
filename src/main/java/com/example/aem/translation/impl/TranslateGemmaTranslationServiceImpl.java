package com.example.aem.translation.impl;

import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.adobe.granite.translation.api.*;
import com.adobe.granite.translation.api.TranslationConstants.ContentType;
import com.adobe.granite.translation.api.TranslationConstants.TranslationStatus;
import com.adobe.granite.translation.api.TranslationConstants.TranslationMethod;
import com.adobe.granite.comments.Comment;
import com.adobe.granite.comments.CommentCollection;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of TranslateGemma Translation Service using Google Cloud Vertex AI.
 */
@Component(
    service = TranslateGemmaTranslationService.class,
    immediate = true,
    property = {
        "service.ranking:Integer=100"
    }
)
@Designate(ocd = TranslateGemmaConfig.class)
public class TranslateGemmaTranslationServiceImpl implements TranslateGemmaTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(TranslateGemmaTranslationServiceImpl.class);
    private static final String TRANSLATEGEMMA_MODEL = "google/translategemma-2b-it";
    private static final String SERVICE_NAME = "TranslateGemma Translation Service";
    private static final String SERVICE_LABEL = "Google TranslateGemma";
    private static final String ATTRIBUTION = "Powered by Google TranslateGemma";

    private volatile VertexAI vertexAI;
    private volatile GenerativeModel model;
    private TranslateGemmaConfig config;
    private final Map<String, String> supportedLanguages = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Activate
    @Modified
    protected void activate(TranslateGemmaConfig config) {
        this.config = config;
        initializeVertexAI();
        initializeSupportedLanguages();
        LOG.info("TranslateGemma Translation Service activated with project: {}, location: {}", 
                config.projectId(), config.location());
    }

    @Deactivate
    protected void deactivate() {
        this.vertexAI = null;
        this.model = null;
        LOG.info("TranslateGemma Translation Service deactivated");
    }

    private void initializeVertexAI() {
        try {
            this.vertexAI = new VertexAI(config.projectId(), config.location());
            this.model = new GenerativeModel(TRANSLATEGEMMA_MODEL, vertexAI);
            LOG.info("Vertex AI client initialized successfully");
        } catch (Exception e) {
            LOG.error("Failed to initialize Vertex AI client", e);
            throw new RuntimeException("Failed to initialize TranslateGemma service", e);
        }
    }

    private void initializeSupportedLanguages() {
        // Common supported languages for TranslateGemma
        supportedLanguages.put("en", "English");
        supportedLanguages.put("es", "Spanish");
        supportedLanguages.put("fr", "French");
        supportedLanguages.put("de", "German");
        supportedLanguages.put("it", "Italian");
        supportedLanguages.put("pt", "Portuguese");
        supportedLanguages.put("ru", "Russian");
        supportedLanguages.put("ja", "Japanese");
        supportedLanguages.put("ko", "Korean");
        supportedLanguages.put("zh", "Chinese");
        supportedLanguages.put("ar", "Arabic");
        supportedLanguages.put("hi", "Hindi");
        supportedLanguages.put("nl", "Dutch");
        supportedLanguages.put("sv", "Swedish");
        supportedLanguages.put("da", "Danish");
        supportedLanguages.put("no", "Norwegian");
        supportedLanguages.put("fi", "Finnish");
        supportedLanguages.put("pl", "Polish");
        supportedLanguages.put("tr", "Turkish");
    }

    @Override
    public TranslationServiceInfo getTranslationServiceInfo() {
        return new TranslationServiceInfo() {
            @Override
            public String getTranslationServiceName() {
                return SERVICE_NAME;
            }

            @Override
            public String getTranslationServiceLabel() {
                return SERVICE_LABEL;
            }

            @Override
            public String getTranslationServiceAttribution() {
                return ATTRIBUTION;
            }

            @Override
            public TranslationConstants.TranslationMethod getSupportedTranslationMethod() {
                return TranslationConstants.TranslationMethod.MACHINE_TRANSLATION;
            }

            @Override
            public String getServiceCloudConfigRootPath() {
                return "/conf/global/settings/cloudconfigs/translate-gemma";
            }
        };
    }

    @Override
    public Map<String, String> supportedLanguages() {
        return new HashMap<>(supportedLanguages);
    }

    public boolean isServiceAvailable() {
        return vertexAI != null && model != null;
    }

    @Override
    public boolean isDirectionSupported(String sourceLanguage, String targetLanguage) throws TranslationException {
        if (!isServiceAvailable()) {
            throw new TranslationException("TranslateGemma service is not available", TranslationException.ErrorCode.NO_ENGINE);
        }
        
        // TranslateGemma supports most major language pairs
        boolean sourceSupported = supportedLanguages.containsKey(sourceLanguage.toLowerCase());
        boolean targetSupported = supportedLanguages.containsKey(targetLanguage.toLowerCase());
        
        return sourceSupported && targetSupported;
    }

    @Override
    public String detectLanguage(String detectSource, ContentType contentType) throws TranslationException {
        if (!isServiceAvailable()) {
            throw new TranslationException("TranslateGemma service is not available", TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
        }

        try {
            // Use this model to detect language
            String prompt = String.format("Detect the language of this text and respond with only the ISO 639-1 language code: %s", detectSource);

            GenerateContentResponse response = model.generateContent(prompt);
            String detectedLang = ResponseHandler.getText(response).trim().toLowerCase();

            return detectedLang.length() == 2 ? detectedLang : "UNKNOWN";
        } catch (Exception e) {
            LOG.error("Error detecting language", e);
            throw new TranslationException("Failed to detect language: " + e.getMessage(), TranslationException.ErrorCode.UNKNOWN_LANGUAGE);
        }
    }

    @Override
    public TranslationResult translateString(String sourceString, String sourceLanguage,
                                           String targetLanguage, ContentType contentType,
                                           String contentCategory) throws TranslationException {
        if (!isServiceAvailable()) {
            throw new TranslationException("TranslateGemma service is not available", TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
        }

        try {
            // Auto-detect source language if not provided
            final String detectedSourceLang;
            if (sourceLanguage == null || sourceLanguage.trim().isEmpty()) {
                detectedSourceLang = detectLanguage(sourceString, contentType);
            } else {
                detectedSourceLang = sourceLanguage;
            }

            // Validate language pair
            if (!isDirectionSupported(detectedSourceLang, targetLanguage)) {
                throw new TranslationException("Translation direction not supported: " +
                    detectedSourceLang + " -> " + targetLanguage, TranslationException.ErrorCode.NOT_SUPPORTED_LANG_DIRECTION);
            }

            // Create translation prompt
            String prompt = createTranslationPrompt(sourceString, detectedSourceLang, targetLanguage, contentType);
            
            GenerateContentResponse response = model.generateContent(prompt);
            String translatedText = ResponseHandler.getText(response).trim();

            // Create TranslationResult
            return new TranslationResult() {
                @Override
                public String getSourceLanguage() {
                    return detectedSourceLang;
                }

                @Override
                public String getTargetLanguage() {
                    return targetLanguage;
                }

                @Override
                public String getSourceString() {
                    return sourceString;
                }

                @Override
                public String getTranslation() {
                    return translatedText;
                }

                @Override
                public ContentType getContentType() {
                    return contentType;
                }

                @Override
                public String getCategory() {
                    return contentCategory != null ? contentCategory : "general";
                }

                @Override
                public int getRating() {
                    return 0; // Default rating for machine translation
                }

                @Override
                public String getUserId() {
                    return null;
                }
            };
        } catch (Exception e) {
            LOG.error("Error translating string", e);
            throw new TranslationException("Translation failed: " + e.getMessage(), TranslationException.ErrorCode.TRANSLATION_FAILED);
        }
    }

    @Override
    public TranslationResult[] translateArray(String[] sourceStringArr, String sourceLanguage,
                                            String targetLanguage, ContentType contentType,
                                            String contentCategory) throws TranslationException {
        if (sourceStringArr == null || sourceStringArr.length == 0) {
            return new TranslationResult[0];
        }

        TranslationResult[] results = new TranslationResult[sourceStringArr.length];
        
        for (int i = 0; i < sourceStringArr.length; i++) {
            results[i] = translateString(sourceStringArr[i], sourceLanguage, targetLanguage, contentType, contentCategory);
        }

        return results;
    }

    private String createTranslationPrompt(String text, String sourceLang, String targetLang, ContentType contentType) {
        String contentTypeDesc = contentType == ContentType.HTML ? "HTML content" : "plain text";
        String sourceLangName = supportedLanguages.getOrDefault(sourceLang.toLowerCase(), sourceLang);
        String targetLangName = supportedLanguages.getOrDefault(targetLang.toLowerCase(), targetLang);

        return String.format(
            "Translate the following %s from %s to %s. " +
            "Preserve the original formatting and structure. " +
            "Respond with only the translated text, no explanations.\n\n%s",
            contentTypeDesc, sourceLangName, targetLangName, text
        );
    }

    // Asynchronous translation methods implementation

    @Override
    public String createTranslationJob(String name, String description, String strSourceLanguage,
                                     String strTargetLanguage, Date dueDate, TranslationState state,
                                     TranslationMetadata jobMetadata) throws TranslationException {
        String jobId = UUID.randomUUID().toString();
        LOG.info("Created translation job: {} for {} -> {}", jobId, strSourceLanguage, strTargetLanguage);
        
        // For synchronous TranslateGemma, we simulate job creation
        // In a real implementation, you might store job metadata in a database
        return jobId;
    }

    @Override
    public String uploadTranslationObject(String strTranslationJobID, TranslationObject translationObject) 
            throws TranslationException {
        String objectId = UUID.randomUUID().toString();
        LOG.info("Uploaded translation object: {} for job: {}", objectId, strTranslationJobID);
        
        // Simulate upload - in reality, you'd store the object
        return objectId;
    }

    @Override
    public TranslationStatus getTranslationJobStatus(String strTranslationJobID) throws TranslationException {
        // For synchronous TranslateGemma, we return complete status
        return TranslationStatus.COMPLETE;
    }

    @Override
    public TranslationStatus updateTranslationJobState(String strTranslationJobID, TranslationState state)
            throws TranslationException {
        LOG.info("Updated job {} state to: {}", strTranslationJobID, state);
        return TranslationStatus.COMPLETE;
    }

    @Override
    public InputStream getTranslatedObject(String strTranslationJobID, TranslationObject translationObject) 
            throws TranslationException {
        try {
            // For demonstration, translate the object content
            String originalContent = getContentFromTranslationObject(translationObject);
            TranslationResult result = translateString(
                originalContent, 
                "en", // Assume English source for demo
                "es", // Target language
                ContentType.PLAIN, 
                "general"
            );
            
            return new ByteArrayInputStream(result.getTranslation().getBytes());
        } catch (Exception e) {
            throw new TranslationException("Failed to get translated object: " + e.getMessage(), TranslationException.ErrorCode.TRANSLATION_FAILED);
        }
    }

    private String getContentFromTranslationObject(TranslationObject translationObject) {
        // Extract content from TranslationObject - this is a simplified implementation
        // In reality, you'd need to properly extract the content based on the object type
        return "Sample content for translation";
    }

    @Override
    public void updateDueDate(String strTranslationJobID, java.util.Date date) throws TranslationException {
        LOG.debug("Updating due date for job {} to {}", strTranslationJobID, date);
    }

    @Override
    public void updateTranslationJobMetadata(String strTranslationJobID, TranslationMetadata jobMetadata,
            TranslationConstants.TranslationMethod translationMethod) throws TranslationException {
        LOG.debug("Updating metadata for job {}", strTranslationJobID);
    }

    @Override
    public TranslationConstants.TranslationStatus updateTranslationObjectState(String strTranslationJobID,
            TranslationObject translationObject, TranslationState state) throws TranslationException {
        return TranslationConstants.TranslationStatus.DRAFT;
    }

    @Override
    public TranslationConstants.TranslationStatus[] updateTranslationObjectsState(String strTranslationJobID,
            TranslationObject[] translationObjects, TranslationState[] states) throws TranslationException {
        TranslationConstants.TranslationStatus[] result = new TranslationConstants.TranslationStatus[translationObjects.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = TranslationConstants.TranslationStatus.DRAFT;
        }
        return result;
    }

    @Override
    public TranslationConstants.TranslationStatus getTranslationObjectStatus(String strTranslationJobID,
            TranslationObject translationObject) throws TranslationException {
        return TranslationConstants.TranslationStatus.DRAFT;
    }

    @Override
    public TranslationConstants.TranslationStatus[] getTranslationObjectsStatus(String strTranslationJobID,
            TranslationObject[] translationObjects) throws TranslationException {
        TranslationConstants.TranslationStatus[] result = new TranslationConstants.TranslationStatus[translationObjects.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = TranslationConstants.TranslationStatus.DRAFT;
        }
        return result;
    }

    @Override
    public TranslationScope getFinalScope(String strTranslationJobID) throws TranslationException {
        return null;
    }

    @Override
    public void addTranslationJobComment(String strTranslationJobID, Comment comment) throws TranslationException {
        LOG.debug("Adding comment to job {}", strTranslationJobID);
    }

    @Override
    public void addTranslationObjectComment(String strTranslationJobID, TranslationObject translationObject,
            Comment comment) throws TranslationException {
        LOG.debug("Adding comment to object in job {}", strTranslationJobID);
    }

    @Override
    public CommentCollection<Comment> getTranslationJobCommentCollection(String strTranslationJobID)
            throws TranslationException {
        return null;
    }

    @Override
    public CommentCollection<Comment> getTranslationObjectCommentCollection(String strTranslationJobID,
            TranslationObject translationObject) throws TranslationException {
        return null;
    }

    @Override
    public String getDefaultCategory() {
        return "general";
    }

    @Override
    public void setDefaultCategory(String defaultCategory) {
        LOG.debug("Setting default category to {}", defaultCategory);
    }

    @Override
    public TranslationResult[] getAllStoredTranslations(String sourceString, String sourceLanguage,
            String targetLanguage, TranslationConstants.ContentType contentType, String contentCategory,
            String userId, int maxTranslations) throws TranslationException {
        return new TranslationResult[0];
    }

    @Override
    public void storeTranslation(String[] originalText, String sourceLanguage, String targetLanguage,
            String[] updatedTranslation, TranslationConstants.ContentType contentType, String contentCategory,
            String userId, int rating, String path) throws TranslationException {
        LOG.debug("Storing multiple translations");
    }

    @Override
    public void storeTranslation(String originalText, String sourceLanguage, String targetLanguage,
            String updatedTranslation, TranslationConstants.ContentType contentType, String contentCategory,
            String userId, int rating, String path) throws TranslationException {
        LOG.debug("Storing translation: {} -> {}", sourceLanguage, targetLanguage);
    }
}