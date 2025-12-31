package com.zfdang.zsmth_android.helpers;

import android.content.Context;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import com.zfdang.zsmth_android.R;

public class NewToast {

    public static void makeText(Context context, String message, int duration) {
        if(context == null) return;
        int color = ContextCompat.getColor(context, R.color.colorPrimary);
        String formattedMessage = message.replace("\\n", "\n");
        makeText(context, formattedMessage, duration, color);
    }

    public static void makeText(Context context, String message, int duration, @ColorInt int backgroundColor) {
        if(context == null) return;
        String formattedMessage = message.replace("\\n", "\n");
        Toast toast = Toast.makeText(context, formattedMessage, duration);
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
