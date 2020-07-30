/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

package com.oracle.idcs.oauth.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeystoreUtil {

    private Logger logger = Logger.getLogger(CertificateUtils.class.getName());

    private KeyStore fnKeystore;
    private String keystorePath;
    private byte [] keystorePassphrase;
    private byte [] privatekeyPassphrase;

    public KeystoreUtil(String keystorePath,
                        byte [] keystorePassphrase,
                        byte [] privatekeyPassphrase) throws Exception {
        this.keystorePath = keystorePath;
        this.keystorePassphrase = keystorePassphrase;
        this.privatekeyPassphrase = privatekeyPassphrase;
        loadKeystore(this.keystorePath);
    }

    /**
     *
     * @param keystorePath
     * @throws Exception
     */
    private void loadKeystore(String keystorePath) throws Exception {
        FileInputStream isks = null;
        logger.log(Level.INFO, "" +
                ": " + keystorePath);
        try {
            isks = new FileInputStream(keystorePath);
            fnKeystore = KeyStore.getInstance("pkcs12");
            fnKeystore.load(isks, new String(keystorePassphrase).toCharArray());
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, "Error while trying to load keystore ["+ keystorePath +"]");
            throw new Exception("Error while trying to load keystore ["+ keystorePath +"]",ex);
        } finally {
            try {
                if (isks != null) {
                    isks.close();
                }
            } catch (IOException ex) {}
        }
    }

    /**
     *
     * @param alias
     * @return
     * @throws Exception
     */
    public Certificate getCertificate(String alias) throws Exception {
        try {
            logger.log(Level.INFO, "Looking for certificate alias: " + alias);
            return fnKeystore.getCertificate(alias);
        } catch (KeyStoreException ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, "Error while trying to get Certificate ["+ alias +"]");
            throw new Exception("Error while trying to get Certificate ["+ alias +"]",ex);
        }
    }

    /**
     *
     * @param alias
     * @return
     * @throws Exception
     */
    public PrivateKey getPrivateKey(String alias) throws Exception {
        logger.log(Level.INFO, "Looking for Private Key with alias: " + alias);
        PrivateKey pk = getPrivateKey(alias, privatekeyPassphrase);
        logger.log(Level.FINEST, "PrivateKey: ["+ pk +"] Type: ["+ pk.getClass().getCanonicalName() +"]");
        return pk;
    }

    /**
     *
     * @param alias
     * @param passphrase
     * @return
     * @throws Exception
     */
    public PrivateKey getPrivateKey(String alias, byte[] passphrase) throws Exception {
        try {
            return (PrivateKey)fnKeystore.getKey(alias, new String(passphrase).toCharArray());
        } catch (KeyStoreException ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, "Error while trying to get Certificate ["+ alias +"]");
            throw new Exception("Error while trying to get Certificate ["+ alias +"]",ex);
        }
    }

}
