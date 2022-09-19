package com.activiti.extension.bean.utils.services;

import com.activiti.domain.idm.User;
import com.activiti.security.SecurityUtils;
import com.activiti.service.api.UserService;
import com.activiti.service.runtime.AlfrescoProcessInstanceService;
import lombok.RequiredArgsConstructor;
import org.activiti.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteProcessInstanceService {

    private final AlfrescoProcessInstanceService processInstanceService;
    private final UserService userService;


    public void deleteProcessInstance(HistoricProcessInstance instance) {
        User startUser = userService.getUser(Long.valueOf(instance.getStartUserId()));
        SecurityUtils.assumeUser(startUser);
        processInstanceService.deleteProcessInstance(instance.getId());
        SecurityUtils.clearAssumeUser();
    }

}
