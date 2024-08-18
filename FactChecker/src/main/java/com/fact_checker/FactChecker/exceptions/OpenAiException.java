package com.fact_checker.FactChecker.exceptions;

/**
 * Custom exception for OpenAI API errors.
 */


public  class OpenAiException extends RuntimeException {
    public OpenAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
