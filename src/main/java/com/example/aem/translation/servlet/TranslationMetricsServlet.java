package com.example.aem.translation.servlet;

import com.example.aem.translation.service.TranslationAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/translation-gemma/metrics",
        "sling.servlet.methods=GET"
    }
)
public class TranslationMetricsServlet extends SlingSafeMethodsServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Reference
    private TranslationAuditService auditService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        List<TranslationAuditService.AuditEntry> entries = auditService.getRecentEntries(1000);
        
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        
        Map<String, Integer> langUsage = new HashMap<>();

        for (TranslationAuditService.AuditEntry entry : entries) {
            if (entry.getSentiment() != null) {
                totalInputTokens += entry.getSentiment().getInputTokens();
                totalOutputTokens += entry.getSentiment().getOutputTokens();
            }
            
            String lang = entry.getTargetLanguage();
            if (lang != null) {
                langUsage.put(lang, langUsage.getOrDefault(lang, 0) + 1);
            }
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalInputTokens", totalInputTokens);
        metrics.put("totalOutputTokens", totalOutputTokens);
        metrics.put("totalEvents", entries.size());
        metrics.put("languageDistribution", langUsage);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), metrics);
    }
}
