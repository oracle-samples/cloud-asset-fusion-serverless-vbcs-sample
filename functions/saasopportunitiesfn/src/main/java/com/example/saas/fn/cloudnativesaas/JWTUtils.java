/*
Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/
package com.example.saas.fn.cloudnativesaas;
import com.example.saas.fn.cloudnativesaas.exceptions.NoBearerTokenException;
import com.fnproject.fn.api.InputEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.example.saas.fn.cloudnativesaas.exceptions.ObjectMapperException;

/**
 * Simple Helper class for getting JWT token and subsequently username from the said token
 */
public class JWTUtils {
    private static final String TOKEN_BEARER_PREFIX="Bearer ";
    private static final Logger LOGGER = Logger.getLogger("CLOUDNATIVESAAS");

    private JWTUtils()
    {
        throw new IllegalStateException("JWTUtil is a utility class");
    }


    public static String getJWTToken ( InputEvent rawInput) throws NoBearerTokenException
    {
        Optional<String> optionalToken=rawInput.getHeaders().get("Fn-Http-H-Authorization");
        if (!optionalToken.isPresent())
        {
            LOGGER.info("No bearer token found in get JWT method");
            throw new NoBearerTokenException("No Bearer token");
        }
        String jwtToken=optionalToken.get().substring(TOKEN_BEARER_PREFIX.length());
        return jwtToken.trim();
    }

    public static String getJWTUsername(InputEvent rawInput) throws ObjectMapperException
    {
        try {
           String username="";
            String jwtToken=getJWTToken(rawInput);
            String[] splitString = jwtToken.split("\\.");
            // String base64EncodedHeader = splitString[0];   // Not used but left for reference
            String base64EncodedBody = splitString[1];
            // String base64EncodedSignature = splitString[2]; // Not used but left for reference
            byte[] decodedJWT = Base64.getDecoder().decode(base64EncodedBody);

                String jsonBody = new String(decodedJWT, "utf-8");
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root=mapper.readTree(jsonBody);
                username=root.get("sub").asText();
                LOGGER.log(Level.INFO,"Username extracted = {} ",username);
            return username;
        } catch (Exception e)
        {
            throw new ObjectMapperException(e.getMessage());
        }

    }

}
