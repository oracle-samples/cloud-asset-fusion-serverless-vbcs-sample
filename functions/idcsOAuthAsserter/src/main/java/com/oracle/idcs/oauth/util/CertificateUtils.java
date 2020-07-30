/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

package com.oracle.idcs.oauth.util;

import com.nimbusds.jose.util.Base64URL;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class CertificateUtils {

    public enum DigestAlgorithms {
        SHA_1("SHA-1"),
        SHA_256("SHA-256");

        private String name;

        private DigestAlgorithms(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return this.name();
        }
    }

    /**
     *
     * Generates as String representation of certificate thumprint using the digest algorithm method specified.
     * If doBase64URLEncode, the string is Base64 URL encoded
     *
     * @param certificate
     * @param algorithm
     * @param doBase64URLEncode
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    public static String getCertificateDigest(X509Certificate certificate,
                                              DigestAlgorithms algorithm,
                                              boolean doBase64URLEncode)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        if(doBase64URLEncode) {
            return getBase64URLCertificateDigest(certificate,algorithm).toString();
        }
        return new String(getCertDigest(certificate , algorithm));
    }

    /**
     *
     * Generates Base64 URL encoded object of certificate thumprint using the digest algorithm method specified
     *
     * @param certificate
     * @param algorithm
     * @return
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    public static Base64URL getBase64URLCertificateDigest(X509Certificate certificate,
                                              DigestAlgorithms algorithm)
            throws NoSuchAlgorithmException, CertificateEncodingException {

        return Base64URL.encode( getCertDigest(certificate, algorithm) );
    }

    /**
     * Get byte array form of certificate thumprint using the digest algorithm method specified
     *
     * @param certificate
     * @param algorithm
     * @return
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    private static byte[] getCertDigest(X509Certificate certificate,
                                        DigestAlgorithms algorithm)
                                        throws NoSuchAlgorithmException, CertificateEncodingException {
        byte[] certEncoded = certificate.getEncoded();
        return MessageDigest.getInstance(algorithm.getName()).digest(certEncoded);
    }
}
