package com.liv.cryptomodule.dto;

public class KycDTO {

    private String firstName;
    private String middleName;
    private String lastName;
    private String passportID;
    private String address;
    private String email;

    public KycDTO() {
    }

    public KycDTO(String firstName, String middleName, String lastName, String address, String passportID, String email) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.passportID = passportID;
        this.address = address;
        this.email = email;
    }

    public KycDTO(String firstName, String middleName, String lastName, String address, String passportID) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.passportID = passportID;
        this.address = address;
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

    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
