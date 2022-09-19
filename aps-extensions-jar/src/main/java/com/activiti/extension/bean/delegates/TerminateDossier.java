package com.activiti.extension.bean.delegates;

import com.activiti.extension.bean.utils.ACSHTTPClient;
import com.activiti.extension.bean.utils.services.ProcessService;
import com.activiti.extension.model.TerminateDossierDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.activiti.extension.bean.utils.literal.EndPoint.ICM;
import static com.activiti.extension.bean.utils.literal.EndPoint.ICM_DOSSIER_TERMINATE;

/**
 * Java Delegate that handles the call to ICM that terminates a dossier.
 */

@Slf4j
@Component("terminateDossier")
@RequiredArgsConstructor
public class TerminateDossier implements JavaDelegate {

    private final ProcessService processService;
    private final ACSHTTPClient httpClient;

    @Override
    public void execute(DelegateExecution execution) {
        log.debug("Executing the terminateDossier JavaDelegate");

        ObjectMapper mapper = new ObjectMapper();

        Map<String, String> serviceTaskVariables = processService.collectServiceTaskVariables(execution);

        // extract dossierId
        String dossierId = serviceTaskVariables.get("dossierId");

        // creating the dto
        TerminateDossierDTO terminateDossierDTO = TerminateDossierDTO.builder()
                .terminatedApprovedBy(serviceTaskVariables.get("taskCompleter"))
                .terminatedBy(serviceTaskVariables.get("requestorFullName"))
                .terminatedOn(serviceTaskVariables.get("taskCompletedDate"))
                .terminatedReason(serviceTaskVariables.get("terminatedReason"))
                .build();

        try {
            String json = mapper.writeValueAsString(terminateDossierDTO);
            httpClient.execute(String.format(ICM_DOSSIER_TERMINATE, dossierId), json, HttpMethod.PUT.name(), null, ICM);
        } catch (JsonProcessingException e) {
            log.error("Could not write to json string");
        }

        log.debug("Finished executing the terminateDossier JavaDelegate");
    }
}
