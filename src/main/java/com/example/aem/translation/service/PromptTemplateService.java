package com.example.aem.translation.service;

import java.util.Map;

/**
 * Service for managing and applying prompt templates for LLM-based translation and analysis.
 * Inspired by Embabel's type-safe domain modeling for AI agents.
 */
public interface PromptTemplateService {

    /**
     * Get a rendered prompt by combining a template with variables.
     * 
     * @param templateName The name of the template (e.g., "translation", "ocr", "sentiment")
     * @param variables Map of variables to inject into the template
     * @return The rendered prompt string
     */
    String renderPrompt(String templateName, Map<String, Object> variables);

    /**
     * Get a rendered prompt for a specific content category.
     * 
     * @param templateName The name of the template
     * @param category The content category (e.g., "legal", "marketing")
     * @param variables Map of variables
     * @return The rendered prompt string
     */
    String renderPrompt(String templateName, String category, Map<String, Object> variables);
    
    /**
     * Check if a template exists.
     */
    boolean hasTemplate(String templateName);
}
