package com.liv.cryptomodule.dto;

public class UserRegistrationDTO {

    private String firstName;
    private String lastName;
    private String password;
    private String email;

    public UserRegistrationDTO() {}

    public UserRegistrationDTO(String firstName, String lastName, String password, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}
