package com.example.aem.translation.impl;

import com.example.aem.translation.service.PromptTemplateService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(service = PromptTemplateService.class)
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(PromptTemplateServiceImpl.class);
    private static final String TEMPLATE_PATH = "/conf/example/settings/translategemma/prompts";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Reference
    private ResourceResolverFactory resolverFactory;

    private final Map<String, String> defaultTemplates = new HashMap<>();

    public PromptTemplateServiceImpl() {
        // Initialize default templates
        defaultTemplates.put("translation", 
            "Translate the following ${contentType} content from ${sourceLanguage} to ${targetLanguage}. " +
            "Maintain the original tone and formatting. Only return the translated content.\n\n" +
            "Content:\n${text}");
            
        defaultTemplates.put("ocr", 
            "Analyze the provided image and extract all translatable text found within it (OCR). " +
            "Then, translate that text into ${targetLanguage}. " +
            "Also, provide a brief descriptive Alt-Text for the image in ${targetLanguage}. " +
            "Return the result as a JSON object with 'ocrText', 'translatedOcrText', and 'altText' fields.");
            
        defaultTemplates.put("sentiment", 
            "Analyze the sentiment of the following text in ${sourceLanguage}. " +
            "Categorize it as POSITIVE, NEGATIVE, or NEUTRAL. " +
            "Provide a confidence score between 0.0 and 1.0. " +
            "Return the result as a JSON object with 'sentiment' and 'confidence' fields.\n\n" +
            "Text:\n${text}");

        defaultTemplates.put("compliance", 
            "Analyze the following content in ${targetLanguage} for brand compliance. " +
            "Check for tone, prohibited terms, and alignment with our brand guidelines. " +
            "Return a JSON object with 'compliant' (boolean) and 'feedback' (string) fields.\n\n" +
            "Content:\n${text}");
    }

    @Override
    public String renderPrompt(String templateName, Map<String, Object> variables) {
        return renderPrompt(templateName, "default", variables);
    }

    @Override
    public String renderPrompt(String templateName, String category, Map<String, Object> variables) {
        String template = getTemplate(templateName, category);
        if (StringUtils.isEmpty(template)) {
            LOG.warn("No template found for {} with category {}. Using fallback.", templateName, category);
            template = defaultTemplates.getOrDefault(templateName, "");
        }
        
        return substitutePlaceholders(template, variables);
    }

    @Override
    public boolean hasTemplate(String templateName) {
        return defaultTemplates.containsKey(templateName) || findTemplateInJcr(templateName, "default") != null;
    }

    private String getTemplate(String templateName, String category) {
        String jcrTemplate = findTemplateInJcr(templateName, category);
        if (jcrTemplate != null) {
            return jcrTemplate;
        }
        
        if (!"default".equals(category)) {
            return findTemplateInJcr(templateName, "default");
        }
        
        return null;
    }

    private String findTemplateInJcr(String templateName, String category) {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "translate-gemma-service");
        
        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            String path = String.format("%s/%s/%s", TEMPLATE_PATH, templateName, category);
            Resource resource = resolver.getResource(path);
            if (resource != null) {
                ValueMap properties = resource.getValueMap();
                return properties.get("template", String.class);
            }
        } catch (Exception e) {
            LOG.error("Error reading template from JCR", e);
        }
        return null;
    }

    private String substitutePlaceholders(String template, Map<String, Object> variables) {
        if (StringUtils.isEmpty(template)) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        int lastEnd = 0;
        
        while (matcher.find()) {
            result.append(template, lastEnd, matcher.start());
            String key = matcher.group(1);
            Object value = variables.get(key);
            result.append(value != null ? value.toString() : matcher.group(0));
            lastEnd = matcher.end();
        }
        result.append(template.substring(lastEnd));
        
        return result.toString();
    }
}
