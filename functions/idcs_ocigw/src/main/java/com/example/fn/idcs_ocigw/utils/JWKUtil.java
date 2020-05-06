/*
Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/
package com.example.fn.idcs_ocigw.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JWKUtil {


    private JWKUtil()
    {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieve Identity Cloud Services Signing Key in JWK (JSON Web Key) format
     * @return Identity Cloud Services signing key
     */
    public static JWKSet getJWK(ResourceServerConfig resourceServerConfig) throws Exception{
        String jwk;
        String authURL = resourceServerConfig.JWK_URL;
        Response httpResponse;

        //HEADERS
        Map<String, String> requestOptions = new HashMap<>();

        requestOptions.put("Authorization", "Bearer "  + getBearer(resourceServerConfig,
                                                            "urn:opc:idm:__myscopes__"));
        httpResponse = doHttpRequest( resourceServerConfig, authURL, "GET", null, requestOptions);
        jwk = httpResponse.getResponseBodyAsString("UTF-8");
        return JWKSet.parse(jwk);
    }

    /**
     * Gets an Access token for application using the client_credentials flow
     * @return Access token
     * @throws Exception
     */
    public static String getBearer(ResourceServerConfig resourceServerConfig, String scope) throws Exception{
        String bearer = "";
        String url = resourceServerConfig.TOKEN_URL;
        Response httpResponse;

        //HEADER
        Map<String, String> requestOptions = new HashMap<>();
        String authzHdrVal = resourceServerConfig.CLIENT_ID + ":" + resourceServerConfig.CLIENT_SECRET;
        requestOptions.put("Authorization", "Basic "  + Base64.getEncoder().encodeToString(authzHdrVal.getBytes(StandardCharsets.UTF_8)));

        //BODY
        String postBody = "grant_type=client_credentials"+"&scope=" + scope;

        //REQUEST
        httpResponse = doHttpRequest( resourceServerConfig,url, "POST", postBody, requestOptions);
        bearer = httpResponse.getResponseBodyAsString("UTF-8");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root=mapper.readTree(bearer);
        bearer=root.get("access_token").asText();


        return bearer;
    }

    public static Response doHttpRequest(ResourceServerConfig resourceServerConfig, final String urlStr,
                                         final String requestMethod, final String body,
                                         final Map<String, String> header) throws Exception {
        return doHttpRequest(urlStr, requestMethod, body, header,
                resourceServerConfig.HAS_PROXY,
                resourceServerConfig.PROXY_HOST,
                resourceServerConfig.PROXY_PORT);
    }

    public static Response doHttpRequest(final String urlStr,
                                         final String requestMethod, final String body,
                                         final Map<String, String> header,
                                         boolean useProxy, String proxyHost, int proxyPort) throws Exception {
        HttpURLConnection conn;
        try {
            URL url = new URL(urlStr);
            String completeUrl = (body != null) ? urlStr+"?"+body : urlStr;
            Logger.getLogger(JWKUtil.class.getName()).log(Level.INFO, "Request URL: "+completeUrl);
            if (useProxy) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(true);
            if (requestMethod != null) {
                conn.setRequestMethod(requestMethod);
            }
            if (header != null) {
                for (String key : header.keySet()) {
                    conn.setRequestProperty(key, header.get(key));
                    Logger.getLogger(JWKUtil.class.getName()).log(Level.INFO, "Header: "+key+": "+header.get(key));
                }
            }

            // If use POST or PUT must use this
            OutputStreamWriter wr = null;
            if (body != null
                && requestMethod != null
                && !"GET".equals(requestMethod)
                && !"DELETE".equals(requestMethod)) {
                wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(body);
                wr.flush();
            }

            conn.connect();
        } catch (Exception e) {
            Logger.getLogger(AccessTokenValidator.class.getName()).log(Level.SEVERE, "HTTP UTIL: exception :" + e.getMessage(), e);
            throw new Exception(e);
        }
        return new Response(conn);

    }
}
