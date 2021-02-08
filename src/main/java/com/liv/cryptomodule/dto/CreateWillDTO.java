package com.liv.cryptomodule.dto;

public class CreateWillDTO {

    private String email;
    private KYC kyc;

    public CreateWillDTO() {}

    public CreateWillDTO(String email, KYC kyc) {
        this.email = email;
        this.kyc = kyc;
    }

    public String getEmail() {
        return email;
    }

    public KYC getKyc() {
        return kyc;
    }

}
