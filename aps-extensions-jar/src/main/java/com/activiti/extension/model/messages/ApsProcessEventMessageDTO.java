package com.activiti.extension.model.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApsProcessEventMessageDTO {
    private String eventType;
    private String processInstanceId;
    private String dossierId;

}
