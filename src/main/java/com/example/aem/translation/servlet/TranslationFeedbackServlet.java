package com.example.aem.translation.servlet;

import com.example.aem.translation.service.TranslationFeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(TranslationFeedbackServlet.class);

    @Reference
    private TranslationFeedbackService feedbackService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Map<String, String> data = objectMapper.readValue(request.getReader(), Map.class);

            String sourceString = data.get("sourceString");
            String originalTranslation = data.get("originalTranslation");
            String humanCorrection = data.get("humanCorrection");
            String sourceLang = data.get("sourceLang");
            String targetLang = data.get("targetLang");

            if (sourceString == null || humanCorrection == null || sourceLang == null || targetLang == null) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(response.getWriter(),
                        Map.of("error", "sourceString, humanCorrection, sourceLang and targetLang are required"));
                return;
            }

            String userId = request.getResourceResolver().getUserID();
            feedbackService.recordFeedback(sourceString, originalTranslation, humanCorrection,
                                          sourceLang, targetLang, userId);

            response.setStatus(SlingHttpServletResponse.SC_OK);
            objectMapper.writeValue(response.getWriter(), Map.of("status", "Feedback recorded"));
        } catch (Exception e) {
            LOG.error("Failed to record translation feedback", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("error", "Internal error while recording feedback"));
        }
    }
}
