package com.example.aem.translation.util;

import org.apache.commons.codec.digest.DigestUtils;
import java.nio.charset.StandardCharsets;

/**
 * Principal-grade utility for generating idempotency keys.
 * Ensures we don't re-translate content unless the source or model changes.
 */
public class HashUtils {

    /**
     * Generates a stable fingerprint for a translation request.
     * @param text The source text.
     * @param targetLang The target language code.
     * @param modelName The specific model version (e.g., gemma-4-26b).
     * @return A SHA-256 hash.
     */
    public static String generateIdempotencyKey(String text, String targetLang, String modelName) {
        if (text == null) return "";
        // Including the modelName ensures we auto-retranslate if the user upgrades the model version.
        String salt = String.format("%s:%s:%s", targetLang, modelName, text);
        return DigestUtils.sha256Hex(salt.getBytes(StandardCharsets.UTF_8));
    }
}
