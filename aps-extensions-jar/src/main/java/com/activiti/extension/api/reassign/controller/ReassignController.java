package com.activiti.extension.api.reassign.controller;

import com.activiti.extension.api.reassign.model.ReassignDossierBody;
import com.activiti.extension.api.reassign.model.ReassignTaskBody;
import com.activiti.extension.api.reassign.service.ReassignService;
import com.activiti.extension.bean.aspects.Authority;
import com.activiti.service.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/enterprise/custom/reassign",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class ReassignController {

    private final ReassignService reassignService;

    /**
     * Reassign a dossier to a given user
     *
     * @param dossierId the dossier
     * @param body      body containing the new writer
     * @return ResponseEntity<String>
     */
    @Authority(requiredAuthorities = {"PROCES_REGISSEURS"})
    @PutMapping("/{dossierId}/writer")
    public void reassignDossier(
            @PathVariable(name = "dossierId") String dossierId,
            @Valid @RequestBody ReassignDossierBody body) {
        log.debug("Reassigning dossier [{}] to new steller [{}]", dossierId, body.getNewWriter());
        reassignService.reassignDossier(dossierId, body);
    }

    /**
     * Reassign a task to a given user.
     *
     * @param taskId the ID of the task.
     * @param body   the requestBody containing the dossier ID and new assignee ID.
     * @return ResponseEntity<String>
     */
    @Authority(requiredAuthorities = {"PROCES_REGISSEURS"})
    @PutMapping("/{taskId}/assignee")
    public ResponseEntity<String> reassignTask(
            @PathVariable(name = "taskId") String taskId,
            @Valid @RequestBody ReassignTaskBody body) {
        log.debug("Reassigning task with ID [{}] to user [{}]", taskId, body.getUserId());
        try {
            return reassignService.reassignTask(taskId, body);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(String.format("Task '%s' not found", taskId), HttpStatus.NOT_FOUND);
        }
    }
}
