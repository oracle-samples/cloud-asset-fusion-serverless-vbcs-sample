/*
Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/
package com.example.fn.idcs_ocigw.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Validates access tokens sent to sales insight
 */
public class AccessTokenValidator {

    private static boolean isSafe = false;
    private static final ConfigurableJWTProcessor JWT_PROCESSOR = new DefaultJWTProcessor();
    private  static final Logger LOGGER = Logger.getLogger("IDCS_GTW_LOGGER");

    public void init(ResourceServerConfig rsc) {
        if (!AccessTokenValidator.isSafe) {
            try {
                JWKSet jwk = JWKUtil.getJWK(rsc);
                JWKSource keySource = new ImmutableJWKSet(jwk);
                JWSKeySelector keySelector = new JWSVerificationKeySelector(JWSAlgorithm.RS256, keySource);
                JWT_PROCESSOR.setJWSKeySelector(keySelector);
                AccessTokenValidator.isSafe = true;
                LOGGER.info("Signing Key from IDCS successfully loaded!");
            } catch (Exception ex) {
                LOGGER.severe("Error loading Signing Key from IDCS: " + ex);
                AccessTokenValidator.isSafe = false;
            }
        }
    }

    //checks if the token is valid
    public JWTClaimsSet validate(ResourceServerConfig rsc, String accessToken) {
        if (AccessTokenValidator.isSafe) {

            try {
                SecurityContext ctx = null;
                JWTClaimsSet claimsSet = JWT_PROCESSOR.process(accessToken, ctx);

                //VALIDATE AUDIENCE
                if (claimsSet.getAudience().indexOf(rsc.SCOPE_ID) >= 0) {


                    //CORRECT AUDIENCE
                    LOGGER.fine("Valid Audience found");

                    return claimsSet;
                } else {
                    String message = "Incorrect audience, got " + claimsSet.getAudience() + " instead of expected " + rsc.SCOPE_ID;
                    LOGGER.severe(message);
                    throw new InvalidTokenException(message);
                }
            } catch (JOSEException ex) {

                LOGGER.severe("Invalid Token Exception" + ex.getMessage());
                throw new InvalidTokenException(ex.getMessage());
            } catch (BadJOSEException ex) {
                LOGGER.severe("Bad Token Exception " + ex.getMessage());
                throw new InvalidTokenException(ex.getMessage());
                //BadJWEException, BadJWSException, BadJWTException
                //Bad JSON Web Encryption (JWE) exception. Used to indicate a JWE-protected object that couldn't be successfully decrypted or its integrity has been compromised.
                //Bad JSON Web Signature (JWS) exception. Used to indicate an invalid signature or hash-based message authentication code (HMAC).
                //Bad JSON Web Token (JWT) exception.
            } catch (ParseException ex) {
                LOGGER.severe(ex.getLocalizedMessage());
                throw new InvalidTokenException(ex.getMessage());
            }
        } else {
            LOGGER.severe("Resource Server application is not able to validate tokens");
            throw new InvalidTokenException("Resource Server application is not able to validate tokens");
        }
    }
}
