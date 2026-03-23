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
@Designate(ocd = OpenAIProvider.Config.class)
public class OpenAIProvider implements TranslationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAIProvider.class);

    private final Set<String> supportedLanguages = new HashSet<>(Arrays.asList(
            "en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "ar", "hi", "nl", "sv", "da", "no", "fi", "pl", "tr", "vi", "th", "id", "ms", "uk", "he"
    ));

    private Config config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Activate
    public void activate(Config config) {
        this.config = config;
        LOG.info("OpenAI Translation Provider activated with model: {}", config.model());
    }

    @Override
    public String getProviderName() {
        return "OpenAI Translator";
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.OPENAI;
    }

    @Override
    public boolean isAvailable() {
        return config != null && config.apiKey() != null && !config.apiKey().isEmpty();
    }

    @Override
    public TranslationResult translate(String sourceText, String sourceLanguage,
                                       String targetLanguage, TranslationConstants.ContentType contentType,
                                       String category) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("OpenAI API key not configured");
        }

        String systemPrompt = buildSystemPrompt(sourceLanguage, targetLanguage, contentType, category);
        String userPrompt = buildUserPrompt(sourceText, sourceLanguage, targetLanguage, contentType);

        String jsonBody = String.format(
                "{\"model\": \"%s\", \"messages\": [{\"role\": \"system\", \"content\": \"%s\"}, {\"role\": \"user\", \"content\": \"%s\"}], \"temperature\": 0.3, \"max_tokens\": %d}",
                config.model(),
                escapeJson(systemPrompt),
                escapeJson(userPrompt),
                config.maxTokens()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        String translatedText = parseOpenAIResponse(response.body());

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
        String jsonBody = String.format(
                "{\"model\": \"%s\", \"messages\": [{\"role\": \"system\", \"content\": \"Detect the language of the following text and respond with only the ISO 639-1 language code.\"}, {\"role\": \"user\", \"content\": \"%s\"}], \"temperature\": 0.1, \"max_tokens\": 10}",
                config.model(),
                escapeJson(text)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error: " + response.statusCode());
        }

        return parseOpenAIResponse(response.body()).toLowerCase();
    }

    @Override
    public boolean supportsLanguagePair(String sourceLanguage, String targetLanguage) {
        return supportedLanguages.contains(sourceLanguage.toLowerCase()) &&
               supportedLanguages.contains(targetLanguage.toLowerCase());
    }

    @Override
    public double getMatchingScore() {
        return 0.85;
    }

    @Override
    public long getAverageResponseTime() {
        return 1500;
    }

    private String buildSystemPrompt(String sourceLanguage, String targetLanguage, 
                                     TranslationConstants.ContentType contentType, String category) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional translator. ");
        prompt.append("Translate the text from ").append(getLanguageName(sourceLanguage));
        prompt.append(" to ").append(getLanguageName(targetLanguage)).append(". ");
        
        if (contentType == TranslationConstants.ContentType.HTML) {
            prompt.append("Preserve all HTML tags and structure. ");
        }
        
        if (category != null && !category.isEmpty()) {
            prompt.append("Use terminology appropriate for: ").append(category).append(". ");
        }
        
        prompt.append("Respond with only the translation, no explanations.");
        return prompt.toString();
    }

    private String buildUserPrompt(String sourceText, String sourceLanguage, 
                                   String targetLanguage, TranslationConstants.ContentType contentType) {
        return sourceText;
    }

    private String getLanguageName(String code) {
        Map<String, String> languageNames = new HashMap<>();
        languageNames.put("en", "English");
        languageNames.put("es", "Spanish");
        languageNames.put("fr", "French");
        languageNames.put("de", "German");
        languageNames.put("it", "Italian");
        languageNames.put("pt", "Portuguese");
        languageNames.put("ru", "Russian");
        languageNames.put("ja", "Japanese");
        languageNames.put("ko", "Korean");
        languageNames.put("zh", "Chinese");
        languageNames.put("ar", "Arabic");
        languageNames.put("hi", "Hindi");
        
        return languageNames.getOrDefault(code.toLowerCase(), code);
    }

    private String parseOpenAIResponse(String json) {
        int contentStart = json.indexOf("\"content\":\"");
        if (contentStart == -1) return "";
        contentStart += 11;
        int contentEnd = json.indexOf("\"", contentStart);
        if (contentEnd == -1) return "";
        
        String text = json.substring(contentStart, contentEnd);
        return text.replace("\\n", "\n").replace("\\\"", "\"");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    @ObjectClassDefinition(name = "OpenAI Translation Provider")
    public @interface Config {
        String apiKey() default "";
        String baseUrl() default "https://api.openai.com";
        String model() default "gpt-4o-mini";
        int timeoutSeconds() default 120;
        int maxTokens() default 4000;
    }
}
