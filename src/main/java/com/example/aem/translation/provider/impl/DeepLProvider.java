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
@Designate(ocd = DeepLProvider.Config.class)
public class DeepLProvider implements TranslationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DeepLProvider.class);

    private final Set<String> supportedLanguages = new HashSet<>(Arrays.asList(
            "en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "nl", "pl", "el", "sv", "cs", "da", "fi", "hu", "sk", "ro", "bg", "lt", "lv", "et", "uk"
    ));

    private Config config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Activate
    public void activate(Config config) {
        this.config = config;
        LOG.info("DeepL Translation Provider activated");
    }

    @Override
    public String getProviderName() {
        return "DeepL Translator";
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.DEEPL;
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
            throw new IllegalStateException("DeepL API key not configured");
        }

        String url = config.useFreeApi() 
                ? "https://api-free.deepl.com/v2/translate" 
                : "https://api.deepl.com/v2/translate";

        String jsonBody = String.format(
                "{\"text\": [\"%s\"], \"source_lang\": \"%s\", \"target_lang\": \"%s\"}",
                escapeJson(sourceText),
                sourceLanguage.toUpperCase(),
                targetLanguage.toUpperCase()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "DeepL-Auth-Key " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepL API error: " + response.statusCode() + " - " + response.body());
        }

        String translatedText = parseDeepLResponse(response.body());

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
            throw new IllegalStateException("DeepL API key not configured");
        }

        String url = config.useFreeApi() 
                ? "https://api-free.deepl.com/v2/detect" 
                : "https://api.deepl.com/v2/detect";

        String jsonBody = String.format("{\"text\": \"%s\"}", escapeJson(text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "DeepL-Auth-Key " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepL API error: " + response.statusCode());
        }

        return parseDeepLDetectResponse(response.body());
    }

    @Override
    public boolean supportsLanguagePair(String sourceLanguage, String targetLanguage) {
        return supportedLanguages.contains(sourceLanguage.toLowerCase()) &&
               supportedLanguages.contains(targetLanguage.toLowerCase());
    }

    @Override
    public double getMatchingScore() {
        return 0.9;
    }

    @Override
    public long getAverageResponseTime() {
        return 500;
    }

    private String parseDeepLResponse(String json) {
        int start = json.indexOf("\"text\":\"");
        if (start == -1) return "";
        start += 8;
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        String text = json.substring(start, end);
        return text.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String parseDeepLDetectResponse(String json) {
        int langStart = json.indexOf("\"language\":\"");
        if (langStart == -1) return "UNKNOWN";
        langStart += 12;
        int langEnd = json.indexOf("\"", langStart);
        if (langEnd == -1) return "UNKNOWN";
        return json.substring(langStart, langEnd).toLowerCase();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    @ObjectClassDefinition(name = "DeepL Translation Provider")
    public @interface Config {
        String apiKey() default "";
        boolean useFreeApi() default true;
        int timeoutSeconds() default 60;
    }
}
