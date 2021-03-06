package com.liv.cryptomodule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class InvalidRoleIdException extends Exception {
    public InvalidRoleIdException(String message) {
        super(message);
    }

    public InvalidRoleIdException(String message, Throwable cause) {
        super(message, cause);
    }
}