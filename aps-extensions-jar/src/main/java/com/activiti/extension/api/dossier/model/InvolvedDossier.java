package com.activiti.extension.api.dossier.model;

import com.activiti.model.common.AbstractRepresentation;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@JsonSerialize
@NoArgsConstructor
public class InvolvedDossier extends AbstractRepresentation {

    private String dossierId;
    private String subject;
    private String owner;
    private String dossierType;
    private Date deadline;
    private Date startDate;

    @Nullable
    private List<LightTaskRepresentation> tasks;

    public List<LightTaskRepresentation> getTasks() {
        return Objects.requireNonNullElseGet(tasks, ArrayList::new);
    }
}
