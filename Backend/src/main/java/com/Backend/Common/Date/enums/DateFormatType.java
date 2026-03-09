package com.Backend.Common.Date.enums;


public enum DateFormatType {

    DATE_FORMAT_TYPE_DD_MM_YYYYY("dd-MM-yyyy");

    private final String dateFormat;

    private DateFormatType(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getFormat() {
        return dateFormat;
    }
}
