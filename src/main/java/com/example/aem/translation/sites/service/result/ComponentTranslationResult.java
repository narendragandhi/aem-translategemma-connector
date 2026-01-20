package com.example.aem.translation.sites.service.result;

import java.util.Map;

public class ComponentTranslationResult {
    private final String componentPath;
    private final String componentType;
    private final Map<String, String> translatedProperties;
    private final boolean success;
    private final String errorMessage;

    public ComponentTranslationResult(String componentPath, String componentType,
                                   Map<String, String> translatedProperties) {
        this.componentPath = componentPath;
        this.componentType = componentType;
        this.translatedProperties = translatedProperties;
        this.success = true;
        this.errorMessage = null;
    }

    public ComponentTranslationResult(String componentPath, String componentType, String errorMessage) {
        this.componentPath = componentPath;
        this.componentType = componentType;
        this.translatedProperties = null;
        this.success = false;
        this.errorMessage = errorMessage;
    }

    public String getComponentPath() { return componentPath; }
    public String getComponentType() { return componentType; }
    public Map<String, String> getTranslatedProperties() { return translatedProperties; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}