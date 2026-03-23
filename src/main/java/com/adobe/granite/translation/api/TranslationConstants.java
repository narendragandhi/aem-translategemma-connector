package com.adobe.granite.translation.api;

public interface TranslationConstants {
    enum TranslationMethod {
        MACHINE,
        HUMAN,
        HYBRID,
        MACHINE_TRANSLATION
    }
    
    enum ContentType {
        PLAIN,
        HTML,
        MARKDOWN
    }
    
    enum TranslationStatus {
        DRAFT,
        SUBMITTED,
        IN_PROGRESS,
        TRANSLATION_IN_PROGRESS,
        TRANSLATED,
        ERROR,
        ERROR_UPDATE,
        COMPLETED,
        COMPLETE,
        CANCELLED,
        CANCEL,
        DELETED,
        UNKNOWN_STATE,
        READY_FOR_REVIEW,
        PUBLISHED,
        COMMITTED_FOR_TRANSLATION;
        
        public static TranslationStatus fromString(String value) {
            for (TranslationStatus status : TranslationStatus.values()) {
                if (status.name().equalsIgnoreCase(value)) {
                    return status;
                }
            }
            return UNKNOWN_STATE;
        }
    }
}
