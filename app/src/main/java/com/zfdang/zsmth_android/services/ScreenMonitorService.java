package com.zfdang.zsmth_android.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.R;

public class ScreenMonitorService extends Service {
    private static final String TAG = "ScreenMonitorService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "screen_monitor_channel";

    private BroadcastReceiver screenReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "屏幕监听服务已创建");

        // 创建通知渠道（Android 8.0+）
        createNotificationChannel();
        // 启动前台服务并指定类型（Android 14+ 要求）
        Notification notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 需要指定前台服务类型
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        registerScreenReceiver();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "屏幕监听服务",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("用于监听屏幕状态变化的后台服务");
        channel.setShowBadge(false);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("水木社区")
                .setContentText("屏幕状态监听服务运行中...")
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void registerScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "收到屏幕状态广播: " + action);

                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    handleScreenOff();
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    handleScreenOn();
                    // 屏幕亮起时升级为前台服务
                    upgradeToForegroundService();
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    handleUserPresent();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }

        Log.d(TAG, "屏幕状态广播接收器已注册");
    }

    private void upgradeToForegroundService() {
        try {
            startForeground(NOTIFICATION_ID, createNotification());
            Log.d(TAG, "服务已升级为前台服务");
        } catch (Exception e) {
            Log.e(TAG, "升级前台服务失败: " + e.getMessage());
        }
    }

    private void handleScreenOff() {
        Log.d(TAG, "屏幕关闭，检查 KeepAliveService 状态");
        if (SMTHApplication.isValidUser()) {
            // 延迟检查，确保系统稳定
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // 检查并启动 KeepAliveService
                Intent keepAliveIntent = new Intent(this, KeepAliveService.class);
                startForegroundService(keepAliveIntent);
                Log.d(TAG, "已启动 KeepAliveService");
            }, 3000);
        }
    }

    private void handleScreenOn() {
        Log.d(TAG, "屏幕亮起");
        // 屏幕亮起时的处理逻辑
    }

    private void handleUserPresent() {
        Log.d(TAG, "用户解锁");
        // 用户解锁时的处理逻辑
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "屏幕监听服务已启动");
        // 返回 START_STICKY 确保服务被杀死后会重启
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 不支持绑定
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
            Log.d(TAG, "屏幕状态广播接收器已注销");
        }
        Log.d(TAG, "屏幕监听服务已销毁");
    }
}
