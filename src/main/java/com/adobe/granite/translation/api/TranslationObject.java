package com.adobe.granite.translation.api;

import java.io.InputStream;

public interface TranslationObject {
    String getPath();
    TranslationConstants.ContentType getContentType();
    Object getContent();
    String getVersion();
    String getTitle();
}
