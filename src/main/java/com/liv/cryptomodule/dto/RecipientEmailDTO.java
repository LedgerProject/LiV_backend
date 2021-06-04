package com.liv.cryptomodule.dto;

public class RecipientEmailDTO {
    private String recipientFirstName;
    private String recipientLastName;

    public RecipientEmailDTO() {
    }

    public RecipientEmailDTO(String recipientFirstName, String recipientLastName) {
        this.recipientFirstName = recipientFirstName;
        this.recipientLastName = recipientLastName;
    }

    public String getRecipientFirstName() {
        return recipientFirstName;
    }

    public void setRecipientFirstName(String recipientFirstName) {
        this.recipientFirstName = recipientFirstName;
    }

    public String getRecipientLastName() {
        return recipientLastName;
    }

    public void setRecipientLastName(String recipientLastName) {
        this.recipientLastName = recipientLastName;
    }
}
