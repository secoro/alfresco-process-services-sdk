package com.activiti.extension.bean.utils;

import java.util.ArrayList;
import java.util.List;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.el.ExpressionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.activiti.domain.runtime.RelatedContent;
import com.activiti.service.runtime.RelatedContentService;

/**
 * @author incentro
 * Based on https://github.com/cijujoseph/activiti-examples/tree/master/aps-acs-integration-utils
 */
@Component("commonUtils")
public class CommonUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);

    private RelatedContentService relatedContentService;

    public CommonUtils(RelatedContentService relatedContentService){
        this.relatedContentService = relatedContentService;
    }

    /**
     * Get the field id from for example a form uploadFile controler, and return the content of the file which that field contains.
     * @param processInstanceId Id of the current process instance.
     * @param field Field id which contains the file.
     * @return  List with the content of all the files found. It should contains only one.
     * @throws BpmnError
     */
    public List<RelatedContent> getFieldContent(String processInstanceId, String field) throws BpmnError {
        List<RelatedContent> relatedContent = new ArrayList<RelatedContent>();
        Page<RelatedContent> page = null;
        int pageNumber = 0;
        try {
            while ((page == null) || (page.hasNext())) {
                page = relatedContentService
                        .getFieldContentForProcessInstance(
                                processInstanceId,field, 50,
                                pageNumber);
                relatedContent.addAll(page.getContent());
                pageNumber++;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return relatedContent;
    }

    /**
     * Gets an APS expresion which contains the id of a variable and return the value of the variable. The variable must contains ${}, Ex: ${parentId}.
     * @param execution   Context of the execution.
     * @param field Expresion which contains the id of a variable. The variable id must contains ${}, Ex: ${parentId}.
     * @return String with the value that was stored in the variable.
     */
    public String getExpressionValue(DelegateExecution execution, Expression field) {
        ExpressionManager expressionManager = Context.getProcessEngineConfiguration().getExpressionManager();
        Expression expression = expressionManager.createExpression(field.getExpressionText());
        return expression.getValue(execution).toString();
    }

    public boolean isNullOrEmptyString(String stringToCheck) {
        log.debug("Checking if input string is null or empty - [{}]", stringToCheck);
        return stringToCheck == null || stringToCheck.isEmpty();
    }
}
