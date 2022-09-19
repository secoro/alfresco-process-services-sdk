package com.activiti.extension.api.user;

import com.activiti.domain.idm.UserStatus;
import com.activiti.extension.bean.aspects.Authority;
import com.activiti.model.idm.LightUserRepresentation;
import com.activiti.service.api.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author Incentro
 * This class is the entry point for all the calls related to users.
 */

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("enterprise/custom/users")
public class CustomUserController {

    private final UserService userService;

    @Authority(requiredAuthorities = {"FUNCTIONEEL_BEHEER", "PROCES_REGISSEURS"})
    @GetMapping()
    public List<LightUserRepresentation> getUsers(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "tenantId") Long tenantId) {

        log.debug("Retrieving users with filter [{}] and status [{}]", filter, status);

        return userService.findUsers(
                        filter,
                        false,
                        null,
                        null,
                        null,
                        UserStatus.valueOf(status.toUpperCase(Locale.ROOT)),
                        null,
                        null,
                        null,
                        null,
                        tenantId,
                PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .map(LightUserRepresentation::new)
                .collect(Collectors.toList());
    }
}
