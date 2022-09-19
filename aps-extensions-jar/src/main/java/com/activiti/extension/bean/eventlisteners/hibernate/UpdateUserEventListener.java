package com.activiti.extension.bean.eventlisteners.hibernate;

import com.activiti.domain.idm.User;
import com.activiti.domain.idm.UserStatus;
import com.activiti.extension.bean.utils.SpringUtility;
import com.activiti.extension.bean.utils.services.ReassignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
public class UpdateUserEventListener implements PostUpdateEventListener {

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return false;
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return PostUpdateEventListener.super.requiresPostCommitHandling(persister);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (event.getEntity() instanceof User) {
            ReassignService reassignService = SpringUtility.getBean(ReassignService.class);

            User user = (User) event.getEntity();

            Optional<Object> oldStatusOptional = Arrays.stream(event.getOldState())
                    .filter(UserStatus.class::isInstance)
                    .findFirst();

            UserStatus newStatus = user.getStatus();

            if (oldStatusOptional.isPresent()) {
                UserStatus oldStatus = (UserStatus) oldStatusOptional.get();

                if (oldStatus.equals(UserStatus.ACTIVE) && newStatus.equals(UserStatus.INACTIVE)) {
                    log.info("UserStatus changed for user [{}] from [{}] to [{}]", user.getFullName(), oldStatus, newStatus);

                    try {
                        reassignService.sendEmailNotifyingProcessManagersOfUserResignation(user.getId());
                    } catch (Exception e) {
                        log.info("Something went wrong while sending emails to process managers");
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
