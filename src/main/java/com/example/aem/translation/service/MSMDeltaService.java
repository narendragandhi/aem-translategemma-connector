package com.example.aem.translation.service;

import org.apache.sling.api.resource.Resource;

/**
 * Bead for detecting if a Live Copy resource actually needs re-translation.
 */
public interface MSMDeltaService {
    
    /**
     * Checks if the resource has been modified since its last translation, 
     * respecting MSM rollout states.
     * 
     * @return true if translation is required, false if it can be skipped.
     */
    boolean isTranslationRequired(Resource resource, String targetLanguage);

    /**
     * Marks a resource as translated to update its delta state.
     */
    void markAsTranslated(Resource resource, String targetLanguage);
}
