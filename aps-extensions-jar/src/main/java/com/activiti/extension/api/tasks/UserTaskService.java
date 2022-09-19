package com.activiti.extension.api.tasks;

import com.activiti.extension.bean.utils.ACSHTTPClient;
import org.springframework.stereotype.Service;

@Service
public class UserTaskService {
    private final ACSHTTPClient acshttpClient;

    public UserTaskService(ACSHTTPClient acshttpClient) { this.acshttpClient = acshttpClient; }

    /**
     * @param requestJson - can be from the original historic task API call
     * @return String
     */
    public String getHistoricTasks(String requestJson) {
        final String URI = "/historic-tasks/query";
        final String REQUEST_METHOD = "POST";
        final String ENDPOINT_NAME = "APS";
        return acshttpClient.execute(URI, requestJson, REQUEST_METHOD, null, ENDPOINT_NAME);
    }
}
