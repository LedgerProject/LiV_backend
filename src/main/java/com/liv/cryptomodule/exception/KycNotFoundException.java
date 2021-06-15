package com.liv.cryptomodule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class KycNotFoundException extends Exception{
    public KycNotFoundException(String message) {
        super(message);
    }
}
