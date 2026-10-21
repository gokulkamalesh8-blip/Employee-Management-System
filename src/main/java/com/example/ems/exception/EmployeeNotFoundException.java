package com.example.ems.exception;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(long id) {
        super("No employee found with ID " + id + ".");
    }
}
