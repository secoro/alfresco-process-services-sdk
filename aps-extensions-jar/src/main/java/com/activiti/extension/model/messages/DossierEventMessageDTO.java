package com.activiti.extension.model.messages;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DossierEventMessageDTO {

    protected static final List<String> KEYS_DOSSIER_EVENT = Arrays.asList("dossierId", "dossierType", "confidentiality");

    @NotEmpty
    @ApiModelProperty(required = true)
    private String dossierId;

    @NotEmpty
    @ApiModelProperty(required = true)
    private String dossierType;

    @NotEmpty
    @ApiModelProperty(required = true)
    private String confidentiality;

    public DossierEventMessageDTO(Map<String, String> vars) {
        this.dossierId = vars.get("dossierId");
        this.dossierType = vars.get("dossierType");
        this.confidentiality = vars.get("confidentiality");
    }
}
