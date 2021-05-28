package com.liv.cryptomodule.dto;

public class KycDTO {

    private String firstName;
    private String lastName;
    private String secondName;
    private String address;
    //TODO: Add validation for NIF to remove extra symbols
    private String nif;
    private String birthday;
    private String email;

    public KycDTO() {
    }

    public KycDTO(String firstName, String lastName, String secondName, String address, String nif, String birthday, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.secondName = secondName;
        this.address = address;
        this.nif = nif;
        this.birthday = birthday;
        this.email = email;
    }

    public KycDTO(String firstName, String lastName, String secondName, String address, String nif, String birthday) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.secondName = secondName;
        this.address = address;
        this.nif = nif;
        this.birthday = birthday;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "KycDTO{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", secondName='" + secondName + '\'' +
                ", address='" + address + '\'' +
                ", nif='" + nif + '\'' +
                ", birthday='" + birthday + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
