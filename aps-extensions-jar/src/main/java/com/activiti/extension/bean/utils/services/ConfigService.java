package com.activiti.extension.bean.utils.services;

import com.activiti.domain.idm.EndpointConfiguration;
import com.activiti.service.api.EndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private final EndpointService endpointService;

    public ConfigService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    /**
     * Retrieve an endpoint configuration based on the given  name
     * @param endPointName The end point name
     * @return The found endpoint configuration in APS
     */
    public EndpointConfiguration getEndpoint(String endPointName) {
        logger.debug("Getting endpoint {}", endPointName);
        return endpointService.getConfigurationsForTenant(1L).stream()
                .filter(endpointConfiguration -> endPointName.equalsIgnoreCase(endpointConfiguration.getName()))
                .findFirst().orElse(null);
    }
}
