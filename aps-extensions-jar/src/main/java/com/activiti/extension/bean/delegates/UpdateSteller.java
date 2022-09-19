package com.activiti.extension.bean.delegates;

import com.activiti.extension.bean.utils.ACSHTTPClient;
import com.activiti.extension.bean.utils.CommonUtils;
import com.activiti.extension.bean.utils.services.CustomTaskService;
import com.activiti.extension.model.relations.RelatedProcessTaskListModel;
import com.activiti.extension.model.relations.RelatedProcessTaskModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.activiti.extension.bean.utils.literal.EndPoint.ICM;
import static com.activiti.extension.bean.utils.literal.EndPoint.ICM_PROCESS_RELATIONS;
import static com.activiti.extension.constant.APSConstants.Process.DELAYREQUEST;
import static com.activiti.extension.constant.APSConstants.Process.MAIN;

/**
 * @deprecated This Java delegate was used only in the Omboeken steller process. With DIVA-11560 we will be using an API
 * to reassign dossiers. However, this delegate will still be used by old omboeken processes (if any).
 */
@Deprecated(since = "2220", forRemoval = true)
@Slf4j
@RequiredArgsConstructor
@Component("updateSteller")
public class UpdateSteller implements JavaDelegate {

    private static final String MAIN_PROCESS_INSTANCE_ID = "mainProcessInstanceId";
    private static final String TUSSENBERICHT_PROCESS_INSTANCE_ID = "tussenberichtProcessInstanceId";

    private Expression dossierId;

    private final CommonUtils commonUtils;
    private final CustomTaskService customTaskService;
    private final ACSHTTPClient acsHttpClient;

    @Override
    public void execute(DelegateExecution execution) {
        log.debug("Executing task updateSteller...");

        Objects.requireNonNull(dossierId, "Did not receive dossierId.");
        String dossierIdValue = commonUtils.getExpressionValue(execution, dossierId);

        //set main process id
        RelatedProcessTaskModel mainProcess = getProcessInstance(dossierIdValue, MAIN.name());
        setProcessId(execution, getProcessId(mainProcess), MAIN_PROCESS_INSTANCE_ID);

        //set tussenbericht process id
        RelatedProcessTaskModel tussenberichtProcess = getProcessInstance(dossierIdValue, DELAYREQUEST.name());
        setProcessId(execution, getProcessId(tussenberichtProcess), TUSSENBERICHT_PROCESS_INSTANCE_ID);

        //set active stellertasks
        List<Integer> activeStellerTasksPerDossier = customTaskService.getActiveStellerTasksPerDossier(dossierIdValue);
        execution.setVariable("tasks", activeStellerTasksPerDossier);
        execution.setVariable("tasksLength", activeStellerTasksPerDossier.size());

        //set neccessary omboeken steller process variables
        customTaskService.setOmboekenVariables(execution, dossierIdValue);
    }

    private int getProcessId(RelatedProcessTaskModel process) {
        return process == null ? 0 : Integer.parseInt(process.getProcessInstanceId());
    }

    public void setProcessId(DelegateExecution execution, int processId, String processName) {
        if (processId != 0) {
            execution.setVariable(processName, processId);
            log.info("Set " + processName + " to " + processId + ".");
        } else {
            log.warn("Did not find a " + processName + ".");
        }
    }

    private RelatedProcessTaskModel getProcessInstance(String dossierId, String relationType) {
        try {
            String path = String.format(ICM_PROCESS_RELATIONS, dossierId, relationType);
            String json = acsHttpClient.execute(
                    path, null, HttpMethod.GET.name(), null, ICM);
            RelatedProcessTaskListModel relatedProcessTaskListModel = new ObjectMapper().readValue(
                    json, RelatedProcessTaskListModel.class);
            return relatedProcessTaskListModel.getRelations().stream().findFirst().orElse(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
