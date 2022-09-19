package com.activiti.extension.bean.utils.services;

import com.activiti.extension.bean.utils.ACSHTTPClient;
import com.activiti.model.runtime.TaskRepresentation;
import com.activiti.model.runtime.dto.TaskQueryRepresentation;
import com.activiti.model.runtime.dto.TaskState;
import com.activiti.service.runtime.AlfrescoTaskQueryService;
import com.amazonaws.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.util.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.activiti.extension.bean.utils.literal.EndPoint.ICM;
import static com.activiti.extension.bean.utils.literal.EndPoint.ICM_DOSSIER;
import static com.activiti.extension.constant.APSConstants.*;
import static com.activiti.extension.constant.APSConstants.Process.DELAYREQUEST;
import static com.activiti.extension.constant.APSConstants.Process.MAIN;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomTaskService {

    private final AlfrescoTaskQueryService alfrescoTaskQueryService;
    private final ACSHTTPClient acshttpClient;
    private final HistoryService historyService;

    /**
     * Get active tasks from a process instance
     *
     * @param processInstanceId Id of the process instance
     * @return List of active tasks of the process whose id is the parameter
     */
    public List<TaskRepresentation> getActiveTasks(String processInstanceId) {
        TaskQueryRepresentation taskQuery = new TaskQueryRepresentation();
        taskQuery.setProcessInstanceId(processInstanceId);
        taskQuery.setState(TaskState.ACTIVE);
        return alfrescoTaskQueryService.listTasks(taskQuery).getData();
    }

    /**
     * Sets some neccessary steller omboeken process variables on the omboeken steller process.
     *
     * @param execution is the omboeken steller process.
     * @param dossierId Id of the dossier, for example M2000-1
     */
    public void setOmboekenVariables(DelegateExecution execution, String dossierId) {
        String responseJsonAsString = acshttpClient.execute(String.format(ICM_DOSSIER, dossierId), null, HttpMethod.GET.name(), null, ICM);
        JSONObject jsonResponse = new JSONObject(responseJsonAsString);
        try {
            execution.setVariable("externalId", jsonResponse.getString("externalId"));
            execution.setVariable("type_LABEL", jsonResponse.getString("type"));
            execution.setVariable("subject", jsonResponse.getString("subject"));
            execution.setVariable("dossierNodeId", jsonResponse.getString("nodeId"));
        } catch (Exception e) {
            log.error("Was not able to set the necessary variables for the steller omboeken process: {}", e.getMessage());
            log.debug("Exception details: {}", e);
        }
    }

    public List<Integer> getActiveStellerTasksPerDossier(String dossierId) {
        return historyService.createHistoricTaskInstanceQuery()
                .processVariableValueEqualsIgnoreCase(DOSSIER_ID, dossierId)
                .or()
                .processVariableValueEqualsIgnoreCase(PROCESS_TYPE, MAIN.name())
                .processVariableValueEqualsIgnoreCase(PROCESS_TYPE, DELAYREQUEST.name())
                .endOr()
                .taskVariableValueEqualsIgnoreCase(IS_ASSIGNEE, STELLER)
                .unfinished()
                .list()
                .stream()
                .map(HistoricTaskInstance::getId)
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }
}
