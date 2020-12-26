package com.liv.cryptomodule.dto;

public class KycDTO {

    private String firstName;
    private String middleName;
    private String lastName;
    private String passportID;
    private String email;

    public KycDTO() {}

    public KycDTO(String firstName, String middleName, String lastName, String passportID, String email) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.passportID = passportID;
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassportID() {
        return passportID;
    }

    public String getEmail() {
        return email;
    }

}
