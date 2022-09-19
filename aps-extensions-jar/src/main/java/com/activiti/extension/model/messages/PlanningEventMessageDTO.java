package com.activiti.extension.model.messages;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotEmpty;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static com.activiti.extension.bean.utils.literal.EventMessageConstants.*;
import static com.activiti.extension.constant.DateConstants.APS_DATE_FORMAT;
import static com.activiti.extension.constant.DateConstants.OFFSET_DATETIME_FORMAT;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Slf4j
public class PlanningEventMessageDTO {

    @NotEmpty
    @ApiModelProperty(required = true)
    private String dossierId;
    @NotEmpty
    @ApiModelProperty(required = true)
    private String eventType;
    @NotEmpty
    @ApiModelProperty(required = true)
    private String source;
    @NotEmpty
    @ApiModelProperty(required = true)
    private String sender;

    private String startDate;
    private String deadline;
    private String reachedDate;
    private Long dossierConfiguration;
    private String milestoneGenericName;

    public PlanningEventMessageDTO(String eventType, Map<String, String> vars) throws ParseException {
        this.dossierId = vars.get("dossierId");
        this.source = vars.get("source");
        this.eventType = eventType;

        switch (eventType) {
            case START_DOSSIER:
                this.startDate = dateTransformer(vars.get("startdate"), false);
                this.deadline = dateTransformer(vars.get("deadline"), false);
                this.sender = vars.get("initiatorUserName");
                this.dossierConfiguration = Long.parseLong(vars.get("dossier_config"));

                break;

            case MILESTONE_REACHED:
                if (vars.containsKey("execBoardMeetingDate")) {
                    this.reachedDate = dateTransformer(vars.get("execBoardMeetingDate"), false);
                } else {
                    this.reachedDate = dateTransformer(vars.get("taskCompletedDate"), true);
                }
                this.milestoneGenericName = vars.get("milestoneGenericName");
                this.sender = vars.get("taskCompleter");

                break;

            case MILESTONE_REACHED_BACKWARD:
                this.milestoneGenericName = vars.get("milestoneGenericName");

                break;

            case UPDATE_DOSSIER_CONFIGURATION:
                this.dossierConfiguration = Long.parseLong(vars.get("dossier_config"));

                break;

            default:
                log.debug("The eventType [{}] couldn't be recognized", eventType);
        }
    }

    /**
     * Method for transforming the 'prettified' APS date String to a date string the event service will accept.
     *
     * @param dateString The String to parse. (has the following format: 'Thu Apr 08 07:52:45 CEST 2021')
     * @return A date string suitable for the event service
     * format for startDate/deadline: 2021-04-08T07:52:45.000+02:00
     * format for reachedDate: 2021-04-08T07:52:45.283Z
     * @throws ParseException
     */
    private String dateTransformer(String dateString, boolean taskCompletedDate) throws ParseException {
        DateTimeFormatter eventMessageDateFormatter = DateTimeFormatter.ofPattern(OFFSET_DATETIME_FORMAT);
        if (!taskCompletedDate) {
            Date date = new SimpleDateFormat(APS_DATE_FORMAT, Locale.ENGLISH).parse(dateString);
            ZonedDateTime zonedDateTime = date.toInstant().atZone(ZoneId.of("Europe/Amsterdam"));
            return eventMessageDateFormatter.format(zonedDateTime);
        }
        ZonedDateTime reachedDateObj;

        try {
            reachedDateObj = ZonedDateTime.parse(dateString);
        } catch (Exception e) {
            log.debug("Couldn't parse the passed date [{}]", dateString);
            Date reachedDateDate = new SimpleDateFormat(APS_DATE_FORMAT, Locale.ENGLISH).parse(dateString);
            reachedDateObj = reachedDateDate.toInstant().atZone(ZoneId.of("Europe/Amsterdam"));
        }

        return eventMessageDateFormatter.format(reachedDateObj);
    }
}
