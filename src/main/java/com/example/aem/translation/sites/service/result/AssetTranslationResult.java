package com.example.aem.translation.sites.service.result;

import java.util.Map;

public class AssetTranslationResult {
    private final String assetPath;
    private final String mimeType;
    private final Map<String, String> translatedMetadata;
    private final boolean success;
    private final String errorMessage;

    public AssetTranslationResult(String assetPath, String mimeType,
                                Map<String, String> translatedMetadata) {
        this.assetPath = assetPath;
        this.mimeType = mimeType;
        this.translatedMetadata = translatedMetadata;
        this.success = true;
        this.errorMessage = null;
    }

    public AssetTranslationResult(String assetPath, String mimeType, String errorMessage) {
        this.assetPath = assetPath;
        this.mimeType = mimeType;
        this.translatedMetadata = null;
        this.success = false;
        this.errorMessage = errorMessage;
    }

    public String getAssetPath() { return assetPath; }
    public String getMimeType() { return mimeType; }
    public Map<String, String> getTranslatedMetadata() { return translatedMetadata; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}