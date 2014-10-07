package com.weatherlight.elloshare;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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
  private ProgressDialog dialog;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ello_web_view_login);
    cm = CookieManager.getInstance();
    cm.setAcceptCookie(true);
    dialog = new ProgressDialog(this);
  }

  @Override protected void onResume() {
    super.onResume();

    WebView wv = (WebView)findViewById(R.id.loginWebView);
    wv.setWebViewClient(new ElloLoginWebViewClient());
    wv.setWebChromeClient(new ElloCsrfScraper());
    wv.getSettings().setJavaScriptEnabled(true);
    wv.loadUrl("https://ello.co/enter");
    CookieManager.getInstance().setAcceptCookie(true);

  }

  private class ElloCsrfScraper extends WebChromeClient {

    @Override public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
      String value = message;
      Log.i(TAG, "CSRF scrape returns: " + value);
      value = value.replace("\"", "");
      ElloWebViewLoginActivity.this.getSharedPreferences("ello_data", Context.MODE_PRIVATE).edit()
          .putString("csrf", value).commit();
      Log.i(TAG, "Login worked.  Closing activity.");
      dialog.hide();
      ElloWebViewLoginActivity.this.finish();
      return true;
    }

  }

  private class ElloLoginWebViewClient extends WebViewClient {

    @Override public void onPageFinished(WebView view, String url) {
      Log.i(TAG, "Page finished loading");
      if(url.endsWith("ello.co/friends")) {
        String cookie = cm.getCookie("https://ello.co/friends/");
        Log.i(TAG, "Cookie manager returns: " + cookie);
        ElloWebViewLoginActivity.this.getSharedPreferences("ello_data", Context.MODE_PRIVATE).edit()
            .putString("cookie", cookie).commit();
        view.loadUrl("javascript:alert(eval(\"var metaTags=document.getElementsByTagName(\\\"meta\\\");var fbAppIdContent = \\\"\\\";for (var i = 0; i < metaTags.length; i++) {if (metaTags[i].getAttribute(\\\"name\\\") == \\\"csrf-token\\\") {fbAppIdContent = metaTags[i].getAttribute(\\\"content\\\"); break; } }\"));");
      }
    }

    @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if(url.endsWith("ello.co/friends")) {
        dialog.setMessage("Logging in and getting CSRF Token...");
        dialog.setCancelable(false);
        dialog.show();

      }
      return false;
    }

  }
}
