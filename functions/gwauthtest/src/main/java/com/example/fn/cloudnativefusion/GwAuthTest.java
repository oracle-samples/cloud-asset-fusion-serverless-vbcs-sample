/*
Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/
package com.example.fn.cloudnativefusion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;

import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GwAuthTest class, used for testing GTW Authentication. This function will display most/all header variables, username etc
 */
public class GwAuthTest {
    public   static final String TOKEN_BEARER_PREFIX="Bearer";
    private  static final Logger LOGGER = Logger.getLogger(GwAuthTest.class.getName());
    private  static final int SERVER_ERROR_SC = 500;

    @FnConfiguration
    public void config(RuntimeContext ctx) {

        LOGGER.setLevel(Level.parse(ctx.getConfigurationByKey("debug_level").orElse("INFO")));
    }
    /**
     * Main function
     * @param rawInput
     * @return
     */
    public OutputEvent handleRequest(HTTPGatewayContext hctx, InputEvent rawInput) {


         try {
             LOGGER.info("handleRequest called with following data");
             // Logging disabled otherwise we'd be printing the JWT token to the log file too - not secure.
             // LOGGER.info("FN Request Headers " + rawInput.getHeaders().toString());

             String httpMethod = rawInput.getHeaders().get("Fn-Http-Method").orElse("NOTSET");
             String jwtUsername = getJWTUsername(rawInput);
             String jwtToken = getJWTToken(rawInput);
             LOGGER.info("Username found = " + jwtUsername);


             String response="<h1>HandleRequest called with following Data<h1>";
             response+="<table>";
             response+="<tr><td>All Headers</td><td>"+rawInput.getHeaders().toString()+"</td><tr>";
             response+="<tr><td>Raw Token</td><td>"+jwtToken+"</td><tr>";
             response+="<tr><td>Token Username</td><td>"+jwtUsername+"</td><tr>";
             response+="<tr><td>httpMethod</td><td>"+httpMethod+"</td><tr>";
             response+="<tr><td>httpRequestURI</td><td>"+httpMethod+"</td><tr>";

             return OutputEvent.fromBytes(
                     response.getBytes(),           // Data
                     OutputEvent.Status.Success,    // Any numeric HTTP status code can be used here
                     "text/html"         // Content type
             );
         }
         catch (Exception e)
         {
             String errorMessage="Exception occured, check logs, "+e.getLocalizedMessage();
             LOGGER.severe  (errorMessage);
             hctx.setStatusCode(SERVER_ERROR_SC);
             return OutputEvent.fromBytes(
                     errorMessage.getBytes(),       // Data
                     OutputEvent.Status.Success,    // Any numeric HTTP status code can be used here
                     "text/plain"         // Content type
             );
         }

    }

    public  String getJWTToken (InputEvent rawInput) throws Exception
    {
        Optional<String> optionalToken=rawInput.getHeaders().get("Fn-Http-H-Authorization");
        if (!optionalToken.isPresent())
        {
            throw new Exception("No Bearer token");
        }
        return optionalToken.get().substring(TOKEN_BEARER_PREFIX.length());
    }

    /**
     * Gets hte JWTUsername from the oAuth token
     * @param rawInput : rawInput from fn
     * @return
     * @throws Exception
     */
    public  String getJWTUsername(InputEvent rawInput) throws Exception
    {

        String username="";
        String jwtToken=getJWTToken(rawInput);
        String[] splitString = jwtToken.split("\\.");
        // You can get the Encoded header using this piece of java String base64EncodedHeader = splitString[0];
        String base64EncodedBody = splitString[1];
        // You can get the Encoded Signature using String base64EncodedSignature = splitString[2];
        byte[] decodedJWT = Base64.getDecoder().decode(base64EncodedBody);
        try {
            String jsonBody = new String(decodedJWT, "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root=mapper.readTree(jsonBody);
            username=root.get("sub").asText();

        } catch (Exception e)
        {
            LOGGER.log(Level.SEVERE,"Something seriously wennt wrong {} ",e.getLocalizedMessage());
            throw e;
        }
        return username;
    }
}