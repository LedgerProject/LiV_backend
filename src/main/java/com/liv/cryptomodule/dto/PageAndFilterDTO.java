package com.liv.cryptomodule.dto;

public class PageAndFilterDTO {
    private PageDTO pageDto;
    private FilterDTO filterDto;

    public PageAndFilterDTO(PageDTO pageDto, FilterDTO filterDto) {
        this.pageDto = pageDto;
        this.filterDto = filterDto;
    }

    public PageDTO getPageDto() {
        return pageDto;
    }

    public FilterDTO getFilterDto() {
        return filterDto;
    }
}
