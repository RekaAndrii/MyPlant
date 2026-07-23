package com.my.plant.exception;

/**
 * Exception thrown when attempting to register with an email that already exists in the database.
 */
public class DuplicateEmailException extends RegistrationException {
    
    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
