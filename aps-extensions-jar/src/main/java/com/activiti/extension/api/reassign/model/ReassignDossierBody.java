package com.activiti.extension.api.reassign.model;

import com.activiti.extension.constant.APSConstants;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class ReassignDossierBody {

    @NotNull
    @ApiModelProperty(required = true, value = "New steller")
    private Long newWriter;
    @NotNull
    @ApiModelProperty(required = true, value = "List of processes for which the steller needs to be reassigned")
    private List<APSConstants.Process> processTypes = List.of(APSConstants.Process.MAIN, APSConstants.Process.DELAYREQUEST);
}
