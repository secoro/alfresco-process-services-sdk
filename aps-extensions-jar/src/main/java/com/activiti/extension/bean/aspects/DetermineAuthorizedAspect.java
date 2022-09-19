package com.activiti.extension.bean.aspects;

import com.activiti.domain.idm.Group;
import com.activiti.domain.idm.User;
import com.activiti.extension.bean.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author Incentro
 * <p>
 * Aspect that determines if a user has the necessary authority. It listens to methods annotated with '@Authority'.
 * Throws a ForbiddenException if the user doesn't have the correct authority.
 */

@Slf4j
@RequiredArgsConstructor
@Aspect
@Component
public class DetermineAuthorizedAspect {

    private final SecurityUtils securityUtils;

    @Before("@annotation(com.activiti.extension.bean.aspects.Authority)")
    public void belongsToGroups(JoinPoint joinPoint) throws AccessDeniedException {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        List<String> requiredAuthorities = Arrays.asList(method.getAnnotation(Authority.class).requiredAuthorities());

        User currentActivitiUser = securityUtils.getCurrentActivitiUser();

        currentActivitiUser.getGroups().stream()
                .map(Group::getName)
                .filter(group -> requiredAuthorities.stream().anyMatch(group::contains))
                .findAny()
                .orElseThrow(() -> new AccessDeniedException(String.format("Current user [%s] does not have the required authorities %s!", currentActivitiUser.getFullName(), requiredAuthorities)));
    }
}
