package com.activiti.extension.api.tasks;

import com.activiti.domain.idm.Group;
import com.activiti.domain.idm.User;
import com.activiti.extension.api.tasks.model.AssignTaskToUserBody;
import com.activiti.extension.api.tasks.model.CustomTaskRepresentation;
import com.activiti.extension.bean.tasklisteners.PermissionsTaskListener;
import com.activiti.extension.bean.utils.PaginationUtil;
import com.activiti.extension.bean.utils.services.CustomEmailService;
import com.activiti.extension.constant.APSConstants;
import com.activiti.extension.model.Pagination;
import com.activiti.model.common.ResultListDataRepresentation;
import com.activiti.model.editor.form.FormFieldRepresentation;
import com.activiti.model.idm.LightGroupRepresentation;
import com.activiti.model.idm.LightUserRepresentation;
import com.activiti.model.runtime.RestVariable;
import com.activiti.model.runtime.SubmittedFormRepresentation;
import com.activiti.model.runtime.TaskRepresentation;
import com.activiti.service.api.GroupService;
import com.activiti.service.api.UserService;
import com.activiti.service.exception.ConflictingRequestException;
import com.activiti.service.exception.NotFoundException;
import com.activiti.service.runtime.RestVariableFactory;
import com.activiti.service.runtime.SubmittedFormService;
import com.amazonaws.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.activiti.extension.api.util.HttpStatusMessages.COULD_NOT_RETRIEVE_TASK_SUBMITTED_FORM;
import static com.activiti.extension.constant.APSConstants.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomTaskService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RestVariableFactory restVariableFactory;
    private final UserService userService;
    private final PaginationUtil paginationUtil;
    private final SubmittedFormService submittedFormService;
    private final GroupService groupService;
    private final TaskService taskService;
    private final CustomEmailService customEmailService;
    private final RepositoryService repositoryService;
    private final PermissionsTaskListener permissionsTaskListener;
    @Autowired
    private final ApplicationContext applicationContext;

    /**
     * Method that fetches a task by taskId.
     *
     * @param taskId the taskId
     * @return TaskRepresentation
     */
    public Optional<HistoricTaskInstance> getTaskById(String taskId) {
        log.debug("Getting task with ID '{}'", taskId);

        return historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .list()
                .stream()
                .findFirst();
    }

    /**
     * This method returns all tasks for a given user and list of clusters
     *
     * @param apsId     The APS ID of the user.
     * @param clusters  A list of clusters (example ORG_MO,ORG_DV,ORG_SB).
     * @param sort      The field to sort on (created, duedate).
     * @param ascending The order to sort.
     * @param skipCount How many tasks to skip.
     * @param maxItems  The maximum amount of tasks to return
     * @return ResultListDataRepresentation<TaskRepresentation>
     */
    public ResultListDataRepresentation<TaskRepresentation> getReassignableTasksForUserWithinCluster(Long apsId, List<String> clusters, String sort, boolean ascending, int skipCount, int maxItems) {
        log.info("Retrieving all tasks where assignee equals [{}]", apsId);
        return baseQuery(apsId, clusters, sort, ascending)
                .list()
                .stream()
                .map(this::createTaskRepresentation)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> paginationUtil.getPage(list, Pagination.getPagination(maxItems, skipCount))));
    }

    /**
     * This method retrieves all tasks for a given dossier. Other than the task it also returns the suspended state per
     * task and the submitted form fields for unfinished tasks.
     *
     * @param dossierId     The dossier for which the process relations need to be fetched.
     * @param relationTypes The relation types (i.e. MAIN,ADHOC,DELAYREQUEST,TERMINATE_DOSSIER) to include in the response.
     * @param active        Filter for (un)finished tasks.
     * @param size          The amount of tasks to return.
     * @return List<CustomTaskRepresentation> A list of tasks.
     */
    public List<CustomTaskRepresentation> getProcessRelations(String dossierId, List<String> relationTypes, boolean active, int size) {
        log.debug("Returning all process relations for dossier with following parameters: dossierId: {}, relationTypes: {}, active: {}, size: {}", dossierId, relationTypes, active, size);

        return getProcessRelationsQuery(dossierId, relationTypes, active).listPage(0, size)
                .stream()
                .map(CustomTaskRepresentation::new)
                .filter(this::filterOutDeletedTasks)
                .map(customTaskRepresentation -> {
                    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(customTaskRepresentation.getProcessInstanceId()).singleResult();

                    if (processInstance != null) {
                        customTaskRepresentation.setSuspended(processInstance.isSuspended());
                    }

                    if (customTaskRepresentation.getAssigneeId() != null) {
                        LightUserRepresentation assignee = new LightUserRepresentation(userService.getUser(Long.valueOf(customTaskRepresentation.getAssigneeId())));
                        customTaskRepresentation.setAssignee(assignee);
                    }

                    setTaskCandidateGroup(customTaskRepresentation);
                    setTaskSubmittedFormForHistoricTask(customTaskRepresentation);

                    return customTaskRepresentation;

                }).collect(Collectors.toList());
    }

    public List<String> getActiveStellerTasksPerDossierAndProcessType(String dossierId, APSConstants.Process processType) {
        return historyService.createHistoricTaskInstanceQuery()
                .processVariableValueEqualsIgnoreCase(DOSSIER_ID, dossierId)
                .processVariableValueEqualsIgnoreCase(PROCESS_TYPE, processType.name())
                .taskVariableValueEqualsIgnoreCase(IS_ASSIGNEE, STELLER)
                .unfinished()
                .list()
                .stream()
                .map(HistoricTaskInstance::getId)
                .collect(Collectors.toList());
    }


    /**
     * This method assigns a task to a given user.
     *
     * @param taskId The task.
     * @param assignTaskToUserBody Body containing the assignee.
     * @return TaskRepresentation
     */
    public TaskRepresentation assignTaskToUser(String taskId, AssignTaskToUserBody assignTaskToUserBody) {
        String assignee = assignTaskToUserBody.getAssignee();

        log.debug("Assigning task [{}] to user [{}]", taskId, assignee);

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        User user = userService.getUser(Long.valueOf(assignee));

        String dossierId = taskService.getVariable(taskId, DOSSIER_ID, String.class);
        String subject = taskService.getVariable(taskId, SUBJECT, String.class);
        String owner = taskService.getVariable(taskId, OWNER, String.class);
        Boolean isUrgent = taskService.hasVariable(taskId, IS_URGENT) ? taskService.getVariable(taskId, IS_URGENT, Boolean.class) : null;

        verifyCanAssign(taskId, assignee, task, user);

        taskService.claim(taskId, assignee);

        try {
            HtmlEmail newTaskEmail = customEmailService.prepareNewTaskEmail(user.getTenantId(), List.of(user.getEmail()), task.getName(), dossierId, subject, owner, isUrgent);
            newTaskEmail.send();
        } catch (Exception e) {
            log.error("Could not send email to [{}]", user.getEmail());
        }

        Task taskWithNewAssignee = taskService.createTaskQuery().taskId(taskId).singleResult();
        return new TaskRepresentation(taskWithNewAssignee);
    }

    /**
     * Helper method that checks if a task can be assigned
     *
     * @param taskId the task id
     * @param assignee the assignee id
     * @param task the task object
     * @param user the assignee user object
     */
    private void verifyCanAssign(String taskId, String assignee, Task task, User user) {
        if (task == null) {
            throw new NotFoundException(String.format("Task %s not found", taskId));
        }
        if (user == null) {
            throw new NotFoundException(String.format("User %s not found", assignee));
        }
        if (task.getAssignee() != null) {
            throw new ConflictingRequestException(String.format("Task %s is already claimed", taskId));
        }
    }

    private boolean filterOutDeletedTasks(CustomTaskRepresentation task) {
        if (task.getDeleteReason() != null) {
            return !task.getDeleteReason().equals("deleted");
        } else {
            return true;
        }
    }

    private void setTaskCandidateGroup(CustomTaskRepresentation task) {
        List<LightGroupRepresentation> candidateGroups = getIdentityLinksForTask(task);
        task.setInvolvedGroups(candidateGroups);
    }

    private List<LightGroupRepresentation> getIdentityLinksForTask(CustomTaskRepresentation task) {
        log.debug("Getting identity links for task [{}] - {}", task.getId(), task.getName());

        List<HistoricIdentityLink> historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(task.getId());
        return historicIdentityLinksForTask.stream()
                .filter(historicIdentityLink -> historicIdentityLink.getType().equals("candidate"))
                .map(historicIdentityLink -> {
                    Group group = groupService.getGroup(Long.valueOf(historicIdentityLink.getGroupId()));
                    return new LightGroupRepresentation(group);
                }).collect(Collectors.toList());
    }

    private void setTaskSubmittedFormForHistoricTask(CustomTaskRepresentation task) {
        if (task.getDeleteReason() != null && task.getDeleteReason().equals("completed")) {
            log.debug("Attempting to get the submitted form for task with ID [{}]", task.getId());

            try {
                SubmittedFormRepresentation taskSubmittedForm = submittedFormService.getTaskSubmittedForm(task.getId());
                Map<String, FormFieldRepresentation> formFieldsMap = taskSubmittedForm.getForm().allFieldsAsMap();
                task.setTaskSubmittedFormFields(formFieldsMap);
            } catch (NotFoundException e) {
                log.error(COULD_NOT_RETRIEVE_TASK_SUBMITTED_FORM, task.getId());
            }
        }
    }

    /**
     * The base query to be used to get process relations for a dossier.
     *
     * @param dossierId     The dossier for which the process relations should be fetched.
     * @param relationTypes The relation types to include.
     * @param active        Filter on unfinished or finished tasks.
     * @return HistoricTaskInstanceQuery that can be used to query a list of historic tasks.
     */
    private HistoricTaskInstanceQuery getProcessRelationsQuery(String dossierId, List<String> relationTypes, boolean active) {
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .processVariableValueEquals("dossierId", dossierId).or();

        if (!CollectionUtils.isNullOrEmpty(relationTypes)) {
            relationTypes.forEach(relationType -> query.processVariableValueEquals("processType", relationType));
        }

        query.endOr();
        if (active) {
            query.unfinished();
        } else {
            query.finished();
        }
        return query;
    }

    /**
     * The base query to use for the getOmboekenTasks method.
     *
     * @param apsId     The APS ID of the user.
     * @param clusters  A list of clusters (example ORG_MO,ORG_DV,ORG_SB).
     * @param sort      The field to sort on (created, duedate).
     * @param ascending The order to sort.
     * @return TaskQuery
     */
    private TaskQuery baseQuery(Long apsId, List<String> clusters, String sort, boolean ascending) {
        TaskQuery baseQuery = taskService.createTaskQuery()
                .active()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .taskAssignee(String.valueOf(apsId))
                .taskVariableValueNotEquals(IS_ASSIGNEE, STELLER)
                .or();
        clusters.forEach(cluster -> baseQuery.processVariableValueEqualsIgnoreCase(OWNER, cluster));
        baseQuery.endOr();
        setOrderAndSort(sort, ascending, baseQuery);
        return baseQuery;
    }

    private TaskRepresentation createTaskRepresentation(final Task task) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();

        if (processInstance != null && !processInstance.isSuspended()) {
            TaskRepresentation taskRepresentation = new TaskRepresentation(task);

            if (task.getAssignee() != null) {
                taskRepresentation.setAssignee(new LightUserRepresentation(userService.getUser(Long.parseLong(task.getAssignee()))));
            }

            List<RestVariable> variables = restVariableFactory
                    .createRestVariables(task.getTaskLocalVariables(), RestVariable.RestVariableScope.LOCAL);
            variables.addAll(restVariableFactory.createRestVariables(task.getProcessVariables(), RestVariable.RestVariableScope.GLOBAL));
            taskRepresentation.setVariables(variables);
            return taskRepresentation;
        }
        return null;
    }

    /**
     * Helper method that sets the order on a HistoricTaskInstanceQuery.
     *
     * @param sort      The field to sort on (created, duedate).
     * @param ascending The order to sort.
     * @param baseQuery The query to which the order will be appended.
     */
    private void setOrderAndSort(String sort, boolean ascending, TaskQuery baseQuery) {
        if (sort.equals("created")) {
            if (ascending) {
                baseQuery.orderByTaskCreateTime().asc();
            } else {
                baseQuery.orderByTaskCreateTime().desc();
            }
        }
        if (sort.equals("duedate")) {
            if (ascending) {
                baseQuery.orderByTaskDueDate().asc();
            } else {
                baseQuery.orderByTaskDueDate().desc();
            }
        }
    }

    public void replayTask(String taskId) {
        log.debug("Replaying task with ID [{}]", taskId);

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();

        log.debug("Execution found: [{}]", execution.getName());

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowElement flowElement = bpmnModel.getFlowElement(execution.getActivityId());
        UserTask userTask = (UserTask) flowElement;
        List<ActivitiListener> taskListeners = userTask.getTaskListeners();

        log.debug("before permission task listener call");

        String implementation = taskListeners.get(0).getImplementation();
        String strippedExpression = implementation.replaceAll("[${}]", "");
        String bean = strippedExpression.substring(0, strippedExpression.indexOf("."));
        String method = strippedExpression.substring(strippedExpression.indexOf(".") + 1, strippedExpression.indexOf("("));
        String arguments = strippedExpression.substring(strippedExpression.indexOf("(") + 1, strippedExpression.indexOf(")"));
        List<String> argumentList = Arrays.asList(arguments.split("\\s*,\\s*"));
        PermissionsTaskListener permissionsTaskListener = (PermissionsTaskListener) applicationContext.getBean(bean);

        permissionsTaskListener.updatePermissions(task, "create", "WRITE", true);
        log.debug("after permission task listener call");

        log.debug("Task listeners found for following events: [{}]", taskListeners.stream().map(ActivitiListener::getEvent).collect(Collectors.toList()));

    }
}
