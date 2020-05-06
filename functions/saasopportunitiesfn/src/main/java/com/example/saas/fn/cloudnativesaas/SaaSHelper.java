/*
Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/

package com.example.saas.fn.cloudnativesaas;


import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import com.example.saas.fn.cloudnativesaas.exceptions.BadRequestException;
import com.example.saas.fn.cloudnativesaas.exceptions.UnAuthorizedException;
import com.example.saas.fn.cloudnativesaas.exceptions.NotFoundException;

import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.http.HttpStatus.*;

public class SaaSHelper {
    private static String REST_FRAMEWORK_CONTENT_TYPE ="REST-Framework-Version";
    private static String BEARER="Bearer ";

    private static final Logger LOGGER = Logger.getLogger("CLOUDNATIVESAAS");
    public static final String OPTY_URI = "/salesApi/resources/latest/opportunities";
    // Provide a default query so that Fusion saas doesnt return the entire Opportunity payload (which is rather large).
    // Use for both single and multiple queries
    private static final String DEFAULT_QUERY_PARAMS = "?onlyData=true&fields=OptyNumber,TargetPartyName,Name,DescriptionText,OptyNumber,StatusCode,PrimaryContactPartyName,PrimaryContactFormattedPhoneNumber,PrimaryContactEmailAddress";
    // Default query is to only query back WON,LOST and OPEN optys
    private static final String DEFAULT_QUERY_PARAMS_FULL = "&q=StatusCode%20in%20('WON','LOST','OPEN')";
    private static String UNAUTH_EXCEPTION_MSG="SaaS Returned UnAuthorized Exception";
    private static String NOTFOUND_EXCEPTION_MSG="SaaS Returned Not Found Exception";
    private static String SAAS_GENERIC_ERROR="Error calling SaaS, got http code {} {}";

    private SaaSHelper()
    {
        throw new IllegalStateException("SaaSHelper is a utility class");
    }

    /**
     * Queries opportunities (plural)
     *
     * @param jwtToken
     * @param fusionURL
     * @return
     * @throws UnAuthorizedException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     * @throws IOException
     * @throws InterruptedException
     */
    public static String queryOptys(String jwtToken, String fusionURL)
            throws UnAuthorizedException, NotFoundException, IOException, BadRequestException {


        LOGGER.info("Entered queryOptys_httpClient with fusionURL=" + fusionURL);
        fusionURL = fusionURL + OPTY_URI + DEFAULT_QUERY_PARAMS + DEFAULT_QUERY_PARAMS_FULL + "&limit=10";

        String responseJson = "";
        int status = 0;

        // Make REST Call to SaaS, ensuring we're using REST-Framework-Version 6
        HttpClient client = HttpClients.custom().build();
        HttpUriRequest request = RequestBuilder.get().setUri(fusionURL).
                setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).
                setHeader(REST_FRAMEWORK_CONTENT_TYPE, "6").
                setHeader(HttpHeaders.AUTHORIZATION, BEARER + jwtToken).
                build();
        HttpResponse response = client.execute(request);
        responseJson = EntityUtils.toString(response.getEntity());
        status = response.getStatusLine().getStatusCode();

        LOGGER.info("Response Status from REST SaaS Call "+status);
        if (status == SC_UNAUTHORIZED) {

            LOGGER.log(Level.INFO,UNAUTH_EXCEPTION_MSG);
            throw new UnAuthorizedException(UNAUTH_EXCEPTION_MSG);
        }

        if (status == SC_NOT_FOUND) {

            LOGGER.info(NOTFOUND_EXCEPTION_MSG);
            throw new NotFoundException(NOTFOUND_EXCEPTION_MSG);
        }
        // Any other error message gets thrown with any response text
        if (status != SC_OK) {
            String errorMessage = String.format(SAAS_GENERIC_ERROR, status, responseJson);
            LOGGER.info(errorMessage);
            throw new BadRequestException(errorMessage);
        }
        LOGGER.finest("JSON Response from SaaS " + responseJson);
        return (responseJson);


    }


    /**
     * queries single opportunity
     * <p>
     *
     * @param jwtToken
     * @param fusionURL
     * @param optyId
     * @return
     * @throws UnAuthorizedException
     * @throws UnAuthorizedException
     * @throws NotFoundException
     */
    public static String querySingleOpty(String jwtToken, String fusionURL, String optyId) throws NotFoundException, IOException, BadRequestException, UnAuthorizedException {

        LOGGER.info("Entered querySingleOpty opty with fusionURL=" + fusionURL + " optyid=" + optyId);

        fusionURL = fusionURL + OPTY_URI + "/" + optyId + DEFAULT_QUERY_PARAMS;
        // Query single opty
        LOGGER.info("Creating client with URL " + fusionURL);

        // Make REST Call to SaaS, ensuring we're using REST-Framework-Version 6
        String responseJson = "";
        int status = 0;
        HttpClient client = HttpClients.custom().build();
        HttpUriRequest request = RequestBuilder.get().setUri(fusionURL).
                setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).
                setHeader(REST_FRAMEWORK_CONTENT_TYPE, "6").
                setHeader(HttpHeaders.AUTHORIZATION, BEARER + jwtToken).
                build();
        HttpResponse response = client.execute(request);
        responseJson = EntityUtils.toString(response.getEntity());
        status = response.getStatusLine().getStatusCode();


        LOGGER.info("Response Status from REST Call " + status);
        if (status == SC_UNAUTHORIZED) {

            LOGGER.severe(UNAUTH_EXCEPTION_MSG);
            throw new UnAuthorizedException(UNAUTH_EXCEPTION_MSG);
        }
        if (status == SC_NOT_FOUND) {
            LOGGER.severe(NOTFOUND_EXCEPTION_MSG);
            throw new NotFoundException(NOTFOUND_EXCEPTION_MSG);
        }
        // Any other error message gets thrown with any response text
        if (status != SC_OK) {
            String errorMessage = String.format(SAAS_GENERIC_ERROR, status, responseJson);
            LOGGER.severe(errorMessage);
            throw new BadRequestException(errorMessage);
        }
        LOGGER.fine("Response from SaaS " + responseJson);
        return (responseJson);
    }

    /**

     *
     * @param jwtToken
     * @param fusionURL
     * @param optyId
     * @param optyUpdatePayload
     * @return
     */
    public static String updateOpty(String jwtToken, String fusionURL, String optyId, String optyUpdatePayload) throws UnAuthorizedException, NotFoundException, IOException, BadRequestException {

        LOGGER.info("Entered updateOpty with fusionURL=" + fusionURL + " OptyId=" + optyId + " Update=" + optyUpdatePayload);
        fusionURL = fusionURL + OPTY_URI + "/" + optyId;
        // Call Fusion SalesCloud PATCH Opportunities
        LOGGER.info("Creating  client with URL " + fusionURL);


        // Make REST Call to SaaS, ensuring we're using REST-Framework-Version 6
        String responseJson = "";
        int status = 0;
        HttpClient client = HttpClients.custom().build();
        HttpUriRequest request = RequestBuilder.patch().setUri(fusionURL).
                setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).
                setHeader(REST_FRAMEWORK_CONTENT_TYPE, "6").
                setHeader(HttpHeaders.AUTHORIZATION, BEARER + jwtToken).
                setEntity(new StringEntity(optyUpdatePayload, ContentType.APPLICATION_JSON)).
                build();
        HttpResponse response = client.execute(request);
        responseJson = EntityUtils.toString(response.getEntity());
        status = response.getStatusLine().getStatusCode();


        LOGGER.info("Response Status from REST Call " + status);
        if (status == SC_UNAUTHORIZED) {
            LOGGER.info(UNAUTH_EXCEPTION_MSG);
            throw new UnAuthorizedException(UNAUTH_EXCEPTION_MSG);
        }
        if (status == SC_NOT_FOUND) {

            LOGGER.info(NOTFOUND_EXCEPTION_MSG);
            throw new NotFoundException(NOTFOUND_EXCEPTION_MSG);
        }
        // Any other error message gets thrown with any response text
        if (status != SC_OK) {
            String errorMessage = String.format(SAAS_GENERIC_ERROR, status, responseJson);
            LOGGER.info(errorMessage);
            throw new BadRequestException(errorMessage);
        }
        LOGGER.fine("Response from SaaS " + responseJson);
        return (responseJson);
    }
}
