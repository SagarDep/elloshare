package com.weatherlight.elloshare;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class ImagePickerActivity extends ActionBarActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_image_picker);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.image_picker, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if(id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void onImagePickerClicked(View v) {
    Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(i, 0);
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(resultCode == Activity.RESULT_OK) {
      Uri selectedImage = data.getData();
      Intent i = new Intent();
      i.setClassName(getPackageName(), ElloShareActivity.class.getCanonicalName());
      i.setAction(Intent.ACTION_SEND);
      i.setType("image/*");
      i.putExtra(Intent.EXTRA_STREAM, selectedImage);
      startActivity(i);
    }
  }
}
