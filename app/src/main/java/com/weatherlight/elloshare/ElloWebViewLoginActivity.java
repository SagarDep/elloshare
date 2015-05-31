package com.weatherlight.elloshare;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ElloWebViewLoginActivity extends Activity {

  private static final String TAG = "ElloWebViewLoginActivity";
  private CookieManager cm;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ello_web_view_login);
    cm = CookieManager.getInstance();
    cm.setAcceptCookie(true);
  }

  @Override protected void onResume() {
    super.onResume();

    WebView wv = (WebView)findViewById(R.id.loginWebView);
    wv.setWebViewClient(new ElloLoginWebViewClient());
    wv.getSettings().setJavaScriptEnabled(true);
    if(wv.getUrl() != null && wv.getUrl().startsWith("https://ello.co")) {
      Log.i(TAG, "URL: " + wv.getUrl());
      wv.reload();
    } else {
      wv.loadUrl("https://ello.co/enter");
    }
    CookieManager.getInstance().setAcceptCookie(true);

  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  private class ElloLoginWebViewClient extends WebViewClient {

    @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if (url.endsWith("ello.co/friends")) {

        String cookie = cm.getCookie("https://ello.co/friends");
        Log.i(TAG, "Cookie manager returns: " + cookie);
        ElloWebViewLoginActivity.this.getSharedPreferences("ello_data", Context.MODE_PRIVATE).edit()
                .putString("cookie", cookie).commit();
        ElloWebViewLoginActivity.this.finish();
        //return true;
      }
        return false;
    }
  }
}
