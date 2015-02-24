package com.samknows.ui2.activity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

import com.samknows.libcore.R;

/**
 * This activity is responsible for the select tests activity.
 * * It stores in shared preferences the tests that were selected
 * <p/>
 * All rights reserved SamKnows
 *
 * @author pablo@samknows.com
 */


public class ActivitySelectTests extends Activity {
  // *** VARIABLES *** //
  // UI elements
  private TextView tv_ok;                                      // Button to confirm the selection
  private Button button_test_download, button_test_upload, button_test_latency_and_packet_loss;  // Buttons representing the tests
  private Typeface typeface_Roboto_Light, typeface_Roboto_Thin, typeface_Roboto_Bold, typeface_Roboto_Regular;  // Type faces to assign to the text

  // *** ACTIVITY LIFECYCLE *** //
  // Called when the activity is starting.
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_select_tests);

    // Bind the resources and set up them
    setUpResources();
  }

  // Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(), for your activity to start interacting with the user.
  @Override
  protected void onResume() {
    super.onResume();
    // Restore the selection to the last selected tests
    restoreTestsState();
  }

  // *** CUSTOM METHODS *** //

  /**
   * Create, bind and set up the resources
   */
  private void setUpResources() {
    // Bind the elements of the UI.
    button_test_download = (Button) findViewById(R.id.button_select_test_download);
    button_test_upload = (Button) findViewById(R.id.button_select_test_upload);
    button_test_latency_and_packet_loss = (Button) findViewById(R.id.button_select_test_latency_loss);
    tv_ok = (TextView) findViewById(R.id.button_select_test_ok);

    // Initialise fonts
    typeface_Roboto_Light = Typeface.createFromAsset(getAssets(), "fonts/roboto_light.ttf");
    typeface_Roboto_Thin = Typeface.createFromAsset(getAssets(), "fonts/roboto_thin.ttf");
    typeface_Roboto_Bold = Typeface.createFromAsset(getAssets(), "fonts/roboto_bold.ttf");
    typeface_Roboto_Regular = Typeface.createFromAsset(getAssets(), "fonts/roboto_regular.ttf");

    // Set the fonts to the UI elements
    button_test_download.setTypeface(typeface_Roboto_Light);
    button_test_upload.setTypeface(typeface_Roboto_Light);
    button_test_latency_and_packet_loss.setTypeface(typeface_Roboto_Light);
    tv_ok.setTypeface(typeface_Roboto_Regular);

    // Listener for the test buttons. Changes between pressed or normal the button state
    OnClickListener select_tests_listener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        // Invert the button selected state
        v.setSelected(!v.isSelected());

        // Set TICKS, if appropriate.
        setButtonTicks();
      }
    };

    // Set the listener to the elements
    button_test_download.setOnClickListener(select_tests_listener);
    button_test_upload.setOnClickListener(select_tests_listener);
    button_test_latency_and_packet_loss.setOnClickListener(select_tests_listener);

    // Set TICKS, if appropriate.
    setButtonTicks();

    // Listener for the confirmation button. The text is showed in bold while pressed
    tv_ok.setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        // True if the event was handled and should not be given further down to other views.
        // If no other child view of this should get the event then return false
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            tv_ok.setTypeface(typeface_Roboto_Bold);  // Set bold font while pressed
            return true;

          case MotionEvent.ACTION_UP:
            tv_ok.setTypeface(typeface_Roboto_Thin);  // Back to the normal font
            saveTestState();            // Save the state of the buttons
            finish();                // Finish the activity
            return false;

          default:
            return true;
        }
      }
    });
  }

  // Set TICKS, if appropriate.
  private void setButtonTicks() {
    button_test_download.setText(getString(R.string.download)
        + (button_test_download.isSelected() ? " \u2713" : ""));
    button_test_upload.setText(getString(R.string.upload)
        + (button_test_upload.isSelected() ? " \u2713" : ""));
    button_test_latency_and_packet_loss.setText(getString(R.string.latency_loss_jitter)
        + (button_test_latency_and_packet_loss.isSelected() ? " \u2713" : ""));
  }

  /**
   * Restore the last state of the tests from shared preferences (whether they were pressed or not the last time) to show the buttons pressed or not
   */
  private void restoreTestsState() {
    SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferencesIdentifier), Context.MODE_PRIVATE);
    // Recover the state of the download button
    button_test_download.setSelected(prefs.getBoolean("downloadTestState", false));

    // Recover the state of the upload button
    button_test_upload.setSelected(prefs.getBoolean("uploadTestState", false));

    // Recover the state of the latency and loss button
    button_test_latency_and_packet_loss.setSelected(prefs.getBoolean("latencyAndLossTestState", false));

    setButtonTicks();
  }

  /**
   * Save the state of the buttons in shared preferences (whether they are pressed or not)
   */
  private void saveTestState() {
    SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferencesIdentifier), Context.MODE_PRIVATE);

    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("downloadTestState", button_test_download.isSelected());            // Save the state of the download button
    editor.putBoolean("uploadTestState", button_test_upload.isSelected());              // Save the state of the upload button
    editor.putBoolean("latencyAndLossTestState", button_test_latency_and_packet_loss.isSelected());  // Save the state of the latency and loss button
    editor.commit();                                      // Commit changes
  }
}