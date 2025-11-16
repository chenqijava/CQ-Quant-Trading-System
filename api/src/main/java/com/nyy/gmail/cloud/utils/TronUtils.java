package com.nyy.gmail.cloud.utils;

import java.math.BigDecimal;

public class TronUtils {
    public static BigDecimal toHumanUsdtDecimal(BigDecimal value) {
        return value.divide(BigDecimal.TEN.pow(6));
    }

    public static BigDecimal toHumanUsdtDecimal(String value) {
        return toHumanUsdtDecimal(new BigDecimal(value));
    }
}
