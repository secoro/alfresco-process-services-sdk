package com.activiti.extension.bean.utils.services;

import com.activiti.domain.idm.Group;
import com.activiti.domain.idm.User;
import com.activiti.domain.runtime.EmailTemplate;
import com.activiti.extension.api.dossier.model.LightDossierRepresentation;
import com.activiti.extension.api.tasks.model.CustomTaskRepresentation;
import com.activiti.extension.bean.utils.literal.Cluster;
import com.activiti.extension.bean.utils.literal.RegexConstants;
import com.activiti.model.runtime.TaskRepresentation;
import com.activiti.service.api.GroupService;
import com.activiti.service.runtime.EmailTemplateService;
import com.amazonaws.util.CollectionUtils;
import com.mchange.util.DuplicateElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.task.IdentityLink;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomEmailService implements EnvironmentAware {

    private final GroupService groupService;
    private final EmailTemplateService emailTemplateService;
    private final AssigneeService assigneeService;

    @Value("${email.from.default.name:DIVA-BB}")
    private String FROM_ALIAS;

    @Value("${email.from.default:activiti@localhost}")
    private String FROM_EMAIL;

    private String baseUrl;
    private String mailServerHost;
    private int mailServerPort;
    private boolean mailServerUseSSL;
    private boolean mailServerUserTLS;
    private String mailServerUsername;
    private String mailServerPassword;

    public static final String NEW_TASK_BB_TEMPLATE = "New task BB template";
    public static final String TASK_REASSIGNED = "Task Reassigned";
    public static final String DOSSIER_REASSIGNED = "Dossier Reassigned";
    public static final String USER_RESIGNED = "User Resigned";

    /**
     * Set the email server properties needed to send the email
     *
     * @param email email object where the properties will be set
     */
    public void setMailServerProperties(Email email) {
        if (mailServerHost == null) {
            throw new ActivitiException("Could not send email: no SMTP host is configured");
        }
        email.setHostName(mailServerHost);
        email.setSmtpPort(mailServerPort);
        email.setSSLOnConnect(mailServerUseSSL);
        email.setStartTLSEnabled(mailServerUserTLS);

        if (mailServerUsername != null && mailServerPassword != null) {
            email.setAuthentication(mailServerUsername, mailServerPassword);
        }
    }

    /**
     * Get a custom email template by name
     * @param tenantId tenant identifier where the template is stored
     * @param emailTemplateName Name of the template we are looking for
     * @return Email template object which represents the found template
     */
    public EmailTemplate getCustomEmailTemplate(Long tenantId, String emailTemplateName) {
        // Get email template
        EmailTemplate emailTemplate = emailTemplateService.findCustomEmailTemplate(tenantId, emailTemplateName);
        if(emailTemplate == null){
            throw new RuntimeException();
        }
        return emailTemplate;
    }

    /**
     * Makes an email for a task.
     * @param delegateTask is the task for which the email should be sent.
     * @param emailTemplateName name of the email template to be used.
     * @param emailAddresses list of email adresses to which the email should be sent.
     * @return calls te createEmail which returns an email.
     * @throws Exception thrown by processCustomEmailTempate method
     */
    public HtmlEmail prepareEmail(DelegateTask delegateTask, String emailTemplateName, List<String> emailAddresses) throws Exception {
        //get email template
        EmailTemplate emailTemplate = getCustomEmailTemplate(Long.parseLong(delegateTask.getTenantId().substring(7)), emailTemplateName);
        //prepare email content
        Map<String, Object> properties = new HashMap<>();
        properties.put("taskName", delegateTask.getName());
        properties.put("dossierId", delegateTask.getVariable("dossierId"));
        properties.put("subject", delegateTask.getVariable("subject"));
        properties.put("owner", delegateTask.getVariable("owner"));
        properties.put("homeUrl", baseUrl);

        EmailTemplateService.ProcessedEmailTemplate processedTemplate = emailTemplateService.processCustomEmailTemplate(emailTemplate.getId(), properties);
        //prepare email
        return createEmail(emailAddresses, processedTemplate);
    }

    public HtmlEmail prepareNewTaskEmail(Long tenantId, List<String> emailAddresses, String taskName, String dossierId, String subject, String owner, Boolean isUrgent) throws Exception {
        EmailTemplate emailTemplate = getCustomEmailTemplate(tenantId, NEW_TASK_BB_TEMPLATE);

        Map<String, Object> properties = new HashMap<>();
        properties.put("homeUrl", this.baseUrl);
        properties.put("taskName", taskName);
        properties.put("dossierId", dossierId);
        properties.put("subject", subject);
        properties.put("owner", owner);
        properties.put("isUrgent", isUrgent);

        EmailTemplateService.ProcessedEmailTemplate processedTemplate = emailTemplateService.processCustomEmailTemplate(emailTemplate.getId(), properties);
        return createEmail(emailAddresses, processedTemplate);
    }

    public HtmlEmail prepareReassignTaskEmail(Long tenantId, List<String> emailAddresses, String recipient, String taskName, String dossierId, String subject, String emailSubject) throws Exception {
        EmailTemplate emailTemplate = getCustomEmailTemplate(tenantId, TASK_REASSIGNED);

        Map<String, Object> properties = new HashMap<>();
        properties.put("recipient", recipient);
        properties.put("taskName", taskName);
        properties.put("dossierId", dossierId);
        properties.put("subject", subject);
        properties.put("emailSubject", emailSubject);

        EmailTemplateService.ProcessedEmailTemplate processedEmailTemplate = emailTemplateService.processCustomEmailTemplate(emailTemplate.getId(), properties);

        return createEmail(emailAddresses, processedEmailTemplate);
    }

    public HtmlEmail prepareReassignDossierEmail(Long tenantId, List<String> emailAddresses, String recipient, String dossierId, String subject, String oldWriterName, String newWriterName, String emailSubject) throws Exception {
        EmailTemplate emailTemplate = getCustomEmailTemplate(tenantId, DOSSIER_REASSIGNED);

        Map<String, Object> properties = new HashMap<>();
        properties.put("recipient", recipient);
        properties.put("dossierId", dossierId);
        properties.put("subject", subject);
        properties.put("oldWriter", oldWriterName);
        properties.put("newWriter", newWriterName);
        properties.put("homeUrl", this.baseUrl);
        properties.put("emailSubject", emailSubject);

        EmailTemplateService.ProcessedEmailTemplate processedEmailTemplate = emailTemplateService.processCustomEmailTemplate(emailTemplate.getId(), properties);

        return createEmail(emailAddresses, processedEmailTemplate);
    }

    public HtmlEmail prepareNotifyProcessManagersEmail(User user, Cluster cluster, List<LightDossierRepresentation> dossierList, List<CustomTaskRepresentation> taskList) throws Exception {
        EmailTemplate emailTemplate = getCustomEmailTemplate(1L, USER_RESIGNED);

        Map<String, Object> properties = new HashMap<>();
        properties.put("userFullName", user.getFullName());
        properties.put("dossierList", dossierList);
        properties.put("taskList", taskList);
        properties.put("homeUrl", baseUrl);

        EmailTemplateService.ProcessedEmailTemplate processedEmailTemplate = emailTemplateService.processCustomEmailTemplate(emailTemplate.getId(), properties);

        String clusterWithoutPrefix = cluster.name().replaceFirst("ORG_", "");

        List<String> emailAddresses = getGroupEmailAddresses("FUNC_PROCES_REGISSEURS_" + clusterWithoutPrefix);

        if (!emailAddresses.isEmpty()) {
            return createEmail(emailAddresses, processedEmailTemplate);
        } else {
            log.debug("There are no users in FUNC_PROCES_REGISSEURS_" + clusterWithoutPrefix);
            return null;
        }
    }

    /**
     * Create an HtmlEmail object which represents the email to be sent, and contains the logic to sent it.
     *
     * @param emailAddresses    List of email addresses of the user that will receive the email.
     * @param processedTemplate Email template already processed with the values of the variables.
     * @return Object which represents the email.
     * @throws EmailException Exception which can be sent by the HtmlEmail class.
     */
    public HtmlEmail createEmail(List<String> emailAddresses, EmailTemplateService.ProcessedEmailTemplate processedTemplate) throws EmailException {
        HtmlEmail email = new HtmlEmail();
        email.setHtmlMsg(processedTemplate.getBody());
        for (String address : emailAddresses) {
            email.addTo(address.trim());
            log.debug(String.format("Email will be sent to address ´%s´.", address.trim()));
        }
        email.setFrom(String.format("%s <%s>", FROM_ALIAS, FROM_EMAIL));
        email.setSubject(processedTemplate.getSubject() != null ? processedTemplate.getSubject() : "");
        email.setCharset(CharEncoding.UTF_8);
        setMailServerProperties(email);
        return email;
    }


    /**
     * Get a list of email addresses of the assignee of the given task.
     * If there is no assignee, the email addresses will belong to the users within the candidate groups.
     *
     * @param activeTask Object which represent the task where the email addresses will be collected from.
     * @return List of email addresses.
     */
    public List<String> getTaskEmailAddresses(TaskRepresentation activeTask) {
        List<String> emailAddresses = new ArrayList<>();
        if (activeTask.getAssignee() != null) {
            if (isValidEmailAddress(activeTask.getAssignee().getEmail())) {
                emailAddresses.add(activeTask.getAssignee().getEmail());
            }
        } else {
            List<IdentityLink> groupLinks = assigneeService.getAssigneeGroups(activeTask.getId());
            for (IdentityLink invGroup : groupLinks) {
                Set<User> users = groupService.getGroup(Long.parseLong(invGroup.getGroupId())).getUsers();
                users.forEach(grpUser -> {
                    if (isValidEmailAddress(grpUser.getEmail())) {
                        emailAddresses.add(grpUser.getEmail());
                    }
                });
            }
        }
        return emailAddresses;
    }

    /**
     * Get a list of email addresses based on a group name
     *
     * @param groupName The group to fetch email addresses for
     * @return A list of email addresses
     */
    public List<String> getGroupEmailAddresses(String groupName) throws NoSuchElementException, DuplicateElementException {
        log.debug("Fetching email addresses for group with name [{}]", groupName);

        List<String> emailAddresses = new ArrayList<>();
        List<Group> groupByNameAndTenantId = groupService.getGroupByNameAndTenantId(groupName, 1L);

        if (CollectionUtils.isNullOrEmpty(groupByNameAndTenantId)) {
            throw new NoSuchElementException(String.format("Couldn't find any groups with name '%s'", groupName));
        }

        if (groupByNameAndTenantId.size() != 1) {
            throw new DuplicateElementException(String.format("Multiple groups found with name '%s'", groupName));
        }

        Group functionalMaintenanceWithoutUsers = groupByNameAndTenantId.get(0);

        // We need to retrieve the group again because it doesn't contain users yet
        Group functionalMaintenanceWithUsers = groupService.getGroup(functionalMaintenanceWithoutUsers.getId());

        if (functionalMaintenanceWithUsers != null) {
            Set<User> users = functionalMaintenanceWithUsers.getUsers();

            users.stream()
                    .map(User::getEmail)
                    .filter(this::isValidEmailAddress)
                    .forEach(emailAddresses::add);
        }

        return emailAddresses;
    }


    /**
     * Checks whether the provided email address is valid.
     *
     * @param emailAddress the email address
     * @return boolean that states whether the email address is valid
     */
    public boolean isValidEmailAddress(String emailAddress) {
        Pattern pat = Pattern.compile(RegexConstants.EMAIL_REGEX);
        return !StringUtils.isEmpty(emailAddress) && pat.matcher(emailAddress).matches();
    }

    @Override
    public void setEnvironment(Environment environment) {
        mailServerHost = environment.getProperty("email.host", String.class);
        mailServerPort = environment.getProperty("email.port", Integer.class);
        mailServerUseSSL = environment.getProperty("email.ssl", Boolean.class);
        mailServerUserTLS = environment.getProperty("email.tls", Boolean.class);
        mailServerUsername = environment.getProperty("email.username", String.class);
        mailServerPassword = environment.getProperty("email.password", String.class);
        baseUrl = environment.getProperty("email.base.url", String.class);
    }
}
