package com.zfdang.zsmth_android.workers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.zfdang.zsmth_android.services.ScreenMonitorService;

public class ScreenMonitorWorker extends Worker {
    private static final String TAG = "ScreenMonitorWorker";

    public ScreenMonitorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "开始启动屏幕监听服务");
            
            // 启动屏幕监听服务
            Intent serviceIntent = new Intent(getApplicationContext(), ScreenMonitorService.class);
            getApplicationContext().startService(serviceIntent);
            
            Log.d(TAG, "屏幕监听服务启动成功");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "启动屏幕监听服务失败: " + e.getMessage());
            return Result.failure();
        }
    }
}
