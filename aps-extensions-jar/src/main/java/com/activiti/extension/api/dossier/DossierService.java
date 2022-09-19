package com.activiti.extension.api.dossier;

import com.activiti.domain.idm.Group;
import com.activiti.domain.idm.User;
import com.activiti.extension.api.dossier.model.InvolvedDossier;
import com.activiti.extension.api.dossier.model.LightTaskRepresentation;
import com.activiti.extension.api.tasks.model.Assignment;
import com.activiti.extension.api.tasks.model.CustomTaskRepresentation;
import com.activiti.extension.api.util.DateParserUtil;
import com.activiti.extension.bean.utils.PaginationUtil;
import com.activiti.extension.bean.utils.literal.Cluster;
import com.activiti.extension.constant.APSConstants;
import com.activiti.extension.model.Pagination;
import com.activiti.model.common.ResultListDataRepresentation;
import com.activiti.service.api.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.query.Query;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.TaskInfo;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.activiti.extension.constant.APSConstants.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class DossierService {

    private final UserService userService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final PaginationUtil paginationUtil;
    private final DateParserUtil dateParserUtil;

    private static final String PARTICIPANT = "participant";

    /**
     * Retrieves a List of InvolvedDossiers for a given user.
     *
     * @param username        username of a user
     * @param assignments     list of assignments Strings. Reference to {@link Assignment}
     * @param finishedAfter   date after which processes are finished
     * @param startedAfter    date after which processes are started
     * @return List of InvolvedDossiers
     */
    public List<InvolvedDossier> getInvolvedDossiers(
            String username, List<String> assignments, Date finishedAfter, Date startedAfter) {
        log.debug(
                "Getting involved dossiers with following parameters: User = {}, Assignments = {}, Finished After = {}, Started After = {}.",
                username, assignments, finishedAfter, startedAfter
        );

        User user = getUserByUserName(username);

        return getParameterizedInvolvedDossierTasks(user, assignments, finishedAfter, startedAfter)
                .map(this::getInvolvedDossier)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        // Collect to map, merge tasks when there are duplicate dossierId's and convert to an ArrayList afterwards
                        Collectors.toMap(InvolvedDossier::getDossierId, d -> d, this::addAllTasks), map -> new ArrayList<>(map.values())
                ));
    }

    /**
     * Method that retrieves candidate tasks and maps them to a dossier based on a query parameter.
     *
     * @param query    the query (i.e. MAIN, M2205-21 or M22)
     * @param type     the type of query (i.e. processType of dossierId)
     * @param clusters the cluster where the tasks belong to
     * @param first    the first item
     * @param maxItems max items to return
     * @return ResultListDataRepresentation<InvolvedDossier>
     */
    public ResultListDataRepresentation<InvolvedDossier> getDossiersWithCandidateTasks(String query, String type, List<Cluster> clusters, int first, int maxItems) {
        log.debug("Retrieving candidate tasks for query: {}, type: {}, clusters {}, first {}, maxItems {}", query, type, clusters, first, maxItems);

        ArrayList<InvolvedDossier> involvedDossiers = baseQueryCandidateTasksForQuery(type, query, clusters).list().stream()
                .map(this::getClaimableCustomTask)
                .map(this::getInvolvedDossier)
                .filter(Objects::nonNull)
                .map(this::enrichInvolvedDossier)
                .collect(Collectors.collectingAndThen(
                        // Collect to map, merge tasks when there are duplicate dossierId's and convert to an ArrayList afterwards
                        Collectors.toMap(InvolvedDossier::getDossierId, d -> d, this::addAllTasks), map -> new ArrayList<>(map.values())));

        return paginationUtil.getPage(involvedDossiers, new Pagination(maxItems, first));
    }

    // The base query used for retrieving candidate tasks using a contains operator for query and type
    private TaskQuery baseQueryCandidateTasksForQuery(String type, String query, List<Cluster> clusters) {
        TaskQuery taskQuery = taskService.createTaskQuery()
                .processVariableValueLikeIgnoreCase(type.trim(), String.format(QUERY_CONTAINS, query.trim()))
                .active()
                .includeProcessVariables()
                .taskUnassigned().or();

        clusters.forEach(cluster -> taskQuery.processVariableValueLikeIgnoreCase("owner", cluster.name()));
        return taskQuery.endOr();
    }

    /**
     * Helper method for enriching an involved dossier with extra properties.
     * It does this by querying the historic process instance.
     *
     * @param dossier the involved dossierId
     * @return InvolvedDossier
     */
    private InvolvedDossier enrichInvolvedDossier(InvolvedDossier dossier) {

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .variableValueEqualsIgnoreCase(DOSSIER_ID, dossier.getDossierId())
                .variableValueEqualsIgnoreCase(PROCESS_TYPE, APSConstants.Process.MAIN.name())
                .singleResult();

        if (Objects.nonNull(historicProcessInstance)) {
            dossier.setDossierType((String) historicProcessInstance.getProcessVariables().get(DOSSIER_TYPE));
            dossier.setSubject((String) historicProcessInstance.getProcessVariables().get(SUBJECT));
            dossier.setDeadline(dateParserUtil.convertObjectToDate(historicProcessInstance.getProcessVariables().get(DEADLINE)));
            dossier.setOwner((String) historicProcessInstance.getProcessVariables().get(OWNER));
            dossier.setStartDate(dateParserUtil.convertObjectToDate(historicProcessInstance.getProcessVariables().get(START_DATE)));
        }

        return dossier;
    }

    /**
     * Helper method for retrieving a user based on a given username
     *
     * @param username username of a user
     * @return User
     */
    private User getUserByUserName(String username) {
        return Optional.ofNullable(userService.findUserByExternalIdFetchGroups(username))
                .or(() -> Optional.ofNullable(userService.findUserByEmailFetchGroups(username)))
                .orElseThrow(RuntimeException::new);
    }

    /**
     * Add the tasks from one Involved Dossier to the other
     *
     * @param dossier main dossier which tasks should be appended
     * @param other   dossier which tasks are merged to the main dossier
     * @return a complete dossier.
     */
    private InvolvedDossier addAllTasks(InvolvedDossier dossier, InvolvedDossier other) {
        dossier.getTasks().addAll(other.getTasks());
        return dossier;
    }

    /**
     * Retrieves the different query definitions, wrapped in a stream for the passed query parameters
     *
     * @param user            username to retrieve involved dossiers for
     * @param assignments     list of assignments Strings. Reference to {@link Assignment}
     * @param finishedAfter   date after which processes are finished
     * @param startedAfter    date after which processes are started
     * @return a stream containing the streams related to the query parameters
     */
    private Stream<CustomTaskRepresentation> getParameterizedInvolvedDossierTasks(
            User user, List<String> assignments, Date finishedAfter, Date startedAfter
    ) {
        String userId = String.valueOf(user.getId());
        // create a list of all id's of the groups a user belongs to.
        List<String> listOfGroupIds = user.getGroups().stream().map(Group::getId).map(String::valueOf).collect(Collectors.toList());

        Stream<CustomTaskRepresentation> customTaskStream = Stream.empty();
        if (CollectionUtils.isNotEmpty(assignments)) {
            // If assignments has values, add tasks conditionally
            if (Assignment.assignee.isPresent(assignments)) {
                customTaskStream = Stream.concat(customTaskStream, getTaskCurrentlyAssignee(userId, startedAfter, finishedAfter));
            }
            if (Assignment.candidate.isPresent(assignments)) {
                customTaskStream = Stream.concat(customTaskStream, getTasksCurrentlyCandidate(listOfGroupIds, startedAfter, finishedAfter));
            }
            if (Assignment.completed.isPresent(assignments)) {
                customTaskStream = Stream.concat(customTaskStream, getDossierNumbersForCompletedTasks(userId, finishedAfter, startedAfter));
                customTaskStream = Stream.concat(customTaskStream, getDossierNumbersForInvolvedProcesses(userId, finishedAfter, startedAfter));
            }
            return customTaskStream.parallel();
        }
        // If assignments do not have values, return all tasks
        return Stream.of(
                getTaskCurrentlyAssignee(userId, startedAfter, finishedAfter),
                getTasksCurrentlyCandidate(listOfGroupIds, startedAfter, finishedAfter),
                getDossierNumbersForCompletedTasks(userId, finishedAfter, startedAfter),
                getDossierNumbersForInvolvedProcesses(userId, finishedAfter, startedAfter)
        ).flatMap(s -> s).parallel();
    }

    /**
     * Convert a customTaskRepresentation to a LightTaskRepresentation.
     *
     * @param customTaskRepresentation a customTaskRepresentation {@link CustomTaskRepresentation}
     * @return returns a LightTaskRepresentation
     */
    private LightTaskRepresentation convertToLightTask(CustomTaskRepresentation customTaskRepresentation) {
        LightTaskRepresentation task = new LightTaskRepresentation();
        task.setTaskId(customTaskRepresentation.getId());
        task.setTaskName(customTaskRepresentation.getName());
        String[] processName = customTaskRepresentation.getProcessDefinitionName().split(":");
        task.setProcessName(processName[0]);
        if (customTaskRepresentation.isClaimable()) {
            task.setAssignment(Assignment.candidate.name());
        } else {
            task.setAssignment(Assignment.assignee.name());
        }
        return task;
    }

    /**
     * Create an InvolvedDossier from a CustomTaskRepresentation
     *
     * @param customTask a CustomTaskRepresentation
     * @return List of InvolvedDossiers {@link InvolvedDossier}
     */
    private InvolvedDossier getInvolvedDossier(CustomTaskRepresentation customTask) {
        String dossierId = (String) customTask.getProcessVariables().get(DOSSIER_ID);
        if (StringUtils.isNotBlank(dossierId)) {

            InvolvedDossier dossier = new InvolvedDossier();
            dossier.setDossierId(dossierId);

            if (StringUtils.isNotBlank(customTask.getId()) && !customTask.isSuspended() && !customTask.isCompleted()) {
                dossier.setTasks(new ArrayList<>(List.of(convertToLightTask(customTask))));
            }
            return dossier;
        }
        return null;
    }

    /**
     * Retrieve all finished Tasks where the user was involved
     *
     * @param userId          the APS id of a user
     * @param finishedAfter   date after which processes are finished
     * @param startedAfter    date after which processes are started
     * @return List of CustomTaskRepresentations where the user was involved.
     */
    private Stream<CustomTaskRepresentation> getDossierNumbersForCompletedTasks(
            String userId, Date finishedAfter, Date startedAfter
    ) {
        return getCompletedTasks(userId, finishedAfter, startedAfter)
                .filter(this::filterNonDeletedTaskProcesses)
                .map(task -> this.getCustomTask(task, false));
    }

    /**
     * This method checks if a tasks process has been deleted.
     *
     * @param historicTaskInstance the task
     * @return boolean (true if the tasks process isn't deleted, false if the tasks process has been deleted)
     */
    private boolean filterNonDeletedTaskProcesses(HistoricTaskInstance historicTaskInstance) {
        return StringUtils.isEmpty(historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(historicTaskInstance.getProcessInstanceId())
                .singleResult()
                .getDeleteReason());
    }

    /**
     * Retrieve a list of HistoricTaskInstances based on a variable input.
     *
     * @param userId          the APS id of a user
     * @param finishedAfter   date after which processes are finished
     * @param startedAfter    date after which processes are started
     * @return List of HistoricTaskInstances
     */
    private Stream<HistoricTaskInstance> getCompletedTasks(String userId, Date finishedAfter, Date startedAfter) {
        log.debug("Getting completed tasks for user with ID {}.", userId);

        HistoricTaskInstanceQuery baseQuery = historicTaskQueryBaseFinished(userId);

        if (startedAfter != null) {
            log.debug("Setting finished historic tasks query created after to {}.", startedAfter);
            baseQuery.taskCreatedAfter(startedAfter);
        }
        if (finishedAfter != null) {
            log.debug("Setting finished historic tasks query completed after to {}.", finishedAfter);
            baseQuery.taskCompletedAfter(finishedAfter);
        }

        return getStreamForQuery(baseQuery);
    }

    /**
     * Base historic task query for finished historic tasks
     *
     * @param userId the id of the associated user
     * @return the base historic task query
     */
    private HistoricTaskInstanceQuery historicTaskQueryBaseFinished(String userId) {
        return historyService.createHistoricTaskInstanceQuery()
                .finished()
                .taskAssignee(userId)
                .includeProcessVariables();
    }

    /**
     * Retrieve all finished processes where the user was involved
     *
     * @param userId          the APS id of a user
     * @param finishedAfter   date after which processes are finished
     * @param startedAfter    date after which processes are started
     * @return List of CustomTaskRepresentations
     */
    private Stream<CustomTaskRepresentation> getDossierNumbersForInvolvedProcesses(
            String userId, Date finishedAfter, Date startedAfter
    ) {
        return getInvolvedProcesses(userId, finishedAfter, startedAfter)
                .filter(process -> this.isProcessParticipant(process, userId))
                .map(CustomTaskRepresentation::new);
    }

    /**
     * Retrieve a list of HistoricProcessInstances based on a variable input.
     *
     * @param userId          the APS id of a user
     * @param finishedAfter   date after which processes are finished
     * @param startedAfter    date after which processes are started
     * @return List of HistoricProcessInstances
     */
    private Stream<HistoricProcessInstance> getInvolvedProcesses(String userId, Date finishedAfter, Date startedAfter) {
        log.debug("Getting involved processes for user with ID {}.", userId);

        HistoricProcessInstanceQuery baseQuery = historicProcessQueryBase(userId);

        if (startedAfter != null) {
            log.debug("Setting finished historic process query started after to {}.", startedAfter);
            baseQuery.startedAfter(startedAfter);
        }
        if (finishedAfter != null) {
            log.debug("Setting finished historic process query finished after to {}.", finishedAfter);
            baseQuery.finishedAfter(finishedAfter);
        }

        return getStreamForQuery(baseQuery);
    }

    /**
     * Base historic process query which applies to every query variant
     *
     * @param userId the id of the associated user
     * @return the base historic process query
     */
    private HistoricProcessInstanceQuery historicProcessQueryBase(String userId) {
        return historyService.createHistoricProcessInstanceQuery()
                .involvedUser(userId)
                .includeProcessVariables()
                .notDeleted();
    }

    /**
     * Retrieve tasks a user is currently assignee of
     *
     * @param userId the APS id of a user
     * @return List of CustomtaskRepresentations
     */
    private Stream<CustomTaskRepresentation> getTaskCurrentlyAssignee(String userId, Date startedAfter, Date finishedAfter) {
        log.debug("Getting assignee tasks for user with ID {}.", userId);

        TaskQuery query = taskQueryBase().taskAssignee(userId);

        if (startedAfter != null) {
            query.taskCreatedAfter(startedAfter);
        }
        if (finishedAfter != null) {
            query.taskDueAfter(finishedAfter);
        }

        return this.getStreamForQuery(query).map(this::getNonClaimableCustomTask);
    }

    /**
     * Retrieve tasks a user is currently candidate of
     *
     * @param listOfGroupIds List of id's of groups a user is a member of
     * @return List of CustomTaskRepresentations
     */
    private Stream<CustomTaskRepresentation> getTasksCurrentlyCandidate(List<String> listOfGroupIds, Date startedAfter, Date finishedAfter) {
        log.debug("Getting candidate tasks for groups with ID's {}.", listOfGroupIds);

        TaskQuery query = taskQueryBase().taskCandidateGroupIn(listOfGroupIds);

        if (startedAfter != null) {
            query.taskCreatedAfter(startedAfter);
        }
        if (finishedAfter != null) {
            query.taskDueAfter(finishedAfter);
        }

        return getStreamForQuery(query).map(this::getClaimableCustomTask);
    }

    /**
     * Base task query used to retrieve tasks
     *
     * @return the base task query
     */
    private TaskQuery taskQueryBase() {
        return taskService.createTaskQuery()
                .includeProcessVariables();
    }


    private <T extends Query<?, ?>, U> Stream<U> getStreamForQuery(Query<T, U> query) {
        return query.list().stream();
    }

    private CustomTaskRepresentation getClaimableCustomTask(TaskInfo taskInfo) {
        return getCustomTask(taskInfo, true);
    }

    private CustomTaskRepresentation getNonClaimableCustomTask(TaskInfo taskInfo) {
        return getCustomTask(taskInfo, false);
    }

    private CustomTaskRepresentation getCustomTask(TaskInfo taskInfo, boolean claimable) {

        String processInstanceId = taskInfo.getProcessInstanceId();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

        String processDefinitionName = repositoryService.getProcessDefinition(taskInfo.getProcessDefinitionId()).getName();
        CustomTaskRepresentation task = new CustomTaskRepresentation();
        task.setId(taskInfo.getId());
        task.setClaimable(claimable);
        task.setName(taskInfo.getName());
        task.setProcessDefinitionName(processDefinitionName);
        task.setProcessVariables(taskInfo.getProcessVariables());

        if (taskInfo instanceof HistoricTaskInstance) {
            if (((HistoricTaskInstance) taskInfo).getEndTime() != null) {
                task.setCompleted(true);
            }
        }

        if (processInstance != null && processInstance.isSuspended()) {
            task.setSuspended(true);
        }

        return task;
    }

    private boolean isProcessParticipant(HistoricProcessInstance historicProcessInstance, String userId) {
        return historyService.getHistoricIdentityLinksForProcessInstance(historicProcessInstance.getId()).stream()
                .anyMatch(identityLink ->
                        identityLink.getUserId().equalsIgnoreCase(userId) && identityLink.getType().equalsIgnoreCase(PARTICIPANT));
    }
}
