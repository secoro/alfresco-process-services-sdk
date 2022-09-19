package com.activiti.extension.bean.utils.services;

import com.activiti.domain.idm.User;
import com.activiti.extension.api.dossier.model.LightDossierRepresentation;
import com.activiti.extension.api.tasks.model.CustomTaskRepresentation;
import com.activiti.extension.bean.utils.literal.Cluster;
import com.activiti.service.api.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Incentro
 * Service class that contains logic concerning reassigning tasks/dossiers.
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class ReassignService {

    private final RuntimeService runtimeService;
    private final UserService userService;
    private final CustomEmailService customEmailService;
    private final HistoryService historyService;
    private final TaskService taskService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendEmailNotifyingProcessManagersOfUserResignation(Long apsID) throws Exception {
        User user = userService.getUser(apsID);

        for (Cluster cluster : Cluster.values()) {
            List<LightDossierRepresentation> userDossiers = getStellerDossiers(apsID, cluster);
            List<CustomTaskRepresentation> userTasks = getOmboekenTasks(apsID, cluster);

            log.debug("Found [{}] dossiers and [{}] tasks for user [{}] in cluster [{}]", userDossiers.size(), userTasks.size(), apsID, cluster.name());

            if (!userDossiers.isEmpty() || !userTasks.isEmpty()) {
                HtmlEmail htmlEmail = customEmailService.prepareNotifyProcessManagersEmail(user, cluster, userDossiers, userTasks);

                if (htmlEmail != null) {
                    htmlEmail.send();
                }
            }
        }
    }

    /**
     * Get dossiers where the supplied user is steller.
     *
     * @param apsId APS ID of a user
     * @return List of LightDossierRepresentation
     */
    public List<LightDossierRepresentation> getStellerDossiers(Long apsId, Cluster cluster) {
        return runtimeService.createProcessInstanceQuery()
                .variableValueEquals("processType", "MAIN")
                .variableValueEquals("steller", apsId)
                .variableValueEquals("owner", cluster.name())
                .includeProcessVariables()
                .list()
                .stream()
                .filter(process -> process.getProcessVariables().containsKey("dossierId")) // in case the bb-process fails at the start, meaning there could be a process without a dossierID
                .filter(process -> !process.isSuspended())
                .map(this::createLightDossierFromProcess)
                .collect(Collectors.toList());
    }

    /**
     * This method returns all tasks for a given user and cluster
     *
     * @param apsId     The APS ID of the user.
     * @param cluster   Name of the cluster
     * @return ResultListDataRepresentation<TaskRepresentation>
     */
    public List<CustomTaskRepresentation> getOmboekenTasks(Long apsId, Cluster cluster) {
        return historyService.createHistoricTaskInstanceQuery()
                .unfinished()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .taskAssignee(String.valueOf(apsId))
                .taskVariableValueNotEquals("isAssignee", "steller")
                .processVariableValueEquals("owner", cluster.name())
                .list()
                .stream()
                .map(this::createHistoricTaskRepresentation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Helper method for mapping a process instance to a dossier.
     *
     * @param processInstance   The process that needs to be mapped.
     * @return                  LightDossierRepresentation
     */
    private LightDossierRepresentation createLightDossierFromProcess(ProcessInstance processInstance) {
        LightDossierRepresentation lightDossierRepresentation = new LightDossierRepresentation();
        lightDossierRepresentation.setDossierId(processInstance.getProcessVariables().get("dossierId").toString());
        lightDossierRepresentation.setSubject(processInstance.getProcessVariables().get("subject").toString());

        return lightDossierRepresentation;
    }

    /**
     * Helper method for mapping a historic task instance to a custom task representation.
     *
     * @param task      The HistoricTaskInstance that needs to be mapped
     * @return          CustomTaskRepresentation
     */
    private CustomTaskRepresentation createHistoricTaskRepresentation(final HistoricTaskInstance task) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();

        if (processInstance != null && !processInstance.isSuspended()) {
            CustomTaskRepresentation customTaskRepresentation = new CustomTaskRepresentation(task);
            customTaskRepresentation.setProcessVariables(taskService.getVariables(task.getId()));

            return customTaskRepresentation;
        }
        return null;
    }
}
