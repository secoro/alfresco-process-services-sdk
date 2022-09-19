package com.activiti.extension.api.processinstance.controller;

import com.activiti.extension.api.processinstance.service.CustomProcessInstanceService;
import com.activiti.extension.bean.exceptions.ForbiddenException;
import com.activiti.model.common.ResultListDataRepresentation;
import com.activiti.model.runtime.ProcessInstanceRepresentation;
import com.activiti.service.exception.ConflictingRequestException;
import com.activiti.service.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/enterprise/custom/process-instances",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class CustomProcessInstanceController {

    private final CustomProcessInstanceService customProcessInstanceService;

    @GetMapping("/{dossierId}")
    public ResultListDataRepresentation<ProcessInstanceRepresentation> getProcessInstancesForDossier(
            @PathVariable(name = "dossierId") String dossierId,
            @RequestParam(name = "processTypes", defaultValue = "MAIN,DELAYREQUEST,ADHOC") List<String> processTypes) {
        log.debug("Getting processes for dossier with ID");
        return customProcessInstanceService.getProcessesForDossier(dossierId, processTypes);
    }

    @PutMapping(value = "/{processInstanceId}/suspend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> suspendProcess(@PathVariable Long processInstanceId) {

        try {
            customProcessInstanceService.suspendProcess(processInstanceId);
            return new ResponseEntity<>(
                    String.format("Process-instance with id: %s, is suspended", processInstanceId),
                    HttpStatus.OK);
        } catch (ConflictingRequestException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (ForbiddenException e) {
            return new ResponseEntity<>("User is not initiator or superuser", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping(value = "/{processInstanceId}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> activateProcess(@PathVariable Long processInstanceId) {

        try {
            customProcessInstanceService.activateProcess(processInstanceId);
            return new ResponseEntity<>(
                    String.format("Process-instance with id: %s, is activated", processInstanceId),
                    HttpStatus.OK);
        } catch (ConflictingRequestException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.ACCEPTED);
        } catch (ForbiddenException e) {
            return new ResponseEntity<>("User is not initiator or superuser", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value = "/{processInstanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteProcess(@PathVariable Long processInstanceId) {

        try {
            customProcessInstanceService.deleteProcessInstance(processInstanceId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);

        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ForbiddenException e) {
            return new ResponseEntity<>("User is not initiator or superuser", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
