package com.Backend.Common.Date.Service;

import com.Backend.Common.Date.enums.DateFormatType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Objects;

public final class DateService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DateFormatType.DATE_FORMAT_TYPE_DD_MM_YYYYY.getFormat());

    private DateService() {
    }

    public static String dateToString_ddMMyyyy(Date date) {
        Objects.requireNonNull(date, "date cannot be null");
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DATE_FORMATTER);
    }

    public static Date stringToDate_ddMMyyyy(String date) {
        try {
            Objects.requireNonNull(date, "date cannot be null");
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid date format. Expected dd-MM-yyyy", exception);
        }
    }
}
