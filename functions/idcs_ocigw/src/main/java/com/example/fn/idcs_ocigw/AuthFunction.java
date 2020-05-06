/*
Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/
package com.example.fn.idcs_ocigw;


import com.example.fn.idcs_ocigw.utils.AccessTokenValidator;
import com.example.fn.idcs_ocigw.utils.InvalidTokenException;
import com.example.fn.idcs_ocigw.utils.ResourceServerConfig;
import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import com.nimbusds.jwt.JWTClaimsSet;

import java.io.IOException;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AuthFunction {

    private  static final Logger LOGGER = Logger.getLogger("IDCS_GTW_LOGGER");
    private static final DateTimeFormatter ISO8601 = DateTimeFormatter.ISO_DATE_TIME;
    private static final String TOKEN_BEARER_PREFIX = "Bearer ";
    private static ResourceServerConfig rsc;

    /**
     * @param ctx : Runtime context passed in by Fn, used to set default parameters
     */
    @FnConfiguration
    public void config(RuntimeContext ctx) throws IOException {
        rsc=new ResourceServerConfig(ctx);
        LOGGER.setLevel(Level.parse(rsc.DEBUG_LEVEL));

    }


    public static class Input {
        public String type;
        public String token;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class Result {
        // required
        private boolean active = false;
        private String principal;
        private String[] scope;
        private String expiresAt;

        // optional
        private String wwwAuthenticate;

        // optional
        private String clientId;

        // optional context
        private Map<String, Object> context;

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getPrincipal() {
            return principal;
        }

        public void setPrincipal(String principal) {
            this.principal = principal;
        }

        public String[] getScope() {
            return scope;
        }

        public void setScope(String[] scope) {
            this.scope = scope;
        }

        public String getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(String expiresAt) {
            this.expiresAt = expiresAt;
        }

        public String getWwwAuthenticate() {
            return wwwAuthenticate;
        }

        public void setWwwAuthenticate(String wwwAuthenticate) {
            this.wwwAuthenticate = wwwAuthenticate;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }
    }



    /**
     *  Main function called to authenticate user
     * @param input
     * @return
     */
    public Result handleRequest(Input input) {

        LOGGER.info("Authentication Request requested");

        if (input==null)
        {
            throw new IllegalArgumentException ("Input to handleReqest null - should never be if we're in FN");
        }

        Result result = new Result();

        if (input.token == null || !input.token.startsWith(TOKEN_BEARER_PREFIX)) {
            result.active = false;
            result.wwwAuthenticate = "Bearer error=\"missing_token\"";
            return result;
        }

        // remove "Bearer " prefix in the token string before processing
        String token = input.token.substring(TOKEN_BEARER_PREFIX.length());

        AccessTokenValidator accessTokenValidator = new AccessTokenValidator();
        accessTokenValidator.init(rsc);

        try {
            JWTClaimsSet claimsSet = accessTokenValidator.validate(rsc,token);

            // Now that we can trust the contents of the JWT we can build the APIGW auth result
            result.active = true;

            result.principal = claimsSet.getSubject();
            result.scope = claimsSet.getStringClaim("scope").split(" ");
            result.expiresAt = ISO8601.format(claimsSet.getExpirationTime().toInstant().atOffset(ZoneOffset.UTC));

            Map<String, Object> context = new HashMap<>();
            context.put("tenant", claimsSet.getStringClaim("tenant"));
            result.context = context;

        } catch (InvalidTokenException e) {

            LOGGER.info("Invalid Token Exception "+e.getMessage());
            result.active = false;
            result.wwwAuthenticate = "Bearer error=\"invalid_token\", error_description=\"" + e.getMessage() + "\"";
        } catch (ParseException ex) {

            LOGGER.info("Parse Exception "+ex.getMessage());
            result.active = false;
            result.wwwAuthenticate = "Bearer error=\"invalid_token_claim\", error_description=\"" + ex.getMessage() + "\"";
        }

        return result;
    }

}