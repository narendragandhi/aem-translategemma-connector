package com.adobe.granite.translation.api;

public interface TranslationResult {
    String getSourceLanguage();
    String getTargetLanguage();
    String getSourceString();
    String getTranslation();
    TranslationConstants.ContentType getContentType();
    String getCategory();
    int getRating();
    String getUserId();
}
