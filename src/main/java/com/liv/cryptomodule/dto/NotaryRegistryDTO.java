package com.liv.cryptomodule.dto;

public class NotaryRegistryDTO {

    private String firstName;
    private String lastName;
    private String password;
    private String email;
    private String pubKey;
    private String roleId; // 1 means notary, 2 means registry

    public NotaryRegistryDTO() {
    }

    public NotaryRegistryDTO(String firstName, String lastName, String password, String email, String pubKey, String roleId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.email = email;
        this.pubKey = pubKey;
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

    public String getPubKey() {
        return pubKey;
    }

    public String getRoleId() {
        return roleId;
    }
}
