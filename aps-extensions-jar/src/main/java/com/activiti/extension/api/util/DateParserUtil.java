package com.activiti.extension.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static com.activiti.extension.constant.DateConstants.APS_DATE_FORMATS;

@Slf4j
@Component
public class DateParserUtil {

    public Date convertObjectToDate(Object dateObject) {
        if (Objects.isNull(dateObject)) {
            return null;
        }
        if (dateObject instanceof Date) {
            return (Date) dateObject;
        } else if (dateObject instanceof Instant) {
            return Date.from((Instant) dateObject);
        } else if (dateObject instanceof String) {

            for (String formatString : APS_DATE_FORMATS) {
                try {
                    return new SimpleDateFormat(formatString, Locale.ENGLISH).parse((String) dateObject);
                } catch (ParseException e) {
                    log.debug("Date String does not match pattern [{}]", formatString);
                }
            }
        }

        log.debug("Data type for Date Object could not be inferred for dateObject [{}]", dateObject);
        return null;
    }
}
