package com.liv.cryptomodule.dto;

public class JWTDTO {

    private String jwt;

    private JWTDTO() {
    }

    public JWTDTO(String jwt) {
        this();
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }
}
