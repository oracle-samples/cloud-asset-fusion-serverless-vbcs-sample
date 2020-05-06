/*
Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/
package com.example.fn.idcs_ocigw.utils;

import com.fnproject.fn.api.RuntimeContext;

import java.util.logging.Logger;

/**
 * It contains the resource server configuration and constants
 * Like a properties file, but simpler
 */
public class ResourceServerConfig {



    public  final String CLIENT_ID;
    public  final String CLIENT_SECRET;
    public  final String IDCS_URL ;
    public  final String SCOPE_ID;

    //INFORMATION ABOUT IDENTITY CLOUD SERVICES
    public  final String JWK_URL;
    public  final String TOKEN_URL;
    public final String KMS_ENDPOINT;
    public final String KMS_IDCS_SECRET_KEY;
    //PROXY
    public  final boolean HAS_PROXY ;
    public  final String PROXY_HOST;
    public  final int PROXY_PORT;
    public  final String DEBUG_LEVEL;
    private static final String NOT_SET_DEFAULT="NOTSET";


    /**
     * Gets defaults out of Oracle Functions Configuration
     */
    private  static final Logger LOGGER = Logger.getLogger("IDCS_GTW_LOGGER");

    public ResourceServerConfig(RuntimeContext ctx)   {
        // Get config variables from Functions Configuration
        HAS_PROXY = Boolean.parseBoolean(ctx.getConfigurationByKey("idcs_proxy").orElse("false"));
        PROXY_HOST = ctx.getConfigurationByKey("idcs_proxy_host").orElse("");
        PROXY_PORT = Integer.parseInt(ctx.getConfigurationByKey("idcs_proxy_port").orElse("80"));

        IDCS_URL = ctx.getConfigurationByKey("idcs_app_url").orElse(NOT_SET_DEFAULT);
        SCOPE_ID = ctx.getConfigurationByKey("idcs_app_scopeid").orElse(NOT_SET_DEFAULT);
        CLIENT_ID = ctx.getConfigurationByKey("idcs_app_clientid").orElse(NOT_SET_DEFAULT);

        DEBUG_LEVEL = ctx.getConfigurationByKey("debug_level").orElse("INFO");
        JWK_URL = IDCS_URL+"/admin/v1/SigningCert/jwk";
        TOKEN_URL=IDCS_URL+"/oauth2/v1/token";

        // KMS Key for IDCS Client Secret
        KMS_ENDPOINT = ctx.getConfigurationByKey("kms_endpoint").orElse(NOT_SET_DEFAULT);
        KMS_IDCS_SECRET_KEY= ctx.getConfigurationByKey("kms_idcs_secret_key").orElse(NOT_SET_DEFAULT);

        String decodedClientSecret="";

        // Decode the client Secret using KMS
        decodedClientSecret=DecryptKMS.decodeKMSString(KMS_ENDPOINT,KMS_IDCS_SECRET_KEY,ctx.getConfigurationByKey("idcs_app_secret").orElse(NOT_SET_DEFAULT));
        decodedClientSecret=decodedClientSecret.trim();

        CLIENT_SECRET = decodedClientSecret;

        LOGGER.info("IDCS Configuration Data read : IDCS_URL=[" + IDCS_URL + "] SCOPE_AUD=[" + SCOPE_ID +"] CLIENT_ID=["+CLIENT_ID+"], DEBUG_LEVEL=["+DEBUG_LEVEL+"]");
    }
}
