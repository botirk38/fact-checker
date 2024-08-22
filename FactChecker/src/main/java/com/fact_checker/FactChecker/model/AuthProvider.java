package com.fact_checker.FactChecker.model;

/**
 * Enumeration representing the authentication providers supported by the FactChecker application.
 * This enum is used to specify the method of authentication for user accounts.
 */
public enum AuthProvider {
    /**
     * Represents local authentication, where user credentials are managed within the application.
     */
    LOCAL,

    /**
     * Represents authentication using Google's OAuth 2.0 service.
     * This allows users to sign in using their Google accounts.
     */
    GOOGLE
}
