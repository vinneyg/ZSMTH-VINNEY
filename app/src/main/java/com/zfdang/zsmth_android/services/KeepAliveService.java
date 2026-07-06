package com.zfdang.zsmth_android.services;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.R;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

public class KeepAliveService extends Service {
    private static final String TAG = "KeepAliveService";
    private static final int notId = 1;
    private static final String channelId = "e729";

    // 检查间隔常量（秒）
    private final static int BASE_CHECK_INTERVAL = 180;    // 基础间隔 3 分钟
    private final static int MIN_CHECK_INTERVAL = 120;     // 最短间隔 2 分钟
    private final static int MAX_CHECK_INTERVAL = 300;     // 最长间隔 5 分钟
    private final static int REFRESH_THRESHOLD = 300;      // 刷新阈值 5 分钟
    private final static int RETRY_INTERVAL = 45;           // 重试基础间隔
    private final static int MAX_RETRY_COUNT = 3;           // 最大连续失败次数
    private final static int USER_ACTIVE_DELAY = 300;       // 用户活跃后延迟检查

    // Alarm 相关
    private static final String ALARM_ACTION = "com.zfdang.zsmth_android.KEEP_ALIVE_ALARM";
    private static final long MIN_ALARM_INTERVAL_MS = 2 * 60 * 1000L; // Alarm 最小间隔 2 分钟（防止高频触发）

    public static final String ACTION_USER_ACTIVE = "com.zfdang.zsmth_android.USER_ACTIVE";
    public static final String ACTION_FORCE_REFRESH = "com.zfdang.zsmth_android.FORCE_REFRESH";

    // 线程安全的状态变量
    private static final Object LOCK = new Object();
    private static volatile int refreshCount = 0;
    private static volatile int failureCount = 0;
    private static volatile long lastRefreshTime = 0;
    private static volatile long lastUserActiveTime = 0;
    private static volatile long lastAlarmTriggerTime = 0;  // 上次 Alarm 触发时间，防止密集触发
    private static volatile boolean isRefreshing = false;
    private static volatile boolean isUserActive = false;

    private BroadcastReceiver userActivityReceiver;
    private BroadcastReceiver alarmReceiver;
    private BroadcastReceiver systemReceiver;  // 保存引用用于注销
    private AlarmManager alarmManager;
    private PowerManager.WakeLock wakeLock;

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
                .setOngoing(true)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(notId, notification);

        registerUserActivityReceiver();
        registerAlarmReceiver();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SMTH:KeepAliveWakeLock");
        wakeLock.setReferenceCounted(false); // 防止多次 acquire/release 不匹配

        long now = System.currentTimeMillis();
        lastRefreshTime = now;
        lastUserActiveTime = now;
        lastAlarmTriggerTime = 0;

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        registerSystemReceivers();
        startHybridKeepAlive();
        heartbeatHandler.post(heartbeatRunnable);

        Log.d(TAG, "KeepAliveService 已创建，启动混合保活机制");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerAlarmReceiver() {
        // 先注销旧的 receiver，防止 START_STICKY 重建时重复注册导致泄漏
        if (alarmReceiver != null) {
            try {
                unregisterReceiver(alarmReceiver);
            } catch (Exception ignored) {}
            alarmReceiver = null;
        }

        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ALARM_ACTION.equals(intent.getAction())) {
                    handleAlarmTrigger();
                }
            }
        };

        IntentFilter filter = new IntentFilter(ALARM_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(alarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(alarmReceiver, filter);
        }
    }

    private void handleAlarmTrigger() {
        long now = System.currentTimeMillis();

        // 防止 Alarm 密集触发（如系统批量投递延迟广播）
        synchronized (LOCK) {
            if (lastAlarmTriggerTime > 0 && (now - lastAlarmTriggerTime) < 60 * 1000L) {
                Log.d(TAG, "Alarm触发过于频繁（距上次 " + (now - lastAlarmTriggerTime) / 1000 + "秒），跳过");
                // 但重新安排一个后续 Alarm
                scheduleAlarmTask(BASE_CHECK_INTERVAL);
                return;
            }
            lastAlarmTriggerTime = now;
        }

        Log.d(TAG, "收到Alarm触发，当前在线用户: " + SMTHApplication.isValidUser());

        if (!SMTHApplication.isValidUser()) {
            Log.d(TAG, "当前无有效用户，跳过保活检查");
            return;
        }

        acquireWakeLock(30 * 1000L);
        try {
            scheduleKeepAliveTask(0);
        } finally {
            releaseWakeLock();
        }
    }

    private void acquireWakeLock(long timeoutMs) {
        if (wakeLock != null && !wakeLock.isHeld()) {
            try {
                wakeLock.acquire(timeoutMs);
            } catch (Exception e) {
                Log.w(TAG, "获取WakeLock失败: " + e.getMessage());
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                Log.w(TAG, "释放WakeLock失败: " + e.getMessage());
            }
        }
    }

    private void startHybridKeepAlive() {
        if (!SMTHApplication.isValidUser()) {
            Log.d(TAG, "非有效用户，不启动保活");
            return;
        }
        int initialDelay = getNextCheckInterval();
        Log.d(TAG, "初始检查延迟: " + initialDelay + "秒");
        scheduleKeepAliveTask(initialDelay);
        scheduleAlarmTask(initialDelay);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerSystemReceivers() {
        // 先注销旧的 receiver，防止 START_STICKY 重建时重复注册导致泄漏
        if (systemReceiver != null) {
            try {
                unregisterReceiver(systemReceiver);
            } catch (Exception ignored) {}
            systemReceiver = null;
        }

        systemReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                Log.d(TAG, "收到系统广播: " + action);

                if (!SMTHApplication.isValidUser()) {
                    Log.d(TAG, "非有效用户，忽略系统广播");
                    return;
                }

                int delay;
                switch (action) {
                    case ConnectivityManager.CONNECTIVITY_ACTION:
                        if (!isNetworkAvailable(context)) {
                            Log.d(TAG, "网络不可用，跳过");
                            return;
                        }
                        delay = 30; // 网络恢复后30秒检查

                        break;
                    case Intent.ACTION_SCREEN_ON:
                        delay = 15; // 屏幕亮起后15秒检查

                        break;
                    case Intent.ACTION_USER_PRESENT:
                        delay = 5;  // 解锁后5秒检查

                        break;
                    case Intent.ACTION_POWER_CONNECTED:
                        delay = 10; // 插电后10秒检查

                        break;
                    default:
                        return;
                }

                scheduleKeepAliveTask(delay);
                scheduleAlarmTask(delay);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(systemReceiver, filter);
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @SuppressLint("SimpleDateFormat")
    private void scheduleAlarmTask(int delaySeconds) {
        // 确保 Alarm 延迟不低于最小间隔（但也不盲目覆盖短延迟）
        int effectiveDelay = Math.max(delaySeconds, 30); // 最少 30 秒

        Intent intent = new Intent(ALARM_ACTION);
        intent.setPackage(getPackageName());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerTime = System.currentTimeMillis() + effectiveDelay * 1000L;

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } catch (SecurityException e) {
            // 部分厂商可能拒绝精确闹钟权限，降级使用普通设置
            Log.w(TAG, "无法设置精确闹钟，使用普通模式: " + e.getMessage());
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.d(TAG, "设置Alarm任务，有效延迟: " + effectiveDelay + "秒，触发时间: " +
                new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(triggerTime)));
    }


    private void registerUserActivityReceiver() {
        // 先注销旧的 receiver，防止 START_STICKY 重建时重复注册导致泄漏
        if (userActivityReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(userActivityReceiver);
            } catch (Exception ignored) {}
            userActivityReceiver = null;
        }

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
        synchronized (LOCK) {
            lastUserActiveTime = currentTime;
            isUserActive = true;
        }

        Log.d(TAG, "检测到用户活跃");

        // 用户活跃但距上次刷新超阈值时才触发
        if (currentTime - lastRefreshTime >= REFRESH_THRESHOLD * 1000L) {
            Log.d(TAG, "用户活跃且距上次刷新超过" + REFRESH_THRESHOLD + "秒，安排延迟刷新");
            scheduleKeepAliveTask(USER_ACTIVE_DELAY);
            scheduleAlarmTask(USER_ACTIVE_DELAY);
        }
    }

    private void forceRefresh() {
        Log.d(TAG, "收到强制刷新请求");
        if (!SMTHApplication.isValidUser()) {
            Log.d(TAG, "非有效用户，忽略强制刷新");
            return;
        }
        scheduleKeepAliveTask(0);
        scheduleAlarmTask(30);
    }

    private static int getNextCheckInterval() {
        long currentTime = System.currentTimeMillis();
        // 最近10分钟内有用户活动 → 放宽间隔；否则缩短间隔
        boolean recentlyActive = (currentTime - lastUserActiveTime) < (10 * 60 * 1000L);
        return recentlyActive ? MAX_CHECK_INTERVAL : MIN_CHECK_INTERVAL;
    }

    private void scheduleKeepAliveTask(int delaySeconds) {
        // 使用 REPLACE 策略：新任务替换旧任务，确保最新调度始终生效
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWork.class)
                .setInitialDelay(Math.max(delaySeconds, 0), TimeUnit.SECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork("keep_alive_task", ExistingWorkPolicy.REPLACE, workRequest);
        Log.d(TAG, "计划WorkManager任务，延迟: " + delaySeconds + "秒");
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
                "SMTH", NotificationManager.IMPORTANCE_LOW); // 低优先级减少打扰
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        channel.setSound(null, null);
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销所有 BroadcastReceiver
        if (userActivityReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(userActivityReceiver);
            } catch (Exception ignored) {}
        }
        if (alarmReceiver != null) {
            try {
                unregisterReceiver(alarmReceiver);
            } catch (Exception ignored) {}
        }
        if (systemReceiver != null) {
            try {
                unregisterReceiver(systemReceiver);
            } catch (Exception ignored) {}
        }
        // 取消 Alarm
        if (alarmManager != null) {
            Intent intent = new Intent(ALARM_ACTION);
            intent.setPackage(getPackageName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
        }
        // 释放 WakeLock
        releaseWakeLock();
        // 取消心跳 Handler
        heartbeatHandler.removeCallbacksAndMessages(null);
        stopForeground(true);
        Log.d(TAG, "KeepAliveService 已销毁");
    }

    public KeepAliveService() {
    }

    // ====================== Worker 内部类 ======================

    public static class MyWork extends Worker {
        public MyWork(Context context, WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            // 快速检查：非有效用户不执行
            if (!SMTHApplication.isValidUser()) {
                Log.d(TAG, "非有效用户，跳过保活任务");
                return Result.success();
            }

            long currentTime;
            long timeSinceLastRefresh;
            int currentCount;

            synchronized (LOCK) {
                if (isRefreshing) {
                    Log.d(TAG, "已有刷新任务在执行，跳过本次");
                    // 安排重试，确保不丢失检查
                    scheduleRetryTask(BASE_CHECK_INTERVAL);
                    return Result.success();
                }
                isRefreshing = true;
                refreshCount++;
                currentCount = refreshCount;
                currentTime = System.currentTimeMillis();
                timeSinceLastRefresh = (currentTime - lastRefreshTime) / 1000;
            }

            try {
                Log.d(TAG, "执行第 " + currentCount + " 次检查: 距上次刷新 " + timeSinceLastRefresh + " 秒");

                if (timeSinceLastRefresh >= REFRESH_THRESHOLD) {
                    Log.d(TAG, "达到刷新阈值(" + REFRESH_THRESHOLD + "秒)，执行保活请求");
                    boolean success = performKeepAlive();

                    if (success) {
                        synchronized (LOCK) {
                            lastRefreshTime = currentTime;
                            failureCount = 0;
                            isUserActive = false;
                        }
                        Log.d(TAG, "保活成功，安排下次检查");
                        scheduleNextTask(getNextCheckInterval());
                    } else {
                        handleFailure();
                    }
                } else {
                    Log.d(TAG, "未达刷新阈值，安排下次检查 (还有 " + (REFRESH_THRESHOLD - timeSinceLastRefresh) + "秒)");
                    scheduleNextTask(getNextCheckInterval());
                }
            } catch (Exception e) {
                Log.e(TAG, "保活任务异常: " + e.getMessage(), e);
                handleFailure();
            } finally {
                synchronized (LOCK) {
                    isRefreshing = false;
                }
            }

            return Result.success();
        }

        /**
         * 执行保活 HTTP 请求，验证响应内容判断会话是否有效
         */
        private boolean performKeepAlive() {
            SMTHHelper helper;
            try {
                helper = SMTHHelper.getInstance();
            } catch (Exception e) {
                Log.e(TAG, "获取SMTHHelper失败: " + e.getMessage());
                return false;
            }

            if (helper.wService == null) {
                Log.e(TAG, "wService 未初始化");
                return false;
            }

            try {
                Log.d(TAG, "发送保活请求: GET /nForum/board/NewExpress?ajax");

                retrofit2.Call<String> call = helper.wService.keepAlive();

                Response<String> response = call.execute();

                if (!response.isSuccessful()) {
                    Log.w(TAG, "保活请求失败: HTTP " + response.code() + " " + response.message());
                    // 4xx 客户端错误（如 401/403）通常表示会话失效
                    if (response.code() >= 400 && response.code() < 500) {
                        handleSessionExpired();
                    }
                    return false;
                }

                String body = response.body();
                if (body == null) {
                    Log.w(TAG, "保活响应 body 为空");
                    return false;
                }

                // 验证响应内容：如果包含"未登录"或"guest"提示，说明会话已失效
                if (body.contains("您未登录") || body.contains("请登录后继续操作") ||
                        body.contains("ajax_st") && body.contains("\"ajax_st\":0")) {
                    Log.w(TAG, "保活响应表明会话已失效: " +
                            (body.length() > 200 ? body.substring(0, 200) + "..." : body));
                    handleSessionExpired();
                    return false;
                }

                Log.d(TAG, "保活请求成功，响应长度: " + body.length() + " 字节");
                return true;

            } catch (SocketTimeoutException e) {
                Log.w(TAG, "保活请求超时: " + e.getMessage());
                return false;
            } catch (UnknownHostException e) {
                Log.w(TAG, "DNS解析失败，可能无网络: " + e.getMessage());
                return false;
            } catch (IOException e) {
                Log.w(TAG, "保活 IO 异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                return false;
            } catch (Exception e) {
                Log.e(TAG, "保活未知异常: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
                return false;
            }
        }

        /**
         * 处理会话过期：立即通知主程序
         */
        private void handleSessionExpired() {
            Log.w(TAG, "检测到会话已过期，通知主程序");
            // 重置失败计数，立即触发通知
            synchronized (LOCK) {
                failureCount = MAX_RETRY_COUNT;
            }
            try {
                if (SMTHApplication.mUserStatusReceiver != null) {
                    SMTHApplication.mUserStatusReceiver.onServiceFailed();
                }
            } catch (Exception e) {
                Log.e(TAG, "通知会话过期失败: " + e.getMessage());
            }
        }

        private void handleFailure() {
            int currentFailures;
            int retryDelay;
            synchronized (LOCK) {
                failureCount++;
                currentFailures = failureCount;
            }
            Log.d(TAG, "保活失败计数: " + currentFailures + "/" + MAX_RETRY_COUNT);

            synchronized (LOCK) {
                if (failureCount >= MAX_RETRY_COUNT) {
                    Log.w(TAG, "连续失败 " + MAX_RETRY_COUNT + " 次，通知主程序");
                    failureCount = 0;
                    retryDelay = BASE_CHECK_INTERVAL;
                } else {
                    // 指数退避重试: 45s, 90s, 135s 但不超过 BASE_CHECK_INTERVAL
                    retryDelay = RETRY_INTERVAL * failureCount;
                    retryDelay = Math.min(retryDelay, BASE_CHECK_INTERVAL);
                }
            }

            if (retryDelay == BASE_CHECK_INTERVAL && currentFailures >= MAX_RETRY_COUNT) {
                try {
                    if (SMTHApplication.mUserStatusReceiver != null) {
                        SMTHApplication.mUserStatusReceiver.onServiceFailed();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "通知失败异常: " + e.getMessage());
                }
            }

            Log.d(TAG, (currentFailures >= MAX_RETRY_COUNT ? "重置" : "指数退避重试") + "，延迟: " + retryDelay + "秒");
            scheduleNextTask(retryDelay);
        }

        private void scheduleNextTask(int delayInSeconds) {
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWork.class)
                    .setInitialDelay(delayInSeconds, TimeUnit.SECONDS)
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .build();
            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("keep_alive_next_task", ExistingWorkPolicy.REPLACE, workRequest);
        }

        private void scheduleRetryTask(int delayInSeconds) {
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWork.class)
                    .setInitialDelay(delayInSeconds, TimeUnit.SECONDS)
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .build();
            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("keep_alive_task", ExistingWorkPolicy.REPLACE, workRequest);
        }
    }

    // ====================== 静态辅助方法 ======================

    public static void notifyUserActive(Context context) {
        Intent intent = new Intent(ACTION_USER_ACTIVE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void requestForceRefresh(Context context) {
        Intent intent = new Intent(ACTION_FORCE_REFRESH);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    // ====================== 心跳日志 ======================

    private final Handler heartbeatHandler = new Handler();
    private final Runnable heartbeatRunnable = new Runnable() {
        @SuppressLint("SimpleDateFormat")
        @Override
        public void run() {
            Log.d(TAG, "KeepAliveService 心跳 - " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) +
                    " | 刷新次数: " + refreshCount + " | 失败次数: " + failureCount +
                    " | 有效用户: " + SMTHApplication.isValidUser());
            heartbeatHandler.postDelayed(this, 120000); // 每2分钟打印一次
        }
    };
}
