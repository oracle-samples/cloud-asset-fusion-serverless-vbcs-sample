/*
Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/

package com.example.fn.idcs_ocigw.utils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class Response {
    private final HttpURLConnection connection;
    private static final int NOTFOUND_SC=404;
    Response(final HttpURLConnection connection) {
        this.connection = connection;
    }

    public void close()  {
        connection.disconnect();
    }

    public String getHeader(final String name) {
        return connection.getHeaderField(name);
    }

    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    public int getStatus() {
        try {
            return connection.getResponseCode();
        } catch (IOException e) {
            return NOTFOUND_SC;
        }
    }


    public String getResponseBodyAsString(final String encoding)
        throws Exception {
        if (encoding==null)
        {
            throw new IllegalArgumentException("Encoding is null");
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            connection.getInputStream(), encoding));
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
