package com.samknows.ui2.activity;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.R;
import com.samknows.measurement.activity.SamKnowsBaseActivity;

import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

public class ActivityAbout extends SamKnowsBaseActivity {
  // *** ACTIVITY LIFECYCLE *** //
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);
    setUpResources();

    String versionName = "";
    try {
      versionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
    } catch (NameNotFoundException e) {
      SKLogger.sAssert(getClass(), false);
    }

    TextView tv = (TextView) findViewById(R.id.version);
    tv.setText(getString(R.string.version) + " " + versionName);
  }

  // *** CUSTOM METHODS *** //
  private void setUpResources() {
    // Initialise fonts
    Typeface typeface_Roboto_Light = Typeface.createFromAsset(getAssets(), "fonts/roboto_light.ttf");
    Typeface typeface_Roboto_Thin = Typeface.createFromAsset(getAssets(), "fonts/roboto_thin.ttf");

    // Assign fonts
    ((TextView) findViewById(R.id.activity_about_label_download)).setTypeface(typeface_Roboto_Light);
    ((TextView) findViewById(R.id.activity_about_label_upload)).setTypeface(typeface_Roboto_Light);
    ((TextView) findViewById(R.id.activity_about_label_latency)).setTypeface(typeface_Roboto_Light);
    ((TextView) findViewById(R.id.activity_about_label_packet_loss)).setTypeface(typeface_Roboto_Light);
    ((TextView) findViewById(R.id.activity_about_label_jitter)).setTypeface(typeface_Roboto_Light);

    ((TextView) findViewById(R.id.activity_about_description_download)).setTypeface(typeface_Roboto_Thin);
    ((TextView) findViewById(R.id.activity_about_description_upload)).setTypeface(typeface_Roboto_Thin);
    ((TextView) findViewById(R.id.activity_about_description_latency)).setTypeface(typeface_Roboto_Thin);
    ((TextView) findViewById(R.id.activity_about_description_packet_loss)).setTypeface(typeface_Roboto_Thin);
    ((TextView) findViewById(R.id.activity_about_description_jitter)).setTypeface(typeface_Roboto_Thin);
  }
}
