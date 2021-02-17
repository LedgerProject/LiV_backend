package com.liv.cryptomodule.dto;

public class CreateWillDTO {

    private String senderId;
    private String recipientEmail;

    public CreateWillDTO() {
    }

    public CreateWillDTO(String senderId, String recipientEmail) {
        this.senderId = senderId;
        this.recipientEmail = recipientEmail;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }
}
