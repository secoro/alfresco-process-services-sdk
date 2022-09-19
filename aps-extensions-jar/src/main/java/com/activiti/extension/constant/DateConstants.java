package com.activiti.extension.constant;

import java.util.List;

public class DateConstants {

    public static final String APS_DATE_FORMAT = "E MMM dd HH:mm:ss z yyyy";
    // Wed Apr 21 2021 15:18:11 GMT+0200 (CEST)
    public static final String APS_DEADLINE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String COMMENT_DATE_FORMAT = "E MMM dd y HH:mm:ss 'GMT'Z (z)";
    // Date pattern used for the planning service
    public static final String OFFSET_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX";
    // Possible date patterns used in APS
    public static final List<String> APS_DATE_FORMATS = List.of(APS_DATE_FORMAT, COMMENT_DATE_FORMAT, APS_DEADLINE_DATE_FORMAT);

    private DateConstants() {
        throw new UnsupportedOperationException();
    }
}
