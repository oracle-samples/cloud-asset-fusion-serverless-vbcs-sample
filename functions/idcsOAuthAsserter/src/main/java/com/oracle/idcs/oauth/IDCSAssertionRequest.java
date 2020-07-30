/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

package com.oracle.idcs.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oracle.idcs.oauth.util.Constants.*;

public class IDCSAssertionRequest {

    private final Logger logger = Logger.getLogger(IDCSAssertionRequest.class.getName());

    // Constants
    private final String TOKEN_URL         = "/oauth2/v1/token";
    private final long DEFAULT_EXPIRY_TIME = (60 * 60 * 1000);

    private String idcsURL;
    private String identityDomain;
    private String principal;
    private String clientID;
    private String scope;
    private String[] audienceList;

    private String keyID;
    private String keystorePath;

    // Data that should come from Secret Vault
    private byte [] keystorePassphrase;
    private byte [] privatekeyPassphrase;

    private SignedJWTBuilder signedJWTBuilderClient;
    private SignedJWTBuilder signedJWTBuilderUser;

    public IDCSAssertionRequest(Map<String, String> props, String[] audienceList) {
        this.idcsURL        = props.get(IDCS_URL);
        this.identityDomain = props.get(IDDOMAIN);
        this.principal      = props.get(PRINCIPAL);
        this.scope          = props.get(SCOPE);
        this.audienceList   = audienceList;
        this.clientID       = props.get(CLIENT_ID);

        this.keyID          = props.get(KEY_ID);
        this.keystorePath   = props.get(KEYSTORE_PATH);
    }

    /**
     * Gets an Access token for application using the jwt_assertion flow
     * @return Access token
     * @throws Exception
     */
    public String getAccessToken() throws Exception {
        if(signedJWTBuilderClient == null || signedJWTBuilderUser == null) {
            buildSignetJWTs();
        }

        HttpClient client = HttpClients.custom().build();
        RequestBuilder builder = RequestBuilder.post().setUri( idcsURL + TOKEN_URL ).
                setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8").
                setHeader("X-USER-IDENTITY-DOMAIN-NAME", identityDomain);
        builder.addParameter("grant_type", GRANT_TYPE)
                .addParameter("assertion", signedJWTBuilderUser.build())
                .addParameter("client_assertion_type", CLIENT_ASSERTION_TYPE)
                .addParameter("client_assertion", signedJWTBuilderClient.build())
                .addParameter("client_id", clientID)
                .addParameter("scope", scope);
        HttpUriRequest request = builder.build();

        logger.log(Level.INFO, "IDCS Assertion Request: " + request.getRequestLine());

        HttpResponse response = client.execute(request);
        String responseJson = EntityUtils.toString(response.getEntity());

        logger.log(Level.FINEST,"ResponseJson: " + responseJson);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(responseJson);
        if( !json.has("access_token") ) {
            throw  new Exception("Error retrieving the token: --"+ responseJson +"--");
        }
        String bearer = json.get("access_token").asText();
        return bearer;
    }


    /**
     * Build both, User and Client Signed JWT Assertions. As it is required to have the keystore prepared to build JWT
     * Assertions, this method initialize the keystore to be used for both.
     *
     * @throws Exception
     */
    private void buildSignetJWTs() throws Exception {
        try {
            SignedJWTBuilder.initKeystore(keystorePath, keystorePassphrase, privatekeyPassphrase);
            signedJWTBuilderClient = getSignedJWTBuilder(clientID);
            signedJWTBuilderUser = getSignedJWTBuilder(principal);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * Build a Signed JWT Assertion Builder object using the specified subject. This builder is prepared to retrieve the
     * JWT Assertion in string form by calling its build() method.
     *
     * @param subject: Subject to be used in the Signet JWT Assertion.
     * @return
     * @throws Exception
     */
    private SignedJWTBuilder getSignedJWTBuilder(String subject) throws Exception {

        if(keystorePassphrase == null) {
            throw new Exception("Keystore Passphrase is missing");
        }

        if(privatekeyPassphrase == null) {
            throw new Exception("Private Key Passphrase is missing");
        }

        Date now = new Date();
        Date expirationTime = new Date((DEFAULT_EXPIRY_TIME) + now.getTime());

        SignedJWTBuilder signedJWTBuilder = new SignedJWTBuilder();
        signedJWTBuilder.setKeyId(keyID);
        signedJWTBuilder.addClaim(SignedJWTBuilder.ISSUER_CLAIM, clientID);
        signedJWTBuilder.addClaim(SignedJWTBuilder.SUBJECT_CLAIM, subject);
        signedJWTBuilder.addClaim(SignedJWTBuilder.AUDIENCE_CLAIM, audienceList);
        signedJWTBuilder.addClaim(SignedJWTBuilder.EXPIRATION_TIME_CLAIM, Long.valueOf((expirationTime).getTime() / 1000L));
        signedJWTBuilder.addClaim(SignedJWTBuilder.NOT_BEFORE_CLAIM, Long.valueOf((now).getTime() / 1000L));
        signedJWTBuilder.addClaim(SignedJWTBuilder.ISSUED_AT_CLAIM, Long.valueOf((now).getTime() / 1000L));
        signedJWTBuilder.addClaim(SignedJWTBuilder.JWT_ID_CLAIM, UUID.randomUUID().toString());
        return signedJWTBuilder;
    }


    /**
     * GETTERS AND SETTERS
     */

    public void setPrivatekeyPassphrase(byte[] privatekeyPassphrase) {
        this.privatekeyPassphrase = privatekeyPassphrase;
    }

    public void setKeystorePassphrase(byte[] keystorePassphrase) {
        this.keystorePassphrase = keystorePassphrase;
    }


}

