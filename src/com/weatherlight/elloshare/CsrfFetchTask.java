package com.weatherlight.elloshare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieManager;

public class CsrfFetchTask extends AsyncTask<Void, Void, Void> {

  private static final String TAG = "CsrfFetchTask";
  private Context context;
  private final ProgressDialog dialog;
  private String cookie;

  public CsrfFetchTask(Context ctx, String ck) {
    super();
    context = ctx;
    cookie = ck;
    dialog = new ProgressDialog(context);
  }

  // can use UI thread here
  @Override protected void onPreExecute() {
    this.dialog.setMessage("Getting CSRF Token...");
    this.dialog.setCancelable(false);
    this.dialog.show();
  }

  @Override protected Void doInBackground(Void... arg0) {
    HttpsURLConnection conn = null;
    try {
      URL url = new URL("https://ello.co/friends/");
      conn = (HttpsURLConnection)url.openConnection();
      // Use a GET method.
      conn.setRequestMethod("GET");
      // Allow Inputs
      conn.setDoInput(true);
      // conn.setDoOutput(true);
      // Don't use a cached copy.
      conn.setUseCaches(false);
      conn.setRequestProperty("cookie", cookie);
      conn.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
      // conn.setRequestProperty("accept-encoding", "gzip,deflate");
      conn.setRequestProperty("accept-language", "en-US,en;q=0.8");
      conn.setRequestProperty("cache-control", "no-cache");

      int errorCode = conn.getResponseCode();
      Log.i(TAG, "Response code is: " + errorCode);
      String ello_cookie = conn.getHeaderField("Set-cookie");
      ello_cookie = ello_cookie.substring(0, ello_cookie.indexOf(';'));
      Log.i(TAG, "Set-cookie: " + ello_cookie);
      context.getSharedPreferences("ello_data", Context.MODE_PRIVATE).edit().putString("cookie", ello_cookie).commit();

      Document doc = Jsoup.parse(conn.getInputStream(), null, "https://ello.co/friends/");

      String html = doc.html();
      FileOutputStream fos = new FileOutputStream(new File("/sdcard/out.html"));
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
      bw.write(html);
      bw.flush();
      bw.close();
      fos.flush();
      fos.close();

      Elements metaElems = doc.select("meta");
      for(Element elem : metaElems) {
        if(elem.hasAttr("name")) {
          if("csrf-token".equals(elem.attr("name"))) {
            String result = elem.attr("content");
            context.getSharedPreferences("ello_data", Context.MODE_PRIVATE).edit().putString("csrf", result).commit();
            Log.i(TAG, "CSRF token is " + result);
          }
        }
      }

    } catch(Exception e) {
      Log.e(TAG, "Error in http connection ", e);
    }

    return null;
  }

  @Override protected void onPostExecute(Void result) {

    if(this.dialog.isShowing()) {
      this.dialog.dismiss();
    }
    CookieManager cookieManager = CookieManager.getInstance();
    String cookie = cookieManager.getCookie("https://ello.co/enter");
  }
}