package com.example.aem.translation.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enterprise Boundary Service (BAML/Embabel Pattern).
 * Ensures structured output from Gemma 4 by enforcing schema boundaries
 * and cleaning conversational "chatter".
 */
public class GemmaBoundaryService {

    private static final Logger LOG = LoggerFactory.getLogger(GemmaBoundaryService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    // Principal's Regex: Finds the JSON block even if the LLM adds chatter
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{.*\\}|\\[.*\\]", Pattern.DOTALL);

    /**
     * Extracts and parses a POJO from a potentially "chatty" LLM response.
     * Simulates the BAML 'Boundary' abstraction.
     */
    public static <T> T parseStructuredOutput(String rawResponse, Class<T> clazz) {
        try {
            String cleanJson = rawResponse.trim();
            
            // 1. Strip Markdown code blocks if present
            if (cleanJson.contains("```json")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("```json") + 7);
                cleanJson = cleanJson.substring(0, cleanJson.lastIndexOf("```"));
            } else if (cleanJson.contains("```")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("```") + 3);
                cleanJson = cleanJson.substring(0, cleanJson.lastIndexOf("```"));
            }

            // 2. Multi-stage Extraction: If still not valid, use regex to find the boundary
            Matcher matcher = JSON_BLOCK_PATTERN.matcher(cleanJson);
            if (matcher.find()) {
                cleanJson = matcher.group();
            }

            return MAPPER.readValue(cleanJson, clazz);
            
        } catch (Exception e) {
            LOG.error("Boundary Violation: Failed to parse structured output from Gemma. Raw: {}", rawResponse);
            throw new RuntimeException("Gemma Output Schema Violation", e);
        }
    }
}
