package com.activiti.extension.bean.tasklisteners;

import com.activiti.domain.idm.User;
import com.activiti.extension.bean.utils.ACSHTTPClient;
import com.activiti.extension.model.VerdictDto;
import com.activiti.service.api.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.ZonedDateTime;

import static com.activiti.extension.bean.utils.literal.EndPoint.ICM;
import static com.activiti.extension.bean.utils.literal.EndPoint.ICM_APPROVALS;

/**
 * TaskListener for setting a verdict on a dossier.
 */
@Slf4j
@RequiredArgsConstructor
@Component("verdictTaskListener")
public class VerdictTaskListener implements TaskListener {

    private final ACSHTTPClient acshttpClient;
    private final UserService userService;

    /**
     * Sets the verdict after a task where a verdict is given. Besides setting the verdict on the dossier it also sets
     * two variables on the process (taskCompleter and taskCompleterFullName).
     *
     * @param delegateTask the task
     * @param approvalType the type of approval
     * @param verdict the verdict given
     * @throws ParseException
     */
    public void setVerdict(DelegateTask delegateTask, String approvalType, String verdict) {
        log.debug("Setting verdict [{}] for approvalType [{}] and task [{}]", verdict, approvalType, delegateTask.getId());

        String dossierIdString = (String) delegateTask.getVariable("dossierId");
        String commentString = (String) delegateTask.getVariable("comment");
        String verdictString = (String) delegateTask.getVariable(verdict);

        // the approvalType might be a variable or a string literal
        String type = approvalType.contains("$") ? (String) delegateTask.getVariable(approvalType.replaceAll("[${}]", "")) : approvalType;

        User user = userService.getUser(Long.parseLong(delegateTask.getAssignee()));
        String userFullName = user.getFullName();
        String username = StringUtils.isNotEmpty(user.getExternalId()) ? user.getExternalId() : user.getEmail();

        delegateTask.setVariable("taskCompleter", username);
        delegateTask.setVariable("taskCompleterUserName", username);
        delegateTask.setVariable("taskCompleterFullName", userFullName);

        JSONObject verdictJson = VerdictDto.builder()
                .approvalType(type)
                .verdict(verdictString)
                .userName(username)
                .fullName(userFullName)
                .comment(commentString)
                .timestamp(ZonedDateTime.now())
                .build()
                .toVerdictJSONObject();

        try {
            acshttpClient.execute(String.format(ICM_APPROVALS, dossierIdString), verdictJson.toString(), HttpMethod.POST.name(), null, ICM);
        } catch (Exception e) {
            log.error("Error while setting verdict {}: {}", verdict, e.getMessage());
            log.debug("Exception details: {}", e);
        }
    }

    @Override
    public void notify(DelegateTask delegateTask) {

    }
}
