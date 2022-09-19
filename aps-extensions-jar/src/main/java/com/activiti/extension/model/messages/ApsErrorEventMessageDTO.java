package com.activiti.extension.model.messages;


import lombok.Data;

@Data
public class ApsErrorEventMessageDTO extends ApsProcessEventMessageDTO {

    private final String errorMessage;
    private final String jobId;

    public ApsErrorEventMessageDTO(String eventType, String errorMessage, String processInstanceId, String jobId, String dossierId) {
        super(eventType, processInstanceId, dossierId);
        this.errorMessage = errorMessage;
        this.jobId = jobId;
    }
}
