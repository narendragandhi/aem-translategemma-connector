package com.example.aem.translation.servlet;

import com.example.aem.translation.service.TranslationFeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/translation-gemma/feedback",
        "sling.servlet.methods=POST"
    }
)
public class TranslationFeedbackServlet extends SlingAllMethodsServlet {

    @Reference
    private TranslationFeedbackService feedbackService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        try {
            Map<String, String> data = objectMapper.readValue(request.getReader(), Map.class);
            
            String sourceString = data.get("sourceString");
            String originalTranslation = data.get("originalTranslation");
            String humanCorrection = data.get("humanCorrection");
            String sourceLang = data.get("sourceLang");
            String targetLang = data.get("targetLang");
            String userId = request.getResourceResolver().getUserID();

            feedbackService.recordFeedback(sourceString, originalTranslation, humanCorrection, 
                                          sourceLang, targetLang, userId);
            
            response.setStatus(SlingHttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\": \"Feedback recorded successfully\"}");
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
