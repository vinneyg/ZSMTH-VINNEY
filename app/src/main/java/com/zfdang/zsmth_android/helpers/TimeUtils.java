package com.zfdang.zsmth_android.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    /**
     * 将字符串时间转换为 Date 对象
     *
     * @param dateStr 时间字符串，如 "2025-05-30 12:06:11"
     * @return 转换后的 Date 对象，失败返回 null
     */
    public static Date convertStringToDate(String dateStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            return format.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将 Date 对象格式化为字符串
     *
     * @param date Date 对象
     * @return 格式化后的时间字符串，如 "2025-05-30 12:06:11"
     */
    public static String convertDateToString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return format.format(date);
    }
}
