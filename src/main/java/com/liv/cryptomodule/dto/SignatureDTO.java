package com.liv.cryptomodule.dto;

import java.security.PublicKey;
import java.util.Base64;

public class SignatureDTO {
    private byte[] messageHash;
    private byte[] signatureValue;
    private PublicKey pk;

    public SignatureDTO() {}

    public SignatureDTO(byte[] signatureValue, byte[] messageHash, PublicKey pk) {
        this.signatureValue = signatureValue;
        this.messageHash = messageHash;
        this.pk = pk;
    }
    public String toString() {
        return getSignatureValue() + "\n" + getMessageHash() + "\n" + getPK();
    }
    public String getSignatureValue() {
        String signatureEncoded = Base64.getEncoder().encodeToString(this.signatureValue);
        return signatureEncoded;
    }
    public String getMessageHash() {
        String hashEncoded = Base64.getEncoder().encodeToString(this.messageHash);
        return hashEncoded;
    }
    public String getPK() {
        String pkString = this.pk.toString();

        String p = pkString.substring(42, 649);
        String q = pkString.substring(657, 723);
        String g = pkString.substring(731, 1338);
        String y = pkString.substring(1345, 1953);
        pkString = p + "\n" + q +"\n" + g + "\n" + y;

        String pkEnc = Base64.getEncoder().encodeToString(pkString.getBytes());
        return pkEnc;
    }
}