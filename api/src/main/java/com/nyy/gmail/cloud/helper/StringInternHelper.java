package com.nyy.gmail.cloud.helper;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

public class StringInternHelper {
    private static final Interner<String> interner = Interners.newStrongInterner();

    public static String intern(String str) {
        return interner.intern(str);
    }

    public static String generateKey(String... keys) {
        return intern(String.join("-", keys));
    }

    public static String projectName(String project) {
        return generateKey("project", project);
    }

    public static String allocateAccount(String project) {
        return generateKey("allocate-account", project);
    }
}
