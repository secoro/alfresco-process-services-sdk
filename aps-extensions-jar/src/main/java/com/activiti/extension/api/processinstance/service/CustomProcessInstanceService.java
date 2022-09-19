package com.activiti.extension.api.processinstance.service;

import com.activiti.domain.idm.User;
import com.activiti.extension.bean.exceptions.ForbiddenException;
import com.activiti.extension.constant.APSConstants;
import com.activiti.model.common.ResultListDataRepresentation;
import com.activiti.model.idm.LightUserRepresentation;
import com.activiti.model.runtime.ProcessInstanceRepresentation;
import com.activiti.security.SecurityUtils;
import com.activiti.service.api.UserService;
import com.activiti.service.exception.NotFoundException;
import com.activiti.service.runtime.AlfrescoProcessInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomProcessInstanceService {

    private final AlfrescoProcessInstanceService processInstanceService;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final UserService userService;

    private enum Operation {ACTIVATE, SUSPEND, DELETE}

    private static final String SUPERUSERS = "Superusers";

    public ResultListDataRepresentation<ProcessInstanceRepresentation> getProcessesForDossier(String dossierId, List<String> processTypes) {
        HistoricProcessInstanceQuery baseQuery = historyService.createHistoricProcessInstanceQuery()
                .variableValueEqualsIgnoreCase("dossierId", dossierId).or();
        processTypes.forEach(type -> baseQuery.variableValueEqualsIgnoreCase("processType", type));
        baseQuery.endOr().includeProcessVariables();

        return baseQuery.list().stream()
                .map(this::mapHistoricProcessInstanceToProcessInstanceRepresentation)
                .collect(Collectors.collectingAndThen(Collectors.toList(), ResultListDataRepresentation::new));
    }

    public void suspendProcess(Long processInstanceId) {
        operateProcessInstance(Operation.SUSPEND, processInstanceId);
    }

    public void activateProcess(Long processInstanceId) {
        operateProcessInstance(Operation.ACTIVATE, processInstanceId);
    }

    public void deleteProcessInstance(Long processInstanceId) throws NotFoundException, ForbiddenException {
        operateProcessInstance(Operation.DELETE, processInstanceId);
    }

    public Optional<ProcessInstance> getProcessInstanceForDossierIdAndProcessType(String dossierId, APSConstants.Process processType) {
        return Optional.ofNullable(runtimeService.createProcessInstanceQuery()
                .variableValueEqualsIgnoreCase("dossierId", dossierId)
                .variableValueEqualsIgnoreCase("processType", processType.name())
                .includeProcessVariables()
                .active()
                .singleResult());
    }

    private ProcessInstanceRepresentation mapHistoricProcessInstanceToProcessInstanceRepresentation(HistoricProcessInstance process) {
        User user = userService.getUser(Long.valueOf(process.getStartUserId()));
        LightUserRepresentation lightUserRepresentation = new LightUserRepresentation(user);

        ProcessInstanceRepresentation processInstanceRepresentation = new ProcessInstanceRepresentation(process, false, lightUserRepresentation);
        ProcessInstance activeProcess = runtimeService.createProcessInstanceQuery().processInstanceId(process.getId()).singleResult();

        if (activeProcess != null) {
            processInstanceRepresentation.setSuspended(activeProcess.isSuspended());
        }
        return processInstanceRepresentation;
    }

    private void operateProcessInstance(Operation operation, Long processInstanceId) {
        Consumer<HistoricProcessInstance> consumer = getProcessOperator(operation);
        // Retrieve process-instance
        HistoricProcessInstance instance = getProcessInstanceById(processInstanceId);
        // Check if user is initiator or a member of superusers.
        User currentUser = SecurityUtils.getCurrentUserObject();
        if (checkPermissions(currentUser, instance)) {
            User startUser = userService.getUser(Long.valueOf(instance.getStartUserId()));
            SecurityUtils.assumeUser(startUser);
            consumer.accept(instance);
            SecurityUtils.clearAssumeUser();
        } else {
            throw new ForbiddenException("User " + currentUser.getId() + "not allowed to operate on process");
        }
    }

    /**
     * Retrieves the operation that has to be executed for this process
     *
     * @param operation the operation
     * @return Consumer with method that has to be executed
     */
    private Consumer<HistoricProcessInstance> getProcessOperator(Operation operation) {
        switch (operation) {
            case SUSPEND:
                return process -> processInstanceService.suspendProcessInstance(process.getId());
            case ACTIVATE:
                return process -> processInstanceService.activateProcessInstance(process.getId());
            case DELETE:
                return process -> processInstanceService.deleteProcessInstance(process.getId());
            default:
                throw new UnsupportedOperationException("Not a valid operation");
        }
    }

    /**
     * Helper method to retrieve a single HistoricProcessInstance
     *
     * @param processInstanceId the Id of the process-instance
     * @return a single HistoricProcessInstance
     */
    private HistoricProcessInstance getProcessInstanceById(Long processInstanceId) {
        return Optional.ofNullable(historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(String.valueOf(processInstanceId))
                .unfinished()
                .singleResult()
        ).orElseThrow(() -> new NotFoundException("Process with id: " + processInstanceId + " does not exist"));
    }

    /**
     * Check if the current user is either the process starter or is part of the superuser group
     *
     * @param currentUser the user to check
     * @param instance    to get the process starter id from
     * @return whether the user is allowed to execute the request
     */
    private boolean checkPermissions(User currentUser, HistoricProcessInstance instance) {
        if (Long.valueOf(instance.getStartUserId()).equals(currentUser.getId())) {
            return true;
        }
        return currentUser.getGroups().stream().anyMatch(group -> group.getName().equals(SUPERUSERS));
    }
}
