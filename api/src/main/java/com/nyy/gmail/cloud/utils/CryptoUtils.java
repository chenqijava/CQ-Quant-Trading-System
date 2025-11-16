package com.nyy.gmail.cloud.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;


public class CryptoUtils {
    private static final int SALT_LENGTH = 16;        
    private static final int IV_LENGTH = 12;          
    private static final int TAG_LENGTH_BIT = 128;    
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    private static SecretKey getKeyFromPassword(char[] password, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        byte[] keyBytes = digest.digest(new String(password).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String encrypt(String plaintext, String password) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        SecretKey key = getKeyFromPassword(password.toCharArray(), salt);
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
        buffer.put(salt).put(iv).put(cipherText);
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public static String decrypt(String cipherMessage, String password) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(cipherMessage);
        ByteBuffer buffer = ByteBuffer.wrap(decoded);
        byte[] salt = new byte[SALT_LENGTH];
        buffer.get(salt);
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);
        SecretKey key = getKeyFromPassword(password.toCharArray(), salt);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }

}
