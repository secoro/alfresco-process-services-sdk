package com.activiti.extension.bean.utils;

import com.activiti.extension.api.dossier.model.UpdateDossierDTO;
import com.activiti.extension.model.Assignee;
import com.activiti.extension.model.AssigneeType;
import com.activiti.extension.model.DossierPermission;
import com.activiti.extension.model.UpdateDossierPermissionsModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.task.Task;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.util.Collections;

import static com.activiti.extension.bean.utils.literal.EndPoint.*;

@Slf4j
@AllArgsConstructor
@Component
public class DossierUtils {

    private final ACSHTTPClient acshttpClient;

    public void updateDossierProperties(String dossierId, UpdateDossierDTO dto) {
        log.debug("Updating properties of dossier [{}] with properties [{}]", dossierId, dto);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(dto);
            Stopwatch stopwatch = Stopwatch.createStarted();
            acshttpClient.execute(String.format(ICM_DOSSIER, dossierId), jsonString, HttpMethod.PUT.name(), null, ICM);
            log.debug("Total HTTP Execution Time: {}", stopwatch.stop());
            log.debug("Dossier [{}] updated with following body [{}]", dossierId, dto);
        } catch (IOException e) {
            log.debug("Could not write object to string");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.debug("Dossier with ID [{}] not found", dossierId);
                throw e;
            }
            throw e;
        }
    }

    public void updateDossierPermissions(String permission, Boolean increase, String dossierId, String taskId, Assignee assignee, DelegateTask task) {
        log.debug("Updating permissions for dossier [{}]. Updating to [{}] permissions for user [{}] and increaseMode [{}].", dossierId, permission, assignee.getId(), increase);

        DossierPermission dossierPermission = DossierPermission.valueOf(permission);

        //on assignment, only set write permissions if assignee.type is user, because groups only get Read permissions.
        if (task.getEventName().equals("assignment") && assignee.getType().equals(AssigneeType.group)) {
            dossierPermission = DossierPermission.READ;
        }

        UpdateDossierPermissionsModel model = UpdateDossierPermissionsModel.builder()
                .increaseMode(increase)
                .taskId(taskId)
                .assignees(Collections.singletonList(assignee))
                .permission(dossierPermission)
                .build();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(model);
            Stopwatch stopwatch = Stopwatch.createStarted();
            acshttpClient.execute(String.format(ICM_DOSSIER_PERMISSIONS, dossierId), jsonString, HttpMethod.PUT.name(), null, ICM);
            log.debug("Total HTTP Execution Time: {}", stopwatch.stop());
            log.debug("Permissions are updated to {} for task {} on dossier {} with increase mode {}", permission, taskId, dossierId, increase);
        } catch (IOException e) {
            log.debug("Could not write object to string");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.debug("Tried to update non-existent dossier", e);
            } else {
                throw e;
            }
        }
    }

    public void updateDossierPermissions(String permission, Boolean increase, String dossierId, String taskId, Assignee assignee, Task task, String eventName) {
        log.debug("Updating permissions for dossier [{}]. Updating to [{}] permissions for user [{}] and increaseMode [{}].", dossierId, permission, assignee.getId(), increase);

        DossierPermission dossierPermission = DossierPermission.valueOf(permission);

        //on assignment, only set write permissions if assignee.type is user, because groups only get Read permissions.
        if (eventName.equals("assignment") && assignee.getType().equals(AssigneeType.group)) {
            dossierPermission = DossierPermission.READ;
        }

        UpdateDossierPermissionsModel model = UpdateDossierPermissionsModel.builder()
                .increaseMode(increase)
                .taskId(taskId)
                .assignees(Collections.singletonList(assignee))
                .permission(dossierPermission)
                .build();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(model);
            Stopwatch stopwatch = Stopwatch.createStarted();
            acshttpClient.execute(String.format(ICM_DOSSIER_PERMISSIONS, dossierId), jsonString, HttpMethod.PUT.name(), null, ICM);
            log.debug("Total HTTP Execution Time: {}", stopwatch.stop());
            log.debug("Permissions are updated to {} for task {} on dossier {} with increase mode {}", permission, taskId, dossierId, increase);
        } catch (IOException e) {
            log.debug("Could not write object to string");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.debug("Tried to update non-existent dossier", e);
            } else {
                throw e;
            }
        }
    }

    public void updateDossierPermissions(DossierPermission permission, Boolean increase, String dossierId, String taskId, Assignee assignee) {
        log.debug("Updating permissions for dossier [{}]. Updating to [{}] permissions for user [{}] and increaseMode [{}].", dossierId, permission, assignee.getId(), increase );

        UpdateDossierPermissionsModel model = UpdateDossierPermissionsModel.builder()
                .increaseMode(increase)
                .taskId(taskId)
                .assignees(Collections.singletonList(assignee))
                .permission(permission)
                .build();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(model);
            Stopwatch stopwatch = Stopwatch.createStarted();
            acshttpClient.execute(String.format(ICM_DOSSIER_PERMISSIONS, dossierId), jsonString, HttpMethod.PUT.name(), null, ICM);
            log.debug("Total HTTP Execution Time: {}", stopwatch.stop());
            log.debug("Permissions are updated to {} for task {} on dossier {} with increase mode {}", permission, taskId, dossierId, increase);
        } catch (IOException e) {
            log.debug("Could not write object to string");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.debug("Tried to update non-existent dossier", e);
                throw e;
            }
            throw e;
        }
    }
}
