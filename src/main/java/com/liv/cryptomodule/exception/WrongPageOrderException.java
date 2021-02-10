package com.liv.cryptomodule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class WrongPageOrderException extends Exception {
    public WrongPageOrderException(String message) {
        super(message);
    }

    public WrongPageOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
