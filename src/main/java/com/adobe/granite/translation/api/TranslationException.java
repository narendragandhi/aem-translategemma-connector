package com.adobe.granite.translation.api;

public class TranslationException extends Exception {
    public enum ErrorCode {
        UNKNOWN,
        JOB_NOT_FOUND,
        SERVICE_UNAVAILABLE,
        TRANSLATION_FAILED,
        INVALID_CONFIGURATION,
        CREDENTIALS_ERROR,
        TIMEOUT,
        NO_ENGINE,
        SERVICE_NOT_IMPLEMENTED,
        UNKNOWN_LANGUAGE,
        NOT_SUPPORTED_LANG_DIRECTION
    }

    private ErrorCode errorCode;

    public TranslationException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TranslationException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
