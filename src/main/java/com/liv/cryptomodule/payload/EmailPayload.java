package com.liv.cryptomodule.payload;

public class EmailPayload {
    private String subject;
    private String recipientEmailAddress;
    private String recipientFirstName;
    private String recipientLastName;
    private String documentLink;

    public EmailPayload(String subject, String recipientEmailAddress, String recipientFirstName, String recipientLastName, String documentLink) {
        this.subject = subject;
        this.recipientEmailAddress = recipientEmailAddress;
        this.recipientFirstName = recipientFirstName;
        this.recipientLastName = recipientLastName;
        this.documentLink = documentLink;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRecipientEmailAddress() {
        return recipientEmailAddress;
    }

    public void setRecipientEmailAddress(String recipientEmailAddress) {
        this.recipientEmailAddress = recipientEmailAddress;
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

    public String getDocumentLink() {
        return documentLink;
    }

    public void setDocumentLink(String documentLink) {
        this.documentLink = documentLink;
    }
}
