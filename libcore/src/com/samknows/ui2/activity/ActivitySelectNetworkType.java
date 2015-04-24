package com.samknows.ui2.activity;

import android.R.string;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.R;
import com.samknows.libcore.SKTypeface;

/**
 * This activity is responsible for the select network activity. It's started from different places in the application with startActivityForResult.
 * The returned values are 0,1 or 2 for All, WiFi and Mobile.
 * <p/>
 * All rights reserved SamKnows
 *
 * @author pablo@samknows.com
 */


public class ActivitySelectNetworkType extends Activity {
  // *** VARIABLES *** //
  // UI elements
  private RelativeLayout layout_rl_main;                  // Main layout
  private LinearLayout layout_ll_network_wifi, layout_ll_network_mobile;  // Buttons representing the "WiFi" and "Mobile" options
  private TextView tv_network_wifi, tv_network_mobile;          // Text views representing the "WiFi" and "Mobile" options
  private Button button_network_all;                    // Button representing the "All" option
  private Typeface typeface_Roboto_Light;                    // Type face


  // *** ACTIVITY LIFECYCLE *** //
  // Called when the activity is starting.
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_select_network_type);

    // Bind resources and set up them
    setUpResources();
  }

  @Override
  protected void onResume() {
    super.onResume();

    View view = findViewById(android.R.id.content);
    SKTypeface.sChangeChildrenToDefaultFontTypeface(view);

    // Set up fonts
    tv_network_wifi.setTypeface(typeface_Roboto_Light);
    tv_network_mobile.setTypeface(typeface_Roboto_Light);
    button_network_all.setTypeface(typeface_Roboto_Light);
  }

  // *** CUSTOM METHODS *** //

  /**
   * Create, bind and set up the resources
   */
  private void setUpResources() {
    layout_rl_main = (RelativeLayout) findViewById(R.id.activity_select_network_type_relativelayout_main);
    layout_ll_network_wifi = (LinearLayout) findViewById(R.id.ll_select_network_wifi);
    layout_ll_network_mobile = (LinearLayout) findViewById(R.id.ll_select_network_mobile);
    button_network_all = (Button) findViewById(R.id.button_select_network_all);
    tv_network_wifi = (TextView) findViewById(R.id.tv_select_network_wifi);
    tv_network_mobile = (TextView) findViewById(R.id.tv_select_network_mobile);

    // Initialise fonts
    typeface_Roboto_Light = SKTypeface.sGetTypefaceWithPathInAssets("fonts/roboto_light.ttf");

    // Switch taking actions depending on the fragment this activity was called from
    switch (getIntent().getIntExtra("currentFragment", 1)) {
      // Case we this activity was called from Archived Results fragment
      case 1:
        // Set the background of the activity layout
        layout_rl_main.setBackgroundResource(R.drawable.background_gradient_nonmain_with_border);

        // Recover the last network type selected
        switch (getSharedPreferences(getString(R.string.sharedPreferencesIdentifier), Context.MODE_PRIVATE).getInt("networkTypeArchivedTests", 0)) {
          case 0:
            // Case "All"
            button_network_all.setSelected(true);
            break;
          case 1:
            // Case "WiFi"
            layout_ll_network_wifi.setSelected(true);
            break;
          case 2:
            // Case "Mobile"
            layout_ll_network_mobile.setSelected(true);
            break;
          default:
            SKLogger.sAssert(getClass(), false);
            break;
        }
        break;
      // Case this activity was called from Summary fragment
      case 2:
        // Set the background of the activity layout
        layout_rl_main.setBackgroundResource(R.drawable.background_gradient_main_with_border);

        // Recover the last network type selected
        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferencesIdentifier), Context.MODE_PRIVATE);
        int networkTypeFromPreferences = prefs.getInt("networkTypeSummary", 0);
        switch (networkTypeFromPreferences) {
          case 0:
            // Case "All"
            button_network_all.setSelected(true);
            break;
          case 1:
            // Case "WiFi"
            layout_ll_network_wifi.setSelected(true);
            break;
          case 2:
            // Case "Mobile"
            layout_ll_network_mobile.setSelected(true);
            break;
          default:
            SKLogger.sAssert(getClass(), false);
            break;
        }
        break;

      // Case default
      default:
        SKLogger.sAssert(getClass(), false);
        break;
    }

    // Set TICKS, if appropriate.
    setButtonTicks();

    // Button listener when "All" button is pressed
    button_network_all.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent_network_type_result = new Intent();      // Create the intent to return back
        intent_network_type_result.putExtra("networkType", 0);    // Set as extra the network type selected

        setResult(0, intent_network_type_result);          // Set the result code and the data to propagate back to the originating fragment

        finish();                          // Finish this activity
      }
    });

    // Button listener when WiFi button is pressed
    layout_ll_network_wifi.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent_network_type_result = new Intent();      // Create the intent to return back
        intent_network_type_result.putExtra("networkType", 1);    // Set as extra the network type selected

        setResult(0, intent_network_type_result);          // Set the result code and the data to propagate back to the originating fragment

        finish();                          // Finish this activity
      }
    });

    // Button listener when Mobile button is pressed
    layout_ll_network_mobile.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent_network_type_result = new Intent();      // Create the intent to return back
        intent_network_type_result.putExtra("networkType", 2);    // Set as extra the network type selected

        setResult(0, intent_network_type_result);          // Set the result code and the data to propagate back to the originating fragment

        finish();                          // Finish this activity
      }
    });

  }

  // Set TICKS, if appropriate.
  private void setButtonTicks() {
    // Set TICKS, if appropriate.
    button_network_all.setText(getString(R.string.network_type_all)
        + (button_network_all.isSelected() ? " \u2713" : ""));
    tv_network_wifi.setText(getString(R.string.network_type_wifi)
        + (layout_ll_network_wifi.isSelected() ? " \u2713" : ""));
    tv_network_mobile.setText(getString(R.string.network_type_mobile)
        + (layout_ll_network_mobile.isSelected() ? " \u2713" : ""));
  }
}
