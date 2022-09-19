package com.activiti.extension.api.group;

import com.activiti.extension.api.user.UserTranslator;
import com.activiti.extension.api.user.model.CustomLightUserRepresentation;
import com.activiti.extension.bean.utils.PaginationUtil;
import com.activiti.extension.model.Pagination;
import com.activiti.model.common.ResultListDataRepresentation;
import com.activiti.service.api.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for custom group-related business logic.
 */

@Service
@RequiredArgsConstructor
public class CustomGroupsService {

    private final GroupService groupService;
    private final UserTranslator userTranslator;
    private final PaginationUtil paginationUtil;

    /**
     * Method to get all users in a group including their full name.
     *
     * @param groupId ID of the group in APS.
     * @return A ResultListDataRepresentation(Data and pagination) of CustomLightUserRepresentation objects.
     */
    public ResultListDataRepresentation<CustomLightUserRepresentation> getGroupUsers(Long groupId) {
        return paginationUtil.getPage(userTranslator.translate(groupService.getUsersForFunctionalGroup(groupId).getData()), Pagination.getDefaultPagination());
    }

}
