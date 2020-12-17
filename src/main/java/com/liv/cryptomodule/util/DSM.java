package com.liv.cryptomodule.util;

import com.liv.cryptomodule.dto.SignatureDTO;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Random;

public class DSM {

    private static BigInteger maxLimit = new BigInteger("500000000000000");
    private static BigInteger minLimit = new BigInteger("25000000000");

    private static byte[] hashSHA256(String msg) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hash = messageDigest.digest(msg.getBytes(StandardCharsets.UTF_8));

        return hash;
    }

    public static String SHA256hex (String msg) {
        try {
            StringBuffer hexString = new StringBuffer();
            byte[] hash = hashSHA256(msg);
            
            for (int i = 0; i < hash.length; i++) {
                if ((0xff & hash[i]) < 0x10) {
                    hexString.append("0"
                            + Integer.toHexString((0xFF & hash[i])));
                } else {
                    hexString.append(Integer.toHexString(0xFF & hash[i]));
                }
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "null";
        }
    }

    public static BigInteger generateSalt() {
        BigInteger bigInteger = maxLimit.subtract(minLimit);
        Random random = new Random();
        int len = maxLimit.bitLength();
        BigInteger salt = new BigInteger(len, random);
        if (salt.compareTo(minLimit) < 0)
            salt = salt.add(minLimit);
        if (salt.compareTo(bigInteger) >= 0)
            salt = salt.mod(bigInteger).add(minLimit);
        return salt;
    }

    private static KeyPair generateKeyPair(byte[] psswdHash) throws NoSuchAlgorithmException {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("DSA");
        generator.initialize(2048, new SecureRandom(psswdHash));
        KeyPair kp = generator.generateKeyPair();

        return kp;
    }

    public static SignatureDTO sign(String msg, String psswd)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        byte[] messageHash = hashSHA256(msg);
        KeyPair kp = generateKeyPair(hashSHA256(psswd));
        Signature signature = Signature.getInstance("SHA256withDSA");
        PrivateKey sk = kp.getPrivate();
        signature.initSign(sk);
        signature.update(messageHash);
        byte[] signatureValue = signature.sign();

        return new SignatureDTO(signatureValue, messageHash, kp.getPublic());
    }

    public static boolean verify(String[] parsed)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        
        Signature vSignature = Signature.getInstance("SHA256withDSA");
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");

        byte[] pkDecoded = Base64.getDecoder().decode(parsed[0]);
        byte[] hashDecoded = Base64.getDecoder().decode(parsed[1]);
        byte[] signatureDecoded = Base64.getDecoder().decode(parsed[2]);
        String pkDecodedString = new String(pkDecoded);
        
        BigInteger p = new BigInteger(pkDecodedString.substring(0, 607).replaceAll("\\s",""),16);
        BigInteger q = new BigInteger(pkDecodedString.substring(608, 674).replaceAll("\\s",""),16);
        BigInteger g = new BigInteger(pkDecodedString.substring(675, 1282).replaceAll("\\s",""),16);
        BigInteger y = new BigInteger(pkDecodedString.substring(1283, 1890).replaceAll("\\s",""),16);

        PublicKey pkRetrieved = keyFactory.generatePublic(new DSAPublicKeySpec(y, p, q, g));
        vSignature.initVerify(pkRetrieved);
        vSignature.update(hashDecoded);
        
        return vSignature.verify(signatureDecoded);
    }
}
