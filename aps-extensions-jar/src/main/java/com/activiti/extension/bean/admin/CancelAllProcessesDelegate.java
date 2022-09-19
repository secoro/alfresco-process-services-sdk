package com.activiti.extension.bean.admin;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("cancelAllProcessesDelegate")
public class CancelAllProcessesDelegate implements JavaDelegate {

    private final HistoryService historyService;
    private final ProcessRemovalService processRemovalService;

    public CancelAllProcessesDelegate(HistoryService historyService, ProcessRemovalService processRemovalService) {
        this.historyService = historyService;
        this.processRemovalService = processRemovalService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        processRemovalService.remove("cancel",
                historyService.createHistoricProcessInstanceQuery().unfinished(),
                execution.getProcessInstanceId());
    }
}
