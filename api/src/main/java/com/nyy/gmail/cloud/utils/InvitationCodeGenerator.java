package com.nyy.gmail.cloud.utils;

import java.util.Random;

public class InvitationCodeGenerator {

    private static final String CHAR_LIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static final int CODE_LENGTH = 4;

    private InvitationCodeGenerator() {
    }

    public static String generateInvitationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHAR_LIST.length());
            code.append(CHAR_LIST.charAt(index));
        }

        return code.toString();
    }

    public static String generateInvitationCode(int size) {
        Random random = new Random();
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < size; i++) {
            int index = random.nextInt(CHAR_LIST.length());
            code.append(CHAR_LIST.charAt(index));
        }

        return code.toString();
    }

    public static void main(String[] args) {
        System.out.println(generateInvitationCode(8));
    }
}
