package com.activiti.extension.bean.tasklisteners;

import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.activiti.engine.delegate.event.ActivitiEventType.ENTITY_CREATED;

@Component("createIsAssigneeVarEventListener")
public class CreateIsAssigneeVarEventListener implements ActivitiEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CreateIsAssigneeVarEventListener.class);

    private final TaskService taskService;

    public CreateIsAssigneeVarEventListener(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void onEvent(ActivitiEvent event) {
        if (ENTITY_CREATED == event.getType()) {
            // the event listener is set such that it only triggers for tasks
            ActivitiEntityEvent entityEvent = (ActivitiEntityEventImpl) event;
            Task task = (Task) entityEvent.getEntity();
            // create task variable 'isAssignee' on the task
            taskService.setVariableLocal(task.getId(), "isAssignee", "general");
        } else {
            logger.debug("This event listener does not support the event type received: " + event.getType());
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}
