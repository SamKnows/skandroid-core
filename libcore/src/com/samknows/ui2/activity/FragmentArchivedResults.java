package com.samknows.ui2.activity;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samknows.libcore.SKCommon;
import com.samknows.libcore.SKLogger;
import com.samknows.libcore.R;
import com.samknows.libcore.SKTypeface;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.SKApplication.eNetworkTypeResults;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.StorageTestResult.*;

/**
 * This fragment is responsible for:
 * * Show a list of archive test
 * * Show details of each archived test
 * * Filter results of the list by network type
 * <p/>
 * All rights reserved by SamKnows
 *
 * @author pablo@samknows.com
 */


public class FragmentArchivedResults extends Fragment {
  // *** CONSTANTS *** //
  private final static String C_TAG_FRAGMENT_ARCHIVED_TEST = "Fragment Archived Tests";    // Tag for this fragment

  // *** VARIABLES *** //
  private int originPositionX, originPositionY;  // Stores the original position of one view
  private int clickedPosition = -1;
  private boolean isListviewHidden = false;    // If true, the list view is hidden, if false, the list view is shown.
  private boolean asyncTask_RefreshListViewData_Running = false;

  // UI elements
  private RelativeLayout rl_main;
  private LinearLayout ll_passive_metrics, ll_passive_metrics_divider_1, ll_passive_metrics_divider_2, ll_passive_metrics_divider_location;
  private ListView lv_archived_results;
  private View listViewRow, clickedView;
  private TextView pm_tv_header_label_sim_and_network_operators, pm_tv_header_label_signal, pm_tv_header_label_device, pm_tv_header_label_location;
  private TextView
      tv_label_sim_operator, tv_label_sim_operator_code, tv_label_network_operator, tv_label_network_operator_code, tv_label_roaming_status,
      tv_label_cell_tower_ID, tv_label_cell_tower_area_location_code, tv_label_signal_strength, tv_label_bearer,
      tv_label_manufacturer, tv_label_model, tv_label_OS, tv_label_OS_version, tv_label_phone_type, tv_label_latitude, tv_label_longitude,
      tv_label_accuracy, tv_label_provider,
      tv_result_sim_operator, tv_result_sim_operator_code, tv_result_network_operator, tv_result_network_operator_code, tv_result_roaming_status,
      tv_result_cell_tower_ID, tv_result_cell_tower_area_location_code, tv_result_signal_strength, tv_result_bearer,
      tv_result_manufacturer, tv_result_model, tv_result_OS, tv_result_OS_version, tv_result_phone_type, tv_result_latitude, tv_result_longitude,
      tv_result_accuracy, tv_result_provider,
      tv_warning_no_results_yet;

  private View tv_group_wifi_ssid;
  private View tv_group_municipality;
  private View tv_group_country_name;
  private View tv_group_wlan_carrier;
  private TextView tv_result_wifi_ssid;
  private TextView tv_result_municipality;
  private TextView tv_result_country_name;
  private TextView tv_result_wlan_carrier;

  private Typeface typeface_Roboto_Light, typeface_Roboto_Thin;
  private MenuItem menu_Item_Network_Type_Filter, menu_Item_Refresh_Spinner, menu_Item_Share_Result;
  private TextView publicIp;
  private TextView submissionId;
  private TextView networkType;
  private TextView target;

  // Complex variables
  // Hosts the archived results
  private ArrayList<TestResult> aList_ArchivedResults = new ArrayList<TestResult>();
  // Temporary list to avoid modified the actual list in background thread which causes IllegalStateException
  private ArrayList<TestResult> aList_TemporaryArchivedTests = new ArrayList<TestResult>();
  private ArrayAdapter<TestResult> adapter_Archived_Results;

  private View mArchiveFilterButton = null;
  private View mArchiveShareButton = null;

  // *** FRAGMENT LIFECYCLE ** //
  // Called to have the fragment instantiate its user interface view.
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Create the view
    View view = inflater.inflate(R.layout.fragment_archive_results, container, false);

    // Bind the UI elements with the objects in this class and set up them
    setUpResources(view);

    // Inflate the layout for this fragment with the view created
    return view;
  }

  // Called when the Fragment is no longer resumed. This is generally tied to Activity.onPause of the containing Activity's lifecycle
  @Override
  public void onResume() {
    super.onResume();

    // Register back button handler...
    registerBackButtonHandler();

    // Register the local broadcast receiver listener to receive messages when the UI data needs to be refreshed.
    Context context = SKApplication.getAppInstance().getApplicationContext();
    LocalBroadcastManager.getInstance(context).registerReceiver(updateScreenMessageReceiver, new IntentFilter("refreshUIMessage"));

    View view = getView();
    SKTypeface.sChangeChildrenToDefaultFontTypeface(view);

//    // Set fonts
//    // Header labels fonts
//    tv_warning_no_results_yet.setTypeface(typeface_Roboto_Light);
//    pm_tv_header_label_sim_and_network_operators.setTypeface(typeface_Roboto_Thin);
//    pm_tv_header_label_signal.setTypeface(typeface_Roboto_Thin);
//    pm_tv_header_label_device.setTypeface(typeface_Roboto_Thin);
//    pm_tv_header_label_location.setTypeface(typeface_Roboto_Thin);
//    // Labels fonts
//    tv_label_sim_operator.setTypeface(typeface_Roboto_Light);
//    tv_label_sim_operator_code.setTypeface(typeface_Roboto_Light);
//    tv_label_network_operator.setTypeface(typeface_Roboto_Light);
//    tv_label_network_operator_code.setTypeface(typeface_Roboto_Light);
//    tv_label_roaming_status.setTypeface(typeface_Roboto_Light);
//    tv_label_cell_tower_ID.setTypeface(typeface_Roboto_Light);
//    tv_label_cell_tower_area_location_code.setTypeface(typeface_Roboto_Light);
//    tv_label_signal_strength.setTypeface(typeface_Roboto_Light);
//    tv_label_bearer.setTypeface(typeface_Roboto_Light);
//    tv_label_manufacturer.setTypeface(typeface_Roboto_Light);
//    tv_label_model.setTypeface(typeface_Roboto_Light);
//    tv_label_OS.setTypeface(typeface_Roboto_Light);
//    tv_label_OS_version.setTypeface(typeface_Roboto_Light);
//    tv_label_phone_type.setTypeface(typeface_Roboto_Light);
//    tv_label_latitude.setTypeface(typeface_Roboto_Light);
//    tv_label_longitude.setTypeface(typeface_Roboto_Light);
//    tv_label_accuracy.setTypeface(typeface_Roboto_Light);
//    tv_label_provider.setTypeface(typeface_Roboto_Light);
//    // Results fonts
//    tv_result_sim_operator.setTypeface(typeface_Roboto_Light);
//    tv_result_sim_operator_code.setTypeface(typeface_Roboto_Light);
//    tv_result_network_operator.setTypeface(typeface_Roboto_Light);
//    tv_result_network_operator_code.setTypeface(typeface_Roboto_Light);
//    tv_result_roaming_status.setTypeface(typeface_Roboto_Light);
//    tv_result_cell_tower_ID.setTypeface(typeface_Roboto_Light);
//    tv_result_cell_tower_area_location_code.setTypeface(typeface_Roboto_Light);
//    tv_result_signal_strength.setTypeface(typeface_Roboto_Light);
//    tv_result_bearer.setTypeface(typeface_Roboto_Light);
//    tv_result_manufacturer.setTypeface(typeface_Roboto_Light);
//    tv_result_model.setTypeface(typeface_Roboto_Light);
//    tv_result_OS.setTypeface(typeface_Roboto_Light);
//    tv_result_OS_version.setTypeface(typeface_Roboto_Light);
//    tv_result_phone_type.setTypeface(typeface_Roboto_Light);
//    tv_result_latitude.setTypeface(typeface_Roboto_Light);
//    tv_result_longitude.setTypeface(typeface_Roboto_Light);
//    tv_result_accuracy.setTypeface(typeface_Roboto_Light);
//    tv_result_provider.setTypeface(typeface_Roboto_Light);
//
//    if (tv_result_wifi_ssid != null) {
//      tv_result_wifi_ssid.setTypeface(typeface_Roboto_Light);
//    }
//    if (tv_result_municipality != null) {
//      tv_result_municipality.setTypeface(typeface_Roboto_Light);
//    }
//    if (tv_result_country_name != null) {
//      tv_result_country_name.setTypeface(typeface_Roboto_Light);
//    }
//    if (tv_result_wlan_carrier != null) {
//      tv_result_wlan_carrier.setTypeface(typeface_Roboto_Light);
//    }
  }

  // Receive the result from a previous call to startActivityForResult(Intent, int)
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    // Check if the request code is same as what is passed  here it is 0
    // Check if the data is different from null, when for example the user leaves the activity with the back button

    if (data != null && requestCode == 0) {
      eNetworkTypeResults networkType;

      switch (data.getIntExtra("networkType", 1)) {
        case 0:
          networkType = eNetworkTypeResults.eNetworkTypeResults_Any;
          break;
        case 1:
          networkType = eNetworkTypeResults.eNetworkTypeResults_WiFi;
          break;
        case 2:
          networkType = eNetworkTypeResults.eNetworkTypeResults_Mobile;
          break;
        default:
          SKLogger.sAssert(getClass(), false);
          networkType = eNetworkTypeResults.eNetworkTypeResults_Any;
      }

      if (networkType != getNetworkTypeSelection()) {
        saveNetworkTypeSelection(networkType);
        new RefreshListViewData().execute();
      }
    }
  }

  // Called when the Fragment is no longer resumed.
  @Override
  public void onPause() {
    super.onPause();

    // Unregister since the activity is about to be closed.
    Context context = SKApplication.getAppInstance().getApplicationContext();
    LocalBroadcastManager.getInstance(context).unregisterReceiver(updateScreenMessageReceiver);
  }

  void doUpdateToolbarSetIsListViewHidden(boolean value) {
    isListviewHidden = value;

    mArchiveFilterButton.setVisibility(View.GONE);
    mArchiveShareButton.setVisibility(View.GONE);

    if (isListviewHidden == true) {
      // Showing result details...
    } else {
      // Not showing result details. Show the button.
      mArchiveFilterButton.setVisibility(View.VISIBLE);
    }

    if (clickedPosition != -1) {

      try {

        switch (aList_ArchivedResults.get(clickedPosition).getNetworkType()) {

          case eNetworkTypeResults_Mobile: {
            if (SKApplication.getAppInstance().isSocialMediaExportSupported() == true) {
              mArchiveShareButton.setVisibility(View.VISIBLE);
            }
          }
          break;
          default:
            break;
        }
      } catch (java.lang.IndexOutOfBoundsException e) {
        // This is a rare error that has been seen to occur!
        SKLogger.sAssert(false);
      }
    }

    // Update menu.
    if (getActivity() != null) {
      getActivity().invalidateOptionsMenu();
    }
  }

  // *** BROADCAST RECEIVERS *** //
  // Handler for received Intents. This will be called whenever an Intent with an action named "refreshUIMessage" is broadcasted.
  private BroadcastReceiver updateScreenMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      // Refresh the UI data
      new RefreshListViewData().execute();

      // If we are in "hidden list view" mode, then return to the default view
      if (isListviewHidden) {
        // Set the list view click events disable
        rl_main.setClickable(false);

        // Hide the header row
        listViewRow.animate().setDuration(300).alpha(0.0f).setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            listViewRow.animate().setListener(null);  // Delete the listener to avoid side effects

            rl_main.removeView(listViewRow);      // Remove the view from the layout
          }

          ;
        });

        // Hide the passive metrics layout
        ll_passive_metrics.animate().setDuration(300).alpha(0.0f).setInterpolator(new OvershootInterpolator(1.2f));
        ll_passive_metrics.setVisibility(View.GONE);

        // Set the list view to the original position
        lv_archived_results.animate().setDuration(300).alpha(1.0f).x(0).setListener(new AnimatorListenerAdapter() {
          // Executed at the start of the animation
          public void onAnimationStart(Animator animation) {
            clickedView.setAlpha(1.0f);          // Make the view on the list view visible
          }

          ;

          // Executed at the end of the animation
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);

            lv_archived_results.animate().setListener(null);  // Remove the listener to avoid side effects

            lv_archived_results.setClickable(true);        // Set the list view to clickable

            // Set the state of the list view to "not hidden"
            doUpdateToolbarSetIsListViewHidden(false);
          }
        });
      }
    }
  };

  // *** INNER CLASSES *** //

  /**
   * This is the background task that update the values of the list view
   */
  private class RefreshListViewData extends AsyncTask<Void, Void, Void> {
    eNetworkTypeResults mSelectedNetworkType;

    @Override
    protected void onPreExecute() {
      super.onPreExecute();

      asyncTask_RefreshListViewData_Running = true;

      // Check if the spinner is not null (for example before the menu is created) and if that is the case then set the loading spinner to visible before start the background processing
      if (menu_Item_Refresh_Spinner != null) {
        menu_Item_Refresh_Spinner.setVisible(true);
      }

      // This must be determined BEFORE doing the background task;
      // we use it to populate the list.
      mSelectedNetworkType = getNetworkTypeSelection();
    }

    // Override this method to perform a computation on a background thread
    @Override
    protected Void doInBackground(Void... params) {
      // Clear the values of the temporary list - by completely re-constructing it,
      // to avoid any danger of array adapters sharing list references.
      aList_TemporaryArchivedTests = new ArrayList<TestResult>();
      //aList_TemporaryArchivedTests.clear();
      // Fill the temporary list
      populateEmptyArchivedTestsList(mSelectedNetworkType, aList_TemporaryArchivedTests);

      return null;
    }


    private void safeRunOnUiThread(Runnable runnable) {
      if (getActivity() == null) {
        SKLogger.sAssert(getClass(), false);
        return;
      }

      getActivity().runOnUiThread(runnable);
    }


    // Runs on the UI thread after doInBackground
    @Override
    protected void onPostExecute(Void result) {
      asyncTask_RefreshListViewData_Running = false;

      // Check if the spinner is not null (for example before the menu is created) and if that is the case then set the loading spinner to invisible after finish the background processing
      if (menu_Item_Refresh_Spinner != null) {
        menu_Item_Refresh_Spinner.setVisible(false);
      }

      // If there are no results archived
      if (aList_TemporaryArchivedTests.size() == 0) {
        safeRunOnUiThread(new Runnable() {
          @Override
          public void run() {
            // Hide the (empty) list view
            lv_archived_results.setVisibility(View.GONE);
            // Show the "No results yet" message
            tv_warning_no_results_yet.setVisibility(View.VISIBLE);
          }
        });
      }
      // If there are results archived
      else {
        safeRunOnUiThread(new Runnable() {
          @Override
          public void run() {
            // Show the "No results yet" message
            tv_warning_no_results_yet.setVisibility(View.GONE);
            // Show the (not empty) list view
            lv_archived_results.setVisibility(View.VISIBLE);

            // Copy the temporary list into the final list.
            // Note that this creates a NEW list, not a simple reference to the original.
            // However, the items in the list are *NOT* clones, but are shared references to the underlying items.
            // If we just did this:
            // aList_ArchivedResults = aList_TemporaryArchivedTests
            // ... then any change to aList_TemporaryArchivedTests would potentially
            // cause this sort of runtime error:
            // java.lang.IllegalStateException:
            // The content of the adapter has changed but ListView did
            // not receive a notification.
            // Make sure the content of your adapter is not modified
            // from a background thread, but only from the UI thread.
            // Make sure your adapter calls notifyDataSetChanged() when its content changes.
            // [in ListView(2131165312, class android.widget.ListView) with
            // Adapter(class com.samknows.ui2.activity.AdapterArchivedResultsListView)]
            aList_ArchivedResults = new ArrayList<TestResult>(aList_TemporaryArchivedTests);
            // Refresh the list view
            adapter_Archived_Results = new AdapterArchivedResultsListView(getActivity(), aList_ArchivedResults);
            lv_archived_results.setAdapter(adapter_Archived_Results);
          }
        });
      }
    }
  }

  // *** CUSTOM METHODS *** //

  /**
   * Bind the layout resources with the resources in this class
   *
   * @param pView
   */
  private void setUpResources(View pView) {
    // Main relative layout
    rl_main = (RelativeLayout) pView.findViewById(R.id.fragment_archived_results_rl_main);

    // View hosting the passive metrics layout
    ll_passive_metrics = (LinearLayout) pView.findViewById(R.id.fragment_passive_metrics_ll);
    ll_passive_metrics.setVisibility(View.GONE);    // Make it invisible in the beginning

    // Set the warning message
    tv_warning_no_results_yet = (TextView) pView.findViewById(R.id.fragment_archived_results_warning_no_results_yet);

    // Passive metrics fields
    // Header labels
    pm_tv_header_label_sim_and_network_operators = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_sim_and_network_operators);
    pm_tv_header_label_signal = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_signal);
    pm_tv_header_label_device = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_device);
    pm_tv_header_label_location = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_location);
    // Dividers
    ll_passive_metrics_divider_1 = (LinearLayout) pView.findViewById(R.id.fragment_passive_metrics_divider_sim_and_network_operators);
    ll_passive_metrics_divider_2 = (LinearLayout) pView.findViewById(R.id.fragment_passive_metrics_divider_signal);
    ll_passive_metrics_divider_location = (LinearLayout) pView.findViewById(R.id.fragment_passive_metrics_divider_location);
    // Labels
    tv_label_sim_operator = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_sim_operator);
    tv_label_sim_operator_code = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_sim_operator_code);
    tv_label_network_operator = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_network_operator);
    tv_label_network_operator_code = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_network_operator_code);
    tv_label_roaming_status = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_roaming_status);
    tv_label_cell_tower_ID = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_cell_tower_ID);
    tv_label_cell_tower_area_location_code = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_cell_tower_area_location_code);
    tv_label_signal_strength = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_signal_strength);
    tv_label_bearer = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_bearer);
    tv_label_manufacturer = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_manufacturer);
    tv_label_model = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_model);
    tv_label_OS = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_OS);
    tv_label_OS_version = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_OS_version);
    tv_label_phone_type = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_phone_type);
    tv_label_latitude = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_latitude);
    tv_label_longitude = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_longitude);
    tv_label_accuracy = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_accuracy);
    tv_label_provider = (TextView) pView.findViewById(R.id.fragment_passive_metrics_label_location_provider);
    // Results
    tv_result_sim_operator = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_sim_operator_name);
    tv_result_sim_operator_code = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_sim_operator_code);
    tv_result_network_operator = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_network_operator_name);
    tv_result_network_operator_code = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_network_operator_code);
    tv_result_roaming_status = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_roaming_status);
    tv_result_cell_tower_ID = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_cell_tower_id);
    tv_result_cell_tower_area_location_code = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_cell_tower_area_location_code);
    tv_result_signal_strength = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_signal_strength);
    tv_result_bearer = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_bearer);
    tv_result_manufacturer = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_manufacturer);
    tv_result_model = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_detail_model);
    tv_result_OS = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_detail_OS);
    tv_result_OS_version = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_OS_version);
    tv_result_phone_type = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_phone_type);
    tv_result_latitude = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_latitude);
    tv_result_longitude = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_longitude);
    tv_result_accuracy = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_accuracy);
    tv_result_provider = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_location_provider);

    // WIFI SSID, and new stuff!
    try {
      tv_group_wifi_ssid = pView.findViewById(R.id.fragment_passive_metrics_group_wifi_ssid);
      tv_group_municipality =  pView.findViewById(R.id.fragment_passive_metrics_group_municipality);
      tv_group_country_name =  pView.findViewById(R.id.fragment_passive_metrics_group_country_name);
      tv_group_wlan_carrier =  pView.findViewById(R.id.fragment_passive_metrics_group_wlan_carrier);
      tv_result_wifi_ssid = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_wifi_ssid);
      tv_result_municipality = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_municipality);
      tv_result_country_name = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_country_name);
      tv_result_wlan_carrier = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_wlan_carrier);
    } catch (java.lang.NoSuchFieldError e) {
      // This happens with some versions of passive_metrics.panel, where these values are not availabe!
      //SKLogger.sAssert(false);
    }

    publicIp = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_your_ip_value);
    submissionId = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_reference_number_value);
    networkType = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_network_type);
    target = (TextView) pView.findViewById(R.id.fragment_passive_metrics_result_target);
    publicIp.setText("");
    submissionId.setText("");
    networkType.setText("");
    target.setText("");

    // Initialise fonts
    typeface_Roboto_Light = SKTypeface.sGetTypefaceWithPathInAssets( "fonts/roboto_light.ttf");

    // Now - what items to show?
    LinearLayout ip_and_reference_metrics = (LinearLayout) pView.findViewById(R.id.ip_and_reference_metrics);
    LinearLayout fragment_passive_metrics_ll_divider_sim_and_network_operators = (LinearLayout) pView.findViewById(R.id.fragment_passive_metrics_divider_sim_and_network_operators);
    LinearLayout signal_metrics = (LinearLayout) pView.findViewById(R.id.signal_metrics);
    LinearLayout device_metrics = (LinearLayout) pView.findViewById(R.id.device_metrics);
    LinearLayout location_metrics = (LinearLayout) pView.findViewById(R.id.location_metrics);

    if (SKApplication.getAppInstance().getPassiveMetricsJustDisplayPublicIpAndSubmissionId() == true) {
      fragment_passive_metrics_ll_divider_sim_and_network_operators.setVisibility(View.GONE);
      signal_metrics.setVisibility(View.GONE);
      device_metrics.setVisibility(View.GONE);
      location_metrics.setVisibility(View.GONE);
    } else {
      ip_and_reference_metrics.setVisibility(View.GONE);
    }

    mArchiveFilterButton = pView.findViewById(R.id.archive_filter_button);
    mArchiveFilterButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        showSelectNetwork();
      }
    });
    mArchiveShareButton = pView.findViewById(R.id.archive_share_button);
    mArchiveShareButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        showShareResult();
      }
    });

    // Get the width of the screen in pixels
    Display display = getActivity().getWindowManager().getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    final int screenWidth = size.x;

    // Report that this fragment would like to participate in populating the options menu by receiving a call to onCreateOptionsMenu(Menu, MenuInflater) and related methods.
    setHasOptionsMenu(true);

    // List view holding the archived results
    lv_archived_results = (ListView) pView.findViewById(R.id.archived_results_list_view);          // Bind the list view
    adapter_Archived_Results = new AdapterArchivedResultsListView(getActivity(), aList_ArchivedResults);    // Set up the list view adapter
    lv_archived_results.setAdapter(adapter_Archived_Results);                        // Assign the adapter to the list view

    // Set the listener to show the passive metrics/details about an specific archived test
    if (SKApplication.getAppInstance().getRevealPassiveMetricsOnArchiveResultsPanel()) {
      lv_archived_results.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          // If the list view is not hidden (i.e. it is visible!)
          if (isListviewHidden == false) {
            showDetailedMetricsHideList(screenWidth, view, position);
          }
        }
      });
    }

    // On click listener that set the list view to the origin position
    rl_main.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // If the list view is hidden
        if (isListviewHidden) {
          showListHideDetailedMetrics();
        }
      }
    });

    // If true, the list view is hidden, if false, the list view is shown.
    doUpdateToolbarSetIsListViewHidden(false);


  }


  Activity mActivity = null;
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    mActivity = activity;
    // WE must DEFER the chart data loading (which actually happens on the main thread!) to prevent the UI blocking on start-up
    // This cannot be done until we're attached to an activity!
    new Handler().post(new Runnable() {

      @Override
      public void run() {
        // Retrieve from data base the data of the archived tests within 1 week (default time period)
        new RefreshListViewData().execute();
      }
    });
  }

  @Override
  public void onDetach() {
    super.onDetach();

    mActivity = null;
  }


  private void registerBackButtonHandler() {
    View view = getView();
    view.setFocusableInTouchMode(true);
    view.requestFocus();
    view.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
          // TODO - should we handle this ourselves, or not?!
          if (isListviewHidden == false) {
            // Don't handle it...
            return false;
          }

          // Handle the back button event directly.
          // The gauge elements are invisible - show them and hide the passive metrics.
          showListHideDetailedMetrics();
          return true;
        } else {
          // Don't handle it...
          return false;
        }
      }
    });
  }

  /**
   * Save the state of the network type filter in shared preferences
   *
   * @param pNetworkType
   */
  private void saveNetworkTypeSelection(eNetworkTypeResults pNetworkType) {
    // Get the shared preferences editor
    Context context = SKApplication.getAppInstance().getApplicationContext();
    SharedPreferences.Editor editor = context.getSharedPreferences(getString(R.string.sharedPreferencesIdentifier), Context.MODE_PRIVATE).edit();

    switch (pNetworkType) {
      case eNetworkTypeResults_Any:
        editor.putInt("networkTypeArchivedTests", 0);
        break;
      case eNetworkTypeResults_WiFi:
        editor.putInt("networkTypeArchivedTests", 1);
        break;
      case eNetworkTypeResults_Mobile:
        editor.putInt("networkTypeArchivedTests", 2);
        break;
      default:
        SKLogger.sAssert(getClass(), false);
    }

    editor.commit();    // Commit changes

    // Verify that it has been saved properly!
    SKLogger.sAssert(getClass(), getNetworkTypeSelection() == pNetworkType);
  }

  /**
   * Get the state of the network type filter from shared preferences
   *
   * @return
   */
  private eNetworkTypeResults getNetworkTypeSelection() {
    SKLogger.sAssert(SKCommon.sGetIsMainThread());

    if (getActivity() == null) {
      SKLogger.sAssert(false);
      return eNetworkTypeResults.eNetworkTypeResults_Any;
    }

    // Get shared preferences
    SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.sharedPreferencesIdentifier), Context.MODE_PRIVATE);
    // Recover the state of the download button
    switch (prefs.getInt("networkTypeArchivedTests", 0)) {
      case 0:
        return eNetworkTypeResults.eNetworkTypeResults_Any;
      case 1:
        return eNetworkTypeResults.eNetworkTypeResults_WiFi;
      case 2:
        return eNetworkTypeResults.eNetworkTypeResults_Mobile;
      default:
        SKLogger.sAssert(getClass(), false);
        return eNetworkTypeResults.eNetworkTypeResults_Any;
    }
  }

  /**
   * This method retrieve information from the database about the tests performed and archived. All (dataType 0), WiFi (dataType 1) or Mobile (dataType 2)
   *
   * @param networkType
   * @param pTemporaryArchivedTestsList
   * @return a list of archived tests
   */
  private void populateEmptyArchivedTestsList(eNetworkTypeResults networkType, ArrayList<TestResult> pTemporaryArchivedTestsList) {
    // Get the current network state
    eNetworkTypeResults previousState = SKApplication.getNetworkTypeResults();

    SKApplication.setNetworkTypeResults(networkType);

    try {
      populateEmptyArrayList(pTemporaryArchivedTestsList);
    } catch ( java.lang.IllegalStateException e) {
      // This should *never* happen, due to the use of isAdded() in populateEmptyArrayList...!
      // But, this is belt-and-braces.
      // http://stackoverflow.com/questions/22366596/android-illegalstateexception-fragment-not-attached-to-activity-webview
      SKLogger.sAssert(false);
    }

    // Back to the previous state
    SKApplication.setNetworkTypeResults(previousState);

    // Order the archived test results by date in case we had retrieved WiFi and Mobile data
    if (networkType == eNetworkTypeResults.eNetworkTypeResults_Any) {
      orderListByDate(pTemporaryArchivedTestsList);
    }
  }

  /**
   * Fill the array list with the data from data base
   *
   * @param pTemporaryArchivedTestsList
   * @return
   */
  private void populateEmptyArrayList(ArrayList<TestResult> pTemporaryArchivedTestsList) {

    if ((getActivity() == null) || (isAdded() == false)) {
      // e.g. test completes, after fragment detatched
      SKLogger.sAssert(false);
      return;
    }

    Context context = SKApplication.getAppInstance();
    DBHelper dbHelper = new DBHelper(context);
    JSONArray archivedResultArray = dbHelper.getArchiveData(-1);

    try {
      // Iterate over the archived results
      for (int i = 0; i < archivedResultArray.length(); i++) {
        TestResult testResult = new TestResult();

        JSONObject thisRow = archivedResultArray.getJSONObject(i);
        testResult.setDtime(Long.valueOf(thisRow.getString("dtime")));

        JSONArray activeMetricResults = thisRow.getJSONArray("activemetrics");
        FormattedValues formattedValues = new FormattedValues();

        // Iterate the active metrics and save them in a list
        for (int itemcount = 0; itemcount < activeMetricResults.length(); itemcount++) {
          JSONObject activeMetricsEntry = activeMetricResults.getJSONObject(itemcount);

          String testnumber = activeMetricsEntry.getString("test");
          String success = activeMetricsEntry.getString("success");
          String hrresult = activeMetricsEntry.getString("hrresult");
          String targetServerLocation = activeMetricsEntry.getString("location");
          testResult.setTargetServerLocation(targetServerLocation);

          if (success.equals("0")) {
            hrresult = getString(R.string.failed);
          }

          if (testnumber.equals(String.valueOf(DETAIL_TEST_ID.DOWNLOAD_TEST_ID.getValueAsInt()))) {
            if (!hrresult.equalsIgnoreCase("failed")) {
              testResult.setDownloadResult(hrresult);
            } else {
              testResult.setDownloadResult("-1");
            }
          }

          if (testnumber.equals(String.valueOf(DETAIL_TEST_ID.UPLOAD_TEST_ID.getValueAsInt()))) {
            if (!hrresult.equalsIgnoreCase("failed")) {
              testResult.setUploadResult(hrresult);
            } else {
              testResult.setUploadResult("-1");
            }
          }

          if (testnumber.equals(String.valueOf(DETAIL_TEST_ID.LATENCY_TEST_ID.getValueAsInt()))) {
            if (!hrresult.equalsIgnoreCase("failed")) {
              testResult.setLatencyResult(hrresult);
            } else {
              testResult.setLatencyResult("-1");
            }
          }

          if (testnumber.equals(String.valueOf(DETAIL_TEST_ID.PACKETLOSS_TEST_ID.getValueAsInt()))) {
            if (!hrresult.equalsIgnoreCase("failed")) {
              testResult.setPacketLossResult(hrresult);
            } else {
              testResult.setPacketLossResult("-1");
            }
          }

          if (testnumber.equals(String.valueOf(DETAIL_TEST_ID.JITTER_TEST_ID.getValueAsInt()))) {
            if (!hrresult.equalsIgnoreCase("failed")) {
              testResult.setJitterResult(hrresult);
            } else {
              testResult.setJitterResult("-1");
            }

          }
        }

        // For some reason (I think it is the background tests that failed), some tests are archived with 0 and null results.
        // This jumps over them to avoid showing them.
        if (testResult.getDownloadResult().equals("0") && testResult.getUploadResult().equals("0") && testResult.getLatencyResult().equals("0")) {
          // Try to track this in the debugger, if it happens - to help figure-out the underlying reason.
          SKLogger.sAssert(getClass(), false);
          continue;
        }

        JSONArray passiveMetricResults = thisRow.getJSONArray("passivemetrics");

        // Iterate the passive metrics to save them in an object and finally save the object in a list
        for (int itemcount = 0; itemcount < passiveMetricResults.length(); itemcount++) {
          JSONObject passiveMetricsEntry = passiveMetricResults.getJSONObject(itemcount);

          String metric = passiveMetricsEntry.getString("metric");
          String value = passiveMetricsEntry.getString("value");

          if (metric.equals("simoperatorname")) {
            testResult.setSimOperatorName(value);
          } else if (metric.equals("simoperatorcode")) {
            testResult.setSimOperatorCode(value);
          } else if (metric.equals("networkoperatorname")) {
            testResult.setNetworkOperatorName(value);
          } else if (metric.equals("networkoperatorcode")) {
            testResult.setNetworkOperatorCode(value);
          } else if (metric.equals("roamingstatus")) {
            testResult.setRoamingStatus(value);
          } else if (metric.equals("gsmcelltowerid")) {
            testResult.setGSMCellTowerID(value);
          } else if (metric.equals("gsmlocationareacode")) {
            testResult.setGSMLocationAreaCode(value);
          } else if (metric.equals("gsmsignalstrength")) {
            testResult.setGSMSignalStrength(value);
          } else if (metric.equals("manufactor")) {
            testResult.setManufactor(value);
          } else if (metric.equals("networktype")) {
            testResult.setBearer(value);
          } else if (metric.equals("model")) {
            testResult.setModel(value);
          } else if (metric.equals("ostype")) {
            testResult.setOSType(value);
          } else if (metric.equals("osversion")) {
            testResult.setOSVersion(value);
          } else if (metric.equals("osversionandroid")) {
            tv_result_OS_version.setText(value);
          } else if (metric.equals("androidbuildversion")) {
            testResult.setOSVersion(value);
          } else if (metric.equals("phonetype")) {
            testResult.setPhoneType(value);
          } else if (metric.equals("latitude")) {
            testResult.setLatitude(value);
          } else if (metric.equals("longitude")) {
            testResult.setLongitude(value);
          } else if (metric.equals("accuracy")) {
            testResult.setAccuracy(value);
          } else if (metric.equals("locationprovider")) {
            testResult.setLocationProvider(value);
          } else if (metric.equals("public_ip")) {
            testResult.setPublicIp(value);
          } else if (metric.equals("submission_id")) {
            testResult.setSubmissionId(value);
          } else if (metric.equals("activenetworktype")) {
            if (value.equals("WiFi")) {
              testResult.setNetworkType(eNetworkTypeResults.eNetworkTypeResults_WiFi);
            } else if (value.equals("mobile")) {
              testResult.setNetworkType(eNetworkTypeResults.eNetworkTypeResults_Mobile);
            } else {
              SKLogger.sAssert(getClass(), false);
            }

          // WIFI_SSID and other new stuff!
          // wifi_ssid
          // municipality
          // country_name
          // android os version string
          } else if (metric.equals("wifi_ssid")){
            testResult.setWifiSSID(value);
          } else if (metric.equals("municipality")){
            testResult.setMunicipality(value);
          } else if (metric.equals("country_name")){
            testResult.setCountryName(value);
          } else if (metric.equals("wlan_carrier")){
            testResult.setWLANCarrier(value);
          }

          {
            //SKLogger.sAssert(getClass(), false);
          }
        }
        pTemporaryArchivedTestsList.add(testResult);
      }
    } catch (JSONException e) {
      Log.d(C_TAG_FRAGMENT_ARCHIVED_TEST, "There was a problem fetching the archived results " + e.getMessage());
    }
  }

  /**
   * Sort array list by date (time stamp)
   *
   * @param pTemporaryArchivedTestsList
   * @return
   */
  private void orderListByDate(ArrayList<TestResult> pTemporaryArchivedTestsList) {
    boolean flag = true;      // Set flag to true to begin first pass. Indicates if we need another loop or if it's enough
    TestResult temp;        // Holding variable

    while (flag) {
      flag = false;    // Set flag to false awaiting a possible swap

      for (int j = 0; j < pTemporaryArchivedTestsList.size() - 1; j++) {
        long currentTimestampValue = pTemporaryArchivedTestsList.get(j).getDtime();
        long nextTimestampValue = pTemporaryArchivedTestsList.get(j + 1).getDtime();

        if (currentTimestampValue < nextTimestampValue)  // change to > for ascending sort
        {
          temp = pTemporaryArchivedTestsList.get(j);    //Swap elements
          pTemporaryArchivedTestsList.set(j, pTemporaryArchivedTestsList.get(j + 1));
          pTemporaryArchivedTestsList.set(j + 1, temp);
          flag = true;                  //Shows a swap occurred
        }
      }
    }
  }

  /**
   * Set the passive metrics fields to the correct values. This method is called when a list view item is clicked.
   *
   * @param pTestResult
   */
  private void fillPassiveMetrics(TestResult pTestResult) {

    // If network type is WiFi, hide passive metrics not related with WiFi. If network type is not WiFi, show all passive metrics related with mobile network
    // int visibilityOfMobilePassiveMetrics = pTestResult.getNetworkType() == eNetworkTypeResults.eNetworkTypeResults_WiFi ? View.GONE : View.VISIBLE;

    /*
    // Fields which visibility depends on the kind of network
    pm_tv_header_label_sim_and_network_operators.setVisibility(visibilityOfMobilePassiveMetrics);
    pm_tv_header_label_signal.setVisibility(visibilityOfMobilePassiveMetrics);

    ll_passive_metrics_divider_1.setVisibility(visibilityOfMobilePassiveMetrics);
    ll_passive_metrics_divider_2.setVisibility(visibilityOfMobilePassiveMetrics);

    tv_label_sim_operator.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_label_sim_operator_code.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_label_network_operator.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_label_network_operator_code.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_label_roaming_status.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_label_cell_tower_ID.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_label_cell_tower_area_location_code.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_label_signal_strength.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_label_bearer.setVisibility(visibilityOfMobilePassiveMetrics);

    tv_result_sim_operator.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_result_sim_operator_code.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_result_network_operator.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_result_network_operator_code.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_result_roaming_status.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_result_cell_tower_ID.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_result_cell_tower_area_location_code.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_result_signal_strength.setVisibility(visibilityOfMobilePassiveMetrics);
    tv_result_bearer.setVisibility(visibilityOfMobilePassiveMetrics);
    */

    // Passive metrics that will be displayed just in mobile network type
    //if (visibilityOfMobilePassiveMetrics == View.VISIBLE)
    {
      tv_result_sim_operator.setText(pTestResult.getSimOperatorName());
      tv_result_sim_operator_code.setText(pTestResult.getSimOperatorCode());
      tv_result_network_operator.setText(pTestResult.getNetworkOperatorName());
      tv_result_network_operator_code.setText(pTestResult.getNetworkOperatorCode());
      tv_result_roaming_status.setText(pTestResult.getRoamingStatus());
      tv_result_cell_tower_ID.setText(pTestResult.getGSMCellTowerID());
      tv_result_cell_tower_area_location_code.setText(pTestResult.getGSMLocationAreaCode());
      tv_result_signal_strength.setText(pTestResult.getGSMSignalStrength());
      tv_result_bearer.setText(pTestResult.getBearer());
    }

//		else if (metricString.equals("public_ip"))
//		{
//			if (publicIp != null) {
//				publicIp.setText(value);
//			}
//		}
//		else if (metricString.equals("submission_id")) 
//		{
//			if (submissionId != null) {
//				submissionId.setText(value);
//			}
//		}

    // Passive metrics that will be shown in any network type
    tv_result_manufacturer.setText(pTestResult.getManufactor());
    tv_result_model.setText(pTestResult.getModel());
    tv_result_OS.setText(pTestResult.getOSType());
    tv_result_OS_version.setText(pTestResult.getOSVersion());
    tv_result_phone_type.setText(pTestResult.getPhoneType());


    // If the data of the location is not available, hide the location data
    int visibilityOfLocation = pTestResult.getLatitude() == null ? View.GONE : View.VISIBLE;

    // Set the visibility of the location data section
    pm_tv_header_label_location.setVisibility(visibilityOfLocation);
    ll_passive_metrics_divider_location.setVisibility(visibilityOfLocation);
    tv_label_latitude.setVisibility(visibilityOfLocation);
    tv_label_longitude.setVisibility(visibilityOfLocation);
    tv_label_accuracy.setVisibility(visibilityOfLocation);
    tv_label_provider.setVisibility(visibilityOfLocation);
    tv_result_latitude.setVisibility(visibilityOfLocation);
    tv_result_longitude.setVisibility(visibilityOfLocation);
    tv_result_accuracy.setVisibility(visibilityOfLocation);
    tv_result_provider.setVisibility(visibilityOfLocation);

    // If the location data is available, then set it into the text views
    if (visibilityOfLocation == View.VISIBLE) {
      tv_result_latitude.setText(pTestResult.getLatitude());
      tv_result_longitude.setText(pTestResult.getLongitude());
      tv_result_accuracy.setText(pTestResult.getAccuracy());
      tv_result_provider.setText(pTestResult.getLocationProvider());
    }

    if (publicIp != null) {
      publicIp.setText(pTestResult.getPublicIp());
    }
    if (submissionId != null) {
      submissionId.setText(pTestResult.getSubmissionId());
    }
    if (networkType != null) {
      networkType.setText(pTestResult.getNetworkTypeAsString());
    }
    if (target != null) {
      target.setText(pTestResult.getTargetServerLocation());
    }

    // Show/hide this ONLY if Network Type is WIFI, *and* if value is NOT empty!
    if (tv_result_wifi_ssid != null) {
      tv_result_wifi_ssid.setText(pTestResult.getWifiSSID());
    }
    if (tv_group_wifi_ssid  != null) {
      int visibility = (pTestResult.getNetworkType() == eNetworkTypeResults.eNetworkTypeResults_WiFi) ? View.VISIBLE : View.GONE;
      if (pTestResult.getWifiSSID().length() == 0)  {
        visibility = View.GONE;
      }
      tv_group_wifi_ssid.setVisibility(visibility);
    }

    if (tv_result_wifi_ssid != null) {
      tv_group_wifi_ssid.setVisibility((pTestResult.getWifiSSID().length() > 0) ? View.VISIBLE : View.GONE);
    }

    // Show/hide this ONLY if value is NOT empty!
    if (tv_result_municipality != null) {
      tv_result_municipality.setText(pTestResult.getMunicipality());
      tv_group_municipality.setVisibility((pTestResult.getMunicipality().length() > 0) ? View.VISIBLE : View.GONE);
    }

    // Show/hide this ONLY if value is NOT empty!
    if (SKApplication.getAppInstance().getDoesAppDisplayCountryNameInMetrics()) {
      if (tv_result_country_name != null) {
        tv_result_country_name.setText(pTestResult.getCountryName());
        tv_group_country_name.setVisibility((pTestResult.getCountryName().length() > 0) ? View.VISIBLE : View.GONE);
      }
    }
    // Show/hide this ONLY if value is NOT empty!
    if (tv_result_wlan_carrier != null) {
      tv_result_wlan_carrier.setText(pTestResult.getWlanCarrier());
      tv_group_wlan_carrier.setVisibility((pTestResult.getWlanCarrier().length() > 0) ? View.VISIBLE : View.GONE);
    }
  }

  // *** MENUS *** //
  // Initialise the contents of the Activity's standard options menu.
  // You should place your menu items in to menu. For this method to be called, you must have first called setHasOptionsMenu(boolean).
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_fragment_archived_tests, menu);

    menu_Item_Network_Type_Filter = menu.findItem(R.id.menu_item_fragment_archived_tests_select_network);  // Identify network type filter menu in the action bar
    menu_Item_Refresh_Spinner = menu.findItem(R.id.menu_item_fragment_archived_tests_refreshSpinner);    // Identify the loading spinner in the action bar
    menu_Item_Share_Result = menu.findItem(R.id.menu_item_fragment_archived_tests_share_result);        // Identify the share results action in the action bar

    // If the list view is hidden, hide as well the network type filter
    if (isListviewHidden) {
      menu_Item_Network_Type_Filter.setVisible(false);
      menu_Item_Share_Result.setVisible(true);        // Show the share result action in the action bar
    } else {
      menu_Item_Share_Result.setVisible(false);        // Hide the share result action in the action bar
    }

    if (SKApplication.getAppInstance().isSocialMediaExportSupported() == false) {
      menu_Item_Share_Result.setVisible(false);        // Hide the share result action in the action bar
    }

    // Assign the resource to the loading spinner
    menu_Item_Refresh_Spinner.setActionView(R.layout.actionbar_indeterminate_progress);

    // When creating the menu, if the asynchronous task is running then show the loading spinner
    menu_Item_Refresh_Spinner.setVisible(asyncTask_RefreshListViewData_Running);
  }

  private void showShareResult() {
    if (clickedPosition != -1) {
      switch (aList_ArchivedResults.get(clickedPosition).getNetworkType()) {

        case eNetworkTypeResults_Mobile: {
          Intent intent_share_result_activity = new Intent(getActivity(), ActivityShareResult.class);
          intent_share_result_activity.putExtra("networkType", 2); // Mobile
          intent_share_result_activity.putExtra("downloadResult", aList_ArchivedResults.get(clickedPosition).getDownloadResult());
          intent_share_result_activity.putExtra("uploadResult", aList_ArchivedResults.get(clickedPosition).getUploadResult());
          intent_share_result_activity.putExtra("latencyResult", aList_ArchivedResults.get(clickedPosition).getLatencyResult());
          intent_share_result_activity.putExtra("packetLossResult", aList_ArchivedResults.get(clickedPosition).getPacketLossResult());
          intent_share_result_activity.putExtra("jitterResult", aList_ArchivedResults.get(clickedPosition).getJitterResult());
          intent_share_result_activity.putExtra("dateResult", aList_ArchivedResults.get(clickedPosition).getDtime());

          startActivity(intent_share_result_activity);
        }
        break;

        case eNetworkTypeResults_WiFi:
          SKLogger.sAssert(getClass(), false);
          return;

        default:
          SKLogger.sAssert(getClass(), false);
          return;
      }

    }

  }

  private void showSelectNetwork() {

    Intent intent_select_network = new Intent(getActivity(), ActivitySelectNetworkType.class);
    // Set the current fragment. This will determine the background of the activity
    intent_select_network.putExtra("currentFragment", 1);
    // Activity is started with requestCode 0
    startActivityForResult(intent_select_network, 0);
  }

  // This hook is called whenever an item in your options menu is selected
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.menu_item_fragment_archived_tests_select_network) {
      // Case select network
      showSelectNetwork();
      return true;
    }

    if (itemId == R.id.menu_item_fragment_archived_tests_share_result) {
      showShareResult();
      return true;
    }

    // Default case
    return true;
  }

  private void showListHideDetailedMetrics() {
    // Set the list view click events disable
    rl_main.setClickable(false);

    // Hide the passive metrics layout
    ll_passive_metrics.animate().setDuration(300).alpha(0.0f).setInterpolator(new OvershootInterpolator(1.2f)).setListener(new AnimatorListenerAdapter() {
      // Executed at the end of the animation
      @Override
      public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);

        ll_passive_metrics.animate().setListener(null);    // Remove the listener to avoid side effects
        ll_passive_metrics.setVisibility(View.GONE);

        // Move the view to the original position
        listViewRow.animate().setDuration(300).x(originPositionX).y(originPositionY).setInterpolator(new OvershootInterpolator(1.2f)).setListener(new AnimatorListenerAdapter() {
          // Executed at the end of the animation
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);

            listViewRow.animate().setListener(null);  // Remove the listener to avoid side effects

            clickedView.setAlpha(0.0f);    // Set the alpha to 0 (make it invisible) to make the transition effect without duplicated views

            // Set the list view to the original position
            lv_archived_results.animate().setDuration(300).alpha(1.0f).x(0).setInterpolator(new OvershootInterpolator(1.2f)).setListener(new AnimatorListenerAdapter() {
              // Executed at the end of the animation
              @Override
              public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                lv_archived_results.animate().setListener(null).setInterpolator(null);  // Remove the listener and interpolator to avoid side effects.

                clickedView.setAlpha(1.0f);              // Make the view on the list view visible

                rl_main.removeView(listViewRow);          // Remove the view

                lv_archived_results.setClickable(true);        // Set the list view to clickable

                clickedPosition = -1;

                // Set the state of the list view to "not hidden"
                doUpdateToolbarSetIsListViewHidden(false);
              }
            });
          }

          ;
        });
      }

      ;
    });
  }

  //
  // Get the height that would be taken by an action bar on this Android system.
  // https://stackoverflow.com/questions/12301510/how-to-get-the-actionbar-height
  // Works even if action bar hidden... note that simply doing getActivity().getActionBar().getHeight() returns 0
  // if the action bar is hidden.
  //
  int getActionBarHeight() {
    TypedValue tv = new TypedValue();
    if (getActivity() == null) {
      return 0;
    }

    if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
      int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
      return actionBarHeight;
    }

    return 0;
  }

  private void showDetailedMetricsHideList(final int screenWidth, View view,
                                           int position) {
    clickedPosition = position;
    setHasOptionsMenu(true);

    // Fill the passive metrics for the selected position
    fillPassiveMetrics(aList_ArchivedResults.get(position));

    // Set the list view click events disable
    lv_archived_results.setClickable(false);

    // Hide the action bar menu filter while the details of a test are shown
    menu_Item_Network_Type_Filter.setVisible(false);
    if (clickedPosition != -1) {
      // Only share MOBILE results!
      menu_Item_Share_Result.setVisible(aList_ArchivedResults.get(clickedPosition).getNetworkType() == eNetworkTypeResults.eNetworkTypeResults_Mobile);
    }

    // Get the position of the clicked list view item
    originPositionX = view.getLeft();
    originPositionY = view.getTop();

    // Get a copy of the view pressed from the list view
    listViewRow = adapter_Archived_Results.getView(position, null, lv_archived_results);
    // Set the position in the screen
    listViewRow.setX(originPositionX);
    listViewRow.setY(originPositionY);
    // Add the view to the main layout
    rl_main.addView(listViewRow);

    // Get the view pressed from the list view
    clickedView = view;

    // Make the view on the list view invisible
    view.setAlpha(0.0f);

    // Immediately update the toolbar!
    doUpdateToolbarSetIsListViewHidden(true);

    final int actionBarHeight = getActionBarHeight();

    // Hide the list view and show the passive metrics
    lv_archived_results.animate().setDuration(300).alpha(0.0f).x(-screenWidth).setListener(new AnimatorListenerAdapter() {
      // Executed at the end of the animation
      @Override
      public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);


        lv_archived_results.animate().setListener(null);  // Clean the listener to avoid side effects

        // Set the position of the list view item to a header position.
        // Considering the top "fake action bar", and the top padding of the list view
        // (that is now gone to the left side of the screen)
        float targetY = actionBarHeight;
        targetY += lv_archived_results.getPaddingTop();
        listViewRow.animate().setDuration(300).x(0).y(targetY).setListener(new AnimatorListenerAdapter() {
          // Executed at the end of the animation
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);

            rl_main.animate().setListener(null);    // Clean the listener to avoid side effects
            ll_passive_metrics.animate().setDuration(300).alpha(1.0f);    // Show the passive metrics layout
            ll_passive_metrics.setVisibility(View.VISIBLE);
            // Set the list view state to hidden
            doUpdateToolbarSetIsListViewHidden(true);
            rl_main.setClickable(true);          // Set the main relative layout as clickable
          }
        });
      }
    });
  }
}
