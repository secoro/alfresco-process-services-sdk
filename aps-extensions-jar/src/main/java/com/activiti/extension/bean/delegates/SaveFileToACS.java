package com.activiti.extension.bean.delegates;

import com.activiti.domain.runtime.RelatedContent;
import com.activiti.extension.bean.utils.ACSHTTPClient;
import com.activiti.extension.bean.utils.CommonUtils;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.util.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * @author incentro
 * Based on https://github.com/cijujoseph/activiti-examples/tree/master/aps-acs-integration-utils
 */
@Component("saveFileToACS")
public class SaveFileToACS implements JavaDelegate {

    private static Logger logger = LoggerFactory.getLogger(SaveFileToACS.class);

    private ACSHTTPClient acsHTTPClient;
    private CommonUtils commonUtils;
    private RuntimeService runtimeService;

    // This is the id of the control which is in the form. Example: upload1
    private Expression fileFieldId;
    // This is the textBox which contains the parent folder id. Example: ${parentId}
    // Be careful, this one needs to be surrounded by ${}
    private Expression parentId;
    // This is the id of the process variable where the new file id will be stored. Example: newFileId
    private Expression uploadedFileId;
    // Name of the endpoint defined in APS
    private Expression endPointName;

    public SaveFileToACS(ACSHTTPClient acsHTTPClient, CommonUtils commonUtils, RuntimeService runtimeService){
        this.acsHTTPClient = acsHTTPClient;
        this.commonUtils = commonUtils;
        this.runtimeService = runtimeService;
    }

    /**
     * Upload a file to Alfresco Content Service
     * @param execution     Current execution context of the process.
     * @throws IOException  It can be thrown by the HTTP client in case something goes wrong when trying to read the file.
     */
    public void execute(DelegateExecution execution) {
        logger.debug("Executing task SaveFileToACS");
        // If parentId input field is empty, throw an error.
        if(fileFieldId == null) {
            throw new RuntimeException("FileFieldId input field wasn't fill in the process design. Please check you process design.");
        }

        List<RelatedContent> relatedContentList = commonUtils.getFieldContent(execution.getProcessInstanceId(), fileFieldId.getExpressionText());
        if(relatedContentList!=null && !relatedContentList.isEmpty()) {
            // If parentId input field is empty, throw an error.
            if(parentId == null){
                throw new RuntimeException("ParentId input field wasn't fill in the process design. Please check you process design.");
            }

            String parentIdValue = commonUtils.getExpressionValue(execution, parentId);

            // If parentIdValue value is empty, throw an error.
            if(parentIdValue == null){
                throw new RuntimeException(String.format("The field set as input in the input field ParentId contains no value.That field is '%s'.", parentId.getExpressionText()));
            }

            // Upload the file
            logger.trace(String.format("Uploading file as child of a node with id '%s'.", parentIdValue));
            String uploadNodeUrl = "/alfresco/api/-default-/public/alfresco/versions/1/nodes/" + parentIdValue + "/children";
            String newFileJson = acsHTTPClient.execute(uploadNodeUrl, null, "POST", relatedContentList.get(0), endPointName.getExpressionText());

            // Output parameter is optional
            if(uploadedFileId != null && !uploadedFileId.getExpressionText().isEmpty()){
                // Process the Json to return only the node id
                JSONObject jsonObject = new JSONObject(newFileJson);
                JSONObject myResponse = jsonObject.getJSONObject("entry");
                String newFileId = myResponse.getString("id");
                logger.debug(String.format("File with id '%s' uploaded to ACS as child of a node with id '%s'.", newFileId, parentIdValue));
                runtimeService.setVariable(execution.getProcessInstanceId(), uploadedFileId.getExpressionText(), newFileId);
            }
        }
    }
}
