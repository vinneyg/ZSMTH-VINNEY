package com.zfdang.zsmth_android.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.R;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class KeepAliveService extends Service {
    private static final int notId = 1;
    private static final String channelId = "e729";
    private final static int BASE_CHECK_INTERVAL = 180;
    private final static int MIN_CHECK_INTERVAL = 120;
    private final static int MAX_CHECK_INTERVAL = 300;
    private final static int REFRESH_THRESHOLD = 300;
    private final static int RETRY_INTERVAL = 60;
    private final static int MAX_RETRY_COUNT = 3;
    private final static int USER_ACTIVE_DELAY = 300;

    public static final String ACTION_USER_ACTIVE = "com.zfdang.zsmth_android.USER_ACTIVE";
    public static final String ACTION_FORCE_REFRESH = "com.zfdang.zsmth_android.FORCE_REFRESH";

    private static int refreshCount = 0;
    private static int failureCount = 0;
    private static long lastRefreshTime = 0;
    private static long lastUserActiveTime = 0;
    private static boolean isRefreshing = false;
    private static boolean isUserActive = false;

    private BroadcastReceiver userActivityReceiver;

    @SuppressLint({"NotificationId0", "ForegroundServiceType"})
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("保持在线服务")
                .setContentText("在线中……")
                .setSmallIcon(R.drawable.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setChannelId(channelId)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(notId, notification);

        registerUserActivityReceiver();

        lastRefreshTime = System.currentTimeMillis();
        lastUserActiveTime = System.currentTimeMillis();

        scheduleKeepAliveTask(getNextCheckInterval());
    }

    private void registerUserActivityReceiver() {
        userActivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USER_ACTIVE.equals(action)) {
                    onUserActive();
                } else if (ACTION_FORCE_REFRESH.equals(action)) {
                    forceRefresh();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USER_ACTIVE);
        filter.addAction(ACTION_FORCE_REFRESH);
        LocalBroadcastManager.getInstance(this).registerReceiver(userActivityReceiver, filter);
    }

    private void onUserActive() {
        long currentTime = System.currentTimeMillis();
        lastUserActiveTime = currentTime;
        isUserActive = true;

        Log.d("KeepAliveService", "检测到用户活跃");

        if (currentTime - lastRefreshTime >= REFRESH_THRESHOLD * 1000L) {
            Log.d("KeepAliveService", "用户活跃且需要刷新，安排延迟刷新");
            scheduleKeepAliveTask(USER_ACTIVE_DELAY);
        }
    }

    private void forceRefresh() {
        Log.d("KeepAliveService", "收到强制刷新请求");
        scheduleKeepAliveTask(0);
    }

    private static int getNextCheckInterval() {
        long currentTime = System.currentTimeMillis();
        boolean recentlyActive = (currentTime - lastUserActiveTime) < (10 * 60 * 1000L);

        if (recentlyActive) {
            return MAX_CHECK_INTERVAL;
        } else {
            return MIN_CHECK_INTERVAL;
        }
    }

    private void scheduleKeepAliveTask(int delaySeconds) {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWork.class)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);
        Log.d("KeepAliveService", "计划下次任务，延迟: " + delaySeconds + "秒");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(channelId,
                "SMTH", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userActivityReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(userActivityReceiver);
        }
        stopForeground(true);
        Log.d("KeepAliveService", "服务已销毁");
    }

    public KeepAliveService() {
    }

    public static class MyWork extends Worker {
        public MyWork(Context context, WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            if (isRefreshing) {
                Log.d("KeepAliveService", "已有刷新任务在执行，跳过本次");
                return Result.success();
            }

            isRefreshing = true;
            refreshCount++;

            try {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastRefresh = (currentTime - lastRefreshTime) / 1000;

                Log.d("KeepAliveService", "执行检查: 距离上次刷新 " + timeSinceLastRefresh + " 秒");

                if (timeSinceLastRefresh >= REFRESH_THRESHOLD) {
                    Log.d("KeepAliveService", "达到刷新阈值，执行刷新");
                    boolean success = performKeepAlive();

                    if (success) {
                        lastRefreshTime = currentTime;
                        failureCount = 0;
                        isUserActive = false;
                        Log.d("KeepAliveService", "刷新成功，安排下次检查");
                        scheduleNextTask(getNextCheckInterval());
                    } else {
                        handleFailure();
                    }
                } else {
                    Log.d("KeepAliveService", "未达到刷新阈值，安排下次检查");
                    scheduleNextTask(getNextCheckInterval());
                }
            } catch (Exception e) {
                Log.e("KeepAliveService", "任务执行异常", e);
                handleFailure();
            } finally {
                isRefreshing = false;
            }

            return Result.success();
        }

        private boolean performKeepAlive() {
            SMTHHelper helper = SMTHHelper.getInstance();

            try {
                Log.d("KeepAliveService", "执行保活策略");
                helper.wService.keepAlive().execute();
                return true;
            } catch (IOException e) {
                Log.w("KeepAliveService", "保活失败", e);
                return false;
            }
        }

        private void handleFailure() {
            failureCount++;
            Log.d("KeepAliveService", "失败计数: " + failureCount);

            if (failureCount >= MAX_RETRY_COUNT) {
                Log.d("KeepAliveService", "连续失败超过限制，通知主程序");
                if (SMTHApplication.mUserStatusReceiver != null) {
                    SMTHApplication.mUserStatusReceiver.onServiceFailed();
                }
                failureCount = 0;
                scheduleNextTask(BASE_CHECK_INTERVAL);
            } else {
                int retryDelay = RETRY_INTERVAL * failureCount;
                retryDelay = Math.min(retryDelay, BASE_CHECK_INTERVAL);
                Log.d("KeepAliveService", "重试延迟: " + retryDelay + "秒");
                scheduleNextTask(retryDelay);
            }
        }

        private void scheduleNextTask(int delayInSeconds) {
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWork.class)
                    .setInitialDelay(delayInSeconds, TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);
        }
    }

    public static void notifyUserActive(Context context) {
        Intent intent = new Intent(ACTION_USER_ACTIVE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void requestForceRefresh(Context context) {
        Intent intent = new Intent(ACTION_FORCE_REFRESH);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
