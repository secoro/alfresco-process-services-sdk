package com.activiti.extension.api.group;

import com.activiti.extension.api.user.model.CustomLightUserRepresentation;
import com.activiti.model.common.ResultListDataRepresentation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller containing customizations on the activiti groups API.
 */

@RestController
@RequestMapping(
        value = "/enterprise/custom/groups",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class CustomGroupsController {

    private final CustomGroupsService customGroupsService;

    public CustomGroupsController(CustomGroupsService customGroupsService) {
        this.customGroupsService = customGroupsService;
    }

    /**
     * Endpoint for getting all users of a group including their full name.
     * @param groupId ID of the group in APS.
     * @return A ResultListDataRepresentation(Data and pagination) of CustomLightUserRepresentation objects.
     */
    @GetMapping("{groupId}/users")
    public ResultListDataRepresentation<CustomLightUserRepresentation> getGroupUsers(@PathVariable Long groupId) {
        return customGroupsService.getGroupUsers(groupId);
    }

}
