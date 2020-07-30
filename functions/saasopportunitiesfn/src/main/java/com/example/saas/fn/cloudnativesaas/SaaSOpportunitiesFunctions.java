/*
Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/
package com.example.saas.fn.cloudnativesaas;

import com.example.saas.fn.cloudnativesaas.exceptions.BadRequestException;
import com.example.saas.fn.cloudnativesaas.exceptions.UnAuthorizedException;
import com.example.saas.fn.cloudnativesaas.exceptions.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.Region;
import com.oracle.idcs.oauth.SecurityHelper;


/**
 * SaaSOpportunitiesFunctions : This function handles FN requests which pertain to opportunities (Serverless service pattern)
 */
public class SaaSOpportunitiesFunctions {

    private static final Logger LOGGER = Logger.getLogger("CLOUDNATIVESAAS");
    private String fnURIBase = "";
    private String debugJWT = "";
    private String fusionHostname = "";
    private String logDebugLevel="INFO";    // Default is Info
    private static final  String NOTSET="NOTSET";
    private static final  int SC_BADREQUEST =400;
    private static final int SC_UNAUTHORIZED = 401;
    private static final int SC_NOTFOUND = 404;
    private static final  int SC_INTERNALERROR = 500;
    private static final  String CT_APPLICATION_JSON="application/json";
    private static final  String CT_TEXT_PLAIN="text/plain";
    private RuntimeContext context;
    private Boolean fullOAauth = false;


    private ObjectMapper objectMapper = new ObjectMapper();

    // inner class used for reporting error messages
    public static class JsonResult
    {
        public String getError() {
            return error;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        private String error;
        private String errorMessage;
        public JsonResult(String error,String errorMessage)
        {
            this.error=error;
            this.errorMessage=errorMessage;
        }

    }

    /**
     * @param ctx : Runtime context passed in by Fn, used to set default parameters
     */
    @FnConfiguration
    public void config(RuntimeContext ctx) {
        context = ctx;

        fusionHostname = ctx.getConfigurationByKey("fusion_hostname").orElse(NOTSET);
        fnURIBase = ctx.getConfigurationByKey("gtw_uri_base").orElse("/cloudnativefusion/opportunities");
        debugJWT = ctx.getConfigurationByKey("debug_jwt").orElse(NOTSET);
        logDebugLevel = ctx.getConfigurationByKey("debug_level").orElse("INFO");

        // Flag to check if use the Full OAuth IDCS Approach
        fullOAauth = Boolean.parseBoolean(ctx.getConfigurationByKey("full_oauth").orElse("false"));

        LOGGER.info("Configuration read : debugJWT=[" + debugJWT + "] fusionHostname=[" + fusionHostname+"] fnuribase=["+fnURIBase+"]");
    }

    /**
     * Helper function to extract body data from fn stream
     * @param s
     * @return
     */
    private  String readData(InputStream s) {
        return new BufferedReader(new InputStreamReader(s))
                .lines().collect(Collectors.joining(" "));

    }

    /**
     * Main Function entry point
     * @param rawInput  : Rawinput, used for getting body
     * @param hctx : HTTPGatewayContext, used for setting response codes
     * @return : Response to client
     * @throws JsonProcessingException
     */
    public OutputEvent handleRequest(InputEvent rawInput, HTTPGatewayContext hctx) throws JsonProcessingException {
        // process request
        LOGGER.setLevel(Level.parse(logDebugLevel));
        try {



            String jwtUsername = JWTUtils.getJWTUsername(rawInput);
            String jwttoken = JWTUtils.getJWTToken(rawInput);
            LOGGER.info("handleRequest called with following data");
            LOGGER.info("Username = " + jwtUsername);
            // To view all headers use rawInput.getHeaders().toString());
            // Override JWT token if there is one in the function config. This allows us to test when OCI & FA are not  associated
            if (!debugJWT.equals(NOTSET) && !debugJWT.equals("")) {
                jwttoken = this.debugJWT;
                LOGGER.info("OVERRIDE JWT TOKEN set");
            }

            // Full Oauth scenario Perform exchange of tokens
            if(fullOAauth) {
                LOGGER.log(Level.INFO, "Full Oauth Assertion scenario - Perform exchange of tokens");
                SecurityHelper idcsSecurityHelper = new SecurityHelper(context)                   // Initialize SecurityHelper with RuntimeContext
                                                    .setOciRegion(Region.US_PHOENIX_1)            // Specify the OCI region, used to retrieve Secrets.
                                                    .extractSubFromJwtTokenHeader(rawInput);      // Extracts the subject from Token in Fn-Http-H-Authorization.

                // Get OAuth Access token with JWT Assertion using the principal extracted from Fn-Http-H-Access-Token Header
                jwttoken = idcsSecurityHelper.getAssertedAccessToken();
                LOGGER.log(Level.INFO, "Successfully token retrived with IDCS Assertion");
                LOGGER.log(Level.FINEST, "Access Token from assertion [" + jwttoken + "]");
            }


            // if no debug jwt then there must be a real jwt or else error
            if (jwttoken.equals("")) {
                LOGGER.severe("Error JWT token empty or null");
                hctx.setStatusCode(SC_BADREQUEST);
                return OutputEvent.fromBytes(
                        "{'error':'Error JWT token empty or null and no debug token provided'}".getBytes(), // Data
                        OutputEvent.Status.Success,     // Any numeric HTTP status code can be used here
                        CT_APPLICATION_JSON
                );
            }
            //
            // Little router within the function to determine which method to call based on the HTTP Method passed in
            // GET /opportunity = Query all
            // GET /opportunity/{number} = Query Single
            // PATCH /opportunity/{number} = Patch single
            //

            String saasResponse = "";
            String httpMethod = hctx.getMethod();
            String httpRequestURI = hctx.getRequestURL();
            if (httpMethod.equalsIgnoreCase("GET")) {
                // Is there a subresource, ie a optyid?
                if (httpRequestURI.equalsIgnoreCase(fnURIBase)) {
                    // Query all optys
                    LOGGER.info("fnURIBase=["+fnURIBase+"] httpRequestURI=["+httpRequestURI+" therefore Query all opty requested");
                    saasResponse = SaaSHelper.queryOptys(jwttoken, fusionHostname);
                } else {
                    LOGGER.info("fnURIBase=["+fnURIBase+"] httpRequestURI=["+httpRequestURI+" therefore Query SINGLE opty requested");

                    // Query Single opportunity
                    // Substring is to remove the initial /
                    String optionalOptyId = httpRequestURI.substring(fnURIBase.length() + 1);
                    saasResponse = SaaSHelper.querySingleOpty(jwttoken, fusionHostname, optionalOptyId);
                }
            } else if (httpMethod.equalsIgnoreCase("PATCH")) {
                // Patch request
                LOGGER.info("Patch Request Detected");

                String optionalOptyId = httpRequestURI.substring(fnURIBase.length());
                saasResponse = SaaSHelper.updateOpty(jwttoken, fusionHostname, optionalOptyId, rawInput.consumeBody(this::readData));
            }
            else
            {
                // Do nothing
                LOGGER.info("Unrecognized HTTP VERB Received");
            }
            return  OutputEvent.fromBytes(
                    saasResponse.getBytes(), // Data
                    OutputEvent.Status.Success,
                    CT_APPLICATION_JSON            // Content type

            );


        } catch (UnAuthorizedException e) {
            // Unauthorized by SaaS
            hctx.setStatusCode(SC_UNAUTHORIZED);
            LOGGER.severe("Received NotAuthorizedException Error from SaaS " + e.getLocalizedMessage());

            return OutputEvent.fromBytes(
                        objectMapper.writeValueAsBytes(
                                    new JsonResult("UnAuthorizedException : ","Received NotAuthorizedException Error from SaaS: "+e.getLocalizedMessage())
                        ),
                    OutputEvent.Status.Success,
                    CT_APPLICATION_JSON
            );



        } catch (NotFoundException e) {
            LOGGER.severe("NotFoundException Error " + e.getLocalizedMessage());
            hctx.setStatusCode(SC_NOTFOUND);
            return OutputEvent.fromBytes(("Not found : " + e.getLocalizedMessage()).getBytes(),
                    OutputEvent.Status.Success,
                    CT_TEXT_PLAIN    // Content type

            );
        } catch (BadRequestException e) {
            // Something else went wrong with the REST Request
            hctx.setStatusCode(SC_BADREQUEST);
            LOGGER.severe("BadRequestException Error " + e.getLocalizedMessage());
            return OutputEvent.fromBytes(
                    objectMapper.writeValueAsBytes(
                            new JsonResult("BadReques: ",e.getLocalizedMessage())
                    ),
                    OutputEvent.Status.Success,
                    CT_APPLICATION_JSON
            );


        } catch (Exception e) {
            // Something else went wrong, really bad
            LOGGER.severe("Exception Error " + e.getLocalizedMessage());
            hctx.setStatusCode(SC_INTERNALERROR);
            return OutputEvent.fromBytes(
                    objectMapper.writeValueAsBytes(
                            new JsonResult("GenericException"," Something went wrong...."+e.getLocalizedMessage())
                    ),
                    OutputEvent.Status.Success,
                    CT_APPLICATION_JSON
            );
        }
    }
}