package com.example.aem.translation.exception;

public class TranslateGemmaException extends Exception {
    private final ErrorType errorType;
    private final String userMessage;

    public enum ErrorType {
        TRANSLATION_FAILED,
        SERVICE_UNAVAILABLE,
        CIRCUIT_BREAKER_OPEN,
        RATE_LIMIT_EXCEEDED,
        INVALID_CONFIGURATION,
        CREDENTIALS_ERROR,
        TIMEOUT,
        NETWORK_ERROR,
        UNKNOWN
    }

    public TranslateGemmaException(String message) {
        super(message);
        this.errorType = ErrorType.UNKNOWN;
        this.userMessage = message;
    }

    public TranslateGemmaException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.UNKNOWN;
        this.userMessage = message;
    }

    public TranslateGemmaException(ErrorType errorType, String message, String userMessage) {
        super(message);
        this.errorType = errorType;
        this.userMessage = userMessage;
    }

    public TranslateGemmaException(ErrorType errorType, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.userMessage = userMessage;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public static TranslateGemmaException translationFailed(String details) {
        return new TranslateGemmaException(
            ErrorType.TRANSLATION_FAILED,
            "Translation failed: " + details,
            "Translation service is temporarily unable to process your request. Please try again later."
        );
    }

    public static TranslateGemmaException serviceUnavailable() {
        return new TranslateGemmaException(
            ErrorType.SERVICE_UNAVAILABLE,
            "TranslateGemma service is not available",
            "Translation service is currently unavailable. Please contact your administrator."
        );
    }

    public static TranslateGemmaException circuitBreakerOpen() {
        return new TranslateGemmaException(
            ErrorType.CIRCUIT_BREAKER_OPEN,
            "Circuit breaker is open - too many failures",
            "Translation service is experiencing high error rates. Please try again later."
        );
    }

    public static TranslateGemmaException rateLimitExceeded() {
        return new TranslateGemmaException(
            ErrorType.RATE_LIMIT_EXCEEDED,
            "Rate limit exceeded",
            "Too many translation requests. Please wait a moment and try again."
        );
    }

    public static TranslateGemmaException invalidConfiguration(String details) {
        return new TranslateGemmaException(
            ErrorType.INVALID_CONFIGURATION,
            "Invalid configuration: " + details,
            "Translation service is misconfigured. Please contact your administrator."
        );
    }

    public static TranslateGemmaException credentialsError(String details) {
        return new TranslateGemmaException(
            ErrorType.CREDENTIALS_ERROR,
            "Credentials error: " + details,
            "Unable to authenticate with translation service. Please verify credentials."
        );
    }

    public static TranslateGemmaException timeout(String details) {
        return new TranslateGemmaException(
            ErrorType.TIMEOUT,
            "Request timeout: " + details,
            "Translation request timed out. Please try again."
        );
    }

    public static TranslateGemmaException networkError(String details) {
        return new TranslateGemmaException(
            ErrorType.NETWORK_ERROR,
            "Network error: " + details,
            "Unable to connect to translation service. Please check your network connection."
        );
    }
}
