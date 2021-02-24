package com.liv.cryptomodule.dto;

import javax.validation.constraints.Min;

public class PageDTO {
    private String order = "DESC";

    @Min(1)
    private Integer limit = 1;

    public PageDTO() {
        this.order = "DESC";
        this.limit = 15;
    }

    public PageDTO(String order, Integer limit) {
        this.order = order;
        this.limit = limit;
    }

    public String getOrder() {
        return order;
    }

    public Integer getLimit() {
        return limit;
    }
}
