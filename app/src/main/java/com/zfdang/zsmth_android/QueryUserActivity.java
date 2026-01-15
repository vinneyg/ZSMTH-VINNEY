package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.fresco.WrapContentDraweeView;
import com.zfdang.zsmth_android.helpers.FragmentStatusBarUtil;
import com.zfdang.zsmth_android.helpers.KeyboardLess;
import com.zfdang.zsmth_android.helpers.NewToast;
import com.zfdang.zsmth_android.helpers.StringUtils;
import com.zfdang.zsmth_android.models.ComposePostContext;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import com.zfdang.zsmth_android.newsmth.UserInfo;
import com.zfdang.zsmth_android.newsmth.HtmlUserInfo;
import java.io.IOException;
import java.util.Objects;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.Observer;
import okhttp3.ResponseBody;

public class QueryUserActivity extends SMTHBaseActivity {
    //private static final String TAG = "QueryUserActivity";
    private String mUsername;

    private WrapContentDraweeView mImageView;

    private TextView mUserId;
    private TextView mUserNickname;
    private TextView mUserGender;
    private TextView mUserConstellation;
    private TextView mUserQq;
    private TextView mUserHomepage;

    private TextView mUserLevel;
    private TextView mUserTotalpost;
    private TextView mUserLogincount;
    private TextView mUserLife;
    private TextView mUserScore;
    private TextView mUserLastLogintime;
    private TextView mUserLoginip;
    private TextView mUserFirstLogintime;
    private TextView mUserCurrentstatus;

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @SuppressLint("ClickableViewAccessibility")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_user);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String title = getIntent().getStringExtra(SMTHApplication.QUERY_USER_INFO);
        if (title != null) {
            Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        }

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        assignViews();

        Button logoutButton = findViewById(R.id.query_user_logout);

        if (shouldHideLogoutButton(title)) {
            logoutButton.setVisibility(View.VISIBLE);
        } else {
            logoutButton.setVisibility(View.GONE);
        }

        findViewById(R.id.query_user_action_query).setOnClickListener(v -> {
            EditText tv = findViewById(R.id.query_user_input);
            String userid = tv.getText().toString().trim();
            if (!userid.isEmpty()) {
                mUsername = userid;
                tv.setText(userid);
                getSupportActionBar().setTitle(mUsername);
                KeyboardLess.$hide(QueryUserActivity.this, tv);
                tv.clearFocus();
                if (shouldHideLogoutButton(userid)) {
                    logoutButton.setVisibility(View.VISIBLE);
                } else {
                    logoutButton.setVisibility(View.GONE);
                }
                //LoadUserInfo();
                LoadHtmlUserInfo();
            }
        });
        FragmentStatusBarUtil.adaptActDarkMode(this, false);
        // get Board information from launcher
        Intent intent = getIntent();
        String username = intent.getStringExtra(SMTHApplication.QUERY_USER_INFO);
        if (username == null) {
            Log.e("QueryUser", "username is null.");
            return;
        }
        mUsername = username;
        //LoadUserInfo();
        LoadHtmlUserInfo();


        GestureDetector mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, @androidx.annotation.NonNull MotionEvent e2, float velocityX, float velocityY) {
                try {
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    if (Math.abs(diffX) > (Math.abs(diffY) +(float)SWIPE_THRESHOLD/2)){
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            finish();
                            //overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        }
                    }
                } catch (Exception exception) {
                    Log.e(exception.toString(), "onFling");
                }
                return false;
            }
        });

        findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
            mGestureDetector.onTouchEvent(event);
            return true;
        });

    }

    private boolean shouldHideLogoutButton(String userID) {
        return SMTHApplication.activeUser != null && SMTHApplication.activeUser.getId().equalsIgnoreCase(userID);
    }

    private void assignViews() {
        mImageView = findViewById(R.id.imageView);

        mUserId = findViewById(R.id.query_user_id);
        mUserNickname = findViewById(R.id.query_user_nickname);
        mUserGender = findViewById(R.id.query_user_gender);
        mUserConstellation = findViewById(R.id.query_user_constellation);
        mUserQq = findViewById(R.id.query_user_qq);
        mUserHomepage = findViewById(R.id.query_user_homepage);

        mUserLevel = findViewById(R.id.query_user_level);
        mUserTotalpost = findViewById(R.id.query_user_totalpost);
        mUserLogincount = findViewById(R.id.query_user_logincount);
        mUserLife = findViewById(R.id.query_user_life);
        mUserScore = findViewById(R.id.query_user_score);
        mUserFirstLogintime = findViewById(R.id.query_user_first_logintime);
        mUserLastLogintime = findViewById(R.id.query_user_last_logintime);
        mUserLoginip = findViewById(R.id.query_user_loginip);
        mUserCurrentstatus = findViewById(R.id.query_user_currentstatus);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.query_user_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int code = item.getItemId();
        if (code == android.R.id.home) {
            //onBackPressed();
            finish();
            //overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } else if (code == R.id.query_user_action_message) {
            // write mail to current user
            ComposePostContext postContext = new ComposePostContext();
            postContext.setComposingMode(ComposePostContext.MODE_NEW_MAIL_TO_USER);
            postContext.setPostAuthor(mUserId.getText().toString());

            Intent intent = new Intent(this, ComposePostActivity.class);
            intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
            startActivity(intent);
            return true;
        } else if (code == R.id.query_user_action_friend) {
            addFriend(mUserId.getText().toString());
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 处理登出成功的逻辑
     */
    private void handleLogoutSuccess() {
        // 更新应用状态为游客
        if (SMTHApplication.activeUser != null) {
            SMTHApplication.activeUser.setId("guest");
        }

        // 更新设置
        Settings.getInstance().setAutoLogin(false);
        Settings.getInstance().setUserOnline(false);

        // 清理缓存
        SMTHApplication.ReadTopicLists.clear();

        // 发送广播清理缓存
        Intent intent = new Intent("com.zfdang.zsmth_android.PREFERENCE_CLICKED");
        intent.putExtra("preference_key", "setting_fresco_cache");
        sendBroadcast(intent);

        intent = new Intent("com.zfdang.zsmth_android.PREFERENCE_CLICKED");
        intent.putExtra("preference_key", "setting_okhttp3_cache");
        sendBroadcast(intent);

        // 完成当前页面
        finish();

        //NewToast.makeText(QueryUserActivity.this, "登出成功", Toast.LENGTH_SHORT);
    }

    public void addFriend(String userid) {
        SMTHHelper helper = SMTHHelper.getInstance();
        helper.wService.addFriend(userid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AjaxResponse>() {
                    @Override public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override public void onNext(@NonNull AjaxResponse ajaxResponse) {
                        //Toast.makeText(QueryUserActivity.this, ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT).show();
                        NewToast.makeText(QueryUserActivity.this, ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT);
                    }

                    @Override public void onError(@NonNull Throwable e) {
                        //Toast.makeText(QueryUserActivity.this, "增加好友失败!\n", Toast.LENGTH_SHORT).show();
                        NewToast.makeText(QueryUserActivity.this, "增加好友失败!\n", Toast.LENGTH_SHORT);

                    }

                    @Override public void onComplete() {

                    }
                });
    }

    public void onLogoutClick(View view) {
        if (SMTHApplication.activeUser != null) {
            SMTHApplication.activeUser.setId("guest");
        }

        SMTHHelper helper = SMTHHelper.getInstance();
        helper.wService.logout()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<AjaxResponse>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {
                    }

                    @Override
                    public void onNext(@NonNull AjaxResponse ajaxResponse) {
                        if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                            Settings.getInstance().setAutoLogin(false);
                            Settings.getInstance().setUserOnline(false);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        // 检查是否为 HTTP 500 错误
                        if (e instanceof retrofit2.adapter.rxjava2.HttpException) {
                            retrofit2.adapter.rxjava2.HttpException httpEx = (retrofit2.adapter.rxjava2.HttpException) e;
                            if (httpEx.code() == 500) {
                                Log.w("QueryUserActivity", "HTTP 500 error during logout, treating as success");

                                // HTTP 500 视为登出成功
                                handleLogoutSuccess();
                                return;
                            }
                        }

                        // 其他错误情况
                        NewToast.makeText(QueryUserActivity.this, "退出登录失败!\n" + e.getMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onComplete() {
                        // 检查是否已经通过 onError 处理了登出
                        // 如果没有错误，则正常处理登出
                        Settings.getInstance().setAutoLogin(false);
                        Settings.getInstance().setUserOnline(false);
                        SMTHApplication.ReadTopicLists.clear();

                        Intent intent = new Intent("com.zfdang.zsmth_android.PREFERENCE_CLICKED");
                        intent.putExtra("preference_key", "setting_fresco_cache");
                        sendBroadcast(intent);

                        intent = new Intent("com.zfdang.zsmth_android.PREFERENCE_CLICKED");
                        intent.putExtra("preference_key", "setting_okhttp3_cache");
                        sendBroadcast(intent);

                        finish();
                    }
                });
    }



    public void LoadHtmlUserInfo() {
        showProgress("加载用户信息中...");
        SMTHHelper helper = SMTHHelper.getInstance();

        // 使用 ResponseBody 接口以获取原始字节数组
        helper.wService.getRawUserInformationResponseBody(mUsername)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {
                        // 开始获取用户信息
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onNext(@NonNull ResponseBody responseBody) {
                        try {
                            // 获取原始字节数组并进行编码转换
                            byte[] bytes = responseBody.bytes();
                            String decodedResponse = SMTHHelper.DecodeResponseFromWWW(bytes);

                            // 使用 HtmlUserInfo 解析HTML格式的数据
                            HtmlUserInfo userInfo = HtmlUserInfo.parseFromHtml(decodedResponse);

                            if (userInfo != null && userInfo.getId() != null) {
                                // 更新UI组件
                                mUserId.setText(userInfo.getId());
                                mUserNickname.setText(userInfo.getNickname());
                                mUserTotalpost.setText(userInfo.getPostCount());
                                mUserLogincount.setText(userInfo.getLoginCount());
                                mUserScore.setText(userInfo.getScore());
                                mUserLife.setText(userInfo.getLife());
                                mUserLevel.setText(userInfo.getIdentity());
                                mUserLastLogintime.setText(userInfo.getLastLoginTime());
                                mUserLoginip.setText(userInfo.getLastLoginIp());
                                mUserCurrentstatus.setText(userInfo.getStatus());

                                // 设置性别、星座、QQ、主页等字段为空（HTML格式中未提供这些信息）
                                mUserGender.setText("");
                                mUserConstellation.setText("");
                                mUserQq.setText("");
                                mUserHomepage.setText("");

                                // 设置默认头像或空值（HTML格式中未提供头像URL）
                                // 当头像URL为空或无效时显示默认图片
                                mImageView.setImageResource(R.drawable.ic_account_circle_black_48dp);


                            } else {
                                // 用户不存在的情况
                                EditText tv = findViewById(R.id.query_user_input);
                                tv.setText(mUsername + "不存在！");
                                tv.setTextColor(Color.RED);
                            }

                            dismissProgress();
                        } catch (IOException e) {
                            dismissProgress();
                            NewToast.makeText(QueryUserActivity.this, "解析用户信息失败！\n" + e.getMessage(), Toast.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dismissProgress();
                        NewToast.makeText(QueryUserActivity.this, "加载用户信息失败！\n" + e.getMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onComplete() {
                        // 响应获取完成
                    }
                });
    }


    public void LoadUserInfo() {

    showProgress("加载用户信息中...");
    SMTHHelper helper = SMTHHelper.getInstance();
    helper.wService.queryUserInformation(mUsername)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<UserInfo>() {
              @Override public void onSubscribe(@NonNull Disposable disposable) {

              }

              @SuppressLint("SetTextI18n")
              @Override public void onNext(@NonNull UserInfo user) {
                if(user.getId() == null)
                {
                  EditText tv = findViewById(R.id.query_user_input);
                  tv.setText(mUsername+"不存在！");
                  tv.setTextColor(Color.RED);
                  dismissProgress();
                  return;
                }

                mUserId.setText(user.getId());
                mUserNickname.setText(user.getUser_name());
                mUserGender.setText(user.getGender());
                mUserConstellation.setText(user.getAstro());
                mUserQq.setText(user.getQq());
                mUserHomepage.setText(user.getHome_page());

                mUserLevel.setText(user.getLevel());
                mUserTotalpost.setText(user.getPost_count());
                mUserLogincount.setText(user.getLogin_count());
                mUserLife.setText(user.getLifeDesc());
                mUserScore.setText(user.getScore_user());
                mUserFirstLogintime.setText(user.getFirst_login_time());
                mUserLastLogintime.setText(user.getLast_login_time());
                mUserLoginip.setText(StringUtils.lookupIPLocationInProfile(user.getLast_login_ip()));
                mUserCurrentstatus.setText(user.getStatus());

                if (user.getFace_url() != null && user.getFace_url().length() > 10) {
                  mImageView.setImageFromStringURL(user.getFace_url());
                }
                dismissProgress();

              }

              @Override public void onError(@NonNull Throwable e) {
                dismissProgress();
                Log.e("vinney", "onError: " + e.getMessage());
                //Toast.makeText(QueryUserActivity.this, "加载用户信息失败！\n", Toast.LENGTH_SHORT).show();
                NewToast.makeText(QueryUserActivity.this, "加载用户信息失败！\n", Toast.LENGTH_SHORT);
              }

              @Override public void onComplete() {

              }
            });
  }

}
