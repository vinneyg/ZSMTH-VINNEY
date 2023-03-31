package com.zfdang.zsmth_android;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.zfdang.SMTHApplication;

import java.net.URL;

// login chains:
// http://m.mysmth.net/index
//   ==> POST: https://m.mysmth.net/user/login
//     ==> 302 location: http://m.mysmth.net/index?m=0108
public class WebviewLoginClient extends WebViewClient {

    private static final String TAG = "WebviewLoginClient";
    private String username;
    private String password;

    private  String SMTH_WWW_URL = SMTHApplication.getWebAddress();

    Activity activity;

    public WebviewLoginClient(Activity activity, String username, String password) {
        this.activity = activity;
        this.username = username;
        this.password = password;
    }
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//        Log.d(TAG, "shouldOverrideUrlLoading" + url);
        if (request.getUrl().toString().startsWith(SMTH_WWW_URL)) {
        //if (request.getUrl().toString().startsWith("https://www.newsmth.net")) {
        //if (request.getUrl().toString().startsWith("https://www.newsmth.net/nforum")) {
            //     if (url.startsWith("https://www.mysmth.net/nforum")) {
            Intent resultIntent = new Intent();
            activity.setResult(Activity.RESULT_OK, resultIntent);
            activity.finish();
        }
        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if(request.getUrl().toString().contains("ads")) {
            return new WebResourceResponse("text/javascript", "UTF-8", null);
        }
        return null;
    }

    public void onPageFinished(WebView view, String url) {
//        Log.d(TAG, "onPageFinished" + url);
        if (url.equals("https://m.newsmth.net/index")) {
        //    if (url.equals("https://www.mysmth.net")) {
            // login page, input id and passwd automatically
            final String js = "javascript: " +
                    "var ids = document.getElementsByName('id');" +
                    "ids[0].value = '" + this.username + "';" +
                    "var passwds = document.getElementsByName('passwd');" +
                    "passwds[0].value = '" + this.password + "';" +
                     "document.getElementById('TencentCaptcha').click();";

                view.evaluateJavascript(js, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                    }
                });
        }
        super.onPageFinished(view, url);
    }
}
