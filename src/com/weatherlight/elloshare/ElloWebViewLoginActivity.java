package com.weatherlight.elloshare;

import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.internal.XWalkCookieManager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;

public class ElloWebViewLoginActivity extends Activity {

  private static final String TAG = "ElloWebViewLoginActivity";
  private XWalkCookieManager cm;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ello_web_view_login);
    cm = new XWalkCookieManager();
    cm.setAcceptCookie(true);
  }

  @Override protected void onResume() {
    super.onResume();

    XWalkView wv = (XWalkView)findViewById(R.id.loginWebView);
    wv.setResourceClient(new ElloLoginWebViewClient(wv));
    wv.load("https://ello.co/enter", null);
    CookieManager.getInstance().setAcceptCookie(true);

  }

  private class ElloCsrfScraper implements ValueCallback<String> {

    @Override public void onReceiveValue(String value) {
      Log.i(TAG, "CSRF scrape returns: " + value);
      value = value.replace("\"", "");
      ElloWebViewLoginActivity.this.getSharedPreferences("ello_data", Context.MODE_PRIVATE).edit()
          .putString("csrf", value).commit();
      Log.i(TAG, "Login worked.  Closing activity.");
      ElloWebViewLoginActivity.this.finish();
    }

  }

  private class ElloCookieScraper implements ValueCallback<String> {

    @Override public void onReceiveValue(String value) {
      Log.i(TAG, "Cookie scrape returns: " + value);
      ElloWebViewLoginActivity.this.getSharedPreferences("ello_data", Context.MODE_PRIVATE).edit()
          .putString("cookie", value).commit();
    }

  }

  private class ElloLoginWebViewClient extends XWalkResourceClient {

    public ElloLoginWebViewClient(XWalkView arg0) {
      super(arg0);
    }

    @Override public void onLoadFinished(XWalkView view, String url) {
      Log.i(TAG, "Page finished loading");
      if(url.endsWith("ello.co/friends")) {
        // view.evaluateJavascript(script, callback)
        String cookie = cm.getCookie("https://ello.co/friends/");
        Log.i(TAG, "Cookie manager returns: " + cookie);
        ElloWebViewLoginActivity.this.getSharedPreferences("ello_data", Context.MODE_PRIVATE).edit()
            .putString("cookie", cookie).commit();

        view.evaluateJavascript(
            "javascript:eval(\"var metaTags=document.getElementsByTagName(\\\"meta\\\");var fbAppIdContent = \\\"\\\";for (var i = 0; i < metaTags.length; i++) {if (metaTags[i].getAttribute(\\\"name\\\") == \\\"csrf-token\\\") {fbAppIdContent = metaTags[i].getAttribute(\\\"content\\\"); break; } }\");",
            new ElloCsrfScraper());
      }
    }

    @Override public boolean shouldOverrideUrlLoading(XWalkView view, String url) {
      return false;
    }

  }
}
