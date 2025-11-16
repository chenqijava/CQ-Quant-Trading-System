package com.nyy.gmail.cloud.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MD5Util {

    public static String MD5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance( "MD5" );
            return ByteUtil.toHexString( md.digest( data.getBytes( StandardCharsets.UTF_8) ) );
        } catch (Exception e) {
            System.out.println("加密中途出错");
        }
        return null;
    }
}
