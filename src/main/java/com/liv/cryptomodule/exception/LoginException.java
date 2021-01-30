package com.liv.cryptomodule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
public class LoginException extends Exception{
    public LoginException(String message) {
        super(message);
    }
}
