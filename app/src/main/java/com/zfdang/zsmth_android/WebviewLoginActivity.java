package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.NewToast;

public class WebviewLoginActivity extends SMTHBaseActivity {

    //private String url = "https://www.newsmth.net/";
    //private String url = "https://m.mysmth.net/index";
    //private String url = "https://m.newsmth.net/index";
    private final String SMTH_WWW_URL = SMTHApplication.getWebAddress();
    private String username;
    private String password;

    Activity activity;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_login);

        activity = this;

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.requestFocus();
        }

        // get username & password
        if (getIntent() != null && getIntent().getExtras() != null) {
            username = getIntent().getStringExtra(LoginActivity.USERNAME);
            password = getIntent().getStringExtra(LoginActivity.PASSWORD);
        }

        WebView mWebView = findViewById(R.id.webview_login);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setGeolocationEnabled(false);
        mWebView.getSettings().setJavaScriptEnabled(true);

        // https://stackoverflow.com/questions/9602124/enable-horizontal-scrolling-in-a-webview
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);

        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);

        mWebView.setWebChromeClient(new WebChromeClient()
        {
            @Override
            public void onProgressChanged(WebView view, int newProgress)
            {
                super.onProgressChanged(view, newProgress);
                view.requestFocus();
            }
        });
        mWebView.setWebViewClient(new WebviewLoginClient(this, username, password));

        mWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void showHTML(String html) {
                runOnUiThread(() -> {
                    if (html.contains("您的用户名并不存在，或者您的密码错误")) {
                        runOnUiThread(() -> {
                            Intent resultIntent = new Intent();
                            activity.setResult(Activity.RESULT_CANCELED, resultIntent);
                            activity.finish();
                            //Toast.makeText(WebviewLoginActivity.this, "您的用户名并不存在，\n或者您的密码错误!", Toast.LENGTH_LONG).show();
                            NewToast.makeText(WebviewLoginActivity.this, "您的用户名并不存在，\n或者您的密码错误!", Toast.LENGTH_LONG);
                        });
                    } else if (html.contains("登陆成功")){
                        // 登录成功
                        runOnUiThread(() -> {
                            Intent resultIntent = new Intent();
                            activity.setResult(Activity.RESULT_OK, resultIntent);
                            activity.finish();
                        });
                    }
                });
            }
        }, "HtmlViewer");

        String url = SMTH_WWW_URL;
        if (SMTH_WWW_URL.contains("login"))
            url= "https://www.newsmth.net/nForum/login";
        else if(SMTH_WWW_URL.contains("newsmth"))
            url= "https://m.newsmth.net/index";
        else if(SMTH_WWW_URL.contains("mysmth"))
            url= "https://m.mysmth.net/index";
        mWebView.loadUrl(url);
    }
}