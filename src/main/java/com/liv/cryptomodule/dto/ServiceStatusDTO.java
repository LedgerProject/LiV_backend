package com.liv.cryptomodule.dto;

public class ServiceStatusDTO {

    private int statusId;
    private String userEmail;
    private String institution;
    private String service;
    private String status;

    public ServiceStatusDTO() {}

    public ServiceStatusDTO(String userEmail, String institution, String service, String status) {
        this.userEmail = userEmail;
        this.institution = institution;
        this.service = service;
        this.status = status;
    }

    public ServiceStatusDTO(int statusId, String institution, String service, String status) {
        this.statusId = statusId;
        this.institution = institution;
        this.service = service;
        this.status = status;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getInstitution() {
        return institution;
    }

    public String getService() {
        return service;
    }

    public String getStatus() {
        return status;
    }
}
