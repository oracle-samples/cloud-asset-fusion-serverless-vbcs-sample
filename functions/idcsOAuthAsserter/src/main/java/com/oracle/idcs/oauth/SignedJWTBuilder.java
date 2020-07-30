/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

package com.oracle.idcs.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64URL;
import com.oracle.idcs.oauth.util.CertificateUtils;
import com.oracle.idcs.oauth.util.KeystoreUtil;
import net.minidev.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SignedJWTBuilder {

    // JSON Web Token Header Parameters
    private static final String ALGORITHM_HEADER = "alg";
    private static final String KEY_ID_HEADER = "kid";
    private static final String X509CERT_SHA1_HEADER = "x5t";
    private static final String X509CERT_SHA256_HEADER = "x5t#S256";

    // JSON Web Token Claims
    public static final String ISSUER_CLAIM = "iss";
    public static final String SUBJECT_CLAIM = "sub";
    public static final String AUDIENCE_CLAIM = "aud";
    public static final String EXPIRATION_TIME_CLAIM = "exp";
    public static final String NOT_BEFORE_CLAIM = "nbf";
    public static final String ISSUED_AT_CLAIM = "iat";
    public static final String JWT_ID_CLAIM = "jti";

    private JWSHeader header;
    private Payload payload;
    private JWSObject jwsObject;

    private Map<String, Object> claims;

    private static KeystoreUtil ksUtil;

    private String keyId;
    private Base64URL base64X5TUrl;
    private Base64URL base64X5T256Url;

    public SignedJWTBuilder() {
        claims = new LinkedHashMap<>();
    }

    /**
     *  Initialize keystore object if is it not initialized yet.
     *
     * @param keystorePath: Keystore file to be loaded.
     * @param keystorePassphrase
     * @param privatekeyPassphrase
     * @throws Exception
     */
    public static KeystoreUtil initKeystore(String keystorePath,
                             byte [] keystorePassphrase,
                             byte [] privatekeyPassphrase) throws Exception {
        if (ksUtil == null) {
            if (keystorePath != null) {
                ksUtil = new KeystoreUtil(keystorePath, keystorePassphrase, privatekeyPassphrase);

            }
        }
        return ksUtil;
    }

    /**
     * Add a Claim to the list to be included in the Signed JWT
     *
     * @param keyClaim
     * @param value
     * @return
     */
    public Map<String, Object> addClaim(String keyClaim, Object value) {
        claims.put(keyClaim, value);
        return claims;
    }

    /**
     * Builds the Signed JWT Assertion with the values loaded.
     *
     * Finally, the JWS is signed  using the private Key from loaded Keystore
     *
     * @return
     * @throws Exception
     */
    public String build()  throws Exception {
        buildHeader();
        buildPayload();
        jwsObject = new JWSObject(header, payload);

        if( ksUtil == null ) {
            throw new Exception("Required Keytore is not loaded.");
        }
        // Apply the RSASSASigner to the JWS object
        RSAPrivateKey privateKey = (RSAPrivateKey) ksUtil.getPrivateKey(keyId);

        jwsObject.sign(new RSASSASigner(privateKey));

        // Output to URL-safe format
        return jwsObject.serialize();
    }

    /**
     * Build the Header object to be used for the Signed JWT
     *
     * @throws Exception
     */
    private void buildHeader() throws Exception {
        try {
            generateThumbprints();
        } catch (Exception ex) {
            throw ex;
        }

        JWSHeader.Builder headerBuilder =
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(keyId)
                        .x509CertThumbprint(base64X5TUrl)
                        .x509CertSHA256Thumbprint(base64X5T256Url);

        header = headerBuilder.build();
    }


    /**
     * Build the Payload object to be used for the Signed JWT using the claims added in the list.
     *
     * @throws Exception
     */
    private void buildPayload() throws Exception {
        JSONObject claimsJson = new JSONObject(claims);
        payload = new Payload(claimsJson);
    }


    /**
     * Generate the needed Certificate Thumbprints in x509 and x509CertSHA256 Digest Algorithms to be used in the Header
     * object of Signed JWT.
     *
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    private void generateThumbprints() throws CertificateException,
                                                NoSuchAlgorithmException,
                                                InvalidKeySpecException,
                                                InvalidKeyException,
                                                SignatureException, Exception {
        X509Certificate certificate = null;

        if( ksUtil == null ) {
            throw new Exception("Required Keytore is not loaded.");
        }

        certificate = (X509Certificate) ksUtil.getCertificate(keyId);

        //For x509CertThumbprint. Needs to be in Base64URL Encoded form
        base64X5TUrl    = CertificateUtils.getBase64URLCertificateDigest(certificate, CertificateUtils.DigestAlgorithms.SHA_1);

        // For x509CertSHA256Thumbprint. Needs to be in Base64URL Encoded form
        base64X5T256Url = CertificateUtils.getBase64URLCertificateDigest(certificate, CertificateUtils.DigestAlgorithms.SHA_256);
    }

    /**
     * GETTERS AND SETTERS
     */

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
}
