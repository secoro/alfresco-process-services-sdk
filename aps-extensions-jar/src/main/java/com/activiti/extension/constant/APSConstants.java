package com.activiti.extension.constant;

public class APSConstants {

    public enum Process {
        MAIN, DELAYREQUEST, ADHOC, REASSIGN, TERMINATE_DOSSIER, SHARE_PORTEFEUILLEHOUDER
    }

    /**
     * Query
     */
    public static final String QUERY_CONTAINS = "%%%s%%";

    /**
     * Urgency
     */
    public static final String URGENCY_EMAIL_PREFIX = "--SPOED-- ";

    /**
     * Common APS variables
     */
    public static final String DOSSIER_ID = "dossierId";
    public static final String PROCESS_TYPE = "processType";
    public static final String DEADLINE = "deadline";
    public static final String DOSSIER_TYPE = "dossierType";
    public static final String OWNER = "owner";
    public static final String SUBJECT = "subject";
    public static final String START_DATE = "startdate";
    public static final String STELLER = "steller";
    public static final String STELLER_FULLNAME = "stellerFullName";
    public static final String STELLER_EMAIL = "stellerEmail";
    public static final String IS_ASSIGNEE = "isAssignee";
    public static final String IS_URGENT = "isUrgent";

    // Delayrequest
    public static final String STELLER_TUSSENBERICHT = "steller_tussenbericht";
    public static final String STELLER_TUSSENBERICHT_FULLNAME = "stellerTussenberichtFullName";
}
