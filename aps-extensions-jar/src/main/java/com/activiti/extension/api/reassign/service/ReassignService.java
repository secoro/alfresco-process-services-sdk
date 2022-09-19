package com.activiti.extension.api.reassign.service;

import com.activiti.domain.idm.User;
import com.activiti.extension.api.dossier.model.UpdateDossierDTO;
import com.activiti.extension.api.processinstance.service.CustomProcessInstanceService;
import com.activiti.extension.api.reassign.model.ReassignDossierBody;
import com.activiti.extension.api.reassign.model.ReassignTaskBody;
import com.activiti.extension.api.tasks.CustomTaskService;
import com.activiti.extension.api.variables.service.VariableService;
import com.activiti.extension.bean.utils.DossierUtils;
import com.activiti.extension.bean.utils.SecurityUtils;
import com.activiti.extension.bean.utils.services.CustomEmailService;
import com.activiti.extension.constant.APSConstants;
import com.activiti.extension.model.Assignee;
import com.activiti.extension.model.AssigneeType;
import com.activiti.extension.model.DossierPermission;
import com.activiti.service.api.UserService;
import com.activiti.service.exception.BadRequestException;
import com.activiti.service.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.activiti.extension.constant.APSConstants.*;
import static com.activiti.extension.constant.APSConstants.Process.DELAYREQUEST;
import static com.activiti.extension.constant.APSConstants.Process.MAIN;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReassignService {

    private final SecurityUtils securityUtils;
    private final UserService userService;
    private final DossierUtils dossierUtils;
    private final TaskService taskService;
    private final CustomTaskService customTaskService;
    private final CustomProcessInstanceService customProcessInstanceService;
    private final VariableService variableService;
    private final CustomEmailService customEmailService;

    /**
     * Method for reassigning tasks.
     *
     * @param taskId The task ID to be used.
     * @param body   The body containing dossier ID and user ID of the new assignee.
     * @return ResponseEntity<String>
     */
    public ResponseEntity<String> reassignTask(String taskId, ReassignTaskBody body) {
        User initiator = securityUtils.getCurrentActivitiUser();
        Optional<HistoricTaskInstance> taskByIdOptional = customTaskService.getTaskById(taskId);
        HistoricTaskInstance task = taskByIdOptional.orElseThrow(NotFoundException::new);

        String taskPermission = (String) taskService.getVariable(taskId, "taskPermission");
        DossierPermission permission = DossierPermission.fromText(taskPermission.toLowerCase(Locale.ROOT));

        Assignee oldAssignee = Assignee.builder().id(task.getAssignee()).type(AssigneeType.user).build();
        Assignee newAssignee = Assignee.builder().id(String.valueOf(body.getUserId())).type(AssigneeType.user).build();
        User oldAssigneeUser = userService.getUser(Long.valueOf(oldAssignee.getId()));
        User newAssigneeUser = userService.getUser(Long.valueOf(newAssignee.getId()));

        taskService.unclaim(taskId);
        taskService.claim(taskId, newAssignee.getId());
        dossierUtils.updateDossierPermissions(permission, false, body.getDossierId(), taskId, oldAssignee);
        dossierUtils.updateDossierPermissions(permission, true, body.getDossierId(), taskId, newAssignee);

        sendReassignTaskEmails(body, initiator, task, oldAssigneeUser, newAssigneeUser);

        return new ResponseEntity<>(String.format("Task with ID '%s' has successfully been reassigned to %s", taskId, body.getUserId()), HttpStatus.OK);
    }

    /**
     * This method kicks off the reassignment logic for a dossier.
     *
     * @param dossierId the dossier to reassign
     * @param body      body containing which processes should be included and the newWriter
     */
    public void reassignDossier(String dossierId, ReassignDossierBody body) throws NotFoundException, BadRequestException {
        log.debug("Reassigning dossier [{}] processes [{}] to newWriter [{}]", dossierId, body.getProcessTypes(), body.getNewWriter());

        User newWriter = userService.getUser(body.getNewWriter());

        Optional<ProcessInstance> mainOptional = customProcessInstanceService.getProcessInstanceForDossierIdAndProcessType(dossierId, MAIN);
        Optional<ProcessInstance> delayRequestOptional = customProcessInstanceService.getProcessInstanceForDossierIdAndProcessType(dossierId, DELAYREQUEST);

        verifyCanReassignProcess(dossierId, newWriter, body.getProcessTypes(), mainOptional, delayRequestOptional);

        try {
            body.getProcessTypes().forEach(processType -> reassignProcess(dossierId, processType, newWriter));
            sendReassignDossierEmails(dossierId, newWriter, body.getProcessTypes(), mainOptional, delayRequestOptional, false);
        } catch (Exception e) {
            sendReassignDossierEmails(dossierId, newWriter, body.getProcessTypes(), mainOptional, delayRequestOptional, true);
            throw e; // need to rethrow exception so that RestControllerAdvice picks it up and returns the correct HttpStatus + message to the client
        }
    }

    /**
     * Method responsible for reassigning a process. It does the following:
     * 1. Reassigns active steller tasks
     * 2. Updates dossier permissions
     * 3. Updates relevant process variables
     * <p>
     * If it's a MAIN process it does additional things:
     * 4. Updates the case:writer property of the dossier
     * 5. Updates the steller process variable of the DELAYREQUEST process if present
     *
     * @param dossierId   the dossier
     * @param processType the processType
     * @param newWriter   the newWriter
     */
    private void reassignProcess(String dossierId, APSConstants.Process processType, User newWriter) throws NotFoundException, BadRequestException {
        log.debug("Reassigning process [{}] belonging to dossier [{}] to new writer [{}]", processType, dossierId, newWriter.getFullName());

        Optional<ProcessInstance> processInstanceOptional = customProcessInstanceService.getProcessInstanceForDossierIdAndProcessType(dossierId, processType);

        processInstanceOptional.ifPresentOrElse(processInstance -> {
            User oldWriter = processType.equals(MAIN) ? userService.getUser((Long) processInstance.getProcessVariables().get(STELLER)) : userService.getUser((Long) processInstance.getProcessVariables().get(STELLER_TUSSENBERICHT));

            reassignStellerTasksAndSettingDossierPermissions(dossierId, processType, oldWriter, newWriter);
            Map<String, Object> variables = processType.equals(MAIN) ? Map.of(STELLER, newWriter.getId(), STELLER_FULLNAME, newWriter.getFullName(), STELLER_EMAIL, newWriter.getEmail()) : Map.of(STELLER_TUSSENBERICHT, newWriter.getId(), STELLER_TUSSENBERICHT_FULLNAME, newWriter.getFullName());
            variableService.updateProcessVariablesIfExists(processInstance, variables);

            if (processType.equals(MAIN)) {
                dossierUtils.updateDossierProperties(dossierId, UpdateDossierDTO.builder().writer(newWriter.getFullName()).build());
                Optional<ProcessInstance> delayRequestOptional = customProcessInstanceService.getProcessInstanceForDossierIdAndProcessType(dossierId, DELAYREQUEST);
                delayRequestOptional.ifPresent(delayRequest -> variableService.updateProcessVariablesIfExists(delayRequest, Map.of(STELLER, newWriter.getId())));
            }
        }, () -> log.debug("No process of type [{}] found for dossier [{}]", processType, dossierId));
    }

    /**
     * Helper method that determines if a process can be reassigned.
     * - newWriter should exist
     * - only the MAIN and DELAYREQUEST processTypes can be reassigned
     * - main process should exist
     *
     * @param dossierId            the dossier
     * @param newWriter            the newWriter
     * @param processTypes         the processTypes
     * @param mainOptional         optional main process
     * @param delayRequestOptional optional delayrequest process
     */
    private void verifyCanReassignProcess(String dossierId, User newWriter, List<APSConstants.Process> processTypes, Optional<ProcessInstance> mainOptional, Optional<ProcessInstance> delayRequestOptional) throws NotFoundException, BadRequestException {
        if (Objects.isNull(newWriter)) {
            log.debug("New writer or old writer could not be found");
            throw new NotFoundException("New writer or oldWriter could not be found");
        }

        if (!processTypes.stream().allMatch(type -> type.equals(MAIN) || type.equals(DELAYREQUEST))) {
            log.debug("Process type [{}] not supported", processTypes);
            throw new BadRequestException(String.format("Process type [%s] not supported", processTypes));
        }

        if (mainOptional.isEmpty()) {
            log.debug("No active process MAIN process found for dossier [{}]", dossierId);
            throw new NotFoundException(String.format("No active process MAIN process found for dossier [%s]", dossierId));
        }

        if (processTypes.stream().filter(type -> type.equals(DELAYREQUEST)).count() == 1 && delayRequestOptional.isEmpty()) {
            log.debug("No active process DELAYREQUEST process found for dossier [{}]", dossierId);
            throw new NotFoundException(String.format("No active process DELAYREQUEST process found for dossier [%s]", dossierId));
        }
    }

    /**
     * Method responsible for reassigning the individual tasks and setting dossier permissions for the old- and newWriter.
     *
     * @param dossierId   the dossier
     * @param processType the processType
     * @param oldWriter   the oldWriter
     * @param newWriter   the newWriter
     */
    private void reassignStellerTasksAndSettingDossierPermissions(String dossierId, APSConstants.Process processType, User oldWriter, User newWriter) {
        List<String> activeStellerTasks = customTaskService.getActiveStellerTasksPerDossierAndProcessType(dossierId, processType);

        Assignee oldAssignee = Assignee.fromUser(oldWriter);
        Assignee newAssignee = Assignee.fromUser(newWriter);

        activeStellerTasks.forEach(taskId -> {
            log.debug("Updating steller task [{}]", taskId);

            String taskPermission = taskService.getVariable(taskId, "taskPermission", String.class);
            dossierUtils.updateDossierPermissions(DossierPermission.READ, false, dossierId, taskId, oldAssignee);
            dossierUtils.updateDossierPermissions(DossierPermission.fromText(taskPermission.toLowerCase(Locale.ROOT)), true, dossierId, taskId, newAssignee);
            taskService.unclaim(taskId);
            taskService.claim(taskId, newAssignee.getId());
        });
    }

    /**
     * Method that is responsible for sending all reassign task emails.
     *
     * @param body            the reassignTaskBody
     * @param initiator       the user that initiated the call
     * @param taskById        the task id
     * @param oldAssigneeUser the old assignee user object
     * @param newAssigneeUser the new assignee user object
     */
    private void sendReassignTaskEmails(ReassignTaskBody body, User initiator, HistoricTaskInstance taskById, User oldAssigneeUser, User newAssigneeUser) {
        sendReassignTaskEmailForRecipient(body, "initiator", initiator, taskById);
        sendReassignTaskEmailForRecipient(body, "oldAssignee", oldAssigneeUser, taskById);
        sendReassignTaskEmailForRecipient(body, "newAssignee", newAssigneeUser, taskById);
    }

    /**
     * Method that is responsible for sending all reassign dossier emails
     *
     * @param dossierId            the dossier
     * @param newWriter            the new writer
     * @param mainOptional         optional main process
     * @param delayRequestOptional optional delayrequest process
     * @param errorMail            boolean indicating if this is an errormail
     */
    private void sendReassignDossierEmails(String dossierId, User newWriter, List<APSConstants.Process> processTypes, Optional<ProcessInstance> mainOptional, Optional<ProcessInstance> delayRequestOptional, boolean errorMail) {
        log.debug("Sending out{} email(s) for reassignment of dossier [{}] to [{}]", errorMail ? " error" : "", dossierId, newWriter.getFullName());

        User initiator = securityUtils.getCurrentActivitiUser();

        mainOptional.ifPresentOrElse(main -> {
            String subject = (String) main.getProcessVariables().get(SUBJECT);
            Long oldWriterId = (Long) main.getProcessVariables().get(STELLER);
            User oldWriter = userService.getUser(oldWriterId);

            String urgencyPrefix = Optional
                    .ofNullable((Boolean) main.getProcessVariables().get(IS_URGENT))
                    .filter(isUrgent -> isUrgent)
                    .map(isUrgent -> URGENCY_EMAIL_PREFIX)
                    .orElse("");

            if (errorMail) {
                List<String> emailAddresses = customEmailService.getGroupEmailAddresses("FUNC_FUNCTIONEEL_BEHEER");
                sendReassignDossierEmailForRecipient(dossierId, emailAddresses, "reassignedDossierFailed", oldWriter, newWriter, subject, urgencyPrefix + "Er is iets mis gegaan bij het omboeken!");
            } else {
                sendReassignDossierEmailForRecipient(dossierId, List.of(initiator.getEmail()), "initiator", oldWriter, newWriter, subject, urgencyPrefix + "Een dossier is succesvol omgeboekt!");

                if (processTypes.contains(MAIN) && processTypes.contains(DELAYREQUEST)) {
                    sendReassignDossierEmailForRecipient(dossierId, List.of(oldWriter.getEmail()), "oldWriter", oldWriter, newWriter, subject, urgencyPrefix + "In DIVA-BB is jouw dossier omgeboekt naar een ander!");
                    sendReassignDossierEmailForRecipient(dossierId, List.of(newWriter.getEmail()), "newWriter", oldWriter, newWriter, subject, urgencyPrefix + "In DIVA-BB is een dossier omgeboekt naar jou!");

                    delayRequestOptional.ifPresentOrElse(delayRequest -> {
                        Long oldDelayRequestWriterId = (Long) delayRequest.getProcessVariables().get(STELLER_TUSSENBERICHT);
                        User oldDelayRequestWriter = userService.getUser(oldDelayRequestWriterId);
                        sendReassignDossierEmailForRecipient(dossierId, List.of(oldDelayRequestWriter.getEmail()), "oldDelayRequestWriter", oldWriter, newWriter, subject, urgencyPrefix + "In DIVA-BB is jouw tussenbericht omgeboekt naar een ander!");
                    }, () -> log.debug("No delayrequest found for dossier [{}]", dossierId));
                }

                if (processTypes.size() == 1 && processTypes.contains(MAIN)) {
                    sendReassignDossierEmailForRecipient(dossierId, List.of(oldWriter.getEmail()), "oldWriter", oldWriter, newWriter, subject, urgencyPrefix + "In DIVA-BB is jouw dossier omgeboekt naar een ander!");
                    sendReassignDossierEmailForRecipient(dossierId, List.of(newWriter.getEmail()), "newWriter", oldWriter, newWriter, subject, urgencyPrefix + "In DIVA-BB is een dossier omgeboekt naar jou!");
                }

                if (processTypes.size() == 1 && processTypes.contains(DELAYREQUEST)) {
                    delayRequestOptional.ifPresentOrElse(delayRequest -> {
                        Long oldDelayRequestWriterId = (Long) delayRequest.getProcessVariables().get(STELLER_TUSSENBERICHT);
                        User oldDelayRequestWriter = userService.getUser(oldDelayRequestWriterId);
                        sendReassignDossierEmailForRecipient(dossierId, List.of(newWriter.getEmail()), "newDelayRequestWriter", oldWriter, newWriter, subject, urgencyPrefix + "In DIVA-BB is een tussenbericht omgeboekt naar jou!");
                        sendReassignDossierEmailForRecipient(dossierId, List.of(oldDelayRequestWriter.getEmail()), "oldDelayRequestWriter", oldWriter, newWriter, subject, urgencyPrefix + "In DIVA-BB is jouw tussenbericht omgeboekt naar een ander!");
                    }, () -> {
                        throw new NotFoundException(String.format("No active process DELAYREQUEST found for dossier [%s]", dossierId));
                    });
                }
            }
        }, () -> {
            throw new NotFoundException(String.format("No active process MAIN found for dossier [%s]", dossierId));
        });
    }

    /**
     * Method responsible for sending the reassign task emails
     *
     * @param body the body containing the new assignee ID
     * @param user the person the email will be sent to
     * @param task the task that has been assigned
     */
    private void sendReassignTaskEmailForRecipient(ReassignTaskBody body, String recipient, User user, HistoricTaskInstance task) {
        try {

            String subject = (String) taskService.getVariable(task.getId(), SUBJECT);

            String urgencyPrefix = Optional
                    .ofNullable((Boolean) taskService.getVariable(task.getId(), IS_URGENT))
                    .filter(isUrgent -> isUrgent)
                    .map(isUrgent -> URGENCY_EMAIL_PREFIX)
                    .orElse("");

            HtmlEmail initiatorEmail = customEmailService.prepareReassignTaskEmail(user.getTenantId(), Collections.singletonList(user.getEmail()), recipient, task.getName(), body.getDossierId(), subject, urgencyPrefix + "Er is een taak omgeboekt!");
            initiatorEmail.send();
        } catch (Exception e) {
            log.debug("Email could not be sent to [{}]: [{}]", user.getFullName(), e.getMessage());
        }
    }

    /**
     * Method responsible for sending emails for dossier reassignment
     *
     * @param dossierId      the dossier
     * @param emailAddresses list of email addresses to which the emails will be sent
     * @param recipient      the type of recipient (oldWriter, newWriter, delayRequestWriter)
     * @param oldWriter      the oldWriter user
     * @param newWriter      the newWriter user
     * @param subject        the dossier subject
     * @param emailSubject   the email subject
     */
    public void sendReassignDossierEmailForRecipient(String dossierId, List<String> emailAddresses, String recipient, User oldWriter, User newWriter, String subject, String emailSubject) {
        try {
            HtmlEmail htmlEmail = customEmailService.prepareReassignDossierEmail(oldWriter.getTenantId(), emailAddresses, recipient, dossierId, subject, oldWriter.getFullName(), newWriter.getFullName(), emailSubject);
            htmlEmail.send();
        } catch (Exception e) {
            log.error("Error while sending email: {}", e.getMessage());
            log.debug("Exception details: ", e);
        }
    }
}
