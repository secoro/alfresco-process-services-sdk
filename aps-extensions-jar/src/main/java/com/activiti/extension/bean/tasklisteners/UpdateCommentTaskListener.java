package com.activiti.extension.bean.tasklisteners;

import com.activiti.domain.idm.User;
import com.activiti.extension.bean.utils.services.ProcessService;
import com.activiti.extension.model.Comment;
import com.activiti.service.api.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.activiti.extension.constant.DateConstants.COMMENT_DATE_FORMAT;

@Slf4j
@RequiredArgsConstructor
@Component("updateCommentTaskListener")
public class UpdateCommentTaskListener implements TaskListener {

    private final UserService userService;
    private final ProcessService processService;
    private final TaskService taskService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(COMMENT_DATE_FORMAT).withLocale(Locale.ENGLISH).withZone(ZoneId.of("Europe/Amsterdam"));
    private static final String COMMENT = "comment";
    public static final String TASK_LOCAL_COMMENT = "taskLocalComment";
    private static final List<String> variableList = List.of("newComment", "newComment_optional", "newComment_required");

    public void updateComment(DelegateTask delegateTask) {
        log.debug("Updating comment at task [{}] with ID [{}]", delegateTask.getName(), delegateTask.getId());

        DelegateExecution execution = delegateTask.getExecution();
        User user = userService.getUser(Long.parseLong(delegateTask.getAssignee()));
        JSONArray commentsJSON = new JSONArray();

        Optional<String> optionalComment = getTaskComment(execution);
        optionalComment.filter(StringUtils::isNotBlank).ifPresentOrElse(comment -> {
            JSONObject commentJSON = new Comment(user.getFullName(), ZonedDateTime.now().format(FORMATTER), comment).toCommentJSONObject();
            commentsJSON.put(commentJSON);
            execution.setVariable(COMMENT, commentsJSON.toString());
            taskService.setVariableLocal(delegateTask.getId(), TASK_LOCAL_COMMENT, commentJSON.get(COMMENT));
        }, () -> {
            log.debug("All comment variables were null or blank. Setting the comment variable to null");
            execution.setVariable(COMMENT, null);
        });

        nullifyCommentVariables(execution);
    }

    public void updateComment(DelegateTask delegateTask, String type) {
        log.debug("Updating comment at task [{}] with ID [{}] and comment type [{}]",
                delegateTask.getName(), delegateTask.getId(), type);

        DelegateExecution execution = delegateTask.getExecution();
        User user = userService.getUser(Long.parseLong(delegateTask.getAssignee()));
        JSONArray commentsJSON = getPreviousCommentJSON(execution);

        Optional<String> optionalComment = getTaskComment(execution);
        optionalComment.filter(StringUtils::isNotBlank).ifPresentOrElse(comment -> {
            JSONObject commentJSON = new Comment(user.getFullName(), ZonedDateTime.now().format(FORMATTER), comment, type).toCommentJSONObject();
            commentsJSON.put(commentJSON);
            execution.setVariable(COMMENT, commentsJSON.toString());
        }, () -> log.debug("All comment variables were null or blank"));

        nullifyCommentVariables(execution);
    }

    public void updateTaskLocalComment(DelegateTask delegateTask) {
        log.debug("Updating comment task local variable for task [{}]", delegateTask.getId());

        JSONArray previousComments = getPreviousCommentJSON(delegateTask.getExecution());

        if (previousComments.length() == 0) {
            taskService.setVariableLocal(delegateTask.getId(), TASK_LOCAL_COMMENT, ""); // the previous comment was an empty string
        } else {
            JSONObject previousCommentJSON = (JSONObject) previousComments.get(previousComments.length() - 1);
            taskService.setVariableLocal(delegateTask.getId(), TASK_LOCAL_COMMENT, previousCommentJSON.get(COMMENT));
        }
    }

    private Optional<String> getTaskComment(DelegateExecution execution) {
        Map<String, String> variables = processService.collectVariables(execution, variableList);
        return variables.values().stream().findFirst();
    }

    private JSONArray getPreviousCommentJSON(DelegateExecution delegateExecution) {
        Object previousCommentObject = delegateExecution.getVariable(COMMENT);

        if (previousCommentObject != null) {
            return new JSONArray(previousCommentObject.toString());
        }
        log.debug("No previous comment found");
        return new JSONArray();
    }

    private void nullifyCommentVariables(DelegateExecution execution) {
        log.debug("Setting the following variables to null [{}]", variableList);
        processService.setVariablesToNull(execution, variableList);
    }

    @Override
    public void notify(DelegateTask delegateTask) {
    }
}
