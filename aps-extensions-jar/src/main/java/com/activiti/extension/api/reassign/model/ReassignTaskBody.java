package com.activiti.extension.api.reassign.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ReassignTaskBody {

    @NotBlank
    @ApiModelProperty(required = true)
    private String dossierId;
    @NotNull
    @ApiModelProperty(required = true)
    private Long userId;
}
