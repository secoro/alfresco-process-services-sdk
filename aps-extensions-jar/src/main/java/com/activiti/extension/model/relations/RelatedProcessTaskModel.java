package com.activiti.extension.model.relations;


import com.activiti.extension.model.TaskStatus;
import com.activiti.model.idm.LightGroupRepresentation;
import com.activiti.model.idm.LightUserRepresentation;
import lombok.Data;

import java.util.Map;

/**
 * @author Incentro.
 * Class representing the model of a task that is related to a process that is related to a dossier
 */
@Data
public class RelatedProcessTaskModel {

    private String name;
    private String taskId;
    private LightUserRepresentation assignee;
    private LightGroupRepresentation involvedGroup;
    private String groupDisplayName;
    private String created;
    private String dueDate;
    private String endDate;
    private String processInstanceId;
    private TaskStatus taskStatus;
    private Map<String, Object> variables;
    private Map<String, Object> formValues;
    private String assigneeOrCandidateGroupName;
}
