package com.nyy.gmail.cloud.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class DateUtil {

    /**
     * 获取当月第一天
     *
     * @return
     */
    public static Date getMonthFirstDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date firstDayOfMonth = cal.getTime();
        return firstDayOfMonth;
    }

    /**
     * 获取n月前的日期
     *
     * @param n
     * @return
     */
    public static Date getMonthsBeforeDate(int n) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -n);
        Date monthsBeforeDate = cal.getTime();
        return monthsBeforeDate;
    }

    /**
     * 对给定的时间，月份计算
     * @param n
     * @return
     */
    public static Date getMonthsDate(Date date,int n) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, n);
        return cal.getTime();
    }

    public static Date getDateAfterDate(Date date, int i) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, 5);
        return cal.getTime();
    }

    /**
     * 获取n年前的日期
     *
     * @param n
     * @return
     */
    public static Date getYearsBeforeDate(int n) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -n);
        Date monthsBeforeDate = cal.getTime();
        return monthsBeforeDate;
    }

    /**
     * 时间格式转换
     *
     * @param: [date]
     * @return: java.lang.String
     **/
    public static String dateToString(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDateFormat.format(date);
    }

    /**
     * 时间格式转换
     *
     * @param: [dateString]
     * @return: java.util.Date
     **/
    public static Date stringToDate(String dateString) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取范围
     *
     * @param: dateString 时间字符串
     * @param: days 向前向后的天数
     * @return: java.util.Map<java.lang.String, java.util.Date>
     **/
    public static Map<String, Date> getStartTimeAndEndTime(String dateString, int days) {
        Map<String, Date> map = new HashMap<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date startTime = simpleDateFormat.parse(dateString);
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(startTime);
            calendar.add(Calendar.DATE, days);
            Date endTime = calendar.getTime();
            if (days > 0) {
                map.put("startTime", startTime);
                map.put("endTime", endTime);
            } else {
                map.put("startTime", endTime);
                map.put("endTime", startTime);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public static Date stringToDateTime(String dateString, int days) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date startTime = simpleDateFormat.parse(dateString);
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(startTime);
            calendar.add(Calendar.DATE, days);
            Date endTime = calendar.getTime();
            return endTime;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static int dayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == Calendar.SUNDAY ? 7 : dayOfWeek - 1;
    }

    /**
     * 获取给定日期,所在周周一0点时间
     *
     * @param date
     * @return
     */
    public static Date getStartOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获取给定日期,当天24点时间
     *
     * @param date
     * @return
     */
    public static Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获取给定日期,上周周一0点时间
     *
     * @param date
     * @return
     */
    public static Date getStartOfLastWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();

    }

    /**
     * 获取给定日期,上周周日24点时间
     *
     * @param date
     * @return
     */
    public static Date getEndOfLastWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }



    public enum FORMAT {
        /**
         * 日期格式（天）
         */
        YYYY_MM_DD("yyyy-MM-dd"),
        YYYY_MM_DD_1("yyyyMMdd"),
        YYYY_MM_DD_2("yyyy/MM/dd"),

        /**
         * 时间格式（秒）
         */
        YYYY_MM_DD_HH_SS_MM("yyyy-MM-dd HH:mm:ss"),

        /**
         * 时间格式（分）
         */
        YYYY_MM_DD_HH_SS("yyyy-MM-dd HH:mm"),

        /**
         * 时间格式（时）
         */
        YYYY_MM_DD_HH("yyyy-MM-dd HH"),
        YYYYMMDDHHMMSS("yyyyMMddHHmmss"),
        ;
        public String format;

        FORMAT(String format) {
            this.format = format;
        }
    }

    /**
     * 获取当前格式化时间
     * @param format
     * @return
     */
    public static String getNowTime(FORMAT format){
        return initSimpleFormat(format).format(new Date());
    }

    /**
     * 将指定格式的时间，格式化为时间戳
     * @param date
     * @param format
     * @return
     */
    public static Long getDateMillis(String date,FORMAT format) {
        try {
            return initSimpleFormat(format).parse(date).getTime();
        }catch (Exception e){
            log.error("Date parse error:",e);
            return 0L;
        }
    }

    /**
     * 将字符串时间 按照指定格式转换为 Java时间
     * @param date
     * @param format
     * @return
     * @throws ParseException
     */
    public static Date getDateByFormat(String date,FORMAT format) {
        try {
            return initSimpleFormat(format).parse(date);
        }catch (Exception e){
            log.error("日期解析失败：",e);
            return null;
        }
    }


    /**
     * 将Date格式化
     * @param date
     * @param format
     * @return
     */
    public static String formatByDate(Date date,FORMAT format){
        return initSimpleFormat(format).format(date);
    }

    /**
     * 初始化format
     * @param format
     * @return
     */
    private static SimpleDateFormat initSimpleFormat(FORMAT format){
        return new SimpleDateFormat(format.format);
    }

    /**
     * 检测是否位UTC时间
     * @param time
     * @return
     */
    public static boolean isUTC(String time){
        return time.contains("T");
    }
}
