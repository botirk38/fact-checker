package com.fact_checker.FactChecker.exceptions;

/**
 * Exception thrown when an attempt is made to register a user with a username or email
 * that already exists in the system.
 */
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new UserAlreadyExistsException with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method)
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
