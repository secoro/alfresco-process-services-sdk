package com.activiti.extension.api.util;

public final class HttpStatusMessages {

    public HttpStatusMessages() {
        throw new UnsupportedOperationException("This class should not be instantiated");
    }

    // Task Submitted Forms
    public static final String COULD_NOT_RETRIEVE_TASK_SUBMITTED_FORM = "Could not retrieve the task submitted form for task [{}].";

    // Variables
    public static final String BULK_UPDATE_VARIABLES_SUCCESSFULL = "Bulk update variables successful for dossiers [%s]";

    // Internal Server error
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "Something went wrong";
}
