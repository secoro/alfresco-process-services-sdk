package com.activiti.extension.bean.utils.literal;

/**
 * @author Incentro
 * Class containing regex patterns to be used in APS
 */

public class RegexConstants {

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    private RegexConstants() {
        throw new UnsupportedOperationException("This class is not meant to be instantiated");
    }
}
