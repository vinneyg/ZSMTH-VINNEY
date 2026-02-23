package com.zfdang;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.mob.MobSDK;
import com.umeng.commonsdk.UMConfigure;
import com.zfdang.zsmth_android.Settings;
import com.zfdang.zsmth_android.helpers.GEODatabase;
import com.zfdang.zsmth_android.models.Post;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import com.zfdang.zsmth_android.newsmth.UserStatus;
import com.zfdang.zsmth_android.services.ScreenMonitorService;
import com.zfdang.zsmth_android.services.UserStatusReceiver;
import com.zfdang.zsmth_android.workers.ScreenMonitorWorker;
import okhttp3.OkHttpClient;
import androidx.multidex.MultiDex;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zfdang on 2016-3-18.
 */
public class SMTHApplication extends Application {
    // http://blog.csdn.net/lieren666/article/details/7598288
    // Android Application的作用
    private static final String TAG = "SMTHApplication";
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    // 服务监控相关
    private final Handler serviceMonitorHandler = new Handler();
    private static final long SERVICE_MONITOR_INTERVAL = 300000; // 5分钟检查一次

    public static String App_Title_Prefix = "";
    public static final String READ_MODE = "Read_Mode";
    public static final String FROM_BOARD = "From_Board";
    public static final String FROM_BOARD_HOT = "FROM_HOTTOPICS";
    public static final String FROM_BOARD_BOARD = "FROM_BOARDTOPICS";
    public static final String ATTACHMENT_URLS = "ATTACHMENT_URLS";
    public static final String ATTACHMENT_CURRENT_POS = "ATTACHMENT_CURRENT_POS";
    public static final String QUERY_USER_INFO = "QUERY_USER_ID";
    public static final String BOARD_OBJECT = "BOARD_OBJECT";
    public static final String TOPIC_OBJECT = "TOPIC_OBJECT";
    public static final String MAIL_OBJECT = "MAIL_OBJECT";

    // MaintainUserStatusService to UserStatusReceiver, to onNewIntent
    public static final String SERVICE_NOTIFICATION_MESSAGE = "SERVICE_NOTIFICATION_MESSAGE";

    public static final String COMPOSE_POST_CONTEXT = "Compose_Post_Context";

    public static final String NOTIFICATION_NEW_MAIL = "你有新邮件!";
    public static final String NOTIFICATION_NEW_AT = "你有新@!";
    public static final String NOTIFICATION_NEW_REPLY = "你有新回复!";
    public static final String NOTIFICATION_NEW_LIKE = "你有新Like!";
    public static final String NOTIFICATION_LOGIN_LOST = "登录已过期！请重新登录...";

    public static final int INTERVAL_TO_CHECK_MESSAGE = 2; // 2 minutes for interval to check messages
    public static UserStatusReceiver mUserStatusReceiver = null;

    public static List<String> ReadTopicLists = new ArrayList<>();
    public static Post ReadPostFirst = null;
    public static boolean ReadRec = false;
    public static String ReadMode2 = "2";
    public static String ReadMode1 = "1";
    public static String ReadMode0 = "0";
    public static String ReadBoard1 = "版块(空)";
    public static String ReadBoard2 = "版块(空)";
    public static String ReadBoard3 = "版块(空)";
    public static String ReadBoardEng1 = null;
    public static String ReadBoardEng2 = null;
    public static String ReadBoardEng3 = null;
    public static int ReadBoardCount = 0;
    public static  int deletionCount = 0;
    public static boolean bNewFavoriteBoard = false;
    public static boolean bNewMailSent= false;
    public static boolean bNewPost= false;
    public static boolean bNightModeChange= false;
    public static boolean bNewMailInNotification = false;
    public static boolean bServerKick = false;
    public static String getWebAddress()
    {
        return Settings.getInstance().getWebAddr();
    }

    // IP database
    @SuppressLint("StaticFieldLeak")
    public static GEODatabase geoDB;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    // current logined user
    public static UserStatus activeUser;
    public static String displayedUserId = "guest";
    public static boolean isValidUser() {
        return activeUser != null && !activeUser.getId().equalsIgnoreCase("guest");
    }

    public void onCreate() {
        super.onCreate();

        SMTHApplication.context = getApplicationContext();

        // 启动全局屏幕监听服务（使用延迟启动避免后台限制）
        startScreenMonitorService();

        // 启动服务监控
        startServiceMonitoring();

        // init IP lookup database
        geoDB = new GEODatabase(this);

        // init shareSDK
        MobSDK.init(this);

        // init umeng SDK
        UMConfigure.init(this, "56e8c05567e58e0a9e0011cc", "UMENG_CHANNEL", UMConfigure.DEVICE_TYPE_PHONE, null);

        // init Fresco
        OkHttpClient httpClient = SMTHHelper.getInstance().mHttpClient;

        ImagePipelineConfig.Builder configBuilder = OkHttpImagePipelineConfigFactory
                .newBuilder(this, httpClient);

        configBuilder.setMainDiskCacheConfig(
                DiskCacheConfig.newBuilder(this)
                        .setBaseDirectoryPath(getCacheDir())
                        .setBaseDirectoryName("fresco_cache")
                        .setMaxCacheSize(50L * ByteConstants.MB)  // 设置最大缓存大小
                        .setMaxCacheSizeOnLowDiskSpace(10L * ByteConstants.MB)  // 低磁盘空间时的最大缓存
                        .setMaxCacheSizeOnVeryLowDiskSpace(5L * ByteConstants.MB)  // 极低磁盘空间时的最大缓存
                        .build()
        );

        ImagePipelineConfig config = configBuilder.build();

        Fresco.initialize(context, config);

        boolean bNightMode = Settings.getInstance().isNightMode();
        if (bNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

    }

    private void startScreenMonitorService() {
        // 延迟启动，避免在应用启动过程中触发后台限制
        new Handler().postDelayed(() -> {
            try {
                Intent monitorIntent = new Intent(this, ScreenMonitorService.class);
                startService(monitorIntent);
                Log.d(TAG, "延迟启动屏幕监听服务成功");
            } catch (Exception e) {
                Log.e(TAG, "延迟启动屏幕监听服务失败: " + e.getMessage());
                // 如果还是失败，使用 WorkManager 作为备选方案
                OneTimeWorkRequest screenMonitorWork = new OneTimeWorkRequest.Builder(ScreenMonitorWorker.class)
                        .build();
                WorkManager.getInstance(this).enqueue(screenMonitorWork);
                Log.d(TAG, "已使用 WorkManager 启动屏幕监听服务");
            }
        }, 3000); // 延迟3秒启动
    }

    private void startServiceMonitoring() {
        // 立即执行一次检查
        serviceMonitorHandler.post(this::checkAndRestartServices);

        // 设置定期检查
        serviceMonitorHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAndRestartServices();
                // 继续定期检查
                serviceMonitorHandler.postDelayed(this, SERVICE_MONITOR_INTERVAL);
            }
        }, SERVICE_MONITOR_INTERVAL);

        Log.d(TAG, "服务监控已启动，检查间隔: " + (SERVICE_MONITOR_INTERVAL / 1000 / 60) + "分钟");
    }

    private void checkAndRestartServices() {
        Log.d(TAG, "检查服务状态...");

        // 检查并重启屏幕监听服务
        if (isValidUser()) {
            try {
                Intent monitorIntent = new Intent(this, ScreenMonitorService.class);
                startService(monitorIntent);
                Log.d(TAG, "监控重启屏幕监听服务");
            } catch (Exception e) {
                Log.e(TAG, "监控重启服务失败: " + e.getMessage());
            }
        }

        // 可以在这里添加其他服务的检查和重启逻辑
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // 清理监控handler
        serviceMonitorHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "应用终止，服务监控已停止");
    }

    public static Context getAppContext() {
        return SMTHApplication.context;
    }

}
