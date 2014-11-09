package com.weatherlight.elloshare;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.EditText;
import android.widget.ImageView;


public class ElloShareActivity extends ActionBarActivity implements OnClickListener {

  private static final String TAG = "ElloShareActivity";
  private Uri fileUri = null;
  private boolean rotating = false;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.activity_ello_share);

    CookieSyncManager.createInstance(this);

    // Get intent, action and MIME type
    Intent intent = getIntent();
    String action = intent.getAction();
    String type = intent.getType();

    this.findViewById(R.id.shareButton).setOnClickListener(this);

    if(Intent.ACTION_SEND.equals(action) && type != null) {
      if(type.startsWith("image/")) {
        handleSendImage(intent); // Handle single image being sent
      }
    } else if(Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
      if(type.startsWith("image/")) {
        handleSendMultipleImages(intent); // Handle multiple images being sent
      }
    }
  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    rotating = true;
  }

  @Override protected void onResume() {
    super.onResume();
  }

  void handleSendImage(Intent intent) {
    Uri imageUri = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
    fileUri = imageUri;
    if(imageUri != null) {
      ImageView iv = (ImageView)this.findViewById(R.id.photoImageView);
      Log.i(TAG, "Changing image view to " + imageUri);

      try {
        /*
         * Bitmap d = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri); int nh =
         * (int)(d.getHeight() * (512.0 / d.getWidth())); Bitmap scaled = Bitmap.createScaledBitmap(d, 512, nh, true);
         */

        Bitmap pic = getThumbnailBitmap(imageUri);

        iv.setImageBitmap(pic);
        iv.refreshDrawableState();

      } catch(Exception ex) {
        throw new RuntimeException(ex);
      }

      // Got the thumbnail up. Now, do we have an Ello cookie already? If not, go to the login
      // activity to get one.
      // CookieManager cookieManager = CookieManager.getInstance();
      // String cookie = cookieManager.getCookie("https://ello.co/enter");
      // if(cookie == null) {
      if(!rotating) {
        Intent startIntent = new Intent(this, ElloWebViewLoginActivity.class);
        startActivity(startIntent);
      } else {
        rotating = false;
      }
      // }
    }
  }

  void handleSendMultipleImages(Intent intent) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override public void onClick(View v) {
    CookieManager cookieManager = CookieManager.getInstance();
    String cookie = cookieManager.getCookie("https://ello.co/enter");
    Log.i(TAG, "Starting cookie: " + cookie);
    EditText editBox = (EditText) findViewById(R.id.tag);
    new ShareTask(this, fileUri, editBox.getText().toString()).execute();
  }


  public void toggleTaggingBox(View v) {
    final EditText tagBox = (EditText) findViewById(R.id.tag);

// make the view visible and start the animation
    Animator anim;

    if (tagBox.getAlpha() == 0) {
      anim = ObjectAnimator.ofFloat(tagBox, "alpha", 1f);
    } else {
      anim = ObjectAnimator.ofFloat(tagBox, "alpha", 0f);
      tagBox.getText().clear();
    }
    anim.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        if (tagBox.getAlpha() == 0f) {
          tagBox.setEnabled(false);
        } else {
          tagBox.setEnabled(true);
        }
      }
    });
    anim.start();
  }

  private int getOrientation(Uri photoUri) {
    /* it's on the external media. */
    Cursor cursor = getContentResolver().query(photoUri, new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
        null, null, null);

    if(cursor.getCount() != 1) {
      return -1;
    }

    cursor.moveToFirst();
    return cursor.getInt(0);
  }

  private Bitmap getThumbnailBitmap(Uri uri) {
    String[] proj = { MediaStore.Images.Media._ID };

    CursorLoader cursorLoader = new CursorLoader(this, uri, proj, null, null, null);
    Cursor cursor = cursorLoader.loadInBackground();

    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

    cursor.moveToFirst();
    long imageId = cursor.getLong(column_index);

    Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(), imageId,
        MediaStore.Images.Thumbnails.MINI_KIND, (BitmapFactory.Options)null);

    int orientation = getOrientation(uri);
    Matrix matrix = new Matrix();
    matrix.postRotate(orientation);
    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    return bitmap;
  }
}
