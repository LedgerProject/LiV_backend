package com.liv.cryptomodule.dto;

public class UserRegistrationDTO {

    private String firstName;
    private String lastName;
    private String password;
    private String email;
    private String roleId;

    public UserRegistrationDTO() {}

    public UserRegistrationDTO(String firstName, String lastName, String password, String email, String roleId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.email = email;
        this.roleId = roleId;
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

    public String getRoleId() {
        return roleId;
    }
}
