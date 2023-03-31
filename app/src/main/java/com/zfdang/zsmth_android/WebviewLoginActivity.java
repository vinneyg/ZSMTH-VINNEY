package com.zfdang.zsmth_android;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.zfdang.SMTHApplication;

public class WebviewLoginActivity extends SMTHBaseActivity {

    private WebView mWebView;
    //private String url = "https://www.newsmth.net/";
 //   private String url = "https://m.mysmth.net/index";
    //private String url = "https://m.newsmth.net/index";


    private final String SMTH_WWW_URL = SMTHApplication.getWebAddress();

    private String username;
    private String password;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_login);

        // get username & password
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
        username = extras.getString(LoginActivity.USERNAME);
        password = extras.getString(LoginActivity.PASSWORD);
        }

        mWebView = (WebView) findViewById(R.id.webview_login);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setGeolocationEnabled(false);
        mWebView.getSettings().setSaveFormData(false);
        mWebView.getSettings().setSavePassword(false);
        mWebView.getSettings().setJavaScriptEnabled(true); ///------- 设置javascript 可用

        // https://stackoverflow.com/questions/9602124/enable-horizontal-scrolling-in-a-webview
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
//        mWebView.setInitialScale(120);
//        mWebView.getSettings().setTextZoom(200);

        //支持屏幕缩放
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

        String url = SMTH_WWW_URL;
        if(SMTH_WWW_URL.contains("newsmth"))
            url= "https://m.newsmth.net/index";
        else if(SMTH_WWW_URL.contains("mysmth"))
            url= "https://m.mysmth.net/index";
        mWebView.loadUrl(url);
    }
}