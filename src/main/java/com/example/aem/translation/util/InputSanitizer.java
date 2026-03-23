package com.example.aem.translation.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class InputSanitizer {
    private static final Logger LOG = LoggerFactory.getLogger(InputSanitizer.class);

    private static final int MAX_INPUT_LENGTH = 50000;
    private static final int MAX_SOURCE_TEXT_LENGTH = 10000;

    private static final Pattern DANGEROUS_CHARS_PATTERN = Pattern.compile(
        "[\u0000-\u001F\u007F-\u009F]"
    );

    private static final Pattern PROMPT_INJECTION_PATTERN = Pattern.compile(
        "(?i)(ignore previous instructions|ignore all previous|system prompt|you are now|disregard|forget everything)",
        Pattern.CASE_INSENSITIVE
    );

    public static String sanitizeInputText(String input) {
        if (input == null) {
            return "";
        }

        String sanitized = input.trim();

        sanitized = DANGEROUS_CHARS_PATTERN.matcher(sanitized).replaceAll("");

        if (sanitized.length() > MAX_SOURCE_TEXT_LENGTH) {
            LOG.warn("Input text exceeds maximum length, truncating from {} to {}", 
                    sanitized.length(), MAX_SOURCE_TEXT_LENGTH);
            sanitized = sanitized.substring(0, MAX_SOURCE_TEXT_LENGTH);
        }

        return sanitized;
    }

    public static String sanitizeLanguageCode(String languageCode) {
        if (languageCode == null) {
            return null;
        }

        String sanitized = languageCode.trim().toLowerCase();

        if (!sanitized.matches("^[a-z]{2}(-[a-z]{2,})?$")) {
            LOG.warn("Invalid language code format: {}", languageCode);
            return null;
        }

        return sanitized;
    }

    public static String sanitizeContentCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "general";
        }

        String sanitized = category.trim().toLowerCase();

        if (!sanitized.matches("^[a-z0-9_-]+$")) {
            LOG.warn("Invalid content category format, using default: {}", category);
            return "general";
        }

        return sanitized;
    }

    public static boolean containsPromptInjection(String text) {
        if (text == null) {
            return false;
        }
        return PROMPT_INJECTION_PATTERN.matcher(text).find();
    }

    public static String sanitizeForPrompt(String input) {
        if (input == null) {
            return "";
        }

        String sanitized = sanitizeInputText(input);

        if (containsPromptInjection(sanitized)) {
            LOG.warn("Potential prompt injection detected, sanitizing");
            sanitized = sanitized.replaceAll("(?i)(ignore previous instructions|ignore all previous|system prompt|you are now|disregard|forget everything)", "[filtered]");
        }

        return sanitized;
    }

    public static boolean isValidInput(String input) {
        return StringUtils.isNotBlank(input) && 
               input.length() <= MAX_INPUT_LENGTH;
    }

    public static boolean isValidLanguagePair(String sourceLang, String targetLang) {
        return StringUtils.isNotBlank(sourceLang) && 
               StringUtils.isNotBlank(targetLang) &&
               !sourceLang.equals(targetLang);
    }

    public static int getMaxInputLength() {
        return MAX_INPUT_LENGTH;
    }
}
