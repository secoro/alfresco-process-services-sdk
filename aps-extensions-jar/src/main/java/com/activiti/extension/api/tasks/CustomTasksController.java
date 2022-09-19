package com.activiti.extension.api.tasks;

import com.activiti.extension.api.tasks.model.AssignTaskToUserBody;
import com.activiti.extension.api.tasks.model.CustomTaskRepresentation;
import com.activiti.extension.bean.aspects.Authority;
import com.activiti.model.common.ResultListDataRepresentation;
import com.activiti.model.runtime.TaskRepresentation;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(
        value = "/enterprise/custom/tasks",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class CustomTasksController {

    private final CustomTaskService customTaskService;

    /**
     * Controller method to get all process relations for a given dossier.
     *
     * @param dossierId     The dossier for which the process relations need to be fetched.
     * @param relationTypes The relation types (i.e. MAIN,ADHOC,DELAYREQUEST,TERMINATE_DOSSIER) to include in the response.
     * @param active        Filter for (un)finished tasks.
     * @param size          The amount of tasks to return.
     * @return List<CustomTaskRepresentation> A list of tasks.
     */
    @GetMapping("/{dossierId}/process-relations")
    @ApiOperation("Get process relations")
    public List<CustomTaskRepresentation> getProcessRelations(
            @PathVariable(name = "dossierId") String dossierId,
            @RequestParam(name = "relationTypes") List<String> relationTypes,
            @RequestParam(name = "active", required = false, defaultValue = "true") boolean active,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
        log.debug("Returning all process relations for dossier with following parameters: " +
                "Dossier = {}, Relation types = {}, Active = {}, Size = {}", dossierId, relationTypes, active, size);
        return customTaskService.getProcessRelations(dossierId, relationTypes, active, size);
    }

    /**
     * Retrieves all tasks where the supplied user has a user task in which there is no steller.
     *
     * @param assignee  the aps user id of the assignee
     * @param clusters  string array of clusters
     * @param sort      the field to sort on (created or duedate)
     * @param ascending determines the sort order
     * @param skipCount determines the amount of tasks to skip
     * @param maxItems  the maximum amount of tasks to return
     * @return List of tasks
     */
    @GetMapping("/reassign")
    @ApiOperation("Get all tasks where supplied user is assignee and has no steller")
    public ResultListDataRepresentation<TaskRepresentation> getReassignTasks(
            @RequestParam(name = "assignee") Long assignee,
            @RequestParam(name = "clusters") List<String> clusters,
            @RequestParam(name = "sort", required = false, defaultValue = "created") String sort,
            @RequestParam(name = "ascending", required = false, defaultValue = "false") boolean ascending,
            @RequestParam(name = "skipCount", required = false, defaultValue = "0") int skipCount,
            @RequestParam(name = "maxItems", required = false, defaultValue = "5") int maxItems) {
        log.debug("Getting all reassignable tasks for user. GET REST API called");
        return customTaskService.getReassignableTasksForUserWithinCluster(assignee, clusters, sort, ascending, skipCount, maxItems);
    }

    /**
     * This endpoint can be used to assign a task to a given user. This endpoint differs slightly from the OOTB activiti
     * one, because it's configurable who can assign tasks. Whereas in the OOTB endpoint only admins can assign tasks.
     *
     * @param taskId The task.
     * @param assignTaskToUserBody Body containing the assignee.
     * @return TaskRepresentation
     */
    @PutMapping("/{taskId}/action/assign")
    @Authority(requiredAuthorities = {"FUNCTIONEEL_BEHEER", "PROCES_REGISSEURS"})
    @ApiOperation("Assigns a task to a specific user")
    public TaskRepresentation assignTaskToUser(
            @PathVariable(name = "taskId") String taskId,
            @Valid @RequestBody AssignTaskToUserBody assignTaskToUserBody) {
        log.debug("Assigning unclaimed task to user. PUT REST API called");
        return customTaskService.assignTaskToUser(taskId, assignTaskToUserBody);
    }

    @PutMapping("/{taskId}/action/replay")
    public void replayTask(
            @PathVariable(name = "taskId") String taskId) {
        log.debug("Replaying task. PUT REST API called");
        customTaskService.replayTask(taskId);
    }
}
