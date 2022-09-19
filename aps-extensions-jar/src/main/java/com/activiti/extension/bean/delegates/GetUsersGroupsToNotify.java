package com.activiti.extension.bean.delegates;

import com.activiti.extension.bean.utils.ACSHTTPClient;
import com.activiti.service.api.UserGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.activiti.extension.bean.utils.literal.EndPoint.ICM_GROUPS_GROUP;
import static com.activiti.extension.bean.utils.literal.EndPoint.ICM_PROCESS_RELATIONS;

/**
 * Java Delegate used to retrieve all of the involved users and groups on a dossier.
 */

@Slf4j
@RequiredArgsConstructor
@Component("getUsersGroupsToNotify")
public class GetUsersGroupsToNotify implements JavaDelegate {

    private final ACSHTTPClient httpClient;
    private final UserGroupService userGroupService;

    private Expression assignees; // comma separated list of aps user ids
    private Expression groups; // comma separated list of aps group names
    private Expression taskAssignees; // boolean: whether to return the user/group names of assignees
    private Expression steller; // boolean: whether to return to stellers id

    @Override
    public void execute(DelegateExecution execution) {
        log.debug("Executing the getUsersGroupsToNotify JavaDelegate");

        Set<String> usersToNotify = new HashSet<>();
        Set<String> groupsToNotify = new HashSet<>();

        String dossierId = execution.getVariable("dossierId").toString();
        JSONArray relations = getAllRelations(dossierId);

        // 1. assignees
        String assignee = assignees.getExpressionText();
        extractAssignees(execution, assignee, usersToNotify);

        // 2. groups
        String groupString = groups.getExpressionText();
        extractGroup(groupString, groupsToNotify);

        boolean includeTaskAssignees = Boolean.parseBoolean(taskAssignees.getExpressionText());
        boolean includeStellers = Boolean.parseBoolean(steller.getExpressionText());

        if (includeTaskAssignees) {
            extractTaskAssignees(relations, usersToNotify, groupsToNotify);
        }

        if (includeStellers) {
            extractStellers(relations, usersToNotify);
        }

        removeDuplicateUsers(usersToNotify, groupsToNotify);

        execution.setVariable("usersToNotify", usersToNotify);
        execution.setVariable("groupsToNotify", groupsToNotify);
        execution.setVariable("amountOfUsers", usersToNotify.size());
        execution.setVariable("amountOfGroups", groupsToNotify.size());
    }

    /**
     * This method filters out users from the usersToNotify set that are included in the groupsToNotify set.
     *
     * @param usersToNotify the usersToNotify set
     * @param groupsToNotify the groupsToNotify set
     */
    private void removeDuplicateUsers(Set<String> usersToNotify, Set<String> groupsToNotify) {
        groupsToNotify.forEach(group -> {
            usersToNotify.removeIf(user -> userGroupService.isMember(Long.parseLong(user), Long.parseLong(group)));
        });
    }

    /**
     * Extracts all of the passed through assignees and does a look up in the execution.
     *
     * @param execution       The execution
     * @param assigneesString The (comma separated) list of assignees
     * @param usersToNotify   The set where the users needs to be added
     */
    private void extractAssignees(DelegateExecution execution, String assigneesString, Set<String> usersToNotify) {
        log.debug("Extracting the following assignees [{}]", assigneesString);

        String[] assigneesArray = assigneesString.split(",");
        List<String> assigneeList = Arrays.asList(assigneesArray);

        try {
            assigneeList.forEach(assignee -> {
                String variable = assignee.replaceAll("[${}]", "");
                usersToNotify.add((String) execution.getVariable(variable));
            });
        } catch (Exception e) {
            log.error("There was an error while retrieving the assignees with message [{}]", e.getMessage());
        }

    }

    /**
     * Extracts all of the passed through groups and does a call to ICM to determine the APS ID
     *
     * @param groupsString   The (comma separated) list of groups
     * @param groupsToNotify The set where the groups needs to be added
     */
    private void extractGroup(String groupsString, Set<String> groupsToNotify) {
        log.debug("Extracting the following groups [{}]", groupsString);

        String[] groupsArray = groupsString.split(",");
        List<String> groupList = Arrays.asList(groupsArray);

        try {
            groupList.forEach(group -> {
                String groupResponse = httpClient.execute(String.format(ICM_GROUPS_GROUP, group), null, HttpMethod.GET.toString(), null, "ICM");
                JSONObject groupJSON = new JSONObject(groupResponse);
                groupsToNotify.add(String.valueOf(groupJSON.getLong("apsId")));
            });
        } catch (Exception e) {
            log.error("There was an error while retrieving the groups with message [{}]", e.getMessage());
        }

    }

    /**
     * Retrieves all of the outstanding tasks in the MAIN-, DELAYREQUEST and ADHOC processes for a given dossier.
     *
     * @param dossierId The dossier ID
     * @return A JSONArray containing all of the process-relations
     */
    private JSONArray getAllRelations(String dossierId) {
        log.debug("Getting all relations for dossier with ID [{}]", dossierId);

        JSONArray relations = new JSONArray();

        String uriMain = String.format(ICM_PROCESS_RELATIONS, dossierId, "MAIN");
        String uriDelay = String.format(ICM_PROCESS_RELATIONS, dossierId, "DELAYREQUEST");
        String uriAdHoc = String.format(ICM_PROCESS_RELATIONS, dossierId, "ADHOC");

        try {
            String responseMain = httpClient.execute(uriMain, null, HttpMethod.GET.toString(), null, "ICM");
            String responseDelay = httpClient.execute(uriDelay, null, HttpMethod.GET.toString(), null, "ICM");
            String responseAdHoc = httpClient.execute(uriAdHoc, null, HttpMethod.GET.toString(), null, "ICM");

            JSONObject jsonObjectMain = new JSONObject(responseMain);
            JSONObject jsonObjectDelay = new JSONObject(responseDelay);
            JSONObject jsonObjectAdHoc = new JSONObject(responseAdHoc);

            relations.put(jsonObjectMain);
            relations.put(jsonObjectDelay);
            relations.put(jsonObjectAdHoc);

        } catch (Exception e) {
            log.error("There was an error while retrieving all of the process relations for dossier with ID [{}] returned message [{}]", dossierId, e);
        }

        return relations;
    }

    /**
     * Extracts all of the assignees and involved groups for all of the dossier process-relations.
     *
     * @param relations      The JSONArray containing all of the process-relations.
     * @param usersToNotify  The set where the users needs to be added
     * @param groupsToNotify The set where the groups needs to be added
     */
    private void extractTaskAssignees(JSONArray relations, Set<String> usersToNotify, Set<String> groupsToNotify) {
        log.debug("Extracting all task assignees");

        int amountOfObjects = relations.length();

        for (int i = 0; i < amountOfObjects; i++) {
            JSONObject currentJSONObject = relations.getJSONObject(i);
            JSONArray currentRelations = currentJSONObject.getJSONArray("relations");

            int length = currentRelations.length();

            if (length > 0) {

                for (int j = 0; j < length; j++) {
                    JSONObject current = currentRelations.getJSONObject(j);

                    if (!current.isNull("assignee")) {
                        JSONObject assignee = current.getJSONObject("assignee");
                        String id = String.valueOf(assignee.getLong("id"));
                        usersToNotify.add(id);
                    } else if (!current.isNull("involvedGroup")) {
                        JSONObject involvedGroup = current.getJSONObject("involvedGroup");
                        String id = String.valueOf(involvedGroup.getLong("id"));
                        groupsToNotify.add(id);
                    }
                }
            }
        }
    }

    /**
     * Extracts the steller (and if possible the steller_tussenbericht) from a dossier and adds it to usersToNotify
     *
     * @param relations     The JSONArray containing all of the process-relations.
     * @param usersToNotify The set where the users needs to be added
     */
    private void extractStellers(JSONArray relations, Set<String> usersToNotify) {
        log.debug("Extracting all stellers");

        int amountOfObjects = relations.length();

        for (int i = 0; i < amountOfObjects; i++) {
            JSONObject currentJSONObject = relations.getJSONObject(i);
            JSONArray currentRelations = currentJSONObject.getJSONArray("relations");

            if (currentRelations.length() > 0) {
                JSONObject current = currentRelations.getJSONObject(0);
                JSONObject variables = current.getJSONObject("variables");

                if (variables.has("steller") && !variables.isNull("steller")) {
                    Double steller = variables.getDouble("steller");
                    long stellerLong = steller.longValue();
                    usersToNotify.add(String.valueOf(stellerLong));
                }

                if (variables.has("steller_tussenbericht") && !variables.isNull("steller_tussenbericht")) {
                    Double steller = variables.getDouble("steller_tussenbericht");
                    long stellerLong = steller.longValue();
                    usersToNotify.add(String.valueOf(stellerLong));
                }
            }
        }
    }
}
