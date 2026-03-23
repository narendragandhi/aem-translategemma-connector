package com.example.aem.translation.impl;

import com.example.aem.translation.service.MultiModalTranslationService;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.adobe.granite.translation.api.TranslationException;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(service = MultiModalTranslationService.class)
public class MultiModalTranslationServiceImpl implements MultiModalTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(MultiModalTranslationServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Reference
    private TranslateGemmaConfig config;

    @Override
    public MultiModalResult analyzeImage(byte[] imageData, String mimeType, String prompt) throws TranslationException {
        if (imageData == null || imageData.length == 0) {
            return new MultiModalResult("Empty image data provided.");
        }

        String projectId = System.getenv("GCP_PROJECT_ID");
        String location = System.getenv("GCP_LOCATION");
        if (location == null) location = "us-central1";

        if (projectId == null) {
            LOG.warn("GCP_PROJECT_ID not configured for multi-modal analysis.");
            return new MultiModalResult("GCP project ID not configured.");
        }

        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            GenerativeModel model = new GenerativeModel("gemini-1.5-flash", vertexAI);
            
            GenerateContentResponse response = model.generateContent(
                com.google.cloud.vertexai.generativeai.ContentMaker.fromMultiModalData(
                    prompt,
                    PartMaker.fromMimeTypeAndData(mimeType, imageData)
                )
            );

            String textResponse = ResponseHandler.getText(response);
            LOG.debug("Multi-modal raw response: {}", textResponse);

            // Extract JSON from response (handling potential markdown formatting)
            String jsonContent = extractJson(textResponse);
            
            @SuppressWarnings("unchecked")
            Map<String, String> data = objectMapper.readValue(jsonContent, Map.class);
            
            return new MultiModalResult(textResponse, data);

        } catch (Exception e) {
            LOG.error("Failed to analyze image via Vertex AI", e);
            throw new TranslationException("Multi-modal analysis failed: " + e.getMessage(), e);
        }
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        
        return text;
    }
}
