package com.activiti.extension.bean.utils.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.TaskWithFieldExtensions;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.context.Context;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.activiti.extension.bean.utils.literal.EventMessageConstants.*;

/**
 * @author incentro
 * Service used to interact with process variables.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessService {

    private final EventMessageService eventMessageService;

    /**
     * Method that collects process variables.
     *
     * @param execution The delegate execution.
     * @param vars      A list of variables that needs to be collected.
     * @return A map of variables found (key = variable name, value = variable value)
     */
    public Map<String, String> collectVariables(DelegateExecution execution, List<String> vars) {
        log.debug("Collecting the following variables [{}]", vars);

        return vars.stream()
                .filter(var -> (execution.getVariable(var) != null))
                .collect(Collectors.toMap(key -> key, value -> execution.getVariable(value).toString()));
    }

    /**
     * Method that sets a collection of variables to null when found on the process.
     * @param execution The delegate execution.
     * @param vars A list of variables that needs to be set to null.
     */
    public void setVariablesToNull(DelegateExecution execution, List<String> vars) {
        log.debug("Setting the following variables to null [{}]", vars);

        vars.stream()
                .filter(execution::hasVariable)
                .forEach(var -> execution.setVariable(var, null));
    }

    /**
     * Method that returns all the service task variables for a given execution.
     * It looks at the class fields of a service tasks and distinguishes
     * between the different types of expressions (${var}, var).
     * It does a lookup in the execution and returns a merged map.
     *
     * @param execution The delegate execution.
     * @return A map of service task class fields.
     */
    public Map<String, String> collectServiceTaskVariables(DelegateExecution execution) {
        return collectServiceTaskVariables(execution, null);
    }

    /**
     * Method that returns all the service task variables for a given execution.
     * It looks at the class fields of a service tasks and distinguishes
     * between the different types of expressions (${var}, var).
     * It does a lookup in the execution and returns a merged map.
     *
     * @param execution The delegate execution.
     * @param eventType The current event type.
     * @return A map of service task class fields.
     */
    public Map<String, String> collectServiceTaskVariables(DelegateExecution execution, String eventType) {
        log.debug("Collecting variables for Service Task: [{}]", execution.getCurrentActivityId());

        RepositoryService repositoryService = Context.getProcessEngineConfiguration().getRepositoryService();
        BpmnModel model = repositoryService.getBpmnModel(execution.getProcessDefinitionId());
        FlowElement flowElement = model.getFlowElement(execution.getCurrentActivityId());
        List<FieldExtension> fieldExtensions = ((TaskWithFieldExtensions) flowElement).getFieldExtensions();

        if (eventType != null && eventMessageService.isMilestoneEvent(eventType)) {
            validateMilestoneKeys(eventType, fieldExtensions);
        }

        Map<String, String> expressions = fieldExtensions.stream()
                .filter(var -> var.getStringValue().contains("$"))
                .map(var -> var.getStringValue().replaceAll("[${}]", ""))
                .collect(Collectors.toMap(key -> key, val -> execution.getVariable(val).toString()));

        Map<String, String> nonExpressions = fieldExtensions.stream()
                .filter(var -> !var.getStringValue().contains("$"))
                .collect(Collectors.toMap(key -> key.getFieldName(), value -> value.getStringValue()));

        expressions.putAll(nonExpressions);

        return expressions;
    }

    /**
     * Helper method for validating the provided class fields in a service task that calls the sendEventMessage delegate
     * for a milestone event. Checks the provided eventType and calls the validateMilestoneForGivenEventType method to
     * validate the provided keys with a list of expected keys.
     *
     * @param eventType       The eventTypes [startDossier, milestoneReached, milestoneReachedBackward, updateDossierConfiguration]
     * @param fieldExtensions The class fields that were found on the service task.
     */
    private void validateMilestoneKeys(String eventType, List<FieldExtension> fieldExtensions) {
        log.debug("Validating the keys for eventType [{}]", eventType);

        boolean isBenWOverleg = fieldExtensions.stream()
                .map(FieldExtension::getStringValue)
                .anyMatch("${execBoardMeetingDate}"::equals);

        switch (eventType) {
            case START_DOSSIER:
                validateServiceTaskVariables(fieldExtensions, KEYS_START_DOSSIER);
                break;
            case MILESTONE_REACHED:
                if (!isBenWOverleg) {
                    validateServiceTaskVariables(fieldExtensions, KEYS_MILESTONE_REACHED);
                } else {
                    validateServiceTaskVariables(fieldExtensions, KEYS_MILESTONE_REACHED_EXEC);
                }
                break;
            case MILESTONE_REACHED_BACKWARD:
                validateServiceTaskVariables(fieldExtensions, KEYS_MILESTONE_REACHED_BACKWARD);
                break;
            case UPDATE_DOSSIER_CONFIGURATION:
                validateServiceTaskVariables(fieldExtensions, KEYS_UPDATE_DOSSIER_CONFIGURATION);
                break;
            default:
                log.debug("Event type [{}] not recognized", eventType);
        }
    }


    private void validateServiceTaskVariables(List<FieldExtension> fieldExtensions, List<String> expectedKeys) {
        List<String> serviceVars = new ArrayList<>();

        fieldExtensions
                .forEach(item -> {
                    if (item.getStringValue().contains("$")) {
                        serviceVars.add(item.getStringValue().replaceAll("[${}]", ""));
                    } else {
                        serviceVars.add(item.getFieldName());
                    }
                });

        serviceVars
                .forEach(item -> {
                    if (!expectedKeys.contains(item)) {
                        log.debug("Following expected key not found [{}]", item);
                    }
                });
    }

}
