package com.zfdang.zsmth_android;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceError;
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
    private final Handler loginTimeoutHandler;
    private Runnable loginTimeoutRunnable;

    public WebviewLoginClient(Activity activity, String username, String password) {
        this.activity = activity;
        this.username = username;
        this.password = password;
        // Initialize timeout handler
        loginTimeoutHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        //Log.d(TAG, "shouldOverrideUrlLoading" + request.getUrl().toString());
        cancelLoginTimeout();
        /*
        if (request.getUrl().toString().contains("bbslogin1203.php")) {
            return false;
        }
        if (request.getUrl().toString().startsWith("https://m.newsmth.net/index?m=")
                ||request.getUrl().toString().startsWith("https://m.mysmth.net/index?m=")){
            return false;
        }
        */
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
        if (url == null || view == null) return;
        //Log.d(TAG, "onPageFinished" + url);
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
            // === Add login timeout monitoring ===
            // Cancel existing timeout (if any)
            cancelLoginTimeout();
            // Start new timeout: 10 seconds
            loginTimeoutRunnable = () -> {
                // Notify activity of no response
                if (activity instanceof WebviewLoginActivity) {
                    ((WebviewLoginActivity) activity).onLoginNoResponse();
                }
            };
            loginTimeoutHandler.postDelayed(loginTimeoutRunnable, 50000);

        } else  if (url.equals("https://www.newsmth.net/nForum/login")) {
            final String js = "javascript: " +
                    "var ids = document.getElementsByName('id'); if(ids[0]) ids[0].value = '" + this.username + "';" +
                    "var passwds = document.getElementsByName('passwd'); if(passwds[0]) passwds[0].value = '" + this.password + "';" +
                    "var checkbox = document.getElementsByName('CookieDate'); if(checkbox[0]) checkbox[0].checked = true;" +
                    "var captcha = document.getElementById('u_login_submit');" +
                    "if(captcha) {" +
                    "   captcha.style.position = 'relative';" +
                    "   captcha.style.top = 'auto';" +
                    "   captcha.style.left = 'auto';" +
                    "   captcha.click();";
            view.evaluateJavascript(js, s -> {
            });
        } else  if (url.equals("https://m.newsmth.net/user/login")||
                url.equals("https://m.mysmth.net/user/login")||
                url.startsWith("https://m.newsmth.net/index?m=")||
                url.startsWith("https://m.mysmth.net/index?m=")) {
            //view.setVisibility(WebView.GONE);
            final String js = "javascript: " +
                    "setTimeout(function() { window.HtmlViewer.showHTML(document.body.innerHTML); },100);";
            view.evaluateJavascript(js, null);
        } else if (url.startsWith("https://www.newsmth.net/nForum")) {
            final String js = "javascript: " +
                    "setTimeout(function() { window.HtmlViewer.showHTML(document.body.innerHTML); },100);";
            view.evaluateJavascript(js, null);
        } else if(url.equals("https://www.newsmth.net/index.html")){
            final String js = "javascript: " +
                    "setTimeout(function() {" +
                    "  try {" +
                    "    var mainFrame = window.frames['mainFrame'] || window.frames[0];" +
                    "    if (mainFrame && mainFrame.document) {" +
                    "      var frameDoc = mainFrame.document;" +
                    "      var form = frameDoc.form1;" +
                    "      if (form && form.id && form.passwd) {" +
                    "        form.id.value = '" + this.username + "';" +
                    "        form.passwd.value = '" + this.password + "';" +
                    "        var submitBtn = frameDoc.getElementById('submit1');" +
                    "        if (submitBtn) {" +
                    "          submitBtn.click();" +
                    "        }" +
                    "      } else {" +
                    "        console.log('Form elements not found in mainFrame');" +
                    "      }" +
                    "    } else {" +
                    "      console.log('mainFrame or its document not accessible');" +
                    "    }" +
                    "  } catch(e) {" +
                    "    console.log('Auto login error: ' + e);" +
                    "  }" +
                    "}, 100);";

            view.evaluateJavascript(js, s -> {
            });
        }
        super.onPageFinished(view, url);
    }

    // === Helper method to cancel timeout ===
    private void cancelLoginTimeout() {
        if (loginTimeoutRunnable != null) {
            loginTimeoutHandler.removeCallbacks(loginTimeoutRunnable);
            loginTimeoutRunnable = null;
        }
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        // === Cancel timeout on network error ===
        cancelLoginTimeout();
    }
}