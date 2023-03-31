package com.zfdang.zsmth_android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.StringUtils;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import com.zfdang.zsmth_android.newsmth.UserInfo;
import com.zfdang.zsmth_android.newsmth.UserStatus;

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

  static final int LOGIN_ACTIVITY_REQUEST_CODE = 9528;  // The request code
  static final String USERNAME = "USERNAME";
  static final String PASSWORD = "PASSWORD";

  private final String TAG = "LoginActivity";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    // these two variables should be loaded from preference
    Settings setting = Settings.getInstance();
    String username = setting.getUsername();
    String password = setting.getPassword();
    boolean saveinfo = setting.isSaveInfo();

    m_userNameEditText = (EditText) findViewById(R.id.username_edit);
    m_userNameEditText.setText(username);
    m_passwordEditText = (EditText) findViewById(R.id.password_edit);
    m_passwordEditText.setText(password);

    mSaveInfo = (CheckBox) findViewById(R.id.save_info);
    mSaveInfo.setChecked(saveinfo);

    TextView registerLink = (TextView) findViewById(R.id.register_link);
    registerLink.setMovementMethod(LinkMovementMethod.getInstance());

    TextView asmHelpLink = findViewById(R.id.asm_help_link);
    asmHelpLink.setMovementMethod(LinkMovementMethod.getInstance());

    Button ubutton = findViewById(R.id.signin_button);
    ubutton.setOnClickListener(this);

    // enable back button in the title barT
    ActionBar bar = getSupportActionBar();
    if (bar != null) {
      bar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override public void onClick(View view) {
    if (view.getId() == R.id.signin_button) {
      // verify username & password
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
        // There was an error; don't attempt login and focus the first
        // form field with an error.
        focusView.requestFocus();
        Toast.makeText(SMTHApplication.getAppContext(), "请输入用户名/密码！", Toast.LENGTH_SHORT).show();
      } else {
        // use two methods for login: with verification, or simple login
        if (Settings.getInstance().isLoginWithVerification()) {
          // login with verification
          // save info if selected
          boolean saveinfo = mSaveInfo.isChecked();
          Settings.getInstance().setSaveInfo(saveinfo);

          if (saveinfo) {
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
          startActivityForResult(intent, LOGIN_ACTIVITY_REQUEST_CODE);
        } else {
          // simple login
          Settings.getInstance().setSaveInfo(mSaveInfo.isChecked());
          Settings.getInstance().setLastLoginSuccess(false);
          attemptLoginFromWWW(username, password);
        }
      }
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d(TAG, "receive login result" + requestCode);
    if (requestCode == LOGIN_ACTIVITY_REQUEST_CODE) {
      Log.d(TAG, "receive login result"+ resultCode);
      if (resultCode == RESULT_OK) {
        //Toast.makeText(getApplicationContext(), "登录成功!", Toast.LENGTH_SHORT).show();
        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
      }
    }
  }

  @Override public void onStart() {
    super.onStart();
  }

  @Override public void onStop() {
    super.onStop();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
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
    helper.mCookieJar.clear();
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
                switch (ajaxResponse.getAjax_st()) {
                  case AjaxResponse.AJAX_RESULT_OK:
                    Toast.makeText(getApplicationContext(), "登录成功!", Toast.LENGTH_SHORT).show();

                    // save username & password
                    Settings.getInstance().setUsername(username);
                    Settings.getInstance().setPassword(password);
                    Settings.getInstance().setLastLoginSuccess(true);

                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                    break;
                  default:
                    Toast.makeText(SMTHApplication.getAppContext(), ajaxResponse.toString(), Toast.LENGTH_LONG).show();
                    break;
                }
              }

              @Override public void onError(@NonNull Throwable e) {
                dismissProgress();
                Toast.makeText(SMTHApplication.getAppContext(), "登录失败!\n" + e.toString(), Toast.LENGTH_LONG).show();
              }

              @Override public void onComplete() {

              }
            });
  }

}

