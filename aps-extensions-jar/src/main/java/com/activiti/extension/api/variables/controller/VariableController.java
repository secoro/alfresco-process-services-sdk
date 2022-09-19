package com.activiti.extension.api.variables.controller;

import com.activiti.extension.api.variables.model.BulkUpdateDossiersProcessesModel;
import com.activiti.extension.api.variables.service.VariableService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.activiti.extension.api.util.HttpStatusMessages.BULK_UPDATE_VARIABLES_SUCCESSFULL;
import static com.activiti.extension.api.util.HttpStatusMessages.INTERNAL_SERVER_ERROR_MESSAGE;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/enterprise/custom/variables",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class VariableController {

    private final VariableService variableService;

    @ApiOperation("Bulk update variables")
    @PutMapping()
    public ResponseEntity<String> getProcessesForDossierId(
            @RequestBody BulkUpdateDossiersProcessesModel body) {
        log.debug("Bulk updating processes. PUT REST API called.");
        try {
            variableService.bulkUpdateProcesses(body);
            return new ResponseEntity<>(String.format(BULK_UPDATE_VARIABLES_SUCCESSFULL, body.getDossierIds()), HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
