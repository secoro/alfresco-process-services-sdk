package com.activiti.extension.model;

import com.activiti.domain.idm.User;
import lombok.*;

/**
 * @author Incentro.
 * <p>
 * Model class representing an assignee. This model contains the ID of the assignee and a type (user or group).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignee {

    private String id;
    private AssigneeType type;

    public static Assignee fromUser(User user) {
        return new Assignee(String.valueOf(user.getId()), AssigneeType.user);
    }
}
