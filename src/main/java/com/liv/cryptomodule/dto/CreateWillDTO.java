package com.liv.cryptomodule.dto;

public class CreateWillDTO {

    private String email;
    private KYC kyc;
    private String document;

    public CreateWillDTO() {}

    public CreateWillDTO(String email, KYC kyc, String document) {
        this.email = email;
        this.kyc = kyc;
        this.document = document;
    }

    public String getEmail() {
        return email;
    }

    public KYC getKyc() {
        return kyc;
    }

    public String getDocument() {
        return document;
    }
}
