package com.activiti.extension.api.tasks.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class AssignTaskToUserBody {

    @NotNull
    @ApiModelProperty(required = true, value = "The assignee")
    private String assignee;
}
