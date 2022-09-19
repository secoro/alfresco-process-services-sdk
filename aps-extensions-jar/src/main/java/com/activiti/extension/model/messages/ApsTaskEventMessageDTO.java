package com.activiti.extension.model.messages;


import lombok.Data;

@Data
public class ApsTaskEventMessageDTO extends ApsProcessEventMessageDTO {

    private final String taskId;
    private final String taskName;


    public ApsTaskEventMessageDTO(String eventType, String id, String name, String processInstanceId, String dossierId) {
        super(eventType, processInstanceId, dossierId);
        this.taskId = id;
        this.taskName = name;
    }
}
