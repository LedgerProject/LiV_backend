package com.liv.cryptomodule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IpsfResponse {

    @JsonProperty("Name")
    private String name;
    @JsonProperty("Hash")
    private String hash;
    @JsonProperty("Size")
    private String size;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}
