package com.activiti.extension.bean.utils.services;

import com.activiti.domain.idm.EndpointConfiguration;
import com.activiti.service.api.EndpointService;
import com.google.api.client.util.Base64;
import org.activiti.engine.ActivitiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final ConfigService configService;
    private final EndpointService endpointService;
    private static final String ICM_ENDPOINT = "ICM";

    public AuthenticationService(ConfigService configService, EndpointService endpointService) {
        this.configService = configService;
        this.endpointService = endpointService;
    }

    /**
     * Get headers required to authenticate to the ICM.
     * @return The HttpHeaders to authenticate to the ICM.
     */
    public HttpHeaders getICMAuthorizationHeaders() {

        EndpointConfiguration endpoint = configService.getEndpoint(ICM_ENDPOINT);

        if(endpoint == null){
            throw new ActivitiException("ICM endpoint could not be found!");
        }
        return createAuthorizationHeaders(endpoint.getBasicAuth().getUsername(), endpointService.getDecryptedBasicAuthPassword(endpoint.getBasicAuth().getPassword()));
}

    /**
     * Create a set of authorization headers based on the given username and password
     * @param username The given username
     * @param password The given password
     * @return The resulting HttpHeaders.
     */
    private HttpHeaders createAuthorizationHeaders(String username, String password) {
        logger.debug(String.format("Generating headers"));
        HttpHeaders header = new HttpHeaders();

        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("US-ASCII")) );
        String authHeader = "Basic " + new String( encodedAuth );

        header.add("Authorization", authHeader);
        logger.debug("Generated header with value {}", authHeader);
        return header;
    }
}
