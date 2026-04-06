package com.example.aem.translation.tm.impl;

import com.example.aem.translation.tm.TranslationMemoryService;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.adobe.granite.translation.api.TranslationConstants;
import com.example.aem.translation.util.HttpClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Enterprise-grade external Translation Memory store.
 */
@Component(
    service = TranslationMemoryService.class,
    immediate = true,
    property = {
        "service.ranking:Integer=200",
        "persistence.type=cloud"
    }
)
public class CloudTranslationMemoryService implements TranslationMemoryService {

    private static final Logger LOG = LoggerFactory.getLogger(CloudTranslationMemoryService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Reference
    private HttpClientProvider httpClientProvider;

    private String endpoint;
    private boolean enabled;

    @Activate
    public void activate(TranslateGemmaConfig config) {
        this.endpoint = config.externalTmEndpoint();
        this.enabled = config.useExternalTm() && !endpoint.isEmpty();
        LOG.info("Cloud TM Service activated. Enabled: {}, Endpoint: {}", enabled, endpoint);
    }

    @Override
    public void storeTranslation(String sourceText, String targetText, String sourceLanguage,
                                String targetLanguage, TranslationConstants.ContentType contentType,
                                String category, String path, int rating) throws Exception {
        if (!enabled) return;

        TMEntry entry = new TMEntry(sourceText, targetText, sourceLanguage, targetLanguage, 
                                   contentType, category, path, rating);
        
        HttpPost post = new HttpPost(endpoint + "/store");
        post.setEntity(new StringEntity(objectMapper.writeValueAsString(entry)));
        post.setHeader("Content-Type", "application/json");

        try (CloseableHttpResponse response = httpClientProvider.getHttpClient().execute(post)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                LOG.error("Failed to store TM entry in cloud. Status: {}", response.getStatusLine());
            }
        }
    }

    @Override
    public List<TMEntry> findMatches(String sourceText, String sourceLanguage, String targetLanguage,
                                    TranslationConstants.ContentType contentType, String category,
                                    double minScore, int maxResults) {
        if (!enabled) return Collections.emptyList();

        try {
            HttpPost post = new HttpPost(endpoint + "/query");
            Map<String, Object> query = new HashMap<>();
            query.put("text", sourceText);
            query.put("source", sourceLanguage);
            query.put("target", targetLanguage);
            query.put("minScore", minScore);
            query.put("maxResults", maxResults);

            post.setEntity(new StringEntity(objectMapper.writeValueAsString(query)));
            post.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClientProvider.getHttpClient().execute(post)) {
                String json = EntityUtils.toString(response.getEntity());
                return Arrays.asList(objectMapper.readValue(json, TMEntry[].class));
            }
        } catch (Exception e) {
            LOG.error("Failed to query cloud TM", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Integer> getStatistics() {
        return Collections.singletonMap("remote_persistence", 1);
    }

    @Override
    public void clearMemory() {
        // Implementation for clearing remote cache if supported by endpoint
        LOG.warn("Clear memory requested for Cloud TM (Not implemented locally)");
    }

    @Override
    public String getStoragePath() {
        return endpoint;
    }
}
