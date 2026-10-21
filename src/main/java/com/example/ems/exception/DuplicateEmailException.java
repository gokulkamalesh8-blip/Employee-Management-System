package com.example.ems.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email, Throwable cause) {
        super("An employee with email '" + email + "' already exists.", cause);
    }
    public DuplicateEmailException(String email) { this(email, null); }
}
