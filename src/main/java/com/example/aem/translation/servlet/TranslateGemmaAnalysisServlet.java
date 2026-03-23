package com.example.aem.translation.servlet;

import com.example.aem.translation.service.TranslateGemmaTranslationService;
import com.example.aem.translation.model.SentimentResult;
import com.example.aem.translation.model.ComplianceResult;
import com.example.aem.translation.model.AssetAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.methods=" + HttpConstants.METHOD_POST,
        "sling.servlet.paths=/bin/translategemma/analyze"
    }
)
public class TranslateGemmaAnalysisServlet extends SlingAllMethodsServlet {

    @Reference
    private TranslateGemmaTranslationService gemmaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        Map<String, Object> result = new HashMap<>();

        try {
            // Parse request body
            Map<String, Object> body = objectMapper.readValue(request.getInputStream(), Map.class);
            String path = (String) body.get("path");
            List<String> types = (List<String>) body.get("types");

            if (path == null || types == null) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                result.put("error", "Missing 'path' or 'types' in request body");
                response.getWriter().write(objectMapper.writeValueAsString(result));
                return;
            }

            ResourceResolver resolver = request.getResourceResolver();
            Resource resource = resolver.getResource(path);

            if (resource == null) {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                result.put("error", "Resource not found at path: " + path);
                response.getWriter().write(objectMapper.writeValueAsString(result));
                return;
            }

            // Extract content (assuming cq:Page or dam:Asset for now)
            String content = extractContent(resource);
            
            if (types.contains("sentiment")) {
                SentimentResult sentiment = gemmaService.analyzeSentiment(content);
                result.put("sentiment", sentiment);
            }

            if (types.contains("compliance")) {
                ComplianceResult compliance = gemmaService.analyzeCompliance(content);
                result.put("compliance", compliance);
            }

            if (types.contains("asset") && resource.isResourceType("dam:Asset")) {
                AssetAnalysisResult assetAnalysis = gemmaService.analyzeAsset(path, resolver);
                result.put("asset", assetAnalysis);
            }

            response.getWriter().write(objectMapper.writeValueAsString(result));

        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("error", e.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(result));
        }
    }

    private String extractContent(Resource resource) {
        // Simplified content extraction
        ValueMap vm = resource.getValueMap();
        if (resource.isResourceType("cq:Page")) {
            Resource jcrContent = resource.getChild("jcr:content");
            return jcrContent != null ? jcrContent.getValueMap().get("jcr:title", String.class) : "";
        } else if (resource.isResourceType("dam:Asset")) {
            Resource metadata = resource.getChild("jcr:content/metadata");
            return metadata != null ? metadata.getValueMap().get("dc:description", String.class) : "";
        }
        return vm.get("jcr:description", vm.get("jcr:title", ""));
    }
}
