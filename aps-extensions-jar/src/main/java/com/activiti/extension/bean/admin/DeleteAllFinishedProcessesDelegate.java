package com.activiti.extension.bean.admin;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("deleteAllFinishedProcessesDelegate")
public class DeleteAllFinishedProcessesDelegate implements JavaDelegate {

    private final HistoryService historyService;
    private final ProcessRemovalService processRemovalService;

    public DeleteAllFinishedProcessesDelegate(HistoryService historyService, ProcessRemovalService processRemovalService) {
        this.historyService = historyService;
        this.processRemovalService = processRemovalService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        processRemovalService.remove("delete",
                historyService.createHistoricProcessInstanceQuery().finished(),
                execution.getProcessInstanceId());
    }
}
