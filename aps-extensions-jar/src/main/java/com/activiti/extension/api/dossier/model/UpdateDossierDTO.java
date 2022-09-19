package com.activiti.extension.api.dossier.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDossierDTO {
    private String externalId;
    private String owner;
    private String subject;
    private String writer;
    private String managingBoard;
    private String department;
    private String municipalExecutiveCouncillor;
    private String municipalExecutiveCouncillorTeam;
    private OffsetDateTime execBoardMeetingDate;
    private String execBoardResolution;
    private String execBoardNote;
    private OffsetDateTime cityCouncilMeetingDate;
    private String cityCouncilResolution;
    private String cityCouncilNote;
    private OffsetDateTime startDate;
    private OffsetDateTime completeBefore;
    private String communityBoards;
    private String aspectLink;
}
