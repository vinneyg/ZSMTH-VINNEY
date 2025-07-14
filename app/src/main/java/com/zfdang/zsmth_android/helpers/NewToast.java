package com.zfdang.zsmth_android.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.ColorInt;

import com.zfdang.zsmth_android.R;

/**
 * 简化版自定义 Toast，支持设置背景颜色
 */
public class NewToast {

    @SuppressLint("ResourceAsColor")
    public static void makeText(Context context, String message, int duration) {
        makeText(context, message, duration, R.color.colorPrimary);
    }

    public static void makeText(Context context, String message, int duration, @ColorInt int backgroundColor) {
        Toast toast = Toast.makeText(context, message, duration);
        try {
            View view = toast.getView();
            if (view != null) {
                view.setBackgroundColor(backgroundColor);
            }
        } catch (Exception e) {
            // 忽略异常，使用默认样式继续显示
        }
        toast.show();
    }
}
