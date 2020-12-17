package com.liv.cryptomodule.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import java.util.logging.Logger;

public class BIM {

    private static SecureRandom random = new SecureRandom();

    private BIM () {
        throw new IllegalStateException("Utility class");
    }
    
    private static Properties prop = new Properties();
    private static final Logger log = java.util.logging.Logger.getLogger(BIM.class.getName());

    private static void loadProps() throws IOException {
        InputStream input = BIM.class.getClassLoader().getResourceAsStream("config.properties");
        if (input == null) {
            log.log(Level.SEVERE, "Can''t to find config.properties");
            return;
        }
        prop.load(input);
    }

    public static String storeEventHash(String eventHash, String pubKey, String signature)
            throws IOException {
        
        loadProps();
        String endpointURI = prop.getProperty("hashendpoint") + "/addMessage?kld-from=" + prop.getProperty("kldfrom") + "&kld-sync=true";
        String body = String.format(
                "{ \r%n\"_messageHash\": \"%s\",\r%n"
                    + "\"_pubKey\": \"%s\",\r%n"
                    + "\"_signature\": \"%s\"\r%n}",
                eventHash, pubKey, signature);
        
        JSONObject json = new JSONObject(sendRequestToAPI(endpointURI, body, prop.getProperty("authparams")));
        JSONObject headers = (JSONObject) json.get("headers");
        if (headers.get("type").equals("TransactionSuccess")) {
            return headers.get("type").toString();
        } else {
            log.log(Level.WARNING, "Non-success response from API: {0}", json);
        }
        return null;
    }

    public static String mintDocumentToken() throws IOException {
        loadProps();
        String tokenID = DSM.generateSalt().toString();
        System.out.println(tokenID);
        String endpointURI = prop.getProperty("mintendpoint") + "/mint?kld-from=" + prop.getProperty("kldfrom") + "&kld-sync=true";
        String body = String.format(
                "{ \r%n\"to\": \"%s\",\r%n"
                        + "\"tokenId\": \"%s\"\r%n}",
                prop.getProperty("wallet"), tokenID);
        JSONObject json = new JSONObject(sendRequestToAPI(endpointURI, body, prop.getProperty("authparams")));
        JSONObject headers = (JSONObject) json.get("headers");
        if (headers.get("type").equals("TransactionSuccess")) {
            System.out.println(headers.get("type").toString());
            return tokenID;
        } else {
            log.log(Level.WARNING, "Non-success response from API: {0}", json);
        }
        return null;
    }

    private static String sendRequestToAPI(String endpointURI, String bodyFormat, String authparams) throws IOException {
        
        Response response;
        String responseBody;

        log.log(Level.INFO, "Request URL: {0}", endpointURI);
        log.log(Level.INFO, "Request payload: {0}", bodyFormat);

        for (int retries = 0; retries < 3; retries++) {
            try {
            OkHttpClient client = new OkHttpClient()
            .newBuilder().
            connectTimeout(15, TimeUnit.SECONDS)
            .build();
            MediaType mediaType = MediaType.parse("text/plain");

            RequestBody body = RequestBody.create(bodyFormat, mediaType);
            Request request = new Request.Builder()
            .url(endpointURI)
            .addHeader("Authorization", authparams)
            .addHeader("Content-Type", "text/plain")
            .post(body)
            .build();

            response = client.newCall(request).execute();
            responseBody = response.body().string();
            if (response.code() != 200) {
                log.log(Level.WARNING, "Response body: {0}", responseBody);
                throw new IllegalStateException("Request on '" + endpointURI + "' resulted in " + response.code());
                } else {
                    log.log(Level.INFO, "Response body: {0}", responseBody);
                    return responseBody;
                }
            } catch (final java.net.SocketTimeoutException e) {
                e.printStackTrace();
            }
        }
        log.log(Level.WARNING, "We didn't get response from environment!");
        return null;
    }

}