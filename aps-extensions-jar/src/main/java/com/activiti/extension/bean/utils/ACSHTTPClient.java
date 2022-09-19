package com.activiti.extension.bean.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.activiti.engine.ActivitiException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.activiti.domain.idm.EndpointConfiguration;
import com.activiti.domain.runtime.RelatedContent;
import com.activiti.service.api.EndpointService;
import com.activiti.service.runtime.RelatedContentStreamProvider;

/**
 * @author Ciju Joseph
 * https://github.com/cijujoseph/activiti-examples/tree/master/aps-acs-integration-utils
 */

// http client bean with connection pool
@Component("acsHTTPClient")
public class ACSHTTPClient {

    private static final Logger logger = LoggerFactory.getLogger(ACSHTTPClient.class);
    private final CloseableHttpClient httpClient;

    private Environment env;
    private EndpointService endpointService;
    private RelatedContentStreamProvider relatedContentStreamProvider;

    public ACSHTTPClient(Environment env, EndpointService endpointService, RelatedContentStreamProvider relatedContentStreamProvider) {
        this.env = env;
        this.endpointService = endpointService;
        this.relatedContentStreamProvider = relatedContentStreamProvider;

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(200);
        cm.setMaxTotal(200);
        httpClient = HttpClients.custom().setConnectionManager(cm).build();
    }

    public String execute(String requestURI, String requestJson, String method, RelatedContent relatedContent, String endPointName) {

        EndpointConfiguration endpointConfig = null;

        // Loop through all the endpoints configured in the Identity Management
        // module of activiti-app and find the ACS endpoint config
        List<EndpointConfiguration> endpoints = endpointService.getConfigurationsForTenant(1L);
        for (EndpointConfiguration endpoint : endpoints) {
            logger.trace(endpoint.getName());
            if (endpoint.getName().equalsIgnoreCase(endPointName))
                endpointConfig = endpoint;
        }

        String acsEndpointURL;
        if (endpointConfig == null){
            logger.error("No endpoint configuration was set in order to make the http call.");
            throw new ActivitiException("No endpoint configuration was set in order to make the http call");
        } else {
            acsEndpointURL = endpointConfig.getUrl() + requestURI;
        }

        String jsonString = null;
        CloseableHttpResponse response = null;
        HttpRequestBase httpRequest;
        if (method.equals(HttpPost.METHOD_NAME)) {
            httpRequest = new HttpPost(acsEndpointURL);
        } else if (method.equals(HttpPut.METHOD_NAME)) {
            httpRequest = new HttpPut(acsEndpointURL);
        } else {
            httpRequest = new HttpGet(acsEndpointURL);
        }
        try {
            if (relatedContent != null) {
                try {
                    InputStream inputStream = relatedContentStreamProvider.getContentStream(relatedContent);

                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setCharset(StandardCharsets.UTF_8);
                    builder.addBinaryBody("filedata", inputStream, ContentType.create(relatedContent.getMimeType()),
                            relatedContent.getName());
                    builder.addPart("name", new StringBody(relatedContent.getName(), ContentType.create("text/plain", StandardCharsets.UTF_8)));
                    builder.addTextBody("autoRename", "true");

                    HttpEntity multipart = builder.build();
                    ((HttpPost) httpRequest).setEntity(multipart);
                } catch (Exception e) {
                    throw new ActivitiException("error while performing content extraction" + e.getMessage());
                }

            } else if (method.equals(HttpPost.METHOD_NAME) || method.equals(HttpPut.METHOD_NAME)) {
                StringEntity input = new StringEntity(requestJson, StandardCharsets.UTF_8);
                input.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
                input.setContentEncoding(StandardCharsets.UTF_8.toString());
                if (method.equals(HttpPost.METHOD_NAME)) {
                    ((HttpPost) httpRequest).setEntity(input);
                } else {
                    ((HttpPut) httpRequest).setEntity(input);
                }
            }
            String username = endpointConfig.getBasicAuth().getUsername();
            String password = endpointService
                    .getDecryptedBasicAuthPassword(endpointConfig.getBasicAuth().getPassword());
            String encoding = new String(Base64.encodeBase64((username + ":" + password).getBytes()));
            httpRequest.addHeader("Authorization", "Basic " + encoding);

            long httpStartTime = new Date().getTime();
            response = (CloseableHttpResponse) executeHttpRequest(httpRequest);
            logger.debug("Total HTTP Execution Time: " + ((new Date()).getTime() - httpStartTime));

            try {
                jsonString = EntityUtils.toString(response.getEntity());
            } catch (Exception e) {
                logger.error("error while parsing JSON response: " + jsonString, e);
            }

        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                logger.error("Failed to close HTTP response after invoking: " + method + " " + acsEndpointURL, e);
            }
            httpRequest.releaseConnection();
        }

        return jsonString;
    }

    private HttpResponse executeHttpRequest(HttpRequestBase httpRequest) {

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(httpRequest);
        } catch (IOException e) {
            throw new ActivitiException("error while executing http request: " + httpRequest.getMethod() + " " + httpRequest.getURI(),
                    e);
        }

        if (response.getStatusLine().getStatusCode() >= 400) {
            throw new ActivitiException("error while executing http request " + httpRequest.getMethod() + " " + httpRequest.getURI()
                    + " with status code: " + response.getStatusLine().getStatusCode());
        }
        return response;
    }
}
