package com.activiti.extension.bean.delegates;

import com.activiti.extension.bean.utils.services.EventMessageService;
import com.activiti.extension.bean.utils.services.ProcessService;
import com.activiti.extension.model.messages.EventMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component("sendEventMessage")
public class SendEventMessage implements JavaDelegate {

    private Expression eventType;

    private final EventMessageService eventMessageService;
    private final ProcessService processService;

    /**
     * Delegate responsible for building a EventMessageDTO object
     *
     * @param execution The delegate execution
     * @throws ParseException
     */
    @Override
    public void execute(DelegateExecution execution) {

        String eventTypeString = eventType.getExpressionText();
        Map<String, String> vars = processService.collectServiceTaskVariables(execution, eventTypeString);

        try {
            EventMessageDTO<?> eventMessageDTO = eventMessageService.getEventMessageForEventType(eventTypeString, vars, execution);
            eventMessageService.sendEventMessage(eventMessageDTO);
        } catch (ParseException e) {
            log.error("Could not parse");
        }
    }
}
