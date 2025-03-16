package com.zfdang.zsmth_android;

import android.app.Activity;
import android.content.Intent;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

// login chains:
// http://m.mysmth.net/index
//   ==> POST: https://m.mysmth.net/user/login
//     ==> 302 location: http://m.mysmth.net/index?m=0108
public class WebviewLoginClient extends WebViewClient {

    //private static final String TAG = "WebviewLoginClient";
    private final String username;
    private final String password;

    Activity activity;

    public WebviewLoginClient(Activity activity, String username, String password) {
        this.activity = activity;
        this.username = username;
        this.password = password;
    }
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
       //Log.d(TAG, "shouldOverrideUrlLoading" + request.getUrl().toString());
        if (request.getUrl().toString().startsWith("https://m.newsmth.net/index?m=")||request.getUrl().toString().startsWith("https://m.mysmth.net/index?m=")) {
            Intent resultIntent = new Intent();
            activity.setResult(Activity.RESULT_OK, resultIntent);
            activity.finish();
        }
        return false;
    }

    @Override
    public WebResourceResponse    shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request.getUrl().toString().contains("ads")) {
            return new WebResourceResponse("text/javascript", "UTF-8", null);
        }
        return null;
    }


    public void onPageFinished(WebView view, String url) {
        // Log.d(TAG, "onPageFinished" + url);
        if (url.equals("https://m.newsmth.net/index")||url.equals("https://m.mysmth.net/index")) {
            // login page, input id and passwd automatically
            final String js = "javascript: " +
                    "var ids = document.getElementsByName('id');" +
                    "ids[0].value = '" + this.username + "';" +
                    "var passwds = document.getElementsByName('passwd');" +
                    "passwds[0].value = '" + this.password + "';" +
                    "var checkbox = document.getElementsByName('save');" +
                    "checkbox[0].checked = '" + true + "';" +
                    "var captcha = document.getElementById('TencentCaptcha');" +
                    "captcha.style.position = 'relative';" +
                    "captcha.style.top = 'auto';" +
                    "captcha.style.left = 'auto';" +
                    "document.getElementById('TencentCaptcha').click();";
            view.evaluateJavascript(js, s -> {
            });
        }
        super.onPageFinished(view, url);
    }
}