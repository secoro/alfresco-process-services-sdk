package com.activiti.extension.bean.delegates;

import com.activiti.domain.runtime.RelatedContent;
import com.activiti.extension.bean.utils.ACSHTTPClient;
import com.activiti.extension.bean.utils.CommonUtils;
import com.activiti.extension.bean.utils.services.ReportsFolderService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;


@Component("storeReport")
public class StoreReport implements JavaDelegate {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Expression reportsFile;
    private Expression dossierNodeId;

    private final ACSHTTPClient acsHttpClient;
    private final CommonUtils commonUtils;
    private final ReportsFolderService reportsFolderService;

    private static final String ROOT_URL = "/alfresco/api/-default-/public/alfresco/versions/1/nodes/";
    private static final String URL_QUERY = "/children";


    public StoreReport(ReportsFolderService reportsFolderService, CommonUtils commonUtils, ACSHTTPClient acsHttpClient)  {
        this.reportsFolderService = reportsFolderService;
        this.commonUtils = commonUtils;
        this.acsHttpClient = acsHttpClient;
    }

    @Override
    public void execute(DelegateExecution delegateExecution) {

        logger.debug("Executing task storeReport...");

        Objects.requireNonNull(reportsFile,"Did not receive a reportsFile. Did you pass the value correctly in APS?");
        Objects.requireNonNull(dossierNodeId, "Dossier node id is null. Did you pass the value correctly in APS?");

        String dossierNodeIdValue = commonUtils.getExpressionValue(delegateExecution, dossierNodeId);
        String reportsFolderNodeId = reportsFolderService.getReportsFolder(dossierNodeIdValue);

        List<RelatedContent> relatedContentList = commonUtils.getFieldContent(delegateExecution.getProcessInstanceId(), reportsFile.getExpressionText());

        // Upload the file
        logger.trace(String.format("Uploading file to ACS as child of a node with id '%s'.", reportsFolderNodeId));
        String uploadNodeUrl = ROOT_URL + reportsFolderNodeId + URL_QUERY;
        acsHttpClient.execute(uploadNodeUrl, null, "POST", relatedContentList.get(0), "ACS");
    }
}
