package com.nyy.gmail.cloud.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Slf4j
public abstract class PriceUtil {

    private static final Integer moveDigits = 3;

    private static final String A_ZERO = "0";

    private static final String TWO_ZERO = "00";

    private static final String THREE_ZERO = "000";

    private static final String PRICE_STRING_REG = "^\\d{1,6}(\\.?\\d{0,3})?$";


    /**
     * 校验字符串是否符合价格规则
     *
     * @param price
     * @return 是否符合价格规则
     */
    public static Boolean testPriceString(String price) {
        if (price == null) {
            return null;
        }
        return price.matches(PRICE_STRING_REG);
    }

    /**
     * 价格从String转换为Long
     *
     * @param price
     * @return Long类型价格
     */
    public static Long stringToLong(String price) {
        if (!testPriceString(price)) {
            return null;
        }

        int length = price.length();

        int index = price.indexOf('.');
        // 如果
        if (index == -1) {
            return Long.parseLong(price + THREE_ZERO);
        }
        String newPrice = price.replace(".", "");
        switch (length - 1 - index) {
            case 1:
                newPrice = newPrice + TWO_ZERO;
                break;
            case 2:
                newPrice = newPrice + A_ZERO;
                break;
            case 3:
                break;
            default:
                return null;
        }
        return Long.parseLong(newPrice);
    }


    // 积分转金额
//     public static Double pointToMoney(Long point) {
//         if (point == null){
//             return 0D;
//         }
//         // 保留4位小数
//          BigDecimal result = BigDecimal.valueOf(point).divide(
//                         BigDecimal.valueOf(BALANCE_CONVERSION.getValue())
//                 ).multiply(new BigDecimal(OrderTaskConstants.CONVERSION_FACTOR_DEFAULT))
//                 .setScale(4, RoundingMode.HALF_UP);
// //        log.info("pointToMoney,point: {},bigdecimal result: {},result: {}", point, result, result.longValue());
//         return result.doubleValue();
//     }

//     /**
//      * 单个金额乘以个数,计算使用积分
//      *
//      * @param money
//      * @param size
//      * @return
//      */
//     public static Long moneyToPoint(Double money, long size) {
//         if (money == null){
//             return null;
//         }
//         BigDecimal a = BigDecimal.valueOf(money)
//                 .multiply(BigDecimal.valueOf(BALANCE_CONVERSION.getValue())).multiply(BigDecimal.valueOf(size
//                 ));
//         BigDecimal b = new BigDecimal(OrderTaskConstants.CONVERSION_FACTOR_DEFAULT);

//         BigDecimal result = null;
//         if (a.remainder(b).compareTo(BigDecimal.ZERO) == 0) {
//             result = a.divide(b); // 可以整除，无需指定舍入模式
//         } else {
//             result = a.divide(b, 10, RoundingMode.HALF_UP); // 需要指定舍入模式
//         }
//         log.info("moneyToPoint,money: {},bigdecimal result: {},result: {}", money, result, result.longValue());
//         return result.longValue();
//     }


}
