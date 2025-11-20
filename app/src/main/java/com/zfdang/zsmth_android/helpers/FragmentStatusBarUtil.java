package com.zfdang.zsmth_android.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.app.UiModeManager;
import androidx.fragment.app.Fragment;
import android.view.WindowInsets;

public class FragmentStatusBarUtil {

    // 1. Fragment中设置状态栏颜色（自动适配图标深浅）
    public static void setStatusBarColor(Fragment fragment, int colorResId) {
        Activity activity = fragment.getActivity();
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        Window window = activity.getWindow();
        // 设置状态栏颜色
        int color = activity.getResources().getColor(colorResId, activity.getTheme());
        window.setStatusBarColor(color);

        // 自动判断背景色，设置图标深浅（浅色背景→深色图标）
        boolean isLightBg = Color.luminance(color) > 0.5f;
        WindowInsetsController insetsController = window.getInsetsController();
        if (insetsController != null) {
            int appearance = isLightBg ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0;
            insetsController.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }

    // 2. Fragment中隐藏状态栏（沉浸式）
    public static void hideStatusBar(Fragment fragment) {
        Activity activity = fragment.getActivity();
        if (activity == null) return;

        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 推荐方式
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // 低版本兼容（Android 6.0-10）
            @SuppressLint("DeprecatedApi")
            int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            window.getDecorView().setSystemUiVisibility(flags);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
    public static boolean isSystemDarkMode(Activity activity) {
        UiModeManager uiModeManager = (UiModeManager) activity.getSystemService(Context.UI_MODE_SERVICE);
        boolean isSystemDark = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        return isSystemDark;
    }
    // 3. 深色模式适配（跟随系统/手动控制）
    public static void adaptDarkMode(Fragment fragment, boolean isManualDarkMode) {
        Activity activity = fragment.getActivity();
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        // 读取系统深色模式状态
        boolean isSystemDark = isSystemDarkMode(activity);
        // 最终深色模式状态（系统+手动）
        boolean isDarkMode = isSystemDark || isManualDarkMode;
        Window window = activity.getWindow();

        // 深色模式：黑色背景+白色图标；浅色模式：白色背景+黑色图标
        window.setStatusBarColor(isDarkMode ? Color.BLACK : Color.WHITE);
        WindowInsetsController insetsController = window.getInsetsController();
        if (insetsController != null) {
            int appearance = isDarkMode ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            insetsController.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }
    public static void adaptActDarkMode(Activity activity, boolean isManualDarkMode) {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        // 读取系统深色模式状态
        boolean isSystemDark = isSystemDarkMode(activity);
        // 最终深色模式状态（系统+手动）
        boolean isDarkMode = isSystemDark || isManualDarkMode;
        Window window = activity.getWindow();

        // 深色模式：黑色背景+白色图标；浅色模式：白色背景+黑色图标
        window.setStatusBarColor(isDarkMode ? Color.BLACK : Color.WHITE);
        WindowInsetsController insetsController = window.getInsetsController();
        if (insetsController != null) {
            int appearance = isDarkMode ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            insetsController.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }
}
