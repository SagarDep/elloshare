package com.weatherlight.elloshare;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.entity.ContentType;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntityBuilder;

public class ShareTask extends AsyncTask<Void, Void, Void> {
  private Context context;
  private Uri fileUri;
  private final ProgressDialog dialog;
  private static final String TAG = "ShareTask";

  public ShareTask(Context ctx, Uri file) {
    super();
    context = ctx;
    fileUri = file;
    dialog = new ProgressDialog(context);
  }

  // can use UI thread here
  @Override protected void onPreExecute() {
    this.dialog.setMessage("Loading...");
    this.dialog.setCancelable(false);
    this.dialog.show();
  }

  @Override protected Void doInBackground(Void... arg0) {

    try {
      // First, get the direct upload metadata
      JSONObject metadata = getUploadMetaData();

      String uploadUrl = metadata.getString("endpoint");

      // Build up the multipart POST to Amazon S3
      MultipartEntityBuilder meb = MultipartEntityBuilder.create();
      String key = metadata.getString("prefix") + "/" + getFilenameFromUri(fileUri);
      meb.addTextBody("key", key);

      String accessKey = metadata.getString("access_key");
      meb.addTextBody("AWSAccessKeyId", accessKey);

      meb.addTextBody("acl", "public-read");

      meb.addTextBody("success_action_status", "201");

      String policy = metadata.getString("policy");
      meb.addTextBody("policy", policy);

      String signature = metadata.getString("signature");
      meb.addTextBody("signature", signature);

      meb.addTextBody("Content-Type", context.getContentResolver().getType(fileUri));

      // Build up the post with the image reference in it...

      InputStream is = context.getContentResolver().openInputStream(fileUri);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int bytesRead = 0;
      while(bytesRead >= 0) {
        byte[] buffer = new byte[1024];
        bytesRead = is.read(buffer);
        if(bytesRead >= 0) baos.write(buffer, 0, bytesRead);
      }

      meb.addBinaryBody("file", baos.toByteArray(), ContentType.create(context.getContentResolver().getType(fileUri)),
          getFilenameFromUri(fileUri));

      String amazonResponse = doMultipartPost(uploadUrl, meb.build());

      Log.i(TAG, "Amazon upload response: " + amazonResponse);
      // Done with the file now
      is.close();

      meb = MultipartEntityBuilder.create();

      JSONArray ja = new JSONArray();
      JSONObject post = new JSONObject();
      JSONObject data = new JSONObject();

      data.put("url", uploadUrl + "/" + key);
      data.put("via", "direct");
      data.put("alt", getFilenameFromUri(fileUri));

      post.put("kind", "image");
      post.put("data", data);
      ja.put(post);

      meb.addTextBody("unsanitized_body", ja.toString());
      HttpEntity requestBody = meb.build();

      // Now make the post to Ello

      doMultipartPost("https://ello.co/api/v1/posts.json", requestBody);
    } catch(Exception ex) {
      Log.e(TAG, "Error sharing image", ex);
    }

    return null;
  }

  private String getFilenameFromUri(Uri contentUri) {
    Cursor cursor = null;
    try {
      String[] proj = { MediaStore.Images.Media.DISPLAY_NAME };
      cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
      cursor.moveToFirst();
      return cursor.getString(column_index);
    } finally {
      if(cursor != null) {
        cursor.close();
      }
    }
  }

  private String doMultipartPost(String urlString, HttpEntity requestBody) throws Exception {
    HttpURLConnection conn = null;
    DataOutputStream dos = null;
    DataInputStream inStream = null;
    // ------------------ CLIENT REQUEST
    URL url = new URL(urlString);

    conn = (HttpURLConnection)url.openConnection();
    // Use a post method.
    conn.setRequestMethod("POST");
    // Allow Inputs
    conn.setDoInput(true);
    // Allow Outputs
    conn.setDoOutput(true);
    // Don't use a cached copy.
    conn.setUseCaches(false);

    Log.i(TAG, "Using cookie and token: \n" + getCookie() + "\n\n" + getCsrfToken());
    conn.setRequestProperty("Cookie", getCookie());
    conn.setRequestProperty("x-csrf-token", getCsrfToken());

    if(requestBody.getContentLength() <= 0) {
      throw new Exception("Can't have nonexistant content-length");
    } else {
      conn.setFixedLengthStreamingMode((int)requestBody.getContentLength());
    }
    Log.i(TAG, "Content length: " + requestBody.getContentLength());
    conn.setRequestProperty("Content-Type", requestBody.getContentType().getValue());
    dos = new DataOutputStream(conn.getOutputStream());
    // Log.i(TAG, "Posting: ");
    // Log.i(TAG, "========================");
    // Log.i(TAG, requestBody.toString());
    // Log.i(TAG, "========================");
    requestBody.writeTo(dos);
    dos.flush();
    dos.close();
    // ------------------ read the SERVER RESPONSE
    int errorCode = conn.getResponseCode();
    Log.i(TAG, "Response code: " + errorCode);
    if(errorCode >= 400) {
      inStream = new DataInputStream(conn.getErrorStream());
    } else {
      inStream = new DataInputStream(conn.getInputStream());
    }
    StringBuilder sb = new StringBuilder();
    String str;
    BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
    while((str = br.readLine()) != null) {
      Log.i(TAG, "Server Response " + str);
      sb.append(str);
    }
    inStream.close();
    return str;

  }

  private JSONObject getUploadMetaData() throws Exception {
    HttpURLConnection conn = null;
    URL metaDataUrl = new URL("https://ello.co/api/v1/direct_upload_metadata");
    conn = (HttpURLConnection)metaDataUrl.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Cookie", getCookie());
    conn.setRequestProperty("accept", "application/json; charset=utf-8");
    int responseCode = conn.getResponseCode();
    Log.i(TAG, "Upload metadata GET response code: " + responseCode);
    if(responseCode >= 200 && responseCode < 300) {
      JSONObject retval = new JSONObject(readStream(conn.getInputStream()));
      return retval;
    } else {
      throw new Exception("Failed to get upload meta data");
    }

  }

  private String getCookie() {
    return context.getSharedPreferences("ello_data", Context.MODE_PRIVATE).getString("cookie", "");
  }

  private String getCsrfToken() {
    return context.getSharedPreferences("ello_data", Context.MODE_PRIVATE).getString("csrf", "");
  }

  private String readStream(InputStream in) {
    BufferedReader reader = null;
    StringBuilder builder = new StringBuilder();
    try {
      reader = new BufferedReader(new InputStreamReader(in));
      String line = "";
      while((line = reader.readLine()) != null) {
        builder.append(line);
      }
    } catch(IOException e) {
      e.printStackTrace();
    } finally {
      if(reader != null) {
        try {
          reader.close();
        } catch(IOException e) {
          e.printStackTrace();
        }
      }
    }
    return builder.toString();
  }

  @Override protected void onPostExecute(Void result) {

    if(this.dialog.isShowing()) {
      this.dialog.dismiss();
    }
  }
}