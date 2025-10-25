package com.zfdang.zsmth_android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.NewToast;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * A login screen that offers login to newsmth forum
 */
public class LoginActivity extends SMTHBaseActivity implements OnClickListener {

    private EditText m_userNameEditText;
    private EditText m_passwordEditText;
    private CheckBox mSaveInfo;

    // these 3 parameters are used by webviewlogin only
    static final int LOGIN_ACTIVITY_REQUEST_CODE = 9528;  // The request code
    static final String USERNAME = "USERNAME";
    static final String PASSWORD = "PASSWORD";

    private final String TAG = "LoginActivity";
    private ActivityResultLauncher<Intent> mActivityLoginResultLauncher;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // these two variables should be loaded from preference
        Settings setting = Settings.getInstance();
        String username = setting.getUsername();
        String password = setting.getPassword();
        boolean saveinfo = setting.isSaveInfo();

        m_userNameEditText = findViewById(R.id.username_edit);
        m_userNameEditText.setText(username);
        m_passwordEditText = findViewById(R.id.password_edit);
        m_passwordEditText.setText(password);

        if (username == null || username.isEmpty()) {
            new Handler().postDelayed(() -> {
                m_userNameEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(m_userNameEditText, InputMethodManager.SHOW_IMPLICIT);
            }, 300);
        }

        mSaveInfo = findViewById(R.id.save_info);
        mSaveInfo.setChecked(saveinfo);

        TextView registerLink =  findViewById(R.id.register_link);
        registerLink.setMovementMethod(LinkMovementMethod.getInstance());

        TextView asmHelpLink = findViewById(R.id.asm_help_link);
        asmHelpLink.setMovementMethod(LinkMovementMethod.getInstance());

        Button ubutton = findViewById(R.id.signin_button);
        ubutton.setOnClickListener(this);

        // enable back button in the title barT

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize the ActivityResultLauncher object.
        mActivityLoginResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK)
                    {
                        Intent resultIntent = new Intent();
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                    } else if(result.getResultCode() == Activity.RESULT_CANCELED)
                    {
                        Intent resultIntent = new Intent();
                        setResult(Activity.RESULT_CANCELED, resultIntent);
                        finish();
                    }
                });
    }

    @Override public void onClick(View view) {
        if (view.getId() == R.id.signin_button) {
            // login with provided username and password
            String username = m_userNameEditText.getText().toString();
            String password = m_passwordEditText.getText().toString();

            View focusView = null;
            // Check for a valid username.
            if (TextUtils.isEmpty(username)) {
                focusView = m_userNameEditText;
            } else if (TextUtils.isEmpty(password)) {
                // Check for a valid password
                focusView = m_passwordEditText;
            }

            if (focusView != null) {
                // There was an error; don't attempt login and focus the first field with an alert
                focusView.requestFocus();
                //Toast.makeText(SMTHApplication.getAppContext(), "请输入用户名/密码！", Toast.LENGTH_SHORT).show();
                NewToast.makeText(SMTHApplication.getAppContext(), "请输入用户名/密码！", Toast.LENGTH_SHORT);
            } else {
                // use two methods for login: with verification, or simple login
                //if(Settings.getInstance().isLoginWithVerification()) {
                    // login with gesture verification
                    // save info if selected
                    boolean saveinfo = mSaveInfo.isChecked();
                    Settings.getInstance().setSaveInfo(saveinfo);

                    if(saveinfo) {
                        // save
                        Settings.getInstance().setUsername(username);
                        Settings.getInstance().setPassword(password);
                    } else {
                        // clean existed
                        Settings.getInstance().setUsername("");
                        Settings.getInstance().setPassword("");
                    }

                    // continue to login with nforum web
                    Intent intent = new Intent(this, WebviewLoginActivity.class);
                    intent.putExtra(USERNAME, username);
                    intent.putExtra(PASSWORD, password);
                    //startActivityForResult(intent, LOGIN_ACTIVITY_REQUEST_CODE);
                    mActivityLoginResultLauncher.launch(intent);
                //}
                /*
                else {
                    // simple login
                    Settings.getInstance().setSaveInfo(mSaveInfo.isChecked());
                    Settings.getInstance().setLastLoginSuccess(false);
                    attemptLoginFromWWW(username, password);
                }
                */
            }
        }
    }

    // login from WWW, then nforum / www / m are all logined
    private void attemptLoginFromWWW(final String username, final String password) {
        // perform the user login attempt.
        showProgress("登录中...");

        Log.d(TAG, "start login now...");
        // use attempt to login, so set userOnline = true
        Settings.getInstance().setUserOnline(true);

        // RxJava & Retrofit: VERY VERY good article
        // http://gank.io/post/560e15be2dca930e00da1083
        SMTHHelper helper = SMTHHelper.getInstance();
        // clear cookies upon login
        final String cookieDays = "2";
        helper.wService.login(username, password, cookieDays)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AjaxResponse>() {
                    @Override public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override public void onNext(@NonNull AjaxResponse ajaxResponse) {
                        dismissProgress();
                        // {"ajax_st":0,"ajax_code":"0101","ajax_msg":"您的用户名并不存在，或者您的密码错误"}
                        // {"ajax_st":0,"ajax_code":"0105","ajax_msg":"请勿频繁登录"}
                        // {"ajax_st":1,"ajax_code":"0005","ajax_msg":"操作成功"}
                        Log.d(TAG, ajaxResponse.toString());
                        if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                            //Toast.makeText(getApplicationContext(), "登录成功!", Toast.LENGTH_SHORT).show();
                            NewToast.makeText(getApplicationContext(), "登录成功!", Toast.LENGTH_SHORT);

                            // save username & password
                            Settings.getInstance().setUsername(username);
                            Settings.getInstance().setPassword(password);
                            Settings.getInstance().setLastLoginSuccess(true);

                            Intent resultIntent = new Intent();
                            setResult(Activity.RESULT_OK, resultIntent);
                            finish();
                        } else {
                            //Toast.makeText(SMTHApplication.getAppContext(), ajaxResponse.toString(), Toast.LENGTH_LONG).show();
                            NewToast.makeText(SMTHApplication.getAppContext(), ajaxResponse.toString(), Toast.LENGTH_LONG);
                        }
                    }

                    @Override public void onError(@NonNull Throwable e) {
                        dismissProgress();
                        //Toast.makeText(SMTHApplication.getAppContext(), "登录失败!\n" + e.toString(), Toast.LENGTH_LONG).show();
                        NewToast.makeText(SMTHApplication.getAppContext(), "登录失败!\n" + e.toString(), Toast.LENGTH_LONG);
                    }

                    @Override public void onComplete() {

                    }
                });
    }
    @Override public void onStart() {
        super.onStart();
    }

    @Override public void onStop() {
        super.onStop();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            //onBackPressed();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}