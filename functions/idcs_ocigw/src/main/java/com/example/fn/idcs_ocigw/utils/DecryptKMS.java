/*
Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/
package com.example.fn.idcs_ocigw.utils;


import java.util.Base64;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.keymanagement.KmsCryptoClient;
import com.oracle.bmc.keymanagement.model.DecryptDataDetails;
import com.oracle.bmc.keymanagement.requests.DecryptRequest;
import com.oracle.bmc.keymanagement.responses.DecryptResponse;
import java.util.logging.Logger;


public class DecryptKMS {
    private  static final Logger LOGGER = Logger.getLogger("IDCS_GTW_LOGGER");

    private DecryptKMS()
    {
        throw new IllegalStateException("Utility class");
    }
    public static String decodeKMSString(String kmsEndpoint,String kmsKeyOCID,String encryptedText)  {

        LOGGER.info("Decrypting key");
        AbstractAuthenticationDetailsProvider provider = null;

        provider = ResourcePrincipalAuthenticationDetailsProvider.builder().build();

        KmsCryptoClient cryptoClient = KmsCryptoClient.builder().endpoint(kmsEndpoint).build(provider);
        DecryptDataDetails decryptDataDetails = DecryptDataDetails.builder().keyId(kmsKeyOCID).ciphertext(encryptedText).build();
        DecryptRequest decryptRequest = DecryptRequest.builder().decryptDataDetails(decryptDataDetails).build();
        DecryptResponse decryptResponse = cryptoClient.decrypt(decryptRequest);
        String decryptedDEK = decryptResponse.getDecryptedData().getPlaintext();
        String plainText=  new String (Base64.getDecoder().decode(decryptedDEK.getBytes()));
        LOGGER.info("Key Decrypted");
        return plainText;
    }


}