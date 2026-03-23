package com.example.aem.translation.provider.impl;

import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationResult;
import com.example.aem.translation.provider.TranslationProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component(service = TranslationProvider.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = OllamaProvider.Config.class)
public class OllamaProvider implements TranslationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaProvider.class);

    private final Set<String> supportedLanguages = new HashSet<>(Arrays.asList(
            "en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "ar", "hi", "nl", "sv", "da", "no", "fi", "pl", "tr", "vi", "th", "id", "ms", "uk", "he", "fa", "ur", "bn", "ta", "te", "mr", "gu", "kn", "ml"
    ));

    private Config config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Activate
    public void activate(Config config) {
        this.config = config;
        LOG.info("Ollama Translation Provider activated with model: {} at {}", config.model(), config.baseUrl());
    }

    @Override
    public String getProviderName() {
        return "Ollama Local Translator";
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.OLLAMA;
    }

    @Override
    public boolean isAvailable() {
        if (config == null || config.baseUrl() == null || config.baseUrl().isEmpty()) {
            return false;
        }
        return checkModelAvailability();
    }

    private boolean checkModelAvailability() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains(config.model());
        } catch (Exception e) {
            LOG.warn("Ollama server not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public TranslationResult translate(String sourceText, String sourceLanguage,
                                       String targetLanguage, TranslationConstants.ContentType contentType,
                                       String category) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Ollama server not available or model not installed");
        }

        String prompt = buildTranslationPrompt(sourceText, sourceLanguage, targetLanguage, contentType, category);

        String jsonBody = String.format(
                "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false, \"options\": {\"temperature\": 0.3, \"num_predict\": %d}}",
                config.model(),
                escapeJson(prompt),
                config.maxTokens()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API error: " + response.statusCode() + " - " + response.body());
        }

        String translatedText = parseOllamaResponse(response.body());

        return new TranslationResult() {
            @Override
            public String getSourceLanguage() {
                return sourceLanguage;
            }

            @Override
            public String getTargetLanguage() {
                return targetLanguage;
            }

            @Override
            public String getSourceString() {
                return sourceText;
            }

            @Override
            public String getTranslation() {
                return translatedText;
            }

            @Override
            public TranslationConstants.ContentType getContentType() {
                return contentType;
            }

            @Override
            public String getCategory() {
                return category;
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
    public String detectLanguage(String text) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Ollama server not available");
        }

        String prompt = "Detect the language of the following text and respond with only the ISO 639-1 language code (e.g., en, es, fr):\n\n" + text;

        String jsonBody = String.format(
                "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false, \"options\": {\"temperature\": 0.1, \"num_predict\": 10}}",
                config.model(),
                escapeJson(prompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API error: " + response.statusCode());
        }

        return parseOllamaResponse(response.body()).trim().toLowerCase();
    }

    @Override
    public boolean supportsLanguagePair(String sourceLanguage, String targetLanguage) {
        return supportedLanguages.contains(sourceLanguage.toLowerCase()) &&
               supportedLanguages.contains(targetLanguage.toLowerCase());
    }

    @Override
    public double getMatchingScore() {
        return 0.75;
    }

    @Override
    public long getAverageResponseTime() {
        return 2000;
    }

    private String buildTranslationPrompt(String sourceText, String sourceLanguage,
                                          String targetLanguage, TranslationConstants.ContentType contentType,
                                          String category) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional translator. ");
        
        if (contentType == TranslationConstants.ContentType.HTML) {
            prompt.append("Translate the following HTML content from ").append(sourceLanguage);
            prompt.append(" to ").append(targetLanguage).append(". ");
            prompt.append("Preserve all HTML tags and structure exactly. ");
        } else {
            prompt.append("Translate the following text from ").append(sourceLanguage);
            prompt.append(" to ").append(targetLanguage).append(". ");
        }
        
        if (category != null && !category.isEmpty()) {
            prompt.append("Use terminology appropriate for: ").append(category).append(". ");
        }
        
        prompt.append("Respond with only the translation, no explanations, no quotes.\n\nText:\n");
        prompt.append(sourceText);
        
        return prompt.toString();
    }

    private String parseOllamaResponse(String json) {
        int responseStart = json.indexOf("\"response\":\"");
        if (responseStart == -1) return "";
        responseStart += 12;
        int responseEnd = json.indexOf("\"", responseStart);
        if (responseEnd == -1) return "";
        
        String text = json.substring(responseStart, responseEnd);
        return text.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    @ObjectClassDefinition(name = "Ollama Translation Provider")
    public @interface Config {
        String baseUrl() default "http://localhost:11434";
        String model() default "llama3.2";
        int timeoutSeconds() default 180;
        int maxTokens() default 4000;
    }
}
