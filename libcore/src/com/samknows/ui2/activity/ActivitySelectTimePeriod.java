package com.samknows.ui2.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.samknows.libcore.R;

/**
 * This activity is responsible for the select network activity. Is used in different places in the application and returns a value.
 * * The value returned is 0,1 or 2 for All, WiFi and Mobile.
 * <p/>
 * All rights reserved SamKnows
 *
 * @author pablo@samknows.com
 */


public class ActivitySelectTimePeriod extends Activity {
  // *** VARIABLES *** //
  // UI elements
  private RelativeLayout layout_rl_main;                                // Main layout
  private Button button_time_period_1_year, button_time_period_3_months,                // Buttons representing each time frame
      button_time_period_1_month, button_time_period_1_week, button_time_period_1_day;
  private Typeface typeface_Roboto_Light;                                  // Type face


  // *** ACTIVITY LIFECYCLE *** //
  // Called when the activity is starting.
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_select_time_period);

    // Bind resources and set up them
    setUpResources();
  }

  // *** CUSTOM METHODS *** //

  /**
   * Bind the resources of the layout with the objects in this class and set up them
   */
  private void setUpResources() {
    // Buttons representing the time periods
    button_time_period_1_year = (Button) findViewById(R.id.button_select_time_period_1_year);
    button_time_period_3_months = (Button) findViewById(R.id.button_select_time_period_3_months);
    button_time_period_1_month = (Button) findViewById(R.id.button_select_time_period_1_month);
    button_time_period_1_week = (Button) findViewById(R.id.button_select_time_period_1_week);
    button_time_period_1_day = (Button) findViewById(R.id.button_select_time_period_1_day);

    // Main layout
    layout_rl_main = (RelativeLayout) findViewById(R.id.rl_activity_select_time_period_main);

    // Initialise fonts
    typeface_Roboto_Light = Typeface.createFromAsset(getAssets(), "fonts/roboto_light.ttf");

    // Set up fonts
    button_time_period_1_year.setTypeface(typeface_Roboto_Light);
    button_time_period_3_months.setTypeface(typeface_Roboto_Light);
    button_time_period_1_month.setTypeface(typeface_Roboto_Light);
    button_time_period_1_week.setTypeface(typeface_Roboto_Light);
    button_time_period_1_day.setTypeface(typeface_Roboto_Light);

    // Set up listener for "1 Day" button
    button_time_period_1_day.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent_time_period_result = new Intent();    // Create the intent
        intent_time_period_result.putExtra("timePeriod", 0);  // Put the extra with the time period code

        setResult(0, intent_time_period_result);        // Set the result for the startActivityForResult with the code and data

        finish();                        // Finish this activity
      }
    });

    // Set up listener for "1 Week" button
    button_time_period_1_week.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent_time_period_result = new Intent();    // Create the intent
        intent_time_period_result.putExtra("timePeriod", 1);  // Put the extra with the time period code

        setResult(0, intent_time_period_result);        // Set the result for the startActivityForResult with the code and data

        finish();                        // Finish this activity
      }
    });

    // Set up listener for "1 Month" button
    button_time_period_1_month.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent_time_period_result = new Intent();    // Create the intent
        intent_time_period_result.putExtra("timePeriod", 2);  // Put the extra with the time period code

        setResult(0, intent_time_period_result);        // Set the result for the startActivityForResult with the code and data

        finish();                        // Finish this activity
      }
    });

    // Set up listener for "3 Months" button
    button_time_period_3_months.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent_time_period_result = new Intent();    // Create the intent
        intent_time_period_result.putExtra("timePeriod", 3);  // Put the extra with the time period code

        setResult(0, intent_time_period_result);        // Set the result for the startActivityForResult with the code and data

        finish();                        // Finish this activity
      }
    });

    // Set up listener for "1 Year" button
    button_time_period_1_year.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent_time_period_result = new Intent();    // Create the intent
        intent_time_period_result.putExtra("timePeriod", 4);  // Put the extra with the time period code

        setResult(0, intent_time_period_result);        // Set the result for the startActivityForResult with the code and data

        finish();                        // Finish this activity
      }
    });


    // Switch taking actions depending on the fragment this activity was called from
    switch (getIntent().getIntExtra("currentFragment", 2)) {
      // Case the fragment we came from is Archived Results fragment
      case 0:
        // Set the background of the activity layout
        layout_rl_main.setBackgroundResource(R.drawable.background_gradient_nonmain_with_border);
        break;

      // Case the fragment we came from is Summary fragment
      case 2:
        // Set the background of the activity layout
        layout_rl_main.setBackgroundResource(R.drawable.background_gradient_main_with_border);

        // Recover the last network type selected
        switch (getSharedPreferences(getString(R.string.sharedPreferencesIdentifier), Context.MODE_PRIVATE).getInt("timePeriodSummary", 1)) {
          // Case time period is 1 day
          case 0:
            button_time_period_1_day.setSelected(true);
            break;
          // Case time period is 1 week
          case 1:
            button_time_period_1_week.setSelected(true);
            break;
          // Case time period is 1 month
          case 2:
            button_time_period_1_month.setSelected(true);
            break;
          // Case time period is 3 months
          case 3:
            button_time_period_3_months.setSelected(true);
            break;
          // Case time period is 1 year
          case 4:
            button_time_period_1_year.setSelected(true);
            break;
          // Case default is 1 week
          default:
            button_time_period_1_week.setSelected(true);
            break;
        }
        break;

      // Case default
      default:
        break;
    }

    // Set TICKS, if appropriate.
    setButtonTicks();
  }

  // Set TICKS, if appropriate.
  private void setButtonTicks() {
    button_time_period_1_day.setText(getString(R.string.time_period_1_day)
        + (button_time_period_1_day.isSelected() ? " \u2713" : ""));
    button_time_period_1_week.setText(getString(R.string.time_period_1_week)
        + (button_time_period_1_week.isSelected() ? " \u2713" : ""));
    button_time_period_1_month.setText(getString(R.string.time_period_1_month)
        + (button_time_period_1_month.isSelected() ? " \u2713" : ""));
    button_time_period_3_months.setText(getString(R.string.time_period_3_months)
        + (button_time_period_3_months.isSelected() ? " \u2713" : ""));
    button_time_period_1_year.setText(getString(R.string.time_period_1_year)
        + (button_time_period_1_year.isSelected() ? " \u2713" : ""));
  }
}