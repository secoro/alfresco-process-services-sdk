package com.activiti.extension.bean.scheduled;

import com.activiti.domain.idm.UserStatus;
import com.activiti.domain.sync.ExternalIdmUser;
import com.activiti.service.api.UserService;
import com.activti.idm.ldap.service.LdapRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Class scheduling the activation of users associated to reactivated LDAP users
 */
@Component
@ConditionalOnBean(LdapRegistryService.class)
public class LdapActivationSync {

    private final Logger logger = LoggerFactory.getLogger(LdapActivationSync.class);

    private final LdapRegistryService ldapRegistryService;
    private final UserService userService;

    public LdapActivationSync(LdapRegistryService ldapRegistryService, UserService userService) {
        this.ldapRegistryService = ldapRegistryService;
        this.userService = userService;
    }

    @Scheduled(cron = "${cron.ldapActivationSync:0 3/5 * * * *}")
    public void run() {
        try {
            logger.info("LDAP activation sync started");

            List<? extends ExternalIdmUser> ldapUsers = ldapRegistryService.getAllUsers();
            logger.info("Found {} users in LDAP", ldapUsers.size());

            ldapUsers.stream()
                    .map(ldapUser -> userService.findUserByExternalId(ldapUser.getId()))          // get APS user related to LDAP user
                    .filter(user -> user != null && UserStatus.INACTIVE.equals(user.getStatus())) // take only associated inactive users
                    .peek(user -> userService.activateAccount(user.getId()))                      // activate the APS user
                    .forEach(user -> logger.debug("[{}] activated", user.getId()));

            logger.info("LDAP activation sync finished");
        } catch (Exception e) {
            logger.error("LDAP activation sync error", e);
        }
    }
}
