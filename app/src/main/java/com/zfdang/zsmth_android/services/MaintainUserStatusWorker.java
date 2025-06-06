package com.zfdang.zsmth_android.services;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.Settings;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import com.zfdang.zsmth_android.newsmth.UserInfo;
import com.zfdang.zsmth_android.newsmth.UserStatus;
import java.util.concurrent.TimeUnit;
import io.reactivex.observers.DisposableObserver;
import android.content.BroadcastReceiver;


public class MaintainUserStatusWorker extends Worker {

    public static final String REPEAT = "REPEAT";
    private static final String TAG = "MUSWorker";

    public static final String WORKER_ID=MaintainUserStatusWorker.class.getName();
    private BroadcastReceiver loginReceiver;

    public MaintainUserStatusWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
//        Log.d(TAG, "doWork");
        // do the job here
        doWorkInternal();

        Data myData = getInputData();
        // schedule the next worker
        boolean repeat = myData.getBoolean(MaintainUserStatusWorker.REPEAT, true);
        if (repeat) {
            enqueueNextWorker();
        }

        // return result
        return Result.success();
    }

    private void doWorkInternal() {
        // This describes what will happen when service is triggered
        // process here:
        // 1. get user status
        // 2.1 if it's not guest, go to step 3
        // 2.2 if login-with-guesture-verification is used, go to step 3; otherwise try to login;
        // 2.2.1 if login success, get user status (2.2.1.1) again. go to step 3
        // 2.2.2 if login failed, go to step 3
        // 3. check whether user status == SMTHApplication.activeUser
        // 3.1 if they are same, just return SMTHApplication.activeUser
        // 3.2 if not, get real face URL
        // 4. if user status is a different user, send notification to receiver to update navigationView

        final SMTHHelper helper = SMTHHelper.getInstance();


        //Log.d(TAG, "1.0 get current UserStatus from remote");
        helper.wService.queryActiveUserStatus().map(userStatus -> {
            //Log.d(TAG, "2.0 " + userStatus.toString());
            // create a dummy userStatus if it is null
            if (userStatus == null) {
                userStatus = new UserStatus();
            }


            // now try to login
            final Settings setting = Settings.getInstance();
            String username = setting.getUsername();
            String password = setting.getPassword();
            boolean bSaveInfo = true;
            boolean bLastSuccess = true;
            boolean bUserOnline = true;
            boolean bLoginSuccess = true;

            /*
            Log.d(TAG, "call: 2.2.1 " + String.format(" LastSuccess: %b, Online: %b", bLastSuccess, bUserOnline));
            if (bSaveInfo && bLastSuccess && bUserOnline) {
                Log.d(TAG,username+"--"+password );
                Iterable<Integer> its = helper.wService.login(username, password, "7").map(response -> {
                    if (response.getAjax_st() == 1) {
                        // {"ajax_st":1,"ajax_code":"0005","ajax_msg":"操作成功"}
                        Log.d(TAG,"101");
                        return AjaxResponse.AJAX_RESULT_OK;
                    } else if (response.getAjax_code().equals("0101")) {
                        // {"ajax_st":0,"ajax_code":"0101","ajax_msg":"您的用户名并不存在，或者您的密码错误"}
                        Log.d(TAG,"102");
                        return AjaxResponse.AJAX_RESULT_FAILED;
                    }
                    Log.d(TAG,response.getAjax_msg());
                    Log.d(TAG,"103");
                    return AjaxResponse.AJAX_RESULT_UNKNOWN;
                }).blockingIterable();

                List<Integer> results = MakeList.makeList(its);

                Log.d(TAG, "call: 2.2.2 " + results.size());
                if (results.size() == 1) {
                    int result = results.get(0);
                    if (result == AjaxResponse.AJAX_RESULT_OK) {
                        // set flag, so that we will query user status again
                        Log.d(TAG, "call: 2.2.3. Login success");
                        bLoginSuccess = true;
                    } else if (result == AjaxResponse.AJAX_RESULT_FAILED) {
                        // set flag, so that we will not login again next time
                        //Log.d(TAG, "call: 2.2.4. Login failed");
                        setting.setLastLoginSuccess(false);
                    }
                }
            } // if (bAutoLogin && bLastSuccess)
*/
            // try to find new UserStatus only when login success
            if (bLoginSuccess) {
                Log.d(TAG, "call: " + "2.2.5.1 try to get userstatus again after login action");
                UserStatus stat = SMTHHelper.queryActiveUserStatus().blockingFirst();
                //Log.d(TAG, "call: " + stat.size());
                if (stat == null) {
                    stat = new UserStatus();
                }
                if (stat.getId() == null) {
                    stat.setId("guest");
                }
                Log.d(TAG,stat.getId());
                return stat;
            } else {
                return userStatus;
            }
        }).map(userStatus -> {
            Log.d(TAG, "3.0 call: " + userStatus.toString());
            if (!TextUtils.equals(userStatus.getId(), "guest")) {
                // valid user
                if (SMTHApplication.activeUser != null && TextUtils.equals(userStatus.getId(), SMTHApplication.activeUser.getId())) {
                    // current user is already cached in SMTHApplication
                    Log.d(TAG, "call: " + "3.1 New user is the same with cached user, copy faceURL from local");
                    userStatus.setFace_url(SMTHApplication.activeUser.getFace_url());
                } else {
                    // get correct faceURL
                    Log.d(TAG, "call: " + "3.2 New user is different with cached user, get real face URL from remote");
                    UserInfo userInfo = helper.wService.queryUserInformation(userStatus.getId()).blockingFirst();
                    if (userInfo != null) {
                        userStatus.setFace_url(userInfo.getFace_url());
                    }
                    Settings.getInstance().setUserOnline(true);
                }
            } else {
                Log.d(TAG, "call: 3.3 " + "invalid logined user"+ userStatus.getId());
            }
            return userStatus;
        }).subscribe(new DisposableObserver<UserStatus>() {
            @Override
            public void onNext(UserStatus userStatus) {
                //Log.d(TAG, "4.0 onNext: " + userStatus.toString());
                // cache user if necessary, so we don't have to query User avatar url again in the future
                boolean updateUserIcon = !TextUtils.equals(userStatus.getId(), SMTHApplication.displayedUserId);
                // active user is null, or active user is different with userstatus, update the icon
                // Log.d(TAG, "onNext: " + "4.1 cache userStatus as activeUser");
                SMTHApplication.activeUser = userStatus;

                String message = "";
                if (SMTHApplication.isValidUser()) {
                    // get message if user is valid user
                    message = getNotificationMessage(SMTHApplication.activeUser);
                } else if (updateUserIcon) {
                    // not a valid user, but updateUserIcon is true, means login status has lost
                    message = SMTHApplication.NOTIFICATION_LOGIN_LOST;
                }

                UserStatusReceiver receiver = SMTHApplication.mUserStatusReceiver;
                // send notification: 1. new message 2. new activeUser to update Sidebar status
                if ((updateUserIcon || !message.isEmpty()) && receiver != null) {
                    Log.d(TAG, "4.2 cached user, valid message, valid receiver, send message");
                    Bundle bundle = new Bundle();
                    if (!message.isEmpty()) {
                        bundle.putString(SMTHApplication.SERVICE_NOTIFICATION_MESSAGE, message);
                    }
                    // Here we call send passing a resultCode and the bundle of extras
                    receiver.send(Activity.RESULT_OK, bundle);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: " + Log.getStackTraceString(e));
            }

            @Override
            public void onComplete() {
            }
        });

    }

    public String getNotificationMessage(UserStatus userStatus) {
        String message = "";
        Settings settings = Settings.getInstance();
        if (userStatus.hasNewMail() && settings.isNotificationMail()) {
            message = SMTHApplication.NOTIFICATION_NEW_MAIL + "  ";
        }
        if (userStatus.hasNewLike() && settings.isNotificationLike()) {
            message += SMTHApplication.NOTIFICATION_NEW_LIKE + "  ";
        }
        if (userStatus.hasNewAt() && settings.isNotificationAt()) {
            message += SMTHApplication.NOTIFICATION_NEW_AT + "  ";
        }
        if (userStatus.hasNewReply() && settings.isNotificationReply()) {
            message += SMTHApplication.NOTIFICATION_NEW_REPLY + "  ";
        }
        //Log.d(TAG, message);
        return message;
    }

    private void enqueueNextWorker() {
        Data.Builder inputData = new Data.Builder();
        inputData.putBoolean(MaintainUserStatusWorker.REPEAT, true);
        OneTimeWorkRequest userStatusWorkRequest =
                new OneTimeWorkRequest.Builder(MaintainUserStatusWorker.class)
                        .setInitialDelay(SMTHApplication.INTERVAL_TO_CHECK_MESSAGE, TimeUnit.MINUTES)
                        .setInputData(inputData.build())
                        .build();
    }
}
