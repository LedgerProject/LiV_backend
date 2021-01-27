package com.liv.cryptomodule.dto;

public class KYC {

    private String firstName;
    private String middleName;
    private String lastName;
    private String address;
    private String passportNumber;

    public KYC() {}

    public KYC(String firstName, String middleName, String lastName, String address, String passportNumber) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.address = address;
        this.passportNumber = passportNumber;
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

    public String getAddress() {
        return address;
    }

    public String getPassportNumber() {
        return passportNumber;
    }
}
