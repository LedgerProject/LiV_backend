package com.liv.cryptomodule.dto;

public class WillBasicDTO {

    private String id;
    private String name;
    private String passportId;
    private String status;

    public WillBasicDTO() {
    }

    public WillBasicDTO(String id, String name, String passportId, String status) {
        this.id = id;
        this.name = name;
        this.passportId = passportId;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassportId() {
        return passportId;
    }

    public String getStatus() {
        return status;
    }
}
