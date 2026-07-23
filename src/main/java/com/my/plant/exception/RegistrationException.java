package com.my.plant.exception;

/**
 * Base exception for user registration-related errors.
 */
public class RegistrationException extends RuntimeException {
    
    public RegistrationException(String message) {
        super(message);
    }
    
    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
