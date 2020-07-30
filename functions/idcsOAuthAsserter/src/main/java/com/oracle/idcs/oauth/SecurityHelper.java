/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

package com.oracle.idcs.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import com.oracle.bmc.secrets.responses.GetSecretBundleResponse;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oracle.idcs.oauth.util.Constants.*;

public class SecurityHelper {

    public static final String FN_PREFIX = "Fn-Http-H-";

    private final Logger logger = Logger.getLogger(SecurityHelper.class.getName());

    private final String TOKEN_BEARER_PREFIX = "Bearer ";
    private final String API_GATEWAY_CUSTOM_HEADER = "Authorization";

    private Map<String,String> securityProps;

    private String username;

    private boolean useCacheToken = false;

    private static SecretsClient secretsClient;                              // For Vault usage

    private String localOciConfigFilePath = LOCAL_OCI_CONFIG_FILE_PATH;
    private String localOciProfile = LOCAL_OCI_PROFILE;
    private Region ociRegion = Region.US_PHOENIX_1;                         // Default OCI Region Client Secrets, default PHOENIX


    // Default Keystore Path
    private final String CERTS_DIR     = "/tmp/keystore/";
    private final String KEYSTORE_NAME = "secret-keystore.p12";

    private final String [] mandatoryProps = new String[]{IDCS_URL, CLIENT_ID, KEY_ID, SCOPE, AUDIENCE, IDDOMAIN,
            SECRET_KEYSTORE_ID, SECRET_KS_PASS_ID, SECRET_PK_PASS_ID};

    /**
     * Constructor SecurityHelper
     *
     * @param securityProps: Map with needed values to perform assertion and retrieve  Secrets ocid values.
     */
    public SecurityHelper(Map<String,String> securityProps) {
        this.securityProps = securityProps;
        if(!securityProps.containsKey(KEYSTORE_PATH)) {
            securityProps.put(KEYSTORE_PATH, CERTS_DIR + KEYSTORE_NAME);
        }
        useCacheToken = Boolean.parseBoolean(this.securityProps.get(USE_CACHE_TOKEN));
    }

    /**
     * Constructor SecurityHelper
     *
     * @param ctx: FN RuntimeContext to retrieve needed values to perform assertion and  Secrets ocid values.
     */
    public SecurityHelper(RuntimeContext ctx) {
        initSecurityProps(ctx);
        useCacheToken = Boolean.parseBoolean(this.securityProps.get(USE_CACHE_TOKEN));

        logger.log(Level.FINEST, "Security Props: ");
        securityProps.forEach( (k,v)-> logger.log(Level.FINEST, "KEY: [" + k + "] -- VALUE: [" + v + "]") );
    }

    /**
     * Retrieve from FN RuntimeContext the values needed from assertion
     *
     * @param ctx
     */
    private void initSecurityProps(RuntimeContext ctx) {
        securityProps = new HashMap<String, String>();

        // Properties for assertion
        securityProps.put(CLIENT_ID, ctx.getConfigurationByKey(CLIENT_ID).orElse("") );
        securityProps.put(IDCS_URL, ctx.getConfigurationByKey(IDCS_URL).orElse("") );
        securityProps.put(KEY_ID, ctx.getConfigurationByKey(KEY_ID).orElse("") );
        securityProps.put(SCOPE, ctx.getConfigurationByKey(SCOPE).orElse("") );
        securityProps.put(AUDIENCE, ctx.getConfigurationByKey(AUDIENCE).orElse(",") );
        securityProps.put(IDDOMAIN, ctx.getConfigurationByKey(IDDOMAIN).orElse("") );

        // Using Secrets Vault Service IDs
        securityProps.put(SECRET_KEYSTORE_ID      , ctx.getConfigurationByKey(SECRET_KEYSTORE_ID).orElse(""));
        securityProps.put(SECRET_KS_PASS_ID       , ctx.getConfigurationByKey(SECRET_KS_PASS_ID).orElse(""));
        securityProps.put(SECRET_PK_PASS_ID       , ctx.getConfigurationByKey(SECRET_PK_PASS_ID).orElse(""));

        securityProps.put(USE_CACHE_TOKEN, ctx.getConfigurationByKey(USE_CACHE_TOKEN).orElse("true") );

        /// This is specific from function, so it can be as constant or from FN Config
        securityProps.put(KEYSTORE_PATH, CERTS_DIR + KEYSTORE_NAME);
    }

    /**
     * Validate mandatory properties for IDCS Assertion.
     * Mandatory props:
     *       IDCS_URL, CLIENT_ID, KEY_ID, SCOPE, AUDIENCE, IDDOMAIN,
     *       SECRET_KEYSTORE_ID, SECRET_KS_PASS_ID, SECRET_PK_PASS_ID
     *
     * @throws Exception if one of the mandatory props is missing.
     */
    private void validateMandatoryProps() throws Exception {
        logger.log(Level.INFO, "Validating mandatory properties for IDCS Assertion.");
        String [] mandatoryProps = new String[]{IDCS_URL, CLIENT_ID, KEY_ID, SCOPE, AUDIENCE, IDDOMAIN,
                                                SECRET_KEYSTORE_ID, SECRET_KS_PASS_ID, SECRET_PK_PASS_ID};
        int missingProps = 0;
        for(String prop : mandatoryProps) {
            String v = securityProps.get(prop);
            if(v != null && !v.isEmpty())  {
                continue;
            }
            missingProps ++;
            logger.log(Level.SEVERE, "Mandatory Property missing: " + prop);
        }
        if(missingProps > 0) {
            throw new Exception("There are missing mandatory Properties for IDCS Assertion. Check logs for details.");
        }
    }

    /**
     *
     * @param keystorePath
     * @return
     */
    public SecurityHelper setKeystorePath(String keystorePath) {
        securityProps.put(KEYSTORE_PATH, keystorePath);
        return this;
    }

    /**
     *
     * @param input
     * @return
     * @throws Exception
     */
    public SecurityHelper extractSubFromJwtTokenHeader(InputEvent input)  throws Exception {
        // All headers coming from API Gateway authorizer with functions are prefixed with Fn-Http-H
        Headers headers = input.getHeaders();
        //headers.asMap().forEach( (k,v)-> logger.log(Level.FINEST, "KEY: " + k + " -- VALUE" + v) );

        // ExtractBearerToken
        Optional<String> optionalToken = getHeader(headers, FN_PREFIX + API_GATEWAY_CUSTOM_HEADER);
        if (!optionalToken.isPresent()) {
            // Ensure Bearer token always be in the request
            throw new Exception("No Authentication Bearer token found");
        }

        // Bearer token should start with "Bearer " Prefix
        String jwtToken = optionalToken.get();
        return extractSubFromJwtToken(jwtToken);
    }

    /**
     *
     * @param jwtToken
     * @return
     * @throws Exception
     */
    public SecurityHelper extractSubFromJwtToken(String jwtToken)  throws Exception {
        if (jwtToken != null && !jwtToken.startsWith(TOKEN_BEARER_PREFIX)) {
            throw new Exception("Authentication Bearer token is not valid.");
        }
        jwtToken = jwtToken.substring(TOKEN_BEARER_PREFIX.length());

        // extractSubject
        String fieldSub = "sub";
        username = getBearerTokenFields(jwtToken, fieldSub).get(fieldSub);
        logger.log(Level.INFO, "Username = " + username);
        return this;
    }

    /**
     * This method obtains the Access Token to invoke FA using  securityProps values and Secrets in OCI.
     * The access token is retrieved using IDCS Assertion.
     *
     * @return
     * @throws Exception
     */
    public String getAssertedAccessToken() throws Exception {
        validateMandatoryProps();
        String principal = username;

        // Search for cached tokens
        // NOTE: The objective of this Bearer Token cache approach is to save execution time while invoke FA. If an token
        //       product of the assertion is still valid, it could be of worth to still using it in future invocations.
        //       This caching approach is limited as backend function could hit any container that runs the same code
        //       and not all the VMs can have propagated the 'CACHED_BEARER_TOKEN + "_" + username' property in the JVM
        //       to be used. In such case, if the request hits a container without he token the assertion will be performed
        //       as usual. We need to found a better cache approach able to propagate the valid token across all the backed
        //       containers that.
        logger.log(Level.INFO, "useCacheToken: " + useCacheToken);
        if (useCacheToken) {
            String cachedToken = System.getProperty(CACHED_BEARER_TOKEN + "_" + username);
            if (cachedToken != null) {
                // Validate cached token
                try {
                    logger.log(Level.INFO, "Found cached Access Token");
                    if (validateCachedToken(cachedToken)) {
                        logger.log(Level.INFO, "Valid cached Access Token. Using it to invoke FA");
                        return cachedToken;
                    }
                    logger.log(Level.INFO, "Cached token is not valid anymore. Generate a new one with assertion.");
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error trying to validate cached token [" + ex.getMessage() + "].", ex);
                    logger.log(Level.INFO, "Unable to validate cached token. Generate a new one with assertion.");
                }
            }
        }

        // Ensure Secrets client is initialized.
        initializeSecretsClient();

        // Preparing properties for assertion
        Map<String, String> asserterProps = new HashMap<>();

        // Specific IDCS Properties
        asserterProps.put(IDCS_URL, securityProps.get(IDCS_URL));
        asserterProps.put(CLIENT_ID, securityProps.get(CLIENT_ID));
        asserterProps.put(KEY_ID, securityProps.get(KEY_ID));
        asserterProps.put(SCOPE, securityProps.get(SCOPE));
        asserterProps.put(PRINCIPAL, principal);                               // This principal comes from PRINCIPAL_SOURCE, default BEARER
        asserterProps.put(IDDOMAIN, securityProps.get(IDDOMAIN));              // From function config.

        // Keytore data from Secrets
        String ksPath = securityProps.get(KEYSTORE_PATH);
        if (!new File(ksPath).exists()) {
            if (securityProps.containsKey(SECRET_KEYSTORE_ID)) {                       // If V_KEYSTORE prop, retrieve keystore from Vault
                logger.log(Level.INFO, "Retrieve keystore from Vault... ");
                writeSecretFile(securityProps.get(SECRET_KEYSTORE_ID), ksPath);
            }
        }
        asserterProps.put(KEYSTORE_PATH, ksPath);

        // Create Asserter object
        logger.log(Level.INFO, "Create Asserter Generator Object");
        String [] audienceList = this.securityProps.get(AUDIENCE).split(",");
        IDCSAssertionRequest asserter = new IDCSAssertionRequest(asserterProps , audienceList);
        asserter.setKeystorePassphrase(   getSecretValue(securityProps.get(SECRET_KS_PASS_ID)   ));    // Always from Vault
        asserter.setPrivatekeyPassphrase( getSecretValue(securityProps.get(SECRET_PK_PASS_ID) ));      // Always from Vault

        String bearedAccessToken = asserter.getAccessToken();
        logger.log(Level.FINEST,"bearedAccessToken from Assertion: " + bearedAccessToken);
        if (useCacheToken) {
            logger.log(Level.INFO,"Store token in cache");
            System.setProperty(CACHED_BEARER_TOKEN + "_" + username, bearedAccessToken);     // Caching token in System Properties
        }
        return bearedAccessToken;
    }


    /**
     *
     * Validate the passed cached token and verify if it is still valid to use to invoke FA.
     *
     * @param cachedToken: Cached token retrieved from JVM System properties
     * @return true if the token is good to use. false otherwise.
     * @throws Exception
     */
    private boolean validateCachedToken(String cachedToken) throws Exception {
        Map<String,String> tvs = getBearerTokenFields(cachedToken, "sub", "exp");

        // Validate if the current user to be planned for new assertion match with the cached token.
        // If not, generate a new assertion.
        String cachedUsername = tvs.get("sub");
        logger.log(Level.FINEST, "Cached Token Username ["+ cachedUsername +"]");
        logger.log(Level.FINEST, "Incoming Token Username ["+ username +"]");
        if ( !username.equalsIgnoreCase( tvs.get("sub") )) {
            logger.log(Level.INFO, "Cached token subject ["+ cachedUsername +"] does not match with incoming token subject ["+ username +"]");
            return false;
        }

        // Validate if the token is still valid.
        String cachedExp = tvs.get("exp");
        Long expvalue = 0L;
        try {
            expvalue = Long.valueOf(cachedExp) * 1000;
        }catch (Exception ex) {
            logger.log(Level.SEVERE, "An error ocurred trying to validate cached token expiration time ["+ ex.getMessage() +"]");
            throw ex;
        }
        Long currentTime = System.currentTimeMillis();
        logger.log(Level.FINEST, "Cached Token Expiry time ["+ expvalue +"]");
        logger.log(Level.FINEST, "Current time ["+ currentTime +"]");
        if ( currentTime > expvalue) {
            logger.log(Level.INFO, "Cached token is not valid anymore. Expiry time ["+ expvalue +"]. Current Time ["+ currentTime +"]");
            return false;
        }

        return true;
    }

    /**
     * Initialize secretsClient to be used to retrieve Secrets values from OCI Vault instance.
     *
     * @throws Exception
     */
    private void initializeSecretsClient() throws Exception {
        if (secretsClient == null) {
            // This env variable exists in FN Runtime
            String version = System.getenv("OCI_RESOURCE_PRINCIPAL_VERSION");
            logger.log(Level.FINEST, "Version: [" + version + "]");

            // OCI/BMC AUTH PROVIDER from OCI SDK:
            // https://github.com/oracle/oci-java-sdk
            // https://docs.cloud.oracle.com/en-us/iaas/tools/java/1.15.2/com/oracle/bmc/auth/BasicAuthenticationDetailsProvider.html
            BasicAuthenticationDetailsProvider provider = null;
            if (version != null) {
                //   If version retrieved from OCI_RESOURCE_PRINCIPAL_VERSION env exists means this code is running on
                //   OCI cloud context.
                provider = ResourcePrincipalAuthenticationDetailsProvider.builder().build();
            } else {
                //   NO OCI_RESOURCE_PRINCIPAL_VERSION env found, so it means this code is running on
                //   Local environment. Using ~/.oci/config locally.
                try {
                    provider = new ConfigFileAuthenticationDetailsProvider(localOciConfigFilePath, localOciProfile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            logger.log(Level.INFO, "OCI Provider: [" + provider + "]");
            if (provider == null) {
                throw new Exception("BasicAuthenticationDetailsProvider is null!!");
            }

            // Initialize SecretsClient for our tenancy region
            secretsClient = new SecretsClient(provider);
            secretsClient.setRegion(ociRegion);
        }
    }

    /**
     * Creates a file in the specified location using the byte array retrieved from the Secret with ocid "secretOcid".
     *
     * @param secretOcid
     * @param filepath
     * @throws IOException
     */
    private void writeSecretFile(String secretOcid, String filepath) throws IOException {
        byte[] secretValueDecoded = getSecretValue(secretOcid);
        try {
            File secretFile = new File(filepath);
            secretFile.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(secretFile, secretValueDecoded);
            logger.log(Level.INFO,"Stored Secret file: " + secretFile.getAbsolutePath());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve secret value string from ocid provided
     *
     * @param secretOcid
     * @return Byte array content of the stored file.
     * @throws IOException
     */
    private byte[] getSecretValue(String secretOcid) throws IOException {
        logger.log(Level.INFO, "Get Secret Value: "+ secretOcid);

        // Create get secret bundle request
        //    https://github.com/oracle/oci-java-sdk/blob/master/bmc-secrets/src/main/java/com/oracle/bmc/secrets/requests/GetSecretBundleRequest.java
        GetSecretBundleRequest getSecretBundleRequest = GetSecretBundleRequest
                .builder()
                .secretId(secretOcid)
                .stage(GetSecretBundleRequest.Stage.Current)    // The rotation state of the secret version
                .build();

        // Get Bundle Secret response
        //   https://github.com/oracle/oci-java-sdk/blob/master/bmc-secrets/src/main/java/com/oracle/bmc/secrets/responses/GetSecretBundleResponse.java
        GetSecretBundleResponse getSecretBundleResponse = secretsClient.getSecretBundle(getSecretBundleRequest);

        // Get the bundle response content
        //   https://github.com/oracle/oci-java-sdk/blob/master/bmc-secrets/src/main/java/com/oracle/bmc/secrets/model/Base64SecretBundleContentDetails.java
        Base64SecretBundleContentDetails base64SecretBundleContentDetails =
                (Base64SecretBundleContentDetails) getSecretBundleResponse.
                        getSecretBundle().getSecretBundleContent();


        // Decode the BASE64 encoded secret
        byte[] secretValueDecoded = org.apache.commons.codec.binary.Base64.decodeBase64(base64SecretBundleContentDetails.getContent());
        return secretValueDecoded;
    }

    /**
     * Helper function to get Claim value from Bearer Token
     *
     * @param token
     * @param fields
     * @return
     */
    private Map<String,String> getBearerTokenFields(String token, String ... fields)  throws Exception {
        String[] split_string = token.split("\\.");
        String base64EncodedBody = split_string[1];
        byte[] decodedJWT = Base64.getDecoder().decode(base64EncodedBody);
        try {
            String JSONbody = new String(decodedJWT, "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(JSONbody);

            Map<String,String> values = new HashMap<>();
            for(String field : fields ) {
                values.put(field, root.get(field).asText());
            }
            return values;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception (e.getMessage());
        }
    }

    /**
     * Get header from request to Function
     *
     * @param name
     * @return
     */
    private Optional<String> getHeader(Headers headers, String name) {
        Optional<String> optionalToken = headers.get(name);
        return optionalToken;
    }

    /**
     * GETTERS AND SETTERS
     */

    public String getUsername() {
        return username;
    }

    public String getLocalOciConfigFilePath() {
        return localOciConfigFilePath;
    }

    public SecurityHelper setLocalOciConfigFilePath(String localOciConfigFilePath) {
        this.localOciConfigFilePath = localOciConfigFilePath;
        return this;
    }

    public String getLocalOciProfile() {
        return localOciProfile;
    }

    public SecurityHelper setLocalOciProfile(String localOciProfile) {
        this.localOciProfile = localOciProfile;
        return this;
    }

    public Region getOciRegion() {
        return ociRegion;
    }

    public SecurityHelper setOciRegion(Region ociRegion) {
        this.ociRegion = ociRegion;
        return this;
    }
}
