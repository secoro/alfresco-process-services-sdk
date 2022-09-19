package com.activiti.extension.api.dossier.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LightTaskRepresentation {
    private String taskId;
    private String taskName;
    private String assignment;
    private String processName;
}
