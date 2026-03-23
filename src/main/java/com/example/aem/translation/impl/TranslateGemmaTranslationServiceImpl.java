package com.example.aem.translation.impl;

import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.example.aem.translation.model.SentimentResult;
import com.example.aem.translation.model.ComplianceResult;
import com.example.aem.translation.model.AssetAnalysisResult;
import com.example.aem.translation.terminology.TerminologyService;
import com.example.aem.translation.terminology.TerminologyMatch;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.example.aem.translation.util.TranslationMetrics;
import com.example.aem.translation.util.TranslationCache;
import com.example.aem.translation.util.ResilienceHelper;
import com.example.aem.translation.util.InputSanitizer;
import com.example.aem.translation.exception.TranslateGemmaException;
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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private final Map<String, TranslationJob> translationJobs = new ConcurrentHashMap<>();
    private final Map<String, TranslationObject> translationObjects = new ConcurrentHashMap<>();
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(3);

    private TranslationMetrics metrics;
    private TranslationCache cache;
    private ResilienceHelper resilienceHelper;
    private boolean metricsEnabled;
    private boolean cachingEnabled;

    @Reference
    private TerminologyService terminologyService;


    private static class TranslationJob {
        private final String jobId;
        private final String sourceLanguage;
        private final String targetLanguage;
        private volatile TranslationStatus status;
        private final List<String> objectIds = new ArrayList<>();
        private final Map<String, String> translatedContent = new ConcurrentHashMap<>();


        TranslationJob(String jobId, String sourceLanguage, String targetLanguage) {
            this.jobId = jobId;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.status = TranslationStatus.DRAFT;
        }

        public String getJobId() {
            return jobId;
        }

        public String getSourceLanguage() {
            return sourceLanguage;
        }

        public String getTargetLanguage() {
            return targetLanguage;
        }

        public TranslationStatus getStatus() {
            return status;
        }

        public void setStatus(TranslationStatus status) {
            this.status = status;
        }

        public List<String> getObjectIds() {
            return objectIds;
        }

        public void addTranslatedContent(String objectId, String content) {
            translatedContent.put(objectId, content);
        }

        public String getTranslatedContent(String objectId) {
            return translatedContent.get(objectId);
        }
    }

    @Activate
    @Modified
    public void activate(TranslateGemmaConfig config) {
        this.config = config;
        
        this.metricsEnabled = config.enableMetrics();
        this.cachingEnabled = config.enableCaching();
        
        if (metricsEnabled) {
            MeterRegistry meterRegistry = new SimpleMeterRegistry();
            this.metrics = new TranslationMetrics(meterRegistry);
            LOG.info("Metrics enabled for TranslateGemma service");
        }
        
        if (cachingEnabled) {
            this.cache = new TranslationCache(config.cacheMaxSize(), config.cacheExpireAfterMinutes());
            LOG.info("Caching enabled with maxSize={}, expireAfterMinutes={}", 
                    config.cacheMaxSize(), config.cacheExpireAfterMinutes());
        }
        
        this.resilienceHelper = new ResilienceHelper.Builder()
                .maxRetries(config.retryMaxAttempts())
                .waitDurationMillis(config.retryWaitDurationMs())
                .failureRateThreshold(config.circuitBreakerFailureRateThreshold())
                .slowCallRateThreshold(config.circuitBreakerSlowCallRateThreshold())
                .slowCallDurationMillis(config.circuitBreakerSlowCallDurationMs())
                .circuitBreakerWaitDurationSeconds(config.circuitBreakerWaitDurationS())
                .build();
        
        initializeVertexAI();
        initializeSupportedLanguages();
        LOG.info("TranslateGemma Translation Service activated with project: {}, location: {}", 
                config.projectId(), config.location());
    }

    @Deactivate
    protected void deactivate() {
        this.executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (cache != null) {
            cache.invalidateAll();
        }
        
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

    // Protected method for testing to inject a mock GenerativeModel
    public void setModel(GenerativeModel mockModel) {
        this.model = mockModel;
    }

    // Protected method for testing to inject a mock TerminologyService
    public void setTerminologyService(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
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

    @Override
    public SentimentResult analyzeSentiment(String content) throws TranslateGemmaException {
        if (!isServiceAvailable()) {
            throw TranslateGemmaException.serviceUnavailable();
        }

        if (!InputSanitizer.isValidInput(content)) {
            throw new TranslateGemmaException("Invalid input for sentiment analysis");
        }

        String sanitizedInput = InputSanitizer.sanitizeForPrompt(content);
        String cacheKey = "sentiment:" + sanitizedInput;

        if (cachingEnabled && cache != null) {
            String cachedJson = cache.getTranslation(sanitizedInput, "sentiment", "json", key -> null);
            if (cachedJson != null) {
                try {
                    return objectMapper.readValue(cachedJson, SentimentResult.class);
                } catch (IOException e) {
                    LOG.warn("Failed to parse cached sentiment JSON", e);
                }
            }
        }

        try {
            String jsonResponse = resilienceHelper.executeWithRetryAndCircuitBreaker(
                () -> {
                    String prompt = String.format(
                        "Analyze the sentiment of the following text. Provide a score between -1.0 and 1.0 (where -1.0 is strongly negative and 1.0 is strongly positive), a one-word label (POSITIVE, NEGATIVE, NEUTRAL), and a brief one-sentence explanation. Respond ONLY in JSON format: {\"score\": 0.0, \"label\": \"NEUTRAL\", \"explanation\": \"...\"}\n\nText: %s",
                        sanitizedInput
                    );
                    return executeGemmaPrompt(prompt);
                },
                "analyzeSentiment",
                "translationCircuitBreaker"
            );

            // Strip markdown code blocks if present in LLM response
            String processedJsonResponse = jsonResponse;
            if (processedJsonResponse.startsWith("```json")) {
                processedJsonResponse = processedJsonResponse.substring(7, processedJsonResponse.length() - 3).trim();
            } else if (processedJsonResponse.startsWith("```")) {
                processedJsonResponse = processedJsonResponse.substring(3, processedJsonResponse.length() - 3).trim();
            }

            SentimentResult result = objectMapper.readValue(processedJsonResponse, SentimentResult.class);
            
            if (cachingEnabled && cache != null) {
                final String finalJsonResponse = processedJsonResponse;
                cache.getTranslation(sanitizedInput, "sentiment", "json", key -> finalJsonResponse);
            }

            return result;
        } catch (Exception e) {
            LOG.error("Error analyzing sentiment", e);
            throw TranslateGemmaException.translationFailed(e.getMessage());
        }
    }

    @Override
    public ComplianceResult analyzeCompliance(String content) throws TranslateGemmaException {
        if (!isServiceAvailable()) {
            throw TranslateGemmaException.serviceUnavailable();
        }

        if (!InputSanitizer.isValidInput(content)) {
            throw new TranslateGemmaException("Invalid input for compliance analysis");
        }

        try {
            // 1. Scan for prohibited terms using TerminologyService
            List<TerminologyMatch> matches = terminologyService.findAllTerms(content, "en", "en", "brand", 100);
            
            if (matches.isEmpty()) {
                return new ComplianceResult(content, Collections.emptyList());
            }

            List<ComplianceResult.ComplianceViolation> violations = new ArrayList<>();

            // 2. For each match, use Gemma to get a contextual suggestion
            for (TerminologyMatch match : matches) {
                String prompt = String.format(
                    "The text contains the following prohibited term: '%s'. " +
                    "Based on the surrounding context: '%s', suggest a more brand-aligned alternative. " +
                    "Respond ONLY in JSON format: {\"suggestion\": \"...\", \"reason\": \"...\", \"severity\": \"HIGH\"}",
                    match.getSourceTerm(), content
                );

                String jsonResponse = resilienceHelper.executeWithRetryAndCircuitBreaker(
                    () -> executeGemmaPrompt(prompt),
                    "analyzeCompliance",
                    "translationCircuitBreaker"
                );

                // Clean JSON
                if (jsonResponse.startsWith("```json")) {
                    jsonResponse = jsonResponse.substring(7, jsonResponse.length() - 3).trim();
                } else if (jsonResponse.startsWith("```")) {
                    jsonResponse = jsonResponse.substring(3, jsonResponse.length() - 3).trim();
                }

                // Parse suggestion
                Map<String, Object> suggestionMap = objectMapper.readValue(jsonResponse, Map.class);
                String suggestion = (String) suggestionMap.getOrDefault("suggestion", match.getTargetTerm());
                String reason = (String) suggestionMap.getOrDefault("reason", "Prohibited terminology detected.");
                String severityStr = (String) suggestionMap.getOrDefault("severity", "MEDIUM");
                
                ComplianceResult.ComplianceViolation.Severity severity = 
                        ComplianceResult.ComplianceViolation.Severity.valueOf(severityStr.toUpperCase());

                violations.add(new ComplianceResult.ComplianceViolation(
                    match.getSourceTerm(), suggestion, reason, severity
                ));
            }

            return new ComplianceResult(content, violations);

        } catch (Exception e) {
            LOG.error("Error analyzing brand compliance", e);
            throw TranslateGemmaException.translationFailed(e.getMessage());
        }
    }

    @Override
    public AssetAnalysisResult analyzeAsset(String assetPath, ResourceResolver resolver) throws TranslateGemmaException {
        if (!isServiceAvailable()) {
            throw TranslateGemmaException.serviceUnavailable();
        }

        Resource assetResource = resolver.getResource(assetPath);
        if (assetResource == null) {
            throw new TranslateGemmaException("Asset not found at path: " + assetPath);
        }

        try {
            Resource metadataResource = assetResource.getChild("jcr:content/metadata");
            ValueMap metadata = (metadataResource != null) ? metadataResource.getValueMap() : ValueMap.EMPTY;
            
            String title = metadata.get("dc:title", String.class);
            String description = metadata.get("dc:description", String.class);

            String prompt = String.format(
                "Given the following asset metadata (Title: '%s', Description: '%s'), " +
                "generate a concise, accessible Alt-Text for a screen reader and a list of 10 relevant SEO keywords. " +
                "Respond ONLY in JSON format: {\"altText\": \"...\", \"keywords\": [\"...\"], \"suggestedTitle\": \"...\"}",
                title != null ? title : "Unknown", 
                description != null ? description : "No description available"
            );

            String jsonResponse = resilienceHelper.executeWithRetryAndCircuitBreaker(
                () -> executeGemmaPrompt(prompt),
                "analyzeAsset",
                "translationCircuitBreaker"
            );

            // Clean JSON
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7, jsonResponse.length() - 3).trim();
            } else if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse.substring(3, jsonResponse.length() - 3).trim();
            }

            return objectMapper.readValue(jsonResponse, AssetAnalysisResult.class);

        } catch (Exception e) {
            LOG.error("Error analyzing asset metadata", e);
            throw TranslateGemmaException.translationFailed(e.getMessage());
        }
    }

    public boolean isServiceAvailable() {
        return vertexAI != null && model != null;
    }

    /**
     * Executes a prompt against the Gemma model.
     * @param prompt The prompt to execute.
     * @return The text response from the model.
     */
    public String executeGemmaPrompt(String prompt) {
        try {
            GenerateContentResponse response = model.generateContent(prompt);
            return ResponseHandler.getText(response).trim();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to execute Gemma prompt", e);
        }
    }

    public boolean isCircuitBreakerOpen() {
        if (resilienceHelper != null) {
            return resilienceHelper.isCircuitBreakerOpen("translationCircuitBreaker");
        }
        return false;
    }

    public String getHealthStatus() {
        StringBuilder status = new StringBuilder();
        status.append("TranslateGemma Service Status:\n");
        status.append("- Service Available: ").append(isServiceAvailable()).append("\n");
        status.append("- Circuit Breaker Open: ").append(isCircuitBreakerOpen()).append("\n");
        status.append("- Caching Enabled: ").append(cachingEnabled).append("\n");
        status.append("- Metrics Enabled: ").append(metricsEnabled).append("\n");
        
        if (cache != null) {
            TranslationCache.CacheStats stats = cache.getStats();
            status.append("- Cache Hit Rate: ").append(String.format("%.2f%%", stats.getTranslationHitRate() * 100)).append("\n");
        }
        
        return status.toString();
    }

    public TranslationCache.CacheStats getCacheStats() {
        return cache != null ? cache.getStats() : null;
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

        if (!InputSanitizer.isValidInput(detectSource)) {
            throw new TranslationException("Invalid input for language detection", TranslationException.ErrorCode.UNKNOWN);
        }

        String sanitizedInput = InputSanitizer.sanitizeForPrompt(detectSource);

        if (cachingEnabled && cache != null) {
            String cachedResult = cache.getDetectedLanguage(sanitizedInput, key -> null);
            if (cachedResult != null && metrics != null) {
                metrics.recordCacheHit();
                return cachedResult;
            }
        }

        long startTime = System.currentTimeMillis();
        
        try {
            String detectedLang = resilienceHelper.executeWithRetryAndCircuitBreaker(
                () -> {
                    try {
                        String prompt = String.format("Detect the language of this text and respond with only the ISO 639-1 language code: %s", sanitizedInput);
                        GenerateContentResponse response = model.generateContent(prompt);
                        return ResponseHandler.getText(response).trim().toLowerCase();
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                "detectLanguage",
                "translationCircuitBreaker"
            );

            String result = detectedLang.length() == 2 ? detectedLang : "UNKNOWN";

            if (cachingEnabled && cache != null) {
                cache.getDetectedLanguage(sanitizedInput, key -> result);
            }

            if (metrics != null) {
                metrics.recordTranslationSuccess();
                metrics.recordLatency(System.currentTimeMillis() - startTime);
            }

            return result;
        } catch (Exception e) {
            LOG.error("Error detecting language", e);
            if (metrics != null) {
                metrics.recordTranslationFailure();
            }
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

        if (!InputSanitizer.isValidInput(sourceString)) {
            throw new TranslationException("Invalid input for translation", TranslationException.ErrorCode.UNKNOWN);
        }

        String sanitizedText = InputSanitizer.sanitizeForPrompt(sourceString);
        String sanitizedSourceLang = InputSanitizer.sanitizeLanguageCode(sourceLanguage);
        String sanitizedTargetLang = InputSanitizer.sanitizeLanguageCode(targetLanguage);
        String sanitizedCategory = InputSanitizer.sanitizeContentCategory(contentCategory);

        if (sanitizedSourceLang == null || sanitizedTargetLang == null) {
            throw new TranslationException("Invalid language codes provided", TranslationException.ErrorCode.UNKNOWN);
        }

        if (!InputSanitizer.isValidLanguagePair(sanitizedSourceLang, sanitizedTargetLang)) {
            throw new TranslationException("Invalid language pair: source and target must be different", 
                    TranslationException.ErrorCode.NOT_SUPPORTED_LANG_DIRECTION);
        }

        if (metrics != null) {
            metrics.recordTranslationRequest();
        }

        if (cachingEnabled && cache != null) {
            String cachedTranslation = cache.getTranslation(sanitizedText, sanitizedSourceLang, 
                    sanitizedTargetLang, key -> null);
            if (cachedTranslation != null) {
                LOG.debug("Returning cached translation");
                if (metrics != null) {
                    metrics.recordCacheHit();
                    metrics.recordTranslationSuccess();
                }
                return createTranslationResult(cachedTranslation, sanitizedSourceLang, sanitizedTargetLang, 
                        sanitizedText, contentType, sanitizedCategory);
            }
        }

        final String finalSourceLang;
        if (sanitizedSourceLang == null || sanitizedSourceLang.trim().isEmpty()) {
            finalSourceLang = detectLanguage(sanitizedText, contentType);
        } else {
            finalSourceLang = sanitizedSourceLang;
        }

        if (!isDirectionSupported(finalSourceLang, sanitizedTargetLang)) {
            throw new TranslationException("Translation direction not supported: " +
                finalSourceLang + " -> " + sanitizedTargetLang, TranslationException.ErrorCode.NOT_SUPPORTED_LANG_DIRECTION);
        }

        long startTime = System.currentTimeMillis();

        try {
            String translatedText = resilienceHelper.executeWithRetryAndCircuitBreaker(
                () -> {
                    try {
                        String prompt = createTranslationPrompt(sanitizedText, finalSourceLang, sanitizedTargetLang, contentType);
                        GenerateContentResponse response = model.generateContent(prompt);
                        return ResponseHandler.getText(response).trim();
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                "translateString",
                "translationCircuitBreaker"
            );

            if (cachingEnabled && cache != null && translatedText != null) {
                cache.getTranslation(sanitizedText, finalSourceLang, sanitizedTargetLang, key -> translatedText);
            }

            if (metrics != null) {
                metrics.recordTranslationSuccess();
                metrics.recordLatency(System.currentTimeMillis() - startTime);
            }

            return createTranslationResult(translatedText, finalSourceLang, sanitizedTargetLang, 
                    sanitizedText, contentType, sanitizedCategory);

        } catch (Exception e) {
            LOG.error("Error translating string", e);
            if (metrics != null) {
                metrics.recordTranslationFailure();
            }
            throw new TranslationException("Translation failed: " + e.getMessage(), TranslationException.ErrorCode.TRANSLATION_FAILED);
        }
    }

    private TranslationResult createTranslationResult(String translatedText, String sourceLang, 
            String targetLang, String sourceString, ContentType contentType, String contentCategory) {
        return new TranslationResult() {
            @Override
            public String getSourceLanguage() {
                return sourceLang;
            }

            @Override
            public String getTargetLanguage() {
                return targetLang;
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
                return 0;
            }

            @Override
            public String getUserId() {
                return null;
            }
        };
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
        TranslationJob job = new TranslationJob(jobId, strSourceLanguage, strTargetLanguage);
        translationJobs.put(jobId, job);
        LOG.info("Created translation job: {} for {} -> {}", jobId, strSourceLanguage, strTargetLanguage);
        return jobId;
    }

    @Override
    public String uploadTranslationObject(String strTranslationJobID, TranslationObject translationObject)
            throws TranslationException {
        TranslationJob job = translationJobs.get(strTranslationJobID);
        if (job == null) {
            throw new TranslationException("Translation job not found: " + strTranslationJobID, TranslationException.ErrorCode.JOB_NOT_FOUND);
        }

        String objectId = translationObject.getPath();
        if (objectId == null) {
            objectId = UUID.randomUUID().toString();
        }

        job.getObjectIds().add(objectId);
        translationObjects.put(objectId, translationObject);
        LOG.info("Uploaded translation object: {} for job: {}", objectId, strTranslationJobID);

        final TranslationJob finalJob = job;
        final TranslationObject finalTranslationObject = translationObject;
        final String finalObjectId = objectId;
        final String finalStrTranslationJobID = strTranslationJobID;

        // Asynchronously translate the object
        executor.submit(() -> {
            try {
                finalJob.setStatus(TranslationStatus.TRANSLATION_IN_PROGRESS);
                String content = getContentFromTranslationObject(finalTranslationObject);
                TranslationResult result = translateString(content, finalJob.getSourceLanguage(), finalJob.getTargetLanguage(), finalTranslationObject.getContentType(), "general");
                finalJob.addTranslatedContent(finalObjectId, result.getTranslation());
                finalJob.setStatus(TranslationStatus.TRANSLATED);
                LOG.info("Translation successful for object {} in job {}", finalObjectId, finalStrTranslationJobID);

            } catch (TranslationException e) {
                finalJob.setStatus(TranslationStatus.ERROR_UPDATE);
                LOG.error("Error translating object {} in job {}", finalObjectId, finalStrTranslationJobID, e);
            }
        });

        return objectId;
    }

    @Override
    public TranslationStatus getTranslationJobStatus(String strTranslationJobID) throws TranslationException {
        TranslationJob job = translationJobs.get(strTranslationJobID);
        if (job == null) {
            return TranslationStatus.UNKNOWN_STATE;
        }
        return job.getStatus();
    }

    @Override
    public TranslationStatus updateTranslationJobState(String strTranslationJobID, TranslationState state)
            throws TranslationException {
        TranslationJob job = translationJobs.get(strTranslationJobID);
        if (job == null) {
            throw new TranslationException("Translation job not found: " + strTranslationJobID, TranslationException.ErrorCode.JOB_NOT_FOUND);
        }
        LOG.info("Updated job {} state to: {}", strTranslationJobID, state);
        switch (state.getStatus()) {
            case COMMITTED_FOR_TRANSLATION:
                job.setStatus(TranslationStatus.TRANSLATION_IN_PROGRESS);
                break;
            case CANCEL:
                job.setStatus(TranslationStatus.CANCEL);
                break;
            default:
                // do nothing
        }
        return job.getStatus();
    }

    @Override
    public InputStream getTranslatedObject(String strTranslationJobID, TranslationObject translationObject) 
            throws TranslationException {
        TranslationJob job = translationJobs.get(strTranslationJobID);
        if (job == null) {
            throw new TranslationException("Translation job not found: " + strTranslationJobID, TranslationException.ErrorCode.JOB_NOT_FOUND);
        }

        String objectId = translationObject.getPath();
        String translatedContent = job.getTranslatedContent(objectId);

        if (translatedContent == null) {
             // If content not ready, return original content
            LOG.warn("Translated content not yet available for object {} in job {}. Returning original content.", objectId, strTranslationJobID);
            try {
                // Assuming getContent() returns a String, adjust if it's a byte array or other type
                Object content = translationObject.getContent();
                if (content instanceof String) {
                    return new ByteArrayInputStream(((String) content).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } else if (content instanceof byte[]) {
                    return new ByteArrayInputStream((byte[]) content);
                } else {
                    throw new TranslationException("Unsupported content type for original content", TranslationException.ErrorCode.UNKNOWN);
                }
            } catch (Exception e) {
                 throw new TranslationException("Could not read original content", e);
            }
        }

        return new ByteArrayInputStream(translatedContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String getContentFromTranslationObject(TranslationObject translationObject) throws TranslationException {
        // In a real implementation, you would use translationObject.getDictionary()
        // or parse translationObject.getMimeType() to extract translatable strings.
        // For this example, we'll assume getContent() returns a String.
        Object content = translationObject.getContent();
        if (content instanceof String) {
            return (String) content;
        } else {
            // Handle other types or throw an error if expected content is always String
            throw new TranslationException("Unsupported content type in TranslationObject", TranslationException.ErrorCode.UNKNOWN);
        }
    }

    @Override
    public void updateDueDate(String strTranslationJobID, java.util.Date date) throws TranslationException {
        TranslationJob job = translationJobs.get(strTranslationJobID);
        if (job != null) {
            // In a real implementation, you would update the job's due date.
            LOG.info("Updating due date for job {} to {}", strTranslationJobID, date);
        }
    }

    @Override
    public void updateTranslationJobMetadata(String strTranslationJobID, TranslationMetadata jobMetadata,
            TranslationMethod translationMethod) throws TranslationException {
        TranslationJob job = translationJobs.get(strTranslationJobID);
        if (job != null) {
            // In a real implementation, you would update the job's metadata.
            LOG.info("Updating metadata for job {}", strTranslationJobID);
        }
    }

    @Override
    public TranslationStatus updateTranslationObjectState(String strTranslationJobID,
            TranslationObject translationObject, TranslationState state) throws TranslationException {
        // This is a simplified implementation. A real implementation would track individual object states.
        TranslationJob job = translationJobs.get(strTranslationJobID);
        if (job != null) {
            return job.getStatus();
        }
        return TranslationStatus.UNKNOWN_STATE;
    }

    @Override
    public TranslationStatus[] updateTranslationObjectsState(String strTranslationJobID,
            TranslationObject[] translationObjects, TranslationState[] states) throws TranslationException {
        TranslationStatus[] result = new TranslationStatus[translationObjects.length];
        TranslationJob job = translationJobs.get(strTranslationJobID);
        TranslationStatus status = (job != null) ? job.getStatus() : TranslationStatus.UNKNOWN_STATE;
        for (int i = 0; i < result.length; i++) {
            result[i] = status;
        }
        return result;
    }

    @Override
    public TranslationStatus getTranslationObjectStatus(String strTranslationJobID,
            TranslationObject translationObject) throws TranslationException {
        TranslationJob job = translationJobs.get(strTranslationJobID);
        if (job != null) {
            return job.getStatus();
        }
        return TranslationStatus.UNKNOWN_STATE;
    }

    @Override
    public TranslationStatus[] getTranslationObjectsStatus(String strTranslationJobID,
            TranslationObject[] translationObjects) throws TranslationException {
        TranslationStatus[] result = new TranslationStatus[translationObjects.length];
        TranslationJob job = translationJobs.get(strTranslationJobID);
        TranslationStatus status = (job != null) ? job.getStatus() : TranslationStatus.UNKNOWN_STATE;
        for (int i = 0; i < result.length; i++) {
            result[i] = status;
        }
        return result;
    }

    @Override
    public TranslationScope getFinalScope(String strTranslationJobID) throws TranslationException {
        // A real implementation would calculate the final scope of the translation job.
        return null;
    }

    @Override
    public void addTranslationJobComment(String strTranslationJobID, Comment comment) throws TranslationException {
        // A real implementation would store comments for the job.
        LOG.info("Adding comment to job {}", strTranslationJobID);
    }

    @Override
    public void addTranslationObjectComment(String strTranslationJobID, TranslationObject translationObject,
            Comment comment) throws TranslationException {
        // A real implementation would store comments for the object.
        LOG.info("Adding comment to object in job {}", strTranslationJobID);
    }

    @Override
    public CommentCollection<Comment> getTranslationJobCommentCollection(String strTranslationJobID)
            throws TranslationException {
        // A real implementation would retrieve the comment collection for the job.
        return null;
    }

    @Override
    public CommentCollection<Comment> getTranslationObjectCommentCollection(String strTranslationJobID,
            TranslationObject translationObject) throws TranslationException {
        // A real implementation would retrieve the comment collection for the object.
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
            String targetLanguage, ContentType contentType, String contentCategory,
            String userId, int maxTranslations) throws TranslationException {
        // This is a translation memory feature. A real implementation would query a translation memory.
        return new TranslationResult[0];
    }

    @Override
    public void storeTranslation(String[] originalText, String sourceLanguage, String targetLanguage,
            String[] updatedTranslation, ContentType contentType, String contentCategory,
            String userId, int rating, String path) throws TranslationException {
        // This is a translation memory feature. A real implementation would store the translations.
        LOG.info("Storing multiple translations");
    }

    @Override
    public void storeTranslation(String originalText, String sourceLanguage, String targetLanguage,
            String updatedTranslation, ContentType contentType, String contentCategory,
            String userId, int rating, String path) throws TranslationException {
        // This is a translation memory feature. A real implementation would store the translation.
        LOG.info("Storing translation: {} -> {}", sourceLanguage, targetLanguage);
    }
}
