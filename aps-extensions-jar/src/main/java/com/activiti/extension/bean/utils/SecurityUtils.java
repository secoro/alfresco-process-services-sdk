package com.activiti.extension.bean.utils;

import com.activiti.domain.idm.User;
import com.activiti.service.api.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Incentro
 * Utility class resonspible for security related concerns.
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class SecurityUtils {

    private final UserService userService;

    /**
     * Helper method that retrieves the current user trying to interact with the system.
     * Username might be an external ID or email address.
     *
     * @return User object.
     */
    public User getCurrentActivitiUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username;

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        log.debug("Fetching user with username [{}] from activiti.", username);

        return Optional.ofNullable(userService.findUserByExternalIdFetchGroups(username))
                .or(() -> Optional.ofNullable(userService.findUserByEmailFetchGroups(username)))
                .orElseThrow(RuntimeException::new);
    }
}
