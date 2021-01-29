package com.liv.cryptomodule.dto;

import java.math.BigInteger;

public class Salt {

    private BigInteger salt;
    private String saltedPassword;

    public Salt(BigInteger salt, String saltedPassword) {
        this.salt = salt;
        this.saltedPassword = saltedPassword;
    }

    public BigInteger getSalt() {
        return salt;
    }

    public String getSaltedPassword() {
        return saltedPassword;
    }
}
