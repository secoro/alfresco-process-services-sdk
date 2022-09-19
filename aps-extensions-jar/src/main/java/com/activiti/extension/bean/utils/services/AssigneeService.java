package com.activiti.extension.bean.utils.services;

import com.activiti.extension.model.Assignee;
import com.activiti.extension.model.AssigneeType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Slf4j
@AllArgsConstructor
@Service
public class AssigneeService {

    private final TaskService taskService;

    /**
     * Get the Assignee of a task. An assignee can either be a GROUP or USER.
     * The ID of the assignee is its ACS ID.
     * @param task The current task
     * @return The Assignee of the task
     */
    public Assignee getAssignee(DelegateTask task) {
        log.debug("Getting assignee for task {}", task.getId());
        Assignee assignee = new Assignee();

        Set<IdentityLink> candidates = task.getCandidates();

        String assigneeId = task.getAssignee();
        if (assigneeId!=null) {

            assignee.setId(assigneeId);
            assignee.setType(AssigneeType.user);
        }else {

            if (!candidates.isEmpty()) {
                assigneeId = candidates.iterator().next().getGroupId();
            }
            if (assigneeId == null) {
                return null;
            }

            assignee.setId(assigneeId);
            assignee.setType(AssigneeType.group);

        }
        // set the current assignee as previous assignee so the next time we enter the PermissionsTasklistener
        // we know who the previous assignee was.
        setPreviousAssignee(assignee, task);

        log.debug("Got assignee {} for task {}", assignee.getId(), task.getId());

        return assignee;
    }

    public Assignee getAssignee(Task task) {
        log.debug("Getting assignee for task {}", task.getId());
        Assignee assignee = new Assignee();

        List<IdentityLink> candidates = taskService.getIdentityLinksForTask(task.getId());

        String assigneeId = task.getAssignee();
        if (assigneeId!=null) {

            assignee.setId(assigneeId);
            assignee.setType(AssigneeType.user);
        }else {

            if (!candidates.isEmpty()) {
                assigneeId = candidates.iterator().next().getGroupId();
            }
            if (assigneeId == null) {
                return null;
            }

            assignee.setId(assigneeId);
            assignee.setType(AssigneeType.group);

        }
        // set the current assignee as previous assignee so the next time we enter the PermissionsTasklistener
        // we know who the previous assignee was.
        setPreviousAssignee(assignee, task);

        log.debug("Got assignee {} for task {}", assignee.getId(), task.getId());

        return assignee;
    }

    /**
     * Get all assignees of a certain task.
     *
     * @param task The task of which the assignees should be fetched.
     * @return A list of assignees.
     */

    public List<Assignee> getAssignees(DelegateTask task) {
        log.debug("Getting all assignees for task [{}]", task.getId());
        List<Assignee> assigneeList = new ArrayList<>();
        Assignee assignee = new Assignee();
        Assignee candidateGroup = new Assignee();

        String assigneeID = task.getAssignee();
        Set<IdentityLink> candidateGroups = task.getCandidates();
        Iterator<IdentityLink> iterator = candidateGroups.iterator();

        if (StringUtils.isNotBlank(assigneeID)) {
            log.debug("Assignee found [{}]", assigneeID);
            assignee.setId(assigneeID);
            assignee.setType(AssigneeType.user);
            assigneeList.add(assignee);
        }

        if (iterator.hasNext()) {
            IdentityLink group = iterator.next();
            log.debug("Candidate group found [{}]", group.getGroupId());
            candidateGroup.setId(group.getGroupId());
            candidateGroup.setType(AssigneeType.group);
            assigneeList.add(candidateGroup);
        }

        return assigneeList;
    }

    public List<Assignee> getAssignees(Task task) {
        log.debug("Getting all assignees for task [{}]", task.getId());
        List<Assignee> assigneeList = new ArrayList<>();
        Assignee assignee = new Assignee();
        Assignee candidateGroup = new Assignee();

        String assigneeID = task.getAssignee();
        List<IdentityLink> candidateGroups = taskService.getIdentityLinksForTask(task.getId());
        Iterator<IdentityLink> iterator = candidateGroups.iterator();

        if (StringUtils.isNotBlank(assigneeID)) {
            log.debug("Assignee found [{}]", assigneeID);
            assignee.setId(assigneeID);
            assignee.setType(AssigneeType.user);
            assigneeList.add(assignee);
        }

        if (iterator.hasNext()) {
            IdentityLink group = iterator.next();
            log.debug("Candidate group found [{}]", group.getGroupId());
            candidateGroup.setId(group.getGroupId());
            candidateGroup.setType(AssigneeType.group);
            assigneeList.add(candidateGroup);
        }

        return assigneeList;
    }


    /**
     * set's varialbes locally on the task to identify who the previous assignee of the task was.
     *
     * @param assignee
     * @param task
     */
    private void setPreviousAssignee(Assignee assignee, DelegateTask task) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService activitiTaskService = processEngine.getTaskService();

        activitiTaskService.setVariableLocal(task.getId(),"previousAssigneeId",assignee.getId());
        activitiTaskService.setVariableLocal(task.getId(), "previousAssigneeType", assignee.getType());
        log.info("previousAssignee is now {}", task.getVariableLocal("previousAssignee"));
    }

    private void setPreviousAssignee(Assignee assignee, Task task) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService activitiTaskService = processEngine.getTaskService();

        activitiTaskService.setVariableLocal(task.getId(),"previousAssigneeId",assignee.getId());
        activitiTaskService.setVariableLocal(task.getId(), "previousAssigneeType", assignee.getType());
        log.info("previousAssignee is now {}", taskService.getVariableLocal(task.getId(), "previousAssignee"));
    }

    /**
     * Get the groups which are candidates of the given task
     * @param taskId Unique identifier of the task whose candidate groups will be returned
     * @return List of identity links which represents the candidate groups
     */
    public List<IdentityLink> getAssigneeGroups(String taskId) {
        log.debug("Getting assignees for task {}", taskId);
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        identityLinks.removeIf( link -> link.getGroupId() == null);
        return identityLinks;
    }
}
