package com.liv.cryptomodule.dto;

public class FilterDTO {
    private String accountId;
    private String status;
    private String recipientId;

    public FilterDTO() {
    }

    public FilterDTO(String accountId, String status, String recipientId) {
        this.accountId = accountId;
        this.status = status;
        this.recipientId = recipientId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStatus() {
        return status;
    }

    public String getRecipientId() {
        return recipientId;
    }
}
