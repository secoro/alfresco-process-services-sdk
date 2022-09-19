package com.activiti.extension.api.tasks;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(
        value = "/enterprise/custom/usertasks",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class UserTasksController {

    private final UserTaskService userTaskService;

    public UserTasksController(UserTaskService userTaskService) {
        this.userTaskService = userTaskService;
    }

    /**
     * Service call to get all non-steller tasks for a given user
     *
     * @param requestJson String from the historic task api call
     * @return String
     */
    @PostMapping()
    public String getHistoricTasksResponse(@RequestBody String requestJson) {
        return userTaskService.getHistoricTasks(requestJson);
    }
}
