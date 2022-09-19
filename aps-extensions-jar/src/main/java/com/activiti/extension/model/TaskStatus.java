package com.activiti.extension.model;

/**
 * @author Incentro.
 * <p>
 * Enum representing the filter for different task statuses.
 */
public enum TaskStatus {
    ACTIVE("active"), SUSPENDED("suspended"), COMPLETED("completed");

    private final String status;

    TaskStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static TaskStatus fromText(String text) {
        for (TaskStatus t : TaskStatus.values()) {
            if (t.getStatus().equals(text)) {
                return t;
            }
        }
        throw new IllegalArgumentException();
    }
}
