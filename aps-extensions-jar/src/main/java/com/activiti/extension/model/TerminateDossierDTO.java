package com.activiti.extension.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TerminateDossierDTO {
    private String terminatedApprovedBy;
    private String terminatedBy;
    private String terminatedOn;
    private String terminatedReason;
}
