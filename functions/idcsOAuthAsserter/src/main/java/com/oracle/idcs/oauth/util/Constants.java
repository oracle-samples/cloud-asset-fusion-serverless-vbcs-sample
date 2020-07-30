/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

package com.oracle.idcs.oauth.util;

public class Constants {
    // Properties for assertion
    public final static String CLIENT_ID = "CLIENT_ID";
    public final static String IDCS_URL  = "IDCS_URL";
    public final static String KEY_ID  = "KEY_ID";
    public final static String AUDIENCE  = "AUDIENCE";
    public final static String SCOPE  = "SCOPE";
    public final static String IDDOMAIN = "IDDOMAIN";

    // Certificate and keystore properties
    public final static String KEYSTORE_PATH  = "KEYSTORE_PATH";

    // Secret Vault for Certificate and keystore properties
    public final static String SECRET_KEYSTORE_ID      = "V_KEYSTORE";
    public final static String SECRET_KS_PASS_ID       = "V_KS_PASS";
    public final static String SECRET_PK_PASS_ID       = "V_PK_PASS";

    public final static String USE_CACHE_TOKEN = "USE_CACHE_TOKEN";

    public final static String PRINCIPAL  = "PRINCIPAL";

    // Assertions constants
    public final static String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    public final static String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    // Auth Provider OCI Values for local environment testing
    public final static String LOCAL_OCI_PROFILE = "DEFAULT";                // Use DEFAULT value for PROFILE if you don't have a custom profile for OCI CLI locally
    public final static String LOCAL_OCI_CONFIG_FILE_PATH = "~/.oci/config";

    public final static String CACHED_BEARER_TOKEN     = "CACHED_BEARER_TOKEN";
}
