package com.example.aem.translation.service;

import com.example.aem.translation.model.TranslationFeedback;
import java.util.List;

/**
 * Service for managing human-in-the-loop (HITL) feedback to improve translation quality.
 */
public interface TranslationFeedbackService {

    /**
     * Records a human correction for a machine-translated string.
     */
    void recordFeedback(String sourceString, String originalTranslation, String humanCorrection, 
                        String sourceLang, String targetLang, String userId);

    /**
     * Gets relevant feedback for a given source string to use in few-shot prompts.
     * In this implementation, it retrieves the most recent corrections for the language pair.
     */
    List<TranslationFeedback> getRelevantFeedback(String sourceString, String sourceLang, String targetLang, int limit);
}
