package com.nyy.gmail.cloud.utils;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * @author guochao
 */
public class UUIDUtils {

    private UUIDUtils() {
    }

    public static String get32UUId() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 代付订单ID
     *
     */
    public static String getPaymentOrderId(){
        String s = InvitationCodeGenerator.generateInvitationCode(3);
        return ("P"+generateOrderNumber()+s).toUpperCase(Locale.ROOT);
    }

    /**
     * 代收订单ID
     */
    public static String getCollectionOrderId(){
        String s = InvitationCodeGenerator.generateInvitationCode(3);
        return ("C"+generateOrderNumber()+s).toUpperCase(Locale.ROOT);
    }

    /**
     * 提现订单ID
     */
    public static String getWithdrawOrderId(){
        String s = InvitationCodeGenerator.generateInvitationCode(3);
        return ("W"+generateOrderNumber()+s).toUpperCase(Locale.ROOT);
    }

    private static String generateOrderNumber() {
        String s = DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYYMMDDHHMMSS);
        Random random = new Random();
        // 生成一个0到999之间的随机数
        int randomNum = random.nextInt(1000);
        // 将时间戳和随机数组合成订单号，并确保随机数为三位数
        return s + String.format("%03d", randomNum);
    }

    public static String randomAvatar() {
        String[] color = new String[] {
                "#FF76C1",
                "#4ECDE6",
                "#EE675C",
                "#FCC934",
                "#FA903E",
                "#5BB974",
        };
        // 使用 Random 类生成随机索引
        Random random = new Random();
        return color[random.nextInt(color.length)];
    }

    public static String getBuyEmailOrderNo() {
        // 生成订单号
        String timePart = new SimpleDateFormat("MMddHHmmss").format(new Date());
        // 生成一个4位随机数
        int randomPart = new Random().nextInt(9000) + 1000;
        return "E" + timePart + randomPart; // 共12位
    }

    public static String getBuyEMailOrderDetailNo(String orderNo, int number) {
        String formatted = String.format("%06d", number);
        return orderNo + formatted;
    }
}

