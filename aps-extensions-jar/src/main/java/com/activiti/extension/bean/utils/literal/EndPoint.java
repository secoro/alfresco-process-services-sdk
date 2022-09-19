package com.activiti.extension.bean.utils.literal;

public class EndPoint {

    /**
     * ENDPOINTS
     */
    public static final String ACS = "ACS";
    public static final String APS = "APS";
    public static final String DIVA_SERVICES = "DIVA Services";
    public static final String ICM = "ICM";


    /**
     * EVENT SERVICE URIs
     */
    public static final String EVENTS_MESSAGES = "/diva-bb-event/events/messages";

    /**
     * ICM URIs
     */
    // Process-relations
    public static final String ICM_PROCESS_RELATIONS = "/process-relations/%s/tasks?relationTypes=%s";

    // Groups
    public static final String ICM_GROUPS_GROUP = "/groups/%s";
    
    // Dossier
    public static final String ICM_DOSSIER = "/dossier/%s";
    public static final String ICM_DOSSIER_TERMINATE = "/dossier/%s/terminate";
    public static final String ICM_APPROVALS = "/dossier/%s/approvals";
    public static final String ICM_DOSSIER_PERMISSIONS = "/dossier/%s/permissions";
    
}
