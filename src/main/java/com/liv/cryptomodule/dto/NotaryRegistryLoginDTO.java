package com.liv.cryptomodule.dto;

//TODO: Make this class extend the UserLoginDTO
public class NotaryRegistryLoginDTO {

    private String email;
    private String password;
    private String roleId;

    public NotaryRegistryLoginDTO() {}

    public NotaryRegistryLoginDTO(String email, String password, String roleId) {
        this.email = email;
        this.password = password;
        this.roleId = roleId;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRoleId() {
        return roleId;
    }
}
