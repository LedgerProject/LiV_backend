package com.liv.cryptomodule.dto;

public class UserServicesDTO {

    private String email;

    private UserServicesDTO() {
    }

    public UserServicesDTO(String email) {
        this();
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
