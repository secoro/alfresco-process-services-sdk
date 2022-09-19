package com.activiti.extension.bean.eventlisteners.activiti;


import com.activiti.extension.bean.utils.services.EventMessageService;
import com.activiti.extension.model.messages.EventMessageDTO;
import com.activiti.service.runtime.events.RuntimeEventListener;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.delegate.event.impl.ActivitiEntityExceptionEventImpl;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("globalEventListener")
@RequiredArgsConstructor
public class GlobalEventListener implements RuntimeEventListener {

    private final EventMessageService eventMessageService;
    private final RuntimeService runtimeService;

    @SneakyThrows
    @Override
    public void onEvent(ActivitiEvent activitiEvent) {

        //this switch is used for filtering the events we want to process, as we do not want to act on all events
        switch (activitiEvent.getType()) {
            case TASK_CREATED:
                log.debug("TASK_CREATED_EVENT");
                taskEventMessage(activitiEvent);
                break;

            case TASK_ASSIGNED:
                log.debug("TASK_ASSIGNED_EVENT");
                taskEventMessage(activitiEvent);
                break;

            case TASK_COMPLETED:
                log.debug("TASK_COMPLETED_EVENT");
                taskEventMessage(activitiEvent);
                break;

            case PROCESS_COMPLETED:
                log.debug("PROCES_COMPLETED_EVENT");
                processCompletedEventMessage(activitiEvent);
                break;

            case JOB_EXECUTION_FAILURE:
                log.debug("JOB_EXECUTION_FAILURE_EVENT");
                jobExecutionFailureEventMessage(activitiEvent);
                break;

            default:
                break;
        }
    }

    /**
     * method to check if the EventListener is enabled or not.
     *
     * @return boolean
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * isFailOnException() method will be called when onEvent() method throws an exception.
     * Returning false means the exception is ignored.
     *
     * @return boolean
     **/
    @Override
    public boolean isFailOnException() {
        return false;
    }

    /**
     * Sends a taskEventMessage
     * @param activitiEvent the ActivitiEvent
     */
    void taskEventMessage(ActivitiEvent activitiEvent) throws ParseException {
        log.debug("running taskEventMessage");

        EventMessageDTO<?> eventMessageDTO;
        Map<String, String> vars = new HashMap<>();

        String eventType = activitiEvent.getType().name();

        Map<String, Object> processVariables = runtimeService.getVariables(activitiEvent.getExecutionId());
        TaskEntity taskEntity = (TaskEntity) ((ActivitiEntityEventImpl) activitiEvent).getEntity();
        vars.put("dossierId", (String) processVariables.get("dossierId"));
        vars.put("sender", taskEntity.getProcessInstance().getName());
        vars.put("source", taskEntity.getProcessInstanceId());
        vars.put("assignee", taskEntity.getAssignee());
        eventMessageDTO = eventMessageService.getApsTaskEventMessage(vars, eventType, taskEntity);

        if (activitiEvent.getType().equals(ActivitiEventType.TASK_ASSIGNED)) {
            //send the event twice: with a assignee as id and with a dossierId .
            eventMessageService.sendEventMessage(eventMessageDTO);
            eventMessageDTO.setId(vars.get("dossierId"));
            eventMessageService.sendEventMessage(eventMessageDTO);
        } else {
            eventMessageService.sendEventMessage(eventMessageDTO);
        }
    }

    /**
     * Sends a processCompletedEventMessage
     * @param activitiEvent the ActivitiEvent
     */
    void processCompletedEventMessage(ActivitiEvent activitiEvent) throws ParseException {
        log.debug("running processCompletedEventMessage");

        EventMessageDTO<?> eventMessageDTO;
        Map<String, String> vars = new HashMap<>();

        String eventType = activitiEvent.getType().name();
        Map<String, Object> processVariables = runtimeService.getVariables(activitiEvent.getExecutionId());
        ExecutionEntity executionEntity = (ExecutionEntity) ((ActivitiEntityEventImpl) activitiEvent).getEntity();
        vars.put("dossierId", (String) processVariables.get("dossierId"));
        vars.put("sender", executionEntity.getProcessInstance().getName());
        vars.put("source", executionEntity.getProcessInstance().getId());
        eventMessageDTO = eventMessageService.getApsProcessEventMessage(vars, eventType, executionEntity);

        eventMessageService.sendEventMessage(eventMessageDTO);
    }

    /**
     * Sends a jobExecutionFailureEventMessage
     * @param activitiEvent the ActivitiEvent
     */
    void jobExecutionFailureEventMessage(ActivitiEvent activitiEvent) {
        log.debug("running jobExecutionFailureEventMessage");

        EventMessageDTO<?> eventMessageDTO;
        Map<String, String> vars = new HashMap<>();

        String eventType = activitiEvent.getType().name();
        Map<String, Object> processVariables = runtimeService.getVariables(activitiEvent.getExecutionId());
        Execution execution = runtimeService.createExecutionQuery().executionId(activitiEvent.getExecutionId()).singleResult();

        JobEntity jobEntity = (JobEntity) ((ActivitiEntityExceptionEventImpl) activitiEvent).getEntity();

        vars.put("jobId", execution.getId());
        vars.put("dossierId", (String) processVariables.get("dossierId"));
        vars.put("sender", activitiEvent.getProcessDefinitionId());
        vars.put("source", activitiEvent.getProcessInstanceId());
        vars.put("errorMessage", ((ActivitiEntityExceptionEventImpl) activitiEvent).getCause() + "\n" +
                ((ActivitiEntityExceptionEventImpl) activitiEvent).getCause().getStackTrace().toString());

        eventMessageDTO = eventMessageService.getApsErrorEventMessage(vars, eventType, jobEntity);

        eventMessageService.sendEventMessage(eventMessageDTO);
    }

}
