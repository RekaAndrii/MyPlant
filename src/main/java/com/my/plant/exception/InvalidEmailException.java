package com.my.plant.exception;

/**
 * Exception thrown when an email does not match the required format.
 */
public class InvalidEmailException extends RegistrationException {
    
    public InvalidEmailException(String email) {
        super("Invalid email format: " + email);
    }
}
