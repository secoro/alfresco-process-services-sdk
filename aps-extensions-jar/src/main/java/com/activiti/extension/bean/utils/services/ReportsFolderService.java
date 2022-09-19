package com.activiti.extension.bean.utils.services;

import com.activiti.extension.bean.utils.ACSHTTPClient;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReportsFolderService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SEARCH_URL = "/alfresco/api/-default-/public/search/versions/1/search";

    private final ACSHTTPClient acsHttpClient;

    public ReportsFolderService(ACSHTTPClient acshttpClient){
        this.acsHttpClient = acshttpClient;
    }

    public String getReportsFolder(String dossierNodeId){

        //build query that searches for folders of type reportsFolder.
        String queryJson = String.format("{\n" +
                "    \"query\": {\n" +
                "        \"query\": \"TYPE:'case:subfolder' AND case:subfolderType:'reportsFolder' AND PARENT:'workspace://SpacesStore/%s'\"\n" +
                "    },\n" +
                "    \"localization\": {\n" +
                "        \"locales\": [ \"nl_NL\", \"en_US\" ]\n" +
                "    }\n" +
                "}", dossierNodeId);

        //query the children of dossier node to locate reportsfolder
        String responseJsonAsString = acsHttpClient.execute(SEARCH_URL, queryJson, "POST", null, "ACS");
        JSONObject jsonObject = new JSONObject(responseJsonAsString);
        JSONArray entries = jsonObject.getJSONObject("list").getJSONArray("entries");

        //there should be exactly one folder of type reportsFolder

        if(entries.length() == 0){
            String errorMessage = "Did not find a reportsfolder for node " + dossierNodeId + ".";
            logger.warn(errorMessage);
            throw new IllegalStateException(errorMessage);
        } else if (entries.length() > 1){
            String errorMessage = "Found multiple reportsfolders for node " + dossierNodeId + ". This should not be possible, something is wrong.";
            logger.warn(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        String reportsFolderNodeId = (String) entries.getJSONObject(0).getJSONObject("entry").get("id");
        logger.debug("Found reportsfolder with node id " + reportsFolderNodeId);
        return reportsFolderNodeId;
    }
}
