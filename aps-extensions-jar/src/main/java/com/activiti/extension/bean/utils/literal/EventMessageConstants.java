package com.activiti.extension.bean.utils.literal;

import java.util.Arrays;
import java.util.List;

public class EventMessageConstants {

    // dossierEvent
    public static final String DOSSIER_EVENT = "dossierEvent";

    // milestoneEvent
    public static final String START_DOSSIER = "startDossier";
    public static final String MILESTONE_REACHED = "milestoneReached";
    public static final String MILESTONE_REACHED_BACKWARD = "milestoneReachedBackward";
    public static final String UPDATE_DOSSIER_CONFIGURATION = "updateDossierConfiguration";

    // expected keys for milestoneEvents
    public static final List<String> KEYS_START_DOSSIER = Arrays.asList("dossierId", "source", "startdate", "deadline", "initiatorUserName", "dossier_config", "eventType");
    public static final List<String> KEYS_MILESTONE_REACHED = Arrays.asList("dossierId", "source", "taskCompletedDate", "milestoneGenericName", "taskCompleter", "eventType");
    public static final List<String> KEYS_MILESTONE_REACHED_EXEC = Arrays.asList("dossierId", "source", "execBoardMeetingDate", "milestoneGenericName", "taskCompleter", "eventType");
    public static final List<String> KEYS_MILESTONE_REACHED_BACKWARD = Arrays.asList("dossierId", "source", "milestoneGenericName", "eventType");
    public static final List<String> KEYS_UPDATE_DOSSIER_CONFIGURATION = Arrays.asList("dossierId", "source", "dossier_config", "eventType");

    // milestone eventTypes
    public static final List<String> MILESTONE_EVENT_TYPES = Arrays.asList("startDossier", "milestoneReached", "milestoneReachedBackward", "updateDossierConfiguration");
}
