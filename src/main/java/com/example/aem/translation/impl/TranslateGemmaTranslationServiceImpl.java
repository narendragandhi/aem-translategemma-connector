package com.example.aem.translation.impl;

import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.example.aem.translation.model.SentimentResult;
import com.example.aem.translation.model.ComplianceResult;
import com.example.aem.translation.model.AssetAnalysisResult;
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

import com.example.aem.translation.util.GemmaBoundaryService;
import com.example.aem.translation.provider.TranslationProvider;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.jobs.JobManager;
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
    service = {TranslationService.class, TranslateGemmaTranslationService.class},
    immediate = true,
    property = {
        "service.ranking:Integer=100"
    }
)
@Designate(ocd = TranslateGemmaConfig.class)
public class TranslateGemmaTranslationServiceImpl implements TranslateGemmaTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(TranslateGemmaTranslationServiceImpl.class);
    private static final String DEFAULT_MODEL = "gemma-3-27b-it";
    private static final String SERVICE_NAME = "TranslateGemma Translation Service";
    private static final String SERVICE_LABEL = "Google Gemma (Vertex AI)";
    private static final String ATTRIBUTION = "Powered by Google Gemma via Vertex AI";

    private volatile VertexAI vertexAI;
    private volatile GenerativeModel model;
    private TranslateGemmaConfig config;
    private final Map<String, String> supportedLanguages = new ConcurrentHashMap<>();
    private final Map<String, TranslationJob> translationJobs = new ConcurrentHashMap<>();
    private final Map<String, TranslationObject> translationObjects = new ConcurrentHashMap<>();

    private TranslationMetrics metrics;
    private TranslationCache cache;
    private ResilienceHelper resilienceHelper;
    private SimpleMeterRegistry meterRegistry;
    private volatile boolean metricsEnabled;
    private volatile boolean cachingEnabled;
    private volatile boolean transparencyEnabled;
    private volatile boolean fallbackEnabled;

    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE,
               policy = org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC)
    private volatile List<TranslationProvider> fallbackProviders;

    @Reference
    private com.example.aem.translation.service.TranslationAuditService auditService;

    @Reference
    private com.example.aem.translation.service.TranslationFeedbackService feedbackService;

    @Reference
    private JobManager jobManager;


    private static class TranslationJob {
        private final String jobId;
        private final String sourceLanguage;
        private final String targetLanguage;
        private volatile TranslationStatus status;
        private final List<String> objectIds = new java.util.concurrent.CopyOnWriteArrayList<>();
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
        this.transparencyEnabled = config.enableSentimentAnalysis() || config.enableComplianceCheck();
        this.fallbackEnabled = config.enableFallbackProviders();

        if (this.meterRegistry != null) {
            this.meterRegistry.close();
        }
        this.meterRegistry = null;
        if (metricsEnabled) {
            this.meterRegistry = new SimpleMeterRegistry();
            this.metrics = new TranslationMetrics(this.meterRegistry);
            LOG.info("Metrics enabled for TranslateGemma service");
        } else {
            this.metrics = null;
        }

        if (cachingEnabled) {
            this.cache = new TranslationCache(config.cacheMaxSize(), config.cacheExpireAfterMinutes());
            LOG.info("Caching enabled with maxSize={}, expireAfterMinutes={}",
                    config.cacheMaxSize(), config.cacheExpireAfterMinutes());
        } else {
            this.cache = null;
        }

        this.resilienceHelper = new ResilienceHelper.Builder()
                .maxRetries(config.retryMaxAttempts())
                .waitDurationMillis(config.retryWaitDurationMs())
                .failureRateThreshold(config.circuitBreakerFailureRateThreshold())
                .slowCallRateThreshold(config.circuitBreakerSlowCallRateThreshold())
                .slowCallDurationMillis(config.circuitBreakerSlowCallDurationMs())
                .circuitBreakerWaitDurationSeconds(config.circuitBreakerWaitDurationS())
                .build();

        initializeSupportedLanguages();
        LOG.info("TranslateGemma Translation Service activated with project: {}, location: {}", 
                config.projectId(), config.location());
    }

    @Deactivate
    protected void deactivate() {
        if (cache != null) {
            cache.invalidateAll();
        }
        if (meterRegistry != null) {
            meterRegistry.close();
            meterRegistry = null;
        }

        VertexAI client = this.vertexAI;
        this.vertexAI = null;
        this.model = null;
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                LOG.warn("Failed to close Vertex AI client", e);
            }
        }
        LOG.info("TranslateGemma Translation Service deactivated");
    }

    private synchronized void ensureInitialized() {
        if (this.vertexAI != null || this.model != null) {
            return;
        }
        if (config == null || config.projectId() == null || config.projectId().trim().isEmpty()) {
            return;
        }
        try {
            VertexAI client = new VertexAI(config.projectId(), config.location());
            String modelName = config.modelName() != null ? config.modelName() : DEFAULT_MODEL;
            this.model = new GenerativeModel(modelName, client);
            this.vertexAI = client;
            LOG.info("Vertex AI client initialized with model: {}", modelName);
        } catch (Exception e) {
            LOG.error("Failed to initialize Vertex AI client for project {}: {}",
                    config.projectId(), e.getMessage());
        }
    }

    // Used by unit tests to inject a mock model
    public void setModel(GenerativeModel mockModel) {
        this.model = mockModel;
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
        
        try {
            String jsonResponse = resilienceHelper.executeWithRetryAndCircuitBreaker(
                () -> {
                    String prompt = String.format(
                        "Analyze the sentiment of the following text. " +
                        "Respond ONLY in JSON format: {\"sentiment\": \"POSITIVE|NEGATIVE|NEUTRAL\", \"confidence\": 0.95, \"reasoning\": \"The explanation...\"}\n\nText: %s",
                        sanitizedInput
                    );
                    return executeGemmaPrompt(prompt);
                },
                "analyzeSentiment",
                "translationCircuitBreaker"
            );

            // BAML Pattern: Boundary parsing
            SentimentResult result = GemmaBoundaryService.parseStructuredOutput(jsonResponse, SentimentResult.class);
            
            // Log for audit
            auditService.logEvent("N/A", "en", "en", result, null, "system");

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
            String sanitizedInput = InputSanitizer.sanitizeForPrompt(content);
            String jsonResponse = resilienceHelper.executeWithRetryAndCircuitBreaker(
                () -> {
                    String prompt = String.format(
                        "Analyze the following text for brand compliance and prohibited content. " +
                        "Respond ONLY in JSON format: {\"compliant\": true|false, \"feedback\": \"Summary...\", \"confidence\": 0.98, \"reasoning\": \"Why...\"}\n\nText: %s",
                        sanitizedInput
                    );
                    return executeGemmaPrompt(prompt);
                },
                "analyzeCompliance",
                "translationCircuitBreaker"
            );

            // BAML Pattern: Boundary parsing
            ComplianceResult result = GemmaBoundaryService.parseStructuredOutput(jsonResponse, ComplianceResult.class);
            
            // Log for audit
            auditService.logEvent("N/A", "en", "en", null, result, "system");

            return result;
        } catch (Exception e) {
            LOG.error("Error analyzing compliance", e);
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

            return GemmaBoundaryService.parseStructuredOutput(jsonResponse, AssetAnalysisResult.class);

        } catch (Exception e) {
            LOG.error("Error analyzing asset metadata", e);
            throw TranslateGemmaException.translationFailed(e.getMessage());
        }
    }

    public boolean isServiceAvailable() {
        if (config != null && !config.enabled()) {
            return false;
        }
        ensureInitialized();
        if (model != null) {
            return true;
        }
        List<TranslationProvider> providers = fallbackProviders;
        if (!fallbackEnabled || providers == null) {
            return false;
        }
        for (TranslationProvider provider : providers) {
            try {
                if (provider.isAvailable()) {
                    return true;
                }
            } catch (Exception e) {
                LOG.debug("Fallback provider {} unavailable: {}", provider.getProviderName(), e.getMessage());
            }
        }
        return false;
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
            throw new TranslationException("Invalid input for language detection", TranslationException.ErrorCode.GENERAL_EXCEPTION);
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
                        if (model != null) {
                            String prompt = String.format("Detect the language of this text and respond with only the ISO 639-1 language code: %s", sanitizedInput);
                            GenerateContentResponse response = model.generateContent(prompt);
                            return ResponseHandler.getText(response).trim().toLowerCase();
                        }
                        return detectLanguageWithFallbackProvider(sanitizedInput);
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

    private String detectLanguageWithFallbackProvider(String text) {
        List<TranslationProvider> providers = fallbackProviders;
        if (!fallbackEnabled || providers == null) {
            throw new IllegalStateException("No translation provider available");
        }
        for (TranslationProvider provider : providers) {
            try {
                if (provider.isAvailable()) {
                    String detected = provider.detectLanguage(text);
                    if (detected != null && !detected.trim().isEmpty()) {
                        return detected.trim().toLowerCase();
                    }
                }
            } catch (Exception e) {
                LOG.warn("Fallback provider {} language detection failed: {}", provider.getProviderName(), e.getMessage());
            }
        }
        throw new IllegalStateException("No translation provider available");
    }

    @Override
    public TranslationResult translateString(String sourceString, String sourceLanguage,
                                           String targetLanguage, ContentType contentType,
                                           String contentCategory) throws TranslationException {
        if (!isServiceAvailable()) {
            throw new TranslationException("TranslateGemma service is not available", TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
        }

        if (!InputSanitizer.isValidInput(sourceString)) {
            throw new TranslationException("Invalid input for translation", TranslationException.ErrorCode.GENERAL_EXCEPTION);
        }

        String sanitizedText = InputSanitizer.sanitizeForPrompt(sourceString);
        String sanitizedSourceLang = InputSanitizer.sanitizeLanguageCode(sourceLanguage);
        String sanitizedTargetLang = InputSanitizer.sanitizeLanguageCode(targetLanguage);
        String sanitizedCategory = InputSanitizer.sanitizeContentCategory(contentCategory);

        if (sanitizedTargetLang == null) {
            throw new TranslationException("Invalid language codes provided", TranslationException.ErrorCode.GENERAL_EXCEPTION);
        }

        if (sanitizedSourceLang != null && !InputSanitizer.isValidLanguagePair(sanitizedSourceLang, sanitizedTargetLang)) {
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
            String translatedText;
            if (model == null) {
                translatedText = translateWithFallbackProvider(sanitizedText, finalSourceLang,
                        sanitizedTargetLang, contentType, sanitizedCategory,
                        new IllegalStateException("Vertex AI model is not configured"));
            } else {
                try {
                    translatedText = resilienceHelper.executeWithRetryAndCircuitBreaker(
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
                } catch (RuntimeException primaryFailure) {
                    translatedText = translateWithFallbackProvider(sanitizedText, finalSourceLang,
                            sanitizedTargetLang, contentType, sanitizedCategory, primaryFailure);
                }
            }

            final String finalTranslation = translatedText;
            if (cachingEnabled && cache != null && finalTranslation != null) {
                cache.getTranslation(sanitizedText, finalSourceLang, sanitizedTargetLang, key -> finalTranslation);
            }

            if (metrics != null) {
                metrics.recordTranslationSuccess();
                metrics.recordLatency(System.currentTimeMillis() - startTime);
            }

            // Trust & Transparency: opt-in analysis, never on the default hot path
            if (transparencyEnabled) {
                SentimentResult sentiment = null;
                ComplianceResult compliance = null;
                try {
                    if (config.enableSentimentAnalysis()) {
                        sentiment = analyzeSentiment(translatedText);
                    }
                    if (config.enableComplianceCheck()) {
                        compliance = analyzeCompliance(translatedText);
                    }
                    auditService.logEvent("N/A", finalSourceLang, sanitizedTargetLang, sentiment, compliance, "system");
                } catch (Exception ex) {
                    LOG.warn("Failed to perform transparency analysis for translation", ex);
                }
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

    private String translateWithFallbackProvider(String text, String sourceLang, String targetLang,
            ContentType contentType, String category, RuntimeException primaryFailure) throws TranslationException {
        List<TranslationProvider> providers = fallbackProviders;
        if (!fallbackEnabled || providers == null || providers.isEmpty()) {
            throw primaryFailure;
        }
        for (TranslationProvider provider : providers) {
            if (!provider.supportsLanguagePair(sourceLang, targetLang)) {
                continue;
            }
            try {
                LOG.warn("Primary provider failed ({}). Trying fallback provider {}",
                        primaryFailure.getMessage(), provider.getProviderName());
                TranslationResult result = provider.translate(text, sourceLang, targetLang, contentType, category);
                if (result != null && result.getTranslation() != null) {
                    return result.getTranslation();
                }
            } catch (Exception e) {
                LOG.warn("Fallback provider {} failed: {}", provider.getProviderName(), e.getMessage());
            }
        }
        throw primaryFailure;
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

        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("Translate the following %s from %s to %s. ", contentTypeDesc, sourceLangName, targetLangName));
        prompt.append("Preserve the original formatting and structure. Respond with only the translated text.\n\n");

        // Few-Shot Learning: Add human feedback examples
        List<com.example.aem.translation.model.TranslationFeedback> feedback = 
            feedbackService.getRelevantFeedback(text, sourceLang, targetLang, 3);
        
        if (!feedback.isEmpty()) {
            prompt.append("### Guidelines based on previous human corrections:\n");
            for (com.example.aem.translation.model.TranslationFeedback fb : feedback) {
                prompt.append(String.format("Source: \"%s\"\nCorrection: \"%s\"\n\n",
                    InputSanitizer.sanitizeInputText(fb.getSourceString()),
                    InputSanitizer.sanitizeInputText(fb.getHumanCorrection())));
            }
            prompt.append("### Now translate:\n");
        }

        prompt.append(text);
        return prompt.toString();
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
            throw new TranslationException("Translation job not found: " + strTranslationJobID, TranslationException.ErrorCode.GENERAL_EXCEPTION);
        }

        String objectId = getObjectId(translationObject);
        if (objectId == null) {
            objectId = UUID.randomUUID().toString();
        }

        job.getObjectIds().add(objectId);
        translationObjects.put(objectId, translationObject);
        LOG.info("Uploaded translation object: {} for job: {}", objectId, strTranslationJobID);

        // Enterprise Refactoring: Using Sling Jobs for persistence and scalability
        Map<String, Object> jobProperties = new HashMap<>();
        jobProperties.put("jobId", strTranslationJobID);
        jobProperties.put("objectId", objectId);
        jobProperties.put("sourceLanguage", job.getSourceLanguage());
        jobProperties.put("targetLanguage", job.getTargetLanguage());
        jobProperties.put("content", getContentFromTranslationObject(translationObject));
        jobProperties.put("contentType", contentTypeFor(translationObject).name());

        org.apache.sling.event.jobs.Job slingJob = jobManager.addJob(TranslationJobConsumer.JOB_TOPIC, jobProperties);
        
        if (slingJob == null) {
            LOG.error("Failed to create Sling Job for translation object {}", objectId);
            throw new TranslationException("Failed to queue translation job", TranslationException.ErrorCode.GENERAL_EXCEPTION);
        }

        LOG.info("Successfully queued Sling Job {} for translation object {}", slingJob.getId(), objectId);
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
            throw new TranslationException("Translation job not found: " + strTranslationJobID, TranslationException.ErrorCode.GENERAL_EXCEPTION);
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
            throw new TranslationException("Translation job not found: " + strTranslationJobID, TranslationException.ErrorCode.GENERAL_EXCEPTION);
        }

        String objectId = getObjectId(translationObject);
        String translatedContent = job.getTranslatedContent(objectId);

        if (translatedContent == null) {
            // If content not ready, return original content
            LOG.warn("Translated content not yet available for object {} in job {}. Returning original content.",
                    objectId, strTranslationJobID);
            translatedContent = readContentAsString(translationObject);
        }

        return new ByteArrayInputStream(translatedContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String getContentFromTranslationObject(TranslationObject translationObject) throws TranslationException {
        return readContentAsString(translationObject);
    }

    private String getObjectId(TranslationObject translationObject) {
        String path = translationObject.getTranslationObjectSourcePath();
        return path != null ? path : translationObject.getId();
    }

    private ContentType contentTypeFor(TranslationObject translationObject) {
        String mimeType = translationObject.getMimeType();
        if (mimeType != null && mimeType.toLowerCase().contains("html")) {
            return ContentType.HTML;
        }
        return ContentType.PLAIN;
    }

    private String readContentAsString(TranslationObject translationObject) throws TranslationException {
        try (InputStream in = translationObject.getTranslationObjectInputStream()) {
            if (in == null) {
                throw new TranslationException("Translation object has no content stream",
                        TranslationException.ErrorCode.GENERAL_EXCEPTION);
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TranslationException("Could not read translation object content", e,
                    TranslationException.ErrorCode.GENERAL_EXCEPTION);
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
        return new TranslationScope() {
            @Override
            public int getWordCount() {
                return 0;
            }

            @Override
            public int getImageCount() {
                return 0;
            }

            @Override
            public int getVideoCount() {
                return 0;
            }

            @Override
            public Map<String, String> getFinalScope() {
                return Collections.emptyMap();
            }
        };
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
        return new EmptyCommentCollection();
    }

    @Override
    public CommentCollection<Comment> getTranslationObjectCommentCollection(String strTranslationJobID,
            TranslationObject translationObject) throws TranslationException {
        return new EmptyCommentCollection();
    }

    private static final class EmptyCommentCollection implements CommentCollection<Comment> {
        @Override
        public Comment addComment(String title, String message, String userId) {
            throw new UnsupportedOperationException("Comments are not supported by this connector");
        }

        @Override
        public List<Comment> getCommentList() {
            return Collections.emptyList();
        }

        @Override
        public Calendar getCreated() {
            return null;
        }

        @Override
        public Calendar getLastModified() {
            return null;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public Resource getTarget() {
            return null;
        }

        @Override
        public void remove() {
            // nothing to remove
        }
    }

    @Override
    public String getDefaultCategory() {
        if (config != null && config.defaultContentCategory() != null) {
            return config.defaultContentCategory();
        }
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
