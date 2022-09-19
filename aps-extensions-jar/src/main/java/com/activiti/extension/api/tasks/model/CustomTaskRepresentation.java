package com.activiti.extension.api.tasks.model;

import com.activiti.model.editor.form.FormFieldRepresentation;
import com.activiti.model.runtime.TaskRepresentation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.task.Task;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class CustomTaskRepresentation extends TaskRepresentation {
    private Map<String, Object> processVariables;
    private Map<String, Object> taskLocalVariables;
    private boolean claimable;
    private String deleteReason;
    private String assigneeId;
    private boolean isSuspended;
    private boolean completed;
    private Map<String, FormFieldRepresentation> taskSubmittedFormFields;

    public CustomTaskRepresentation(Task task) {
        this.id = task.getId();
        this.assigneeId = task.getAssignee();
        this.name = task.getName();
        this.category = task.getCategory();
        this.description = task.getDescription();
        this.created = task.getCreateTime();
        this.dueDate = task.getDueDate();
        this.executionId = task.getExecutionId();
        this.formKey = task.getFormKey();
        this.parentTaskId = task.getParentTaskId();
        this.priority = task.getPriority();
        this.processDefinitionId = task.getProcessDefinitionId();
        this.processInstanceId = task.getProcessInstanceId();
        this.processVariables = task.getProcessVariables();
        this.taskLocalVariables = task.getTaskLocalVariables();
    }

    public CustomTaskRepresentation(HistoricProcessInstance historicProcessInstance) {
        this.processVariables = historicProcessInstance.getProcessVariables();
    }

    public CustomTaskRepresentation(HistoricTaskInstance historicTaskInstance) {
        this.id = historicTaskInstance.getId();
        this.name = historicTaskInstance.getName();
        this.description = historicTaskInstance.getDescription();
        this.category = historicTaskInstance.getCategory();
        this.assigneeId = historicTaskInstance.getAssignee();
        this.created = historicTaskInstance.getCreateTime();
        this.dueDate = historicTaskInstance.getDueDate();
        this.endDate = historicTaskInstance.getEndTime();
        this.duration = historicTaskInstance.getDurationInMillis();
        this.priority = historicTaskInstance.getPriority();
        this.parentTaskId = historicTaskInstance.getParentTaskId();
        this.processDefinitionId = historicTaskInstance.getProcessDefinitionId();
        this.formKey = historicTaskInstance.getFormKey();
        this.taskDefinitionKey = historicTaskInstance.getTaskDefinitionKey();
        this.executionId = historicTaskInstance.getExecutionId();
        this.processInstanceId = historicTaskInstance.getProcessInstanceId();
        this.deleteReason = historicTaskInstance.getDeleteReason();
        this.processVariables = mergeVariables(historicTaskInstance);
    }

    /**
     * Helper method that merges the process variables and the task local variables.
     *
     * @param historicTaskInstance The task of which the variables should be merged.
     * @return Map<String, Object> of both process variables and task local variables.
     */
    public Map<String, Object> mergeVariables(HistoricTaskInstance historicTaskInstance) {
        Map<String, Object> mergedMaps = new HashMap<>();
        mergedMaps.putAll(historicTaskInstance.getProcessVariables());
        mergedMaps.putAll(historicTaskInstance.getTaskLocalVariables());

        return mergedMaps;
    }
}
