package com.activiti.extension.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Incentro.
 *
 * Model class representing the body of a PUT addDossierPermissions call.
 * This body consists of the wanted permission and a list of all assignees (can be both users and groups) that need this permission.
 */
@Getter
@Setter
@Builder
public class UpdateDossierPermissionsModel {

    private DossierPermission permission;
    private boolean increaseMode = true;
    private String taskId;
    private List<Assignee> assignees;
}

