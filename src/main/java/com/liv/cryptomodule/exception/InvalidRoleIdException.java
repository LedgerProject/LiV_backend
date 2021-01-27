package com.liv.cryptomodule.exception;

public class InvalidRoleIdException extends RuntimeException {
    public InvalidRoleIdException(String message) {
        super(message);
    }

    public InvalidRoleIdException(String message, Throwable cause) {
        super(message, cause);
    }
}