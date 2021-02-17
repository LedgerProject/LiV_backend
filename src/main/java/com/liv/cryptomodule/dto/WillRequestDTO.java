package com.liv.cryptomodule.dto;

public class WillRequestDTO {

    String id;
    String userId;
    String recipientId;
    String firstName;
    String middleName;
    String lastName;
    String passportId;
    String statusId;
    String did;
    String email;
    String address;
    String documentHash;
    String documentLink;

    public WillRequestDTO() {
    }

    public WillRequestDTO(String id, String userId, String recipientId, String firstName, String middleName, String lastName, String passportId, String statusId, String did, String email, String address, String documentHash, String documentLink) {
        this.id = id;
        this.userId = userId;
        this.recipientId = recipientId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.passportId = passportId;
        this.statusId = statusId;
        this.did = did;
        this.email = email;
        this.address = address;
        this.documentHash = documentHash;
        this.documentLink = documentLink;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassportId() {
        return passportId;
    }

    public void setPassportId(String passportId) {
        this.passportId = passportId;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    public String getDocumentLink() {
        return documentLink;
    }

    public void setDocumentLink(String documentLink) {
        this.documentLink = documentLink;
    }
}
