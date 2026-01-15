package com.zfdang.zsmth_android.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
//import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
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
    private final static int interval = 300; // 300秒
    private final static int retry = 10; // 刷新失败后重试的时间间隔，单位：秒
    private static int i = 0;
    private static int count = 0;           // 网络访问出错的次数


    private static long lastRefreshTime = 0;

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

        scheduleKeepAliveTask();
    }

    private void scheduleKeepAliveTask() {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWork.class)
                .setInitialDelay(interval, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // 服务被杀死后尝试重启
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
        //Log.d("KeepAliveService", "onDestroy");
        stopForeground(true);
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
            if (!shouldRefresh()) {
                Log.d("KeepAliveService", "跳过本次刷新，未到刷新时间");
                scheduleNextTask(interval); // 继续等待剩余时间
                return Result.success();
            }

            ++i;
            Log.d("KeepAliveService", "保持用户在线服务计数: " + i);
            if (i > Integer.MAX_VALUE - 1000) {
                i = 0;
            }

            try {
                SMTHHelper.getInstance().wService.keepAlive().execute();

                lastRefreshTime = System.currentTimeMillis();
                count = 0;
                Log.d("KeepAliveService", "保活请求发送成功，下次刷新: " + interval + "秒后");
                scheduleNextTask(interval); // 正常刷新间隔
            } catch (IOException e) {
                ++count;
                Log.e("KeepAliveService", "与服务器通讯失败", e);
                Log.d("KeepAliveService", "与服务器通讯失败计数: " + count);

                // 事不过三，超过三次与水木的通信失败，就停止服务
                if (count > 3) {
                    Log.d("KeepAliveService", "连续失败超过3次，停止服务");
                    SMTHApplication.mUserStatusReceiver.onServiceFailed();
                } else {
                    Log.d("KeepAliveService", "重试: " + count + "，延迟: " + retry + "秒");
                    scheduleNextTask(retry); // 失败后的重试间隔
                }
            }
            return Result.success();
        }


        private boolean shouldRefresh() {
            long currentTime = System.currentTimeMillis();
            return lastRefreshTime == 0 || (currentTime - lastRefreshTime >= interval * 1000L);
        }

        void scheduleNextTask(int delayInSeconds) {
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWork.class)
                    .setInitialDelay(delayInSeconds, TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);
        }
    }
}
