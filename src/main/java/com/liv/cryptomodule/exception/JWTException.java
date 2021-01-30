package com.liv.cryptomodule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class JWTException extends Exception {
    public JWTException(String message) {
        super(message);
    }
}
