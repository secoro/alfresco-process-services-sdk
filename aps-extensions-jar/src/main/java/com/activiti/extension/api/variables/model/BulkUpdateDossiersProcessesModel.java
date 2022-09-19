package com.activiti.extension.api.variables.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BulkUpdateDossiersProcessesModel {
    private List<String> dossierIds;
    private List<String> processTypes;
    private Map<String, Object> variables;
}
