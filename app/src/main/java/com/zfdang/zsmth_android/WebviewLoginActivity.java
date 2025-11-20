package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.FragmentStatusBarUtil;
import com.zfdang.zsmth_android.helpers.NewToast;

public class WebviewLoginActivity extends SMTHBaseActivity {

    //private String url = "https://www.newsmth.net/";
    //private String url = "https://m.mysmth.net/index";
    //private String url = "https://m.newsmth.net/index";
    private final String SMTH_WWW_URL = SMTHApplication.getWebAddress();
    private String username;
    private String password;
    Activity activity;
    private boolean hasShownToast = false;

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

        //Update Cookie Manager
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);

        // https://stackoverflow.com/questions/9602124/enable-horizontal-scrolling-in-a-webview
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);

        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                view.requestFocus();
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, android.webkit.JsResult result) {
                if (message.contains("用户名") || message.contains("密码") || message.contains("错误")) {
                    // 取消alert弹窗
                    result.cancel();

                    runOnUiThread(() -> {
                        if (!hasShownToast) {
                            hasShownToast = true;
                            NewToast.makeText(WebviewLoginActivity.this, "登录失败，请检查用户名和密码", Toast.LENGTH_LONG);
                        }

                        Intent resultIntent = new Intent();
                        activity.setResult(Activity.RESULT_CANCELED, resultIntent);
                        activity.finish();
                    });

                    return true; // 表示已处理该alert
                }

                // 对于其他alert，正常处理
                return super.onJsAlert(view, url, message, result);
            }

        });


        mWebView.setWebViewClient(new WebviewLoginClient(this, username, password) {
            @SuppressLint("WebViewClientOnReceivedSslError")
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // 忽略 SSL 错误
            }
        });

        mWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void showHTML(String html) {
                runOnUiThread(() -> {
                    if (html.contains("您的用户名并不存在，或者您的密码错误")) {
                        Intent resultIntent = new Intent();
                        activity.setResult(Activity.RESULT_CANCELED, resultIntent);
                        activity.finish();
                        //Toast.makeText(WebviewLoginActivity.this, "您的用户名并不存在，\n或者您的密码错误!", Toast.LENGTH_LONG).show();
                        NewToast.makeText(WebviewLoginActivity.this, "您的用户名并不存在，\n或者您的密码错误!", Toast.LENGTH_LONG);
                    } else if (html.contains("登陆成功")||html.contains("登录成功")|| html.contains("退出登录")){
                        mWebView.setVisibility(WebView.GONE);
                        Intent resultIntent = new Intent();
                        activity.setResult(Activity.RESULT_OK, resultIntent);
                        activity.finish();
                    }  else if (html.contains("504 Gateway Time-out")) {
                        // Handle 504 server timeout error
                        NewToast.makeText(WebviewLoginActivity.this, "服务器网关超时，请稍后再试。", Toast.LENGTH_LONG);
                        Intent resultIntent = new Intent();
                        activity.setResult(Activity.RESULT_CANCELED, resultIntent);
                        activity.finish();
                    } else {
                        Log.d("zSMTH-v",html);
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

        if(!Settings.getInstance().isLoginWithVerification()){
            url= "https://www.newsmth.net/index.html";
        }

        mWebView.loadUrl(url);
        FragmentStatusBarUtil.adaptActDarkMode(this, false);
    }

    // === Handle no server response after login request ===
    public void onLoginNoResponse() {
        runOnUiThread(() -> {
            NewToast.makeText(this, "登录请求无响应，请检查网络或重试", Toast.LENGTH_LONG);
            Intent resultIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, resultIntent);
            finish();
        });
    }

}