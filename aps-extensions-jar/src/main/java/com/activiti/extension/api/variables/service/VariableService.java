package com.activiti.extension.api.variables.service;

import com.activiti.extension.api.variables.model.BulkUpdateDossiersProcessesModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VariableService {

    private final RuntimeService runtimeService;

    public void bulkUpdateProcesses(BulkUpdateDossiersProcessesModel body) {
        log.debug("Bulk update in process for dossiers [{}]", body.getDossierIds());

        body.getDossierIds().forEach(dossierId -> {
            List<ProcessInstance> runningProcessesForDossierId = getRunningProcessesForDossierId(dossierId, body.getProcessTypes());

            runningProcessesForDossierId.stream()
                    .filter(this::filterSuspendedProcesses)
                    .forEach(process -> updateProcessVariablesIfExists(process, body.getVariables()));
        });
    }

    public void updateProcessVariablesIfExists(ProcessInstance processInstance, Map<String, Object> variables) {
        log.debug("Updating process instance [{}] with variables [{}]", processInstance.getId(), variables);

        if (!processInstance.isSuspended()) {
            variables.keySet().forEach(variableName -> {
                if (runtimeService.hasVariable(processInstance.getId(), variableName)) {
                    Object oldValue = runtimeService.getVariable(processInstance.getId(), variableName);
                    setVariableBasedOnDataType(processInstance, variableName, oldValue, variables.get(variableName));
                }
            });
        } else {
            log.debug("Process [{}] is suspended, could not update variables", processInstance.getId());
        }

    }

    private List<ProcessInstance> getRunningProcessesForDossierId(String dossierId, List<String> includeProcessTypes) {
        ProcessInstanceQuery baseQuery = runtimeService.createProcessInstanceQuery()
                .variableValueEqualsIgnoreCase("dossierId", dossierId)
                .or();
        includeProcessTypes.forEach(type -> baseQuery.variableValueEqualsIgnoreCase("processType", type));
        baseQuery.endOr().includeProcessVariables();

        return baseQuery.list();
    }

    private boolean filterSuspendedProcesses(ProcessInstance process) {
        if (process.isSuspended()) {
            String dossierId = process.getProcessVariables().get("dossierId").toString();
            log.debug("Process with name [{}] belonging to dossier with ID [{}] skipped because it is suspended", process.getName(), dossierId);
        }
        return !process.isSuspended();
    }

    private void setVariableBasedOnDataType(ProcessInstance process, String variableName, Object oldValue, Object newValue) {
        String dataType = getDataTypeFromVariable(oldValue);

        switch (dataType) {
            case "String":
                runtimeService.setVariable(process.getId(), variableName, newValue);
                break;
            case "Long":
            case "Integer":
                runtimeService.setVariable(process.getId(), variableName, Long.valueOf(newValue.toString()));
                break;
            case "Boolean":
                runtimeService.setVariable(process.getId(), variableName, Boolean.parseBoolean(newValue.toString()));
                break;
            default:
                throw new ActivitiException(String.format("Could not set variable [%s] for dataType [%s]", variableName, dataType));
        }
    }

    private String getDataTypeFromVariable(Object variable) {
        return variable == null ? "String" : variable.getClass().getSimpleName();
    }
}
