package com.activiti.extension.bean.tasklisteners;

import com.activiti.domain.runtime.EmailTemplate;
import com.activiti.extension.bean.utils.services.CustomEmailService;
import com.activiti.extension.bean.utils.services.CustomTaskService;
import com.activiti.model.runtime.TaskRepresentation;
import com.activiti.service.runtime.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Incentro
 * <p>
 * Original code:
 * https://github.com/Activiti/Activiti/blob/6.x/modules/activiti-engine/src/main/java/org/activiti/engine/impl/bpmn/behavior/MailActivityBehavior.java#L97
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailTaskListener implements TaskListener {

    private final EmailTemplateService emailTemplateService;
    private final CustomEmailService customEmailService;
    private final CustomTaskService customTaskService;

    private static final String PROCESS_VARIABLE_DOSSIERID = "dossierId";
    private static final String PROCESS_VARIABLE_SUBJECT = "subject";

    private static final String TEMPLATE_PARAM_TASK_NAME = "naam_toetstaak";
    private static final String TEMPLATE_PARAM_DOSSIER_NUMBER = "dossiernummer";
    private static final String TEMPLATE_PARAM_DOSSIER_SUBJECT = "dossier_onderwerp";

    /**
     * Send an email with the given email template
     *
     * @param delegateTask      Task which the listener is added to
     * @param emailTemplateName Name of the custom template which will be used to send the email
     * @param variableName      Name of the variable which value will be check to determinate if the emails should be sent or not
     * @param sendIfValue       When the value of the given variable is equals to this parameter, the emails will be sent
     */
    public void sendEmail(DelegateTask delegateTask, String emailTemplateName, String variableName, String sendIfValue, String... includedTaskNames) {
        try {
            // Check if the email should be sent
            if (!StringUtils.equals((String) delegateTask.getVariable(variableName), sendIfValue)) {
                log.debug(String.format("Email won´t be sent because the value of ´%s´ variable is ´%s´ instead of ´%s´.", variableName, delegateTask.getVariable(variableName), sendIfValue));
                return;
            }

            // Get the current active tasks
            List<TaskRepresentation> activeTasks = customTaskService.getActiveTasks(delegateTask.getExecution().getProcessInstanceId());

            log.debug(String.format("Found ´%s´ active tasks.", activeTasks.size()));
            // Get email template
            EmailTemplate emailTemplate = customEmailService.getCustomEmailTemplate(Long.parseLong(delegateTask.getTenantId().substring(7)), emailTemplateName);

            // Execute it once per active task
            for (String includeTask : includedTaskNames) {
                Optional<TaskRepresentation> optionalActiveTask = activeTasks.stream().filter(activeT -> includeTask.equals(activeT.getName())).findFirst();
                // Only send the emails if the task is in the list
                if (optionalActiveTask.isPresent()) {
                    TaskRepresentation activeTask = optionalActiveTask.get();
                    log.debug(String.format("Sending email for task ´%s´.", activeTask.getId()));
                    // Get the user emails
                    List<String> emailAddresses = customEmailService.getTaskEmailAddresses(activeTask);
                    // Prepare email content
                    EmailTemplateService.ProcessedEmailTemplate processedTemplate = processCustomEmailTemplate(delegateTask, emailTemplate, activeTask.getName());
                    // Prepare email
                    HtmlEmail email = customEmailService.createEmail(emailAddresses, processedTemplate);
                    // Send the email only when there are email addresses
                    if (!emailAddresses.isEmpty()) {
                        email.send();
                    }
                }
            }
        } catch (EmailException e) {
            String msg = "Could not send e-mail when complete task " + delegateTask.getExecution().getId();
            log.error("Error trying to send a custom email: " + msg, e);
        } catch (Exception e) {
            log.error("Error trying to send a custom email: " + e.getMessage(), e);
        }
    }


    /**
     * Process the email template replacing the place holders in the template for the final values.
     *
     * @param delegateTask  Object which represent the execution of the current task.
     * @param emailTemplate Email template to process.
     * @param taskName      Name of the task which will be used as a value to process the template.
     * @return Email template processed
     * @throws Exception Exception which can occurs when the email template is processed
     */
    public EmailTemplateService.ProcessedEmailTemplate processCustomEmailTemplate(DelegateTask delegateTask, EmailTemplate emailTemplate, String taskName) throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(TEMPLATE_PARAM_TASK_NAME, taskName);
        String dossierId = (String) delegateTask.getExecution().getVariable(PROCESS_VARIABLE_DOSSIERID);
        properties.put(TEMPLATE_PARAM_DOSSIER_NUMBER, dossierId);
        String dossierSubject = (String) delegateTask.getExecution().getVariable(PROCESS_VARIABLE_SUBJECT);
        properties.put(TEMPLATE_PARAM_DOSSIER_SUBJECT, dossierSubject);
        return emailTemplateService.processCustomEmailTemplate(emailTemplate.getId(), properties);
    }

    @Override
    public void notify(DelegateTask delegateTask) {

    }
}
