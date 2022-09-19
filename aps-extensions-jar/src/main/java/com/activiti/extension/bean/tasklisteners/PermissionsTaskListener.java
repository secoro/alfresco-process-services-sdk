package com.activiti.extension.bean.tasklisteners;

import com.activiti.extension.bean.utils.DossierUtils;
import com.activiti.extension.bean.utils.services.AssigneeService;
import com.activiti.extension.model.Assignee;
import com.activiti.extension.model.AssigneeType;
import lombok.RequiredArgsConstructor;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class PermissionsTaskListener implements TaskListener {

    private static final Logger logger = LoggerFactory.getLogger(PermissionsTaskListener.class);
    public static final String PREVIOUS_ASSIGNEE_ID = "previousAssigneeId";
    public static final String PREVIOUS_ASSIGNEE_TYPE = "previousAssigneeType";

    private final AssigneeService assigneeService;
    private final DossierUtils dossierUtils;
    private final TaskService taskService;

    private static final String TASKPERMISSION = "taskPermission";
    private static final String DOSSIERID = "dossierId";

    /**
     * Set the permission of the assignee of the task on the dossier that is linked to the task to the proper permission level.
     *
     * @param task       The current task
     * @param permission The permission level of the assignee; READ, WRITE OR MANAGE
     * @param increase   Whether the permissions should be increased or revoked
     */
    public void updatePermissions(DelegateTask task, String permission, Boolean increase) {

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService activitiTaskService = processEngine.getTaskService();

        DelegateExecution execution = task.getExecution();
        String dossierId = (String) execution.getVariable(DOSSIERID);
        String taskId = task.getId();
        Assignee previousAssignee = new Assignee();

        // get dossier id
        if (dossierId == null) {
            throw new RuntimeException("Failed to retrieve the dossier ID");
        }

        logger.debug("Setting permissions to {} for task {} on dossier {} with increase mode {}", permission, taskId, dossierId, increase);

        // set task variable when increasing permissions
        if (increase) {
            activitiTaskService.setVariableLocal(task.getId(), TASKPERMISSION, permission);
        }

        logger.debug("Event type [{}] found", task.getEventName());
        Assignee assignee;

        switch (task.getEventName()) {
            case EVENTNAME_CREATE:
                assignee = assigneeService.getAssignee(task);
                if (assignee != null)
                    dossierUtils.updateDossierPermissions(permission, increase, dossierId, taskId, assignee, task);
                break;

            case EVENTNAME_ASSIGNMENT:
                // If this is the first time assigning, there is no previous assignee.
                if (activitiTaskService.getVariableLocal(task.getId(), PREVIOUS_ASSIGNEE_ID) != null) {

                    // If there is a previous assignee set get the information.
                    previousAssignee.setId((String) activitiTaskService.getVariableLocal(task.getId(), PREVIOUS_ASSIGNEE_ID));
                    previousAssignee.setType((AssigneeType) activitiTaskService.getVariableLocal(task.getId(), PREVIOUS_ASSIGNEE_TYPE));

                    logger.debug("The previous assignee = {}", previousAssignee.getId());
                    // Remove Write permissions for the previous assignee.
                    dossierUtils.updateDossierPermissions("WRITE", false, dossierId, taskId, previousAssignee, task);
                }
                assignee = assigneeService.getAssignee(task);
                if (assignee != null)
                    dossierUtils.updateDossierPermissions(permission, increase, dossierId, taskId, assignee, task);
                break;

            case EVENTNAME_DELETE:
                task.removeVariableLocal(PREVIOUS_ASSIGNEE_ID);
                task.removeVariableLocal(PREVIOUS_ASSIGNEE_TYPE);

                List<Assignee> assignees = assigneeService.getAssignees(task);
                assignees.forEach(currAssignee -> dossierUtils.updateDossierPermissions(permission, increase, dossierId, taskId, currAssignee, task));

                break;

            default:
                logger.debug("Event type [{}] not recognized", task.getEventName());
        }
    }

    public void updatePermissions(Task task, String eventName, String permission, boolean increase) {
        String dossierId = taskService.getVariable(task.getId(), DOSSIERID, String.class);
        Assignee previousAssignee = new Assignee();

        // get dossier id
        if (dossierId == null) {
            throw new RuntimeException("Failed to retrieve the dossier ID");
        }

        logger.debug("Setting permissions to {} for task {} on dossier {} with increase mode {}", permission, task.getId(), dossierId, increase);

        // set task variable when increasing permissions
        if (increase) {
            taskService.setVariableLocal(task.getId(), TASKPERMISSION, permission);
        }

        logger.debug("Event type [{}] found", eventName);
        Assignee assignee;

        switch (eventName) {
            case EVENTNAME_CREATE:
                assignee = assigneeService.getAssignee(task);
                if (assignee != null)
                    dossierUtils.updateDossierPermissions(permission, increase, dossierId, task.getId(), assignee, task, eventName);
                break;

            case EVENTNAME_ASSIGNMENT:
                // If this is the first time assigning, there is no previous assignee.
                if (taskService.getVariableLocal(task.getId(), PREVIOUS_ASSIGNEE_ID) != null) {

                    // If there is a previous assignee set get the information.
                    previousAssignee.setId((String) taskService.getVariableLocal(task.getId(), PREVIOUS_ASSIGNEE_ID));
                    previousAssignee.setType((AssigneeType) taskService.getVariableLocal(task.getId(), PREVIOUS_ASSIGNEE_TYPE));

                    logger.debug("The previous assignee = {}", previousAssignee.getId());
                    // Remove Write permissions for the previous assignee.
                    dossierUtils.updateDossierPermissions("WRITE", false, dossierId, task.getId(), previousAssignee, task, eventName);
                }
                assignee = assigneeService.getAssignee(task);
                if (assignee != null)
                    dossierUtils.updateDossierPermissions(permission, increase, dossierId, task.getId(), assignee, task, eventName);
                break;

            case EVENTNAME_DELETE:
                taskService.removeVariableLocal(task.getId(), PREVIOUS_ASSIGNEE_ID);
                taskService.removeVariableLocal(task.getId(), PREVIOUS_ASSIGNEE_TYPE);

                List<Assignee> assignees = assigneeService.getAssignees(task);
                assignees.forEach(currAssignee -> dossierUtils.updateDossierPermissions(permission, increase, dossierId, task.getId(), currAssignee, task, eventName));

                break;

            default:
                logger.debug("Event type [{}] not recognized", eventName);
        }
    }

    @Override
    public void notify(DelegateTask task) {

    }

}
