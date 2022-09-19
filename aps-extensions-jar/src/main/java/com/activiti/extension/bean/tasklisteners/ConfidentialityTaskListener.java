package com.activiti.extension.bean.tasklisteners;

import com.activiti.domain.idm.User;
import com.activiti.extension.bean.utils.services.CustomEmailService;
import com.activiti.service.api.UserService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.task.IdentityLink;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@Component("confidentialityTaskListener")
public class ConfidentialityTaskListener implements TaskListener {

    private static final Logger logger = LoggerFactory.getLogger(ConfidentialityTaskListener.class);

    private static final String CONFIDENTIALITY = "confidentiality";
    private static final String STRICTLY_CONFIDENTIAL = "Strikt vertrouwelijk";
    private final CustomEmailService customEmailService;
    private final UserService userService;

    public ConfidentialityTaskListener(CustomEmailService customEmailService, UserService userService) {
        this.customEmailService = customEmailService;
        this.userService = userService;
    }

    public void updateAssignee(DelegateTask delegateTask, String assigneeId, String emailTemplateName) {
        DelegateExecution delegateExecution = delegateTask.getExecution();
        String confidentiality = (String) delegateExecution.getVariable(CONFIDENTIALITY);

        Objects.requireNonNull(confidentiality, "Failed to retrieve the confidentiality");

        // check dossier confidentiality. If strictly confidential, sets user task assignee to assigneeId parameter and remove candidate group(s)
        // check assigneeId. Continues script if assigneeId is present
        if (STRICTLY_CONFIDENTIAL.equals(confidentiality) && StringUtils.isNotBlank(assigneeId)) {
            delegateTask.setAssignee(assigneeId);
            logger.info("Set assigneeId based on confidentiality level of dossier to {}", assigneeId);
            Set<IdentityLink> candidates = delegateTask.getCandidates();
            for (IdentityLink candidate : candidates) {
                delegateTask.deleteCandidateGroup(candidate.getGroupId());
            }
            // send email to the assignee of the task instead of to a group
            try {
                sendEmail(delegateTask, emailTemplateName);
            } catch (EmailException e) {
                String msg = "Could not send e-mail when complete task " + delegateTask.getExecution().getId();
                logger.error("Error trying to send a custom email: " + msg, e);
            } catch (Exception e) {
                logger.error("Error trying to send a custom email: " + e.getMessage(), e);
            }
        }
    }

    public void sendEmail(DelegateTask delegateTask, String emailTemplateName) throws Exception {
        //get assignee as user
        User AssigneeUser = userService.getUser(Long.parseLong(delegateTask.getAssignee()));
        //get user email, check if valid
        ArrayList<String> emailAddress = new ArrayList<>();
        if (customEmailService.isValidEmailAddress(AssigneeUser.getEmail())) {
            emailAddress.add(AssigneeUser.getEmail());
        }
        HtmlEmail email = customEmailService.prepareEmail(delegateTask, emailTemplateName, emailAddress);
        // Send the email only when there are email addresses
        if (!emailAddress.isEmpty()) {
            email.send();
        }
    }

    @Override
    public void notify(DelegateTask delegateTask) {
    }
}
