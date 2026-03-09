package com.Backend.Common.I18N;

import org.springframework.context.MessageSource;
import java.util.Locale;

public class MessageService {

    MessageSource messageSource;

    public String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, "Default message", locale);
    }
}
