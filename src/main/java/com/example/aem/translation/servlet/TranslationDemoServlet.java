package com.example.aem.translation.servlet;

import com.adobe.granite.translation.api.TranslationConstants.ContentType;
import com.adobe.granite.translation.api.TranslationResult;
import com.example.aem.translation.service.TranslateGemmaTranslationService;
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
import java.util.HashMap;
import java.util.Map;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.methods=POST",
        "sling.servlet.paths=/bin/translation-gemma/translate"
    }
)
public class TranslationDemoServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationDemoServlet.class);
    private static final int MAX_TEXT_LENGTH = 4000;

    @Reference
    private TranslateGemmaTranslationService gemmaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();
        if ("anonymous".equals(request.getResourceResolver().getUserID())) {
            response.setStatus(SlingHttpServletResponse.SC_UNAUTHORIZED);
            result.put("error", "Authentication required");
            objectMapper.writeValue(response.getWriter(), result);
            return;
        }

        try {
            Map<String, String> data = objectMapper.readValue(request.getReader(), Map.class);
            String text = data.get("text");
            String sourceLang = data.get("sourceLang");
            String targetLang = data.get("targetLang");

            if (text == null || text.isEmpty() || text.length() > MAX_TEXT_LENGTH
                    || targetLang == null || targetLang.isEmpty()) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                result.put("error", "text (1-" + MAX_TEXT_LENGTH + " chars) and targetLang are required");
                objectMapper.writeValue(response.getWriter(), result);
                return;
            }

            ContentType contentType = "html".equalsIgnoreCase(data.get("contentType"))
                    ? ContentType.HTML : ContentType.PLAIN;
            String category = data.getOrDefault("category", "general");

            TranslationResult translation = gemmaService.translateString(
                    text, sourceLang == null ? "" : sourceLang, targetLang, contentType, category);

            result.put("translation", translation.getTranslation());
            result.put("sourceLang", translation.getSourceLanguage());
            result.put("targetLang", translation.getTargetLanguage());
            objectMapper.writeValue(response.getWriter(), result);
        } catch (Exception e) {
            LOG.error("Translation request failed", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("error", "Translation failed: " + e.getMessage());
            objectMapper.writeValue(response.getWriter(), result);
        }
    }
}
