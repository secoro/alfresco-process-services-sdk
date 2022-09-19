package com.activiti.extension.bean.admin;

import com.activiti.extension.bean.utils.services.DeleteProcessInstanceService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProcessRemovalService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessRemovalService.class);

    private final DeleteProcessInstanceService deleteProcessInstanceService;

    public ProcessRemovalService(DeleteProcessInstanceService deleteProcessInstanceService){
        this.deleteProcessInstanceService = deleteProcessInstanceService;
    }

    public void remove(String operation, HistoricProcessInstanceQuery historicProcessInstanceQuery, String currentInstanceId){
        logger.info("[{} processes]", operation);
        List<HistoricProcessInstance> processInstances = historicProcessInstanceQuery.list()
                .stream()
                .filter(instance -> !currentInstanceId.equals(instance.getId()))
                .collect(Collectors.toList());

        processInstances
                .forEach(instance -> {
                    logger.info("[{}] {} {}", operation, instance.getId(), instance.getName());
                    deleteProcessInstanceService.deleteProcessInstance(instance);
                });
    }

}
