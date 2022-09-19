package com.activiti.extension.bean.utils.services;

import com.activiti.domain.idm.User;
import com.activiti.extension.bean.utils.ACSHTTPClient;
import com.activiti.extension.model.messages.*;
import com.activiti.runtime.activiti.bean.UserInfoBean;
import com.amazonaws.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Map;

import static com.activiti.extension.bean.utils.literal.EndPoint.EVENTS_MESSAGES;
import static com.activiti.extension.bean.utils.literal.EventMessageConstants.MILESTONE_EVENT_TYPES;
import static com.activiti.extension.bean.utils.literal.EventMessageConstants.START_DOSSIER;
import static com.activiti.extension.constant.EventConstants.DOSSIER_EVENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventMessageService {

    private final ACSHTTPClient acshttpClient;
    private final UserInfoBean userInfoBean;


    /**
     * Create a EventMessage based on an eventType
     *
     * @param eventTypeString the eventType
     * @param vars            Map of variables needed to create a EventMessage
     * @param execution       the DelegateExecution
     * @return a EventMessage.
     * @throws ParseException
     */
    public EventMessageDTO<?> getEventMessageForEventType(
            String eventTypeString, Map<String, String> vars, DelegateExecution execution) throws ParseException {

        if (isMilestoneEvent(eventTypeString)) {
            return getMilestoneEvent(vars, eventTypeString);
        } else {
            switch (eventTypeString) {
                case DOSSIER_EVENT:
                    log.info("Creating a {} ", eventTypeString);
                    return getDossierEvent(vars, execution);
                default:
                    log.warn("EventType: {} does not match any known eventTypes.", eventTypeString);
            }
        }
        return null;
    }

    /**
     * Get a MilestoneEventMessage based on eventType
     *
     * @param vars      Map of variables needed to create MilestoneEvent object.
     * @param eventType The specific milestone event.
     * @return a MilestoneEventMessage object
     * @throws ParseException
     */
    private EventMessageDTO<PlanningEventMessageDTO> getMilestoneEvent(Map<String, String> vars, String eventType) throws ParseException {
        PlanningEventMessageDTO planningEventMessageDTO = new PlanningEventMessageDTO(eventType, vars);

        return EventMessageDTO.<PlanningEventMessageDTO>builder()
                .id(vars.get("dossierId"))
                .event("milestoneEvent")
                .sender(eventType.equals(START_DOSSIER) ? vars.get("initiatorUserName") : vars.get("taskCompleter"))
                .source(vars.get("source"))
                .data(planningEventMessageDTO)
                .build();
    }

    /**
     * Create a DossierEventMessage
     *
     * @param vars      Map of variables needed to create a DossierEvent
     * @param execution The DelegateExecution
     * @return a DossierEventMessage object
     */
    private EventMessageDTO<DossierEventMessageDTO> getDossierEvent(Map<String, String> vars, DelegateExecution execution) {
        DossierEventMessageDTO dossierEventMessageDTO = new DossierEventMessageDTO(vars);
        User user = userInfoBean.getUser(Long.parseLong(execution.getVariable("initiator").toString()), execution);

        return EventMessageDTO.<DossierEventMessageDTO>builder()
                .id(vars.get("initiatorUserName"))
                .event(vars.get("eventType"))
                .sender(user.getFullName())
                .source(vars.get("source"))
                .data(dossierEventMessageDTO)
                .build();
    }

    /**
     * Method for sending EventMessageDTO objects to the event service.
     *
     * @param eventMessageDTO the event message to send
     */
    public void sendEventMessage(EventMessageDTO<?> eventMessageDTO) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String eventMessageJSON = mapper.writeValueAsString(eventMessageDTO);
            acshttpClient.execute(EVENTS_MESSAGES, eventMessageJSON, HttpMethod.POST.toString(), null, "Diva Services");
        } catch (Exception e) {
            log.error("An error occured [{}]", e.getMessage());
        }
    }

    /**
     * Method to check if a given eventType is a MilestoneEvent
     *
     * @param eventType an eventType
     * @return boolean
     */
    public boolean isMilestoneEvent(String eventType) {
        log.debug("Checking if [{}] is a milestoneEvent", eventType);
        return MILESTONE_EVENT_TYPES.contains(eventType);
    }

    /**
     * Get a ApsTaskEventMessageDTO
     *
     * @param vars      Map of variables needed to create ApsTaskEventMessageDTO object.
     * @param eventType The specific activitiEvent.
     * @return a EventmessageDTO object
     * @throws ParseException
     */
    public EventMessageDTO<ApsTaskEventMessageDTO> getApsTaskEventMessage(
            Map<String, String> vars, String eventType,TaskEntity taskEntity) throws ParseException {

        ApsTaskEventMessageDTO apsTaskEventMessageDTO = new ApsTaskEventMessageDTO(eventType, taskEntity.getId(),
                taskEntity.getName(), taskEntity.getProcessInstanceId(), vars.get("dossierId"));

        String id = vars.get("dossierId");
        if (eventType.equals(ActivitiEventType.TASK_ASSIGNED.name())) {
            id = vars.get("assignee"); //userId of the assignee.
        }

        return EventMessageDTO.<ApsTaskEventMessageDTO>builder()
                .id(id)
                .event(eventType)
                .sender(vars.get("sender"))
                .source(vars.get("source"))
                .data(apsTaskEventMessageDTO)
                .build();
    }

    /**
     * Get a ApsProcessEventMessage
     *
     * @param vars      Map of variables needed to create apsProcessEventMessage object.
     * @param eventType The specific activitiEvent.
     * @return a EventmessageDTO object
     * @throws ParseException
     */
    public EventMessageDTO<ApsProcessEventMessageDTO> getApsProcessEventMessage(
            Map<String, String> vars, String eventType, ExecutionEntity executionEntity) throws ParseException {

        ApsProcessEventMessageDTO apsProcessEventMessageDTO = new ApsProcessEventMessageDTO(
                eventType, executionEntity.getProcessInstanceId(), vars.get("dossierId"));

        return EventMessageDTO.<ApsProcessEventMessageDTO>builder()
                .id(vars.get("dossierId"))
                .event(eventType)
                .sender(vars.get("sender"))
                .source(vars.get("source"))
                .data(apsProcessEventMessageDTO)
                .build();
    }

    /**
     * Get a ApsErrorEventsMessageDTO.
     * @param vars Map of variables needed to create apsProcessEventMessage object.
     * @param jobEntity an activitiEvent containing the exception details
     * @return a EventmessageDTO object
     * @throws ParseException
     */
    public EventErrorMessageDTO<ApsErrorEventMessageDTO> getApsErrorEventMessage(Map<String, String> vars, String eventType, JobEntity jobEntity) {
        ApsErrorEventMessageDTO apsErrorEventMessageDTO = new ApsErrorEventMessageDTO(
                eventType, vars.get("errorMessage"), jobEntity.getProcessInstanceId(), vars.get("jobId"), vars.get("dossierId"));

        EventMessageDTO<ApsErrorEventMessageDTO> eventMessageDTO = EventMessageDTO.<ApsErrorEventMessageDTO>builder()
                .id(vars.get("dossierId"))
                .event(eventType+".error")
                .sender(vars.get("sender"))
                .source(vars.get("source"))
                .data(apsErrorEventMessageDTO)
                .build();

        return new EventErrorMessageDTO<>(eventMessageDTO, vars.get("errorMessage"));
    }

}
