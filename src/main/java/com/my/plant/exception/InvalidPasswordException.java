package com.my.plant.exception;

/**
 * Exception thrown when a password does not meet the security requirements.
 */
public class InvalidPasswordException extends RegistrationException {
    
    public InvalidPasswordException(String reason) {
        super("Invalid password: " + reason);
    }
}
