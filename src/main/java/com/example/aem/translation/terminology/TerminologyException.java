package com.example.aem.translation.terminology;

public class TerminologyException extends Exception {
    private final ErrorCode errorCode;

    public enum ErrorCode {
        TERM_NOT_FOUND,
        IMPORT_FAILED,
        EXPORT_FAILED,
        INVALID_FORMAT,
        STORAGE_ERROR
    }

    public TerminologyException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
