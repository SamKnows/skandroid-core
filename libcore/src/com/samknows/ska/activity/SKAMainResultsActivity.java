package com.samknows.ska.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
//import android.os.Trace;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import com.samknows.libcore.ExportFileProvider;
import com.samknows.libcore.SKCommon;
import com.samknows.libcore.SKConstants;
import com.samknows.libcore.SKLogger;
import com.samknows.measurement.TestRunner.ContinuousTestRunner;
import com.samknows.measurement.TestRunner.ManualTestRunner;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.DeviceDescription;
import com.samknows.measurement.MainService;
import com.samknows.measurement.TestRunner.SKTestRunner;
import com.samknows.measurement.schedule.TestDescription.*;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.SKApplication.eNetworkTypeResults;
import com.samknows.libcore.R;
import com.samknows.measurement.SamKnowsLoginService;
import com.samknows.measurement.SamKnowsResponseHandler;
import com.samknows.measurement.Storage;
import com.samknows.measurement.activity.components.ButtonWithRightArrow;
import com.samknows.measurement.activity.components.SKGraphForResults;
import com.samknows.measurement.activity.components.SKGraphForResults.DATERANGE_1w1m3m1y;
import com.samknows.measurement.activity.components.StatModel;
import com.samknows.measurement.activity.components.StatRecord;
import com.samknows.measurement.activity.components.UpdatedTextView;
import com.samknows.measurement.activity.components.Util;
import com.samknows.measurement.environment.NetworkDataCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.schedule.TestDescription;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.ExportFile;
import com.samknows.measurement.storage.StorageTestResult.*;
import com.samknows.measurement.util.SKDateFormat;
import com.samknows.ska.activity.components.StatView;

public class SKAMainResultsActivity extends SKAPostToSocialMedia
		implements OnClickListener {

  SKGraphForResults graphHandlerDownload;
  SKGraphForResults graphHandlerUpload;
  SKGraphForResults graphHandlerLatency;
  SKGraphForResults graphHandlerPacketLoss;
  SKGraphForResults graphHandlerJitter;
  int download_page_index = 0;
  int upload_page_index = 0;
  int latency_page_index = 0;
  int packetloss_page_index = 0;
  int jitter_page_index = 0;

  // For mock testing...
  public SKGraphForResults getDownloadGraphHandler() {
    return graphHandlerDownload;
  }

  private static final String TAG = SKAMainResultsActivity.class.getSimpleName();
  public static final String SETTINGS = "SamKnows";
  private static final int PANEL_HEIGHT = 550;
  private final Context context = this;
  private SamKnowsLoginService service = new SamKnowsLoginService();

  private StatModel statModel = new StatModel();
  private String start_date;
  private JSONObject recentData;
  // private DeviceDescription device;
  // private boolean isCurrentDevice;


  public static final int RECENT = 0;
  public static final int WEEK = 1;
  public static final int MONTH = 2;
  public static final int THREE_MONTHS = 3;
  public static final int SIX_MONTHS = 4;
  public static final int YEAR = 5;

  private static final int ITEMS_PER_PAGE = 5;

  private boolean isDisplayingContent = false;

  private int[] latest;
  private int[] rows;
  private TableLayout table;
  private int[] buttons;

  private long start_dtime = 0;
  private long end_dtime = 0;

  private UpdatedTextView updated;

  View.OnTouchListener gestureListener;
  private GestureDetector gestureDetector;
  DeviceDescription currentDevice;
  private View subview;

  private int total_archive_records = 0;
  private int total_download_archive_records = 0;
  private int total_upload_archive_records = 0;
  private int total_latency_archive_records = 0;
  private int total_packetloss_archive_records = 0;
  private int total_jitter_archive_records = 0;

  private DBHelper dbHelper;
  // private DBHelper dbHelperAsync;

  private MyPagerAdapter adapter = null;
  private ViewPager viewPager;
  private View current_page_view;
  private int current_page_view_position = 0;

  private String last_run_test_formatted;
  private int target_height = 0;
  private boolean on_aggregate_page = true;

  Storage storage;
  ScheduleConfig config;

  List<TestDescription> testList;
  ArrayList<String> array_spinner = new ArrayList<String>();
  ArrayList<SCHEDULE_TEST_ID> array_spinner_int = new ArrayList<SCHEDULE_TEST_ID>();

  TextView tvHeader = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "+++++DEBUG+++++ SamKnowsAggregateStatViewerActivity onCreate...");
		
		/*
		 * device = (DeviceDescription) getIntent().getSerializableExtra(
		 * Constants.INTENT_EXTRA_DEVICE); isCurrentDevice =
		 * getIntent().getBooleanExtra(
		 * Constants.INTENT_EXTRA_IS_CURRENT_DEVICE, false);
		 * 
		 * List<DeviceDescription> devices = AppSettings.getInstance()
		 * .getDevices(); String imei =
		 * PhoneIdentityDataCollector.getImei(this); currentDevice = new
		 * devices);
		 */

    this.setTitle(getString(R.string.sk2_main_results_activity_title));

    setContentView(R.layout.ska_main_results_activity_main_page_views);

    dbHelper = new DBHelper(SKAMainResultsActivity.this);
    //dbHelperAsync = new DBHelper(SamKnowsAggregateStatViewerActivity.this);

    viewPager = (ViewPager) findViewById(R.id.viewPager);

    adapter = new MyPagerAdapter(this);
    viewPager.setAdapter(adapter);
    // viewPager.setOffscreenPageLimit(3);

    tvHeader = (TextView) findViewById(R.id.textViewHeader);

    viewPager.setOnPageChangeListener(new OnPageChangeListener() {

      @Override
      public void onPageSelected(int page) {
        handleOnPageSelected(page);
      }

      @Override
      public void onPageScrolled(int position, float positionOffset,
                                 int positionOffsetPixels) {
      }

      @Override
      public void onPageScrollStateChanged(int state) {
//				if (state == ViewPager.SCROLL_STATE_SETTLING) {
//
//				}
      }
    });


    Util.initializeFonts(this);
    Util.overrideFonts(this, findViewById(android.R.id.content));

    final SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
    final Activity ctx = this;

    // Make sure the service is running - that allows background processing to work.
    MainService.poke(this);
  }

  void handleOnPageSelected(int page) {

    tvHeader.setText(getString(R.string.page) + " " + (page + 1));

    if (page == 0) {
      on_aggregate_page = true;
      boolean db_refresh = false;
      setContinuousTestingButton();
      SKAMainResultsActivity.this.setTitle(getString(R.string.sk2_main_results_activity_title));

      View v = viewPager.findViewWithTag(page);

      if (v == null) {
        // ... we should trap this where possible in the debugger...
        SKLogger.sAssert(getClass(), false);
      } else {
        TextView timestampView = (TextView) v.findViewById(R.id.average_results_title);

        if (timestampView == null) {
          // ... we should trap this where possible in the debugger...
          SKLogger.sAssert(getClass(), false);
        } else {

          timestampView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        }
      }

    } else {
      View v = viewPager.findViewWithTag(page);
      if (v == null) {
        // ... we should trap this where possible in the debugger...
        SKLogger.sAssert(getClass(), false);
      } else {
        TextView timestampView = (TextView) v.findViewById(R.id.timestamp);
        timestampView.setContentDescription(getString(R.string.archive_result) + " " + timestampView.getText());
        timestampView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        on_aggregate_page = false;
        SKAMainResultsActivity.this.setTitle(getString(R.string.archive_result));


        String caption = getString(R.string.archive_result) + " " + page + " " + getString(R.string.archive_result_of) + " " + total_archive_records;
        // Following line was used for testing that the text will fit...
        //caption = getString(R.string.archive_result) + " " + 9999 + " " + getString(R.string.archive_result_of) + " " + 9999;
        TextView captionView = (TextView) v.findViewById(R.id.archived_result_x_of_y);
        captionView.setText(caption);

      }
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "+++++DEBUG+++++ SamKnowsAggregateStatViewerActivity onDestroy...");
  }
	
	/*
	 // Example of async processing using Bolt... we should look to use this sort of approach in a future implementation.
	 // https://github.com/BoltsFramework/Bolts-Android
		  
	 private void onResumeDoAsyncQueryToHandleResult() {

		// Handle this delayed, so we don't mess-up the transition.
		final boolean uiResult = setTotalArchiveRecords();

		Task.callInBackground(new Callable<MyPagerAdapter>() {
			// This is called in the background thread...

			@Override
			public MyPagerAdapter call() throws Exception {
				// TODO Put-in the background work here!
				if (uiResult == true) {
					MyPagerAdapter theAdapter = new MyPagerAdapter(SKAMainResultsActivity.this);
					return theAdapter;
					//viewPager = (ViewPager) findViewById(R.id.viewPager);
					//SKLogger.sAssert(getClass(), viewPager == (ViewPager) findViewById(R.id.viewPager));
				}
				return null;
			}
		}).onSuccess(new Continuation<MyPagerAdapter, Void>() {
			public Void then(Task<MyPagerAdapter> task) throws Exception {
				// This is called in the main UI Thread...!
				MyPagerAdapter theAdapter = task.getResult();

				SKAMainResultsActivity.this.postResumeOnMainThreadUseAdapter(theAdapter);

				// All done!
				return null;
			}
		}, Task.UI_THREAD_EXECUTOR);
	}
	*/

  boolean mbHandlingOnActivityResult = false;

  @Override
  public void onResume() {
    super.onResume(); // Always call the superclass method first

    Log.d(TAG, "+++++DEBUG+++++ SamKnowsAggregateStatViewerActivity onResume...");

    if (mbHandlingOnActivityResult == true) {
      // Already handled on the activity result... which has taken us to the archived results screen.
      mbHandlingOnActivityResult = false;
    } else {
      // Resuming from e.g. T&C screen, or About screen.
      if (setTotalArchiveRecords()) {
        adapter = new MyPagerAdapter(SKAMainResultsActivity.this);
        //viewPager = (ViewPager) findViewById(R.id.viewPager);
        //SKLogger.sAssert(getClass(), viewPager == (ViewPager) findViewById(R.id.viewPager));
        viewPager.setAdapter(adapter);
      }
    }

    setContinuousTestingButton();
    //handling of the continuous testing
        /*if(mContinuous.isRunning() == true){
        	connectToMainService();
        	((Button)findViewById(R.id.btnRunContinuousTests)).setText(R.string.stop_continuous);
        }else if(mMainService != null){
        	((Button)findViewById(R.id.btnRunContinuousTests)).setText(R.string.start_continuous);
        }
        */

    if (SKApplication.sGetUpdateAllDataOnScreen() == true) {
      updateAllDataOnScreen();
    }
  }


  private void setContinuousTestingButton() {

    //
    // Should the button be visible at all?
    //
    View continuousTestingButton = findViewById(R.id.btnRunContinuousTestsLayout);

    if (SKApplication.getAppInstance().supportContinuousTesting() == false) {
      // Might not yet be attached to the main view (this is attached via a view pager, remember!)
      if (continuousTestingButton != null) {
        continuousTestingButton.setVisibility(View.GONE);
      }
      return;
    }

    if (continuousTestingButton == null) {
      // Not yet attached to the main view (this is attached via a view pager, remember!)
      return;
    }

    //
    // The button should be visible and active!
    //
    continuousTestingButton.setVisibility(View.VISIBLE);

    Button b = (Button) findViewById(R.id.btnRunContinuousTests);
    if (b != null) {
      int b_id = R.string.start_continuous;
      switch (mContinuousState) {
        case STOPPED:
          b_id = R.string.start_continuous;
          break;
        case STARTING:
          b_id = R.string.starting_continuous;
          break;
        case STOPPING:
          b_id = R.string.stopping_continuous;
          break;
        case EXECUTING:
          b_id = R.string.stop_continuous;
          break;
      }
      b.setText(b_id);
    }
  }

  @Override
  public void onPause() {
    super.onPause();

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == cRequestCodeForFacebookResult) {
      // Handled by the super class...
    } else if (requestCode == cRunTestActivityRequestCode) {

      if (resultCode == RESULT_OK) {
        mbHandlingOnActivityResult = true;

        // The returned result might be a mobile result, or a network result.
        // We must update the filter to match, and then show that result screen...!
        String activeNetworkType = data.getStringExtra("activeneworktype");

        if (activeNetworkType == null) {
          SKLogger.sAssert(getClass(), false);
        } else if (activeNetworkType.equals("mobile")) {
          if (SKApplication.getNetworkTypeResults() == eNetworkTypeResults.eNetworkTypeResults_WiFi) {
            SKApplication.setNetworkTypeResults(eNetworkTypeResults.eNetworkTypeResults_Any);
          }
        } else if (activeNetworkType.equals("WiFi")) {
          if (SKApplication.getNetworkTypeResults() == eNetworkTypeResults.eNetworkTypeResults_Mobile) {
            SKApplication.setNetworkTypeResults(eNetworkTypeResults.eNetworkTypeResults_Any);
          }
        } else {
          SKLogger.sAssert(getClass(), false);
        }
        setNetworkTypeToggleButton();

        updateAllDataOnScreen();
      }
    } else {
      // Unexpected!
      //SKLogger.sAssert(getClass(),  false);
    }

		/*
		 * if (resultCode>0){
		 * 
		 * //adapter.instantiateItem(viewPager, resultCode);
		 * //adapter.instantiateItem(viewPager, resultCode-1);
		 * 
		 * viewPager.setCurrentItem(resultCode, false);
		 * adapter.notifyDataSetChanged(); overridePendingTransition(0, 0); }
		 */
  }

  private void updateAllDataOnScreen() {
    SKApplication.sSetUpdateAllDataOnScreen(false);

    // refresh data
    adapter = new MyPagerAdapter(SKAMainResultsActivity.this);
    setTotalArchiveRecords();
    //viewPager = (ViewPager) findViewById(R.id.viewPager);
    SKLogger.sAssert(getClass(), viewPager == (ViewPager) findViewById(R.id.viewPager));
    viewPager.setAdapter(adapter);
    viewPager.setCurrentItem(1, true); // true means - perform a smooth scroll!
    //overridePendingTransition(0, 0);
  }

  private void lookBackwardInTime(Calendar fromCal) {
    switch (mDateRange) {
      case DATERANGE_1w1m3m1y_ONE_DAY:
        fromCal.add(Calendar.DAY_OF_YEAR, -1);
        break;
      case DATERANGE_1w1m3m1y_ONE_WEEK:
        fromCal.add(Calendar.WEEK_OF_YEAR, -1);
        break;
      case DATERANGE_1w1m3m1y_ONE_MONTH:
        fromCal.add(Calendar.WEEK_OF_YEAR, -4);
        break;
      case DATERANGE_1w1m3m1y_THREE_MONTHS:
        fromCal.add(Calendar.WEEK_OF_YEAR, -12);
        break;
      case DATERANGE_1w1m3m1y_ONE_YEAR:
        fromCal.add(Calendar.WEEK_OF_YEAR, -52);
        break;
    }
  }

  private Pair<String, String> getAverageDownloadAndUpload() {
    Calendar fromCalNow = Calendar.getInstance();
    long current_dtime = fromCalNow.getTimeInMillis();

    lookBackwardInTime(fromCalNow);
    long starting_dtime = fromCalNow.getTimeInMillis();
    JSONArray jsonResult = dbHelper.getAverageResults(starting_dtime, current_dtime);

    String upload = "";
    String download = "";

    for (int i = 0; i < jsonResult.length(); i++) {
      try {
        JSONObject json_data;
        json_data = jsonResult.getJSONObject(i);

        String value = json_data.getString("value");
        String type = json_data.getString("type");

        if (type.equals("" + DETAIL_TEST_ID.DOWNLOAD_TEST_ID.getValueAsInt())) {
          download = "" + value;
        }
        if (type.equals("" + DETAIL_TEST_ID.UPLOAD_TEST_ID.getValueAsInt())) {
          upload = "" + value;
        }
      } catch (JSONException e) {
        SKLogger.sAssert(getClass(), false);
      }
    }

    return new Pair<String, String>(download, upload);
  }


  private void loadAverage() {
    //Trace.beginSection("loadAverage");

    Calendar fromCalNow = Calendar.getInstance();
    long current_dtime = fromCalNow.getTimeInMillis();

    lookBackwardInTime(fromCalNow);
    long starting_dtime = fromCalNow.getTimeInMillis();


    JSONArray jsonResult = dbHelper.getAverageResults(starting_dtime, current_dtime);

    // Clear out the results, as there might be no results on first
    // use, when toggling mobile results <-> network results.
    ((TextView) subview.findViewById(R.id.download_average)).setText("");
    ((TextView) subview.findViewById(R.id.upload_average)).setText("");
    ((TextView) subview.findViewById(R.id.latency_average)).setText("");
    ((TextView) subview.findViewById(R.id.packetloss_average)).setText("");
    ((TextView) subview.findViewById(R.id.jitter_average)).setText("");

    String result = "";

    for (int i = 0; i < jsonResult.length(); i++) {
      try {
        JSONObject json_data = jsonResult.getJSONObject(i);
        String value = json_data.getString("value");
        String type = json_data.getString("type");

        if (type.equals("" + DETAIL_TEST_ID.DOWNLOAD_TEST_ID.getValueAsInt())) {
          ((TextView) subview.findViewById(R.id.download_average))
              .setText("" + value);
        }
        if (type.equals("" + DETAIL_TEST_ID.UPLOAD_TEST_ID.getValueAsInt())) {
          ((TextView) subview.findViewById(R.id.upload_average))
              .setText("" + value);
        }
        if (type.equals("" + DETAIL_TEST_ID.LATENCY_TEST_ID.getValueAsInt())) {
          ((TextView) subview.findViewById(R.id.latency_average))
              .setText("" + value);
        }
        if (type.equals("" + DETAIL_TEST_ID.PACKETLOSS_TEST_ID.getValueAsInt())) {
          ((TextView) subview.findViewById(R.id.packetloss_average))
              .setText("" + value);
        }
        if (type.equals("" + DETAIL_TEST_ID.JITTER_TEST_ID.getValueAsInt())) {
          ((TextView) subview.findViewById(R.id.jitter_average))
              .setText("" + value);
        }

      } catch (JSONException e) {
        e.printStackTrace();
      }

    }

    //Trace.endSection();
  }

  public boolean setTotalArchiveRecords() {
    boolean result = false;

    total_archive_records = 0;
    total_download_archive_records = 0;
    total_upload_archive_records = 0;
    total_latency_archive_records = 0;
    total_packetloss_archive_records = 0;
    total_jitter_archive_records = 0;

    JSONObject summary = dbHelper.getArchiveDataSummary();

    JSONObject results = null;

    try {
      results = summary.getJSONObject("test_counter");

    } catch (JSONException e) {

      SKLogger.sAssert(getClass(), false);

      return false;
    }


    try {
      if (results.has("" + DETAIL_TEST_ID.DOWNLOAD_TEST_ID.getValueAsInt())) {
        total_download_archive_records = results.getInt("" + DETAIL_TEST_ID.DOWNLOAD_TEST_ID.getValueAsInt());
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    if (mShowArchivedResultsButton != null) {
      mShowArchivedResultsButton.setVisibility((total_download_archive_records > 0) ? View.VISIBLE :
          View.INVISIBLE);
    }

    try {
      if (results.has("" + DETAIL_TEST_ID.UPLOAD_TEST_ID.getValueAsInt())) {
        total_upload_archive_records = results.getInt("" + DETAIL_TEST_ID.UPLOAD_TEST_ID.getValueAsInt());
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    try {
      if (results.has("" + DETAIL_TEST_ID.LATENCY_TEST_ID.getValueAsInt())) {
        total_latency_archive_records = results.getInt("" + DETAIL_TEST_ID.LATENCY_TEST_ID.getValueAsInt());
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    try {
      if (results.has("" + DETAIL_TEST_ID.JITTER_TEST_ID.getValueAsInt())) {
        total_jitter_archive_records = results.getInt("" + DETAIL_TEST_ID.JITTER_TEST_ID.getValueAsInt());
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    try {
      if (results.has("" + DETAIL_TEST_ID.PACKETLOSS_TEST_ID.getValueAsInt())) {
        total_packetloss_archive_records = results.getInt("" + DETAIL_TEST_ID.PACKETLOSS_TEST_ID.getValueAsInt());
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    try {
      int records = summary.getInt("counter");
      if (total_archive_records != records) {
        total_archive_records = records;
        result = true;
      }

      String last_run_test = summary.getString("enddate");
      long last_run_test_l = Long.parseLong(last_run_test);
      if (last_run_test_l != 0) {
        last_run_test_formatted = new SKDateFormat(context)
            .UIDate(last_run_test_l);
      } else {
        last_run_test_formatted = this.getString(R.string.last_run_never);
      }

    } catch (JSONException e1) {
      e1.printStackTrace();
    }
    return result;
  }

  private void loadDownloadGrid(DETAIL_TEST_ID testnumber, int grid, int offset, int rowsPerPage) {

    //Trace.beginSection("loadDownloadGrid");

    Calendar fromCalNow = Calendar.getInstance();
    long current_dtime = fromCalNow.getTimeInMillis();
    lookBackwardInTime(fromCalNow);
    long starting_dtime = fromCalNow.getTimeInMillis();

    //Trace.beginSection("jsonObjectIfNotPaged");
    JSONObject jsonObjectIfNotPaged = dbHelper.getGridData(testnumber, 0, Integer.MAX_VALUE, starting_dtime, current_dtime);
    //Trace.endSection();

    //Trace.beginSection("jsonObject");
    JSONObject jsonObject = dbHelper.getGridData(testnumber, offset, rowsPerPage, starting_dtime, current_dtime);
    //Trace.endSection();
    // jsonObject=dbHelper.getGridData(1, offset, limit);

    //Trace.beginSection("theRest");
    double availableRowsForThisGrid = (double) total_archive_records;
    JSONArray resultsIfNotPaged = null;
    JSONArray results = null;
    try {
      resultsIfNotPaged = jsonObjectIfNotPaged.getJSONArray("results");
      results = jsonObject.getJSONArray("results");
    } catch (JSONException e) {
      SKLogger.sAssert(getClass(), false);
    }

    if (resultsIfNotPaged != null) {
      availableRowsForThisGrid = resultsIfNotPaged.length();
    } else {
      switch (testnumber) {
        case DOWNLOAD_TEST_ID:
          availableRowsForThisGrid = (double) total_download_archive_records;
          break;
        case UPLOAD_TEST_ID:
          availableRowsForThisGrid = (double) total_upload_archive_records;
          break;
        case LATENCY_TEST_ID:
          availableRowsForThisGrid = (double) total_latency_archive_records;
          break;
        case PACKETLOSS_TEST_ID:
          availableRowsForThisGrid = (double) total_packetloss_archive_records;
          break;
        case JITTER_TEST_ID:
          availableRowsForThisGrid = (double) total_jitter_archive_records;
          break;
        default:
          SKLogger.sAssert(getClass(), false);
          break;
      }
    }

    if (offset >= availableRowsForThisGrid) {
      offset = 0;
    }

    int pages = (int) Math.ceil(availableRowsForThisGrid / rowsPerPage);
    if (pages == 0) {
      pages = 1;
    }
    int page_number = (offset + rowsPerPage) / rowsPerPage;

    TextView tv;

    switch (testnumber) {
      case DOWNLOAD_TEST_ID:
        tv = (TextView) subview.findViewById(R.id.download_pagenumber);
        tv.setText(getString(R.string.page) + " " + page_number + " " + getString(R.string.of) + " " + pages);
        break;

      case UPLOAD_TEST_ID:
        tv = (TextView) subview.findViewById(R.id.upload_pagenumber);
        tv.setText(getString(R.string.page) + " " + page_number + " " + getString(R.string.of) + " " + pages);
        break;

      case LATENCY_TEST_ID:
        tv = (TextView) subview.findViewById(R.id.latency_pagenumber);
        tv.setText(getString(R.string.page) + " " + page_number + " " + getString(R.string.of) + " " + pages);
        break;

      case PACKETLOSS_TEST_ID:
        tv = (TextView) subview.findViewById(R.id.packetloss_pagenumber);
        tv.setText(getString(R.string.page) + " " + page_number + " " + getString(R.string.of) + " " + pages);
        break;

      case JITTER_TEST_ID:
        tv = (TextView) subview.findViewById(R.id.jitter_pagenumber);
        tv.setText(getString(R.string.page) + " " + page_number + " " + getString(R.string.of) + " " + pages);
        break;

      default:
        SKLogger.sAssert(getClass(), false);
        break;
    }

    String location;
    String dtime;
    String dtime_formatted = null;
    String result;
    String success;

    int addedRowCount = 0;

    if (results != null) {

      for (int i = 0; i < results.length(); i++) {
        location = "";
        dtime = "";
        result = "";
        success = "";
        String networkType = "";
        JSONObject user = null;
        try {
          user = results.getJSONObject(i);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        if (user != null) {
          try {
            success = user.getString(DBHelper.GRIDDATA_RESULTS_SUCCESS);
            location = user.getString(DBHelper.GRIDDATA_RESULTS_LOCATION);
            dtime = user.getString(DBHelper.GRIDDATA_RESULTS_DTIME);
            networkType = user.getString(DBHelper.GRIDDATA_RESULTS_NETWORK_TYPE);
            if (dtime != "") {

              long datelong = Long.parseLong(dtime);
              if (datelong != 0) {
                //dtime_formatted = new SKDateFormat(context).UIDate(datelong);
                dtime_formatted = new SKDateFormat(context).getGraphMilliAsDateTimeString(datelong);
              }

            } else {
              dtime_formatted = "";
            }
            result = user.getString("hrresult");

          } catch (JSONException e) {
            e.printStackTrace();
          }
        }

        if (addedRowCount == 0) {
          // Add the header, just before adding the first row!
          addGridItemHeader(getString(R.string.results_table_header_datetime),
              getString(R.string.results_table_header_location),
              getString(R.string.results_table_header_result),
              grid);
        }

        if (success.equals("1")) {
          addGridItem(dtime_formatted, location, result, networkType, grid);
        } else {
          result = getString(R.string.failed);
          addGridItemFailed(dtime_formatted, location, result, networkType, grid);
        }

        addedRowCount++;
      }
    }

    TableLayout table = (TableLayout) findViewById(grid);
    if (addedRowCount > 0) {
//			LayoutParams x = new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
//			table.setLayoutParams(x);
      table.setVisibility(View.VISIBLE);
    } else {
//			LayoutParams x = new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1);
//			table.setLayoutParams(x);
//			table.setVisibility(View.VISIBLE);
      table.setVisibility(View.GONE);
      table.getLayoutParams().height = 0;
    }
    table.getParent().requestLayout();

    Util.overrideFonts(this, findViewById(android.R.id.content));
    //Trace.endSection(); // theRest

    //Trace.endSection();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

//	    // Checks the orientation of the screen
//	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//	        Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
//	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
//	        Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
//	    }
  }

  Button mNetworkTypeToggleButton = null;
  //	TextView     mNetworkTypeToggleTextView = null;
  ButtonWithRightArrow mShowArchivedResultsButton = null;

  void setNetworkTypeToggleButton() {
    mNetworkTypeToggleButton = (Button) findViewById(R.id.networkTypeButton);

    if (mNetworkTypeToggleButton == null) {
      SKLogger.sAssert(getClass(), false);
    } else {
      if (SKApplication.getNetworkTypeResults() == eNetworkTypeResults.eNetworkTypeResults_WiFi) {
        mNetworkTypeToggleButton.setText(R.string.network_type_wifi_results);
      } else if (SKApplication.getNetworkTypeResults() == eNetworkTypeResults.eNetworkTypeResults_Mobile) {
        mNetworkTypeToggleButton.setText(R.string.network_type_mobile_results);
      } else {
        mNetworkTypeToggleButton.setText(R.string.network_type_all_results);
      }
    }
  }

  private void buttonSetup() {


    // button setup

//		Button execute_button;
//		execute_button = (Button) subview.findViewById(R.id.btnRunTest);
//		execute_button.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//
////				// Force an exception, for test purposes!
////				Log.d(this.getClass().toString(), "Forcing exception in the Android app, to test Crittercism!");
////				String[] strings = { "a", "b", "c" };
////				for (int i = 0; i <= 1000; i++) {
////					String x = strings[i];
////				}
//
//				Intent intent = new Intent(
//						SamKnowsAggregateStatViewerActivity.this,
//						SamKnowsTestViewerActivity.class);
//
//				startActivityForResult(intent, cRunTestActivityRequestCode);
//				overridePendingTransition(R.anim.transition_in,
//						R.anim.transition_out);
//
//			}
//		});

    Button run_now_choice_button;
    run_now_choice_button = (Button) findViewById(R.id.btnRunTest);

    run_now_choice_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        RunChoice();
      }
    });

    Button continuous_start_stop_toggle;
    continuous_start_stop_toggle = (Button) findViewById(R.id.btnRunContinuousTests);
    continuous_start_stop_toggle.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        ContinuousToggle(v);
      }
    });

    Button timeperiod_button;
    timeperiod_button = (Button) findViewById(R.id.btn_timeperiod);

    timeperiod_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        //// This can be be used to prove crash reporting...
        //throw new RuntimeException("Deliberate test of crash reporting - do not put in live code!");
        SingleChoice();
      }
    });

    String caption = getString(R.string.time_period_1week);

    if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_WEEK) {
      caption = getString(R.string.time_period_1week);
      timeperiod_button.setText(R.string.results_for_1_week);
    } else if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_MONTH) {
      caption = getString(R.string.time_period_1month);
      timeperiod_button.setText(R.string.results_for_1_month);
    } else if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_THREE_MONTHS) {
      caption = getString(R.string.time_period_3months);
      timeperiod_button.setText(R.string.results_for_3_months);
    } else if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_YEAR) {
      caption = getString(R.string.time_period_1year);
      timeperiod_button.setText(R.string.results_for_1_year);
    } else if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_DAY) {
      caption = getString(R.string.time_period_1day);
      timeperiod_button.setText(R.string.results_for_1_day);
    } else {
      timeperiod_button.setText(R.string.results_for_1_week);
      SKLogger.sAssert(getClass(), false);
    }


//		LinearLayout timeperiod_button2;
//		timeperiod_button2 = (LinearLayout) findViewById(R.id.timeperiod_header);
//
//		timeperiod_button2.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				SingleChoice();
//			}
//		});

    // page turn navigation button
    mShowArchivedResultsButton = (ButtonWithRightArrow) findViewById(R.id.main_show_archived_results_button);

    mShowArchivedResultsButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // This is how you scroll "right" to the next page (we always
        // start on page "zero"...)
        viewPager.setCurrentItem(1);
      }
    });

    // When we start-up, the button might be hidden or shown!
    mShowArchivedResultsButton.setVisibility((total_download_archive_records > 0) ? View.VISIBLE : View.INVISIBLE);


    // Network type results button, and associated text label.
    setNetworkTypeToggleButton();

    // Set-up the button listener.
    mNetworkTypeToggleButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        String the_array_spinner[];
        the_array_spinner = new String[3];
        the_array_spinner[0] = getString(R.string.network_type_choose_mobile_results);
        the_array_spinner[1] = getString(R.string.network_type_choose_wifi_results);
        the_array_spinner[2] = getString(R.string.network_type_choose_all_results);

        Builder builder = new AlertDialog.Builder(SKAMainResultsActivity.this);
        builder.setTitle(getString(R.string.network_type_choose));

        builder.setItems(the_array_spinner, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {

            dialog.dismiss();

            //Trace.beginSection("processNetworkTypeButton");

            boolean bChanged = false;
            if (which == 0) {
              if (SKApplication.getNetworkTypeResults() != eNetworkTypeResults.eNetworkTypeResults_Mobile) {
                SKApplication.setNetworkTypeResults(eNetworkTypeResults.eNetworkTypeResults_Mobile);
                bChanged = true;
              }
            } else if (which == 1) {
              if (SKApplication.getNetworkTypeResults() != eNetworkTypeResults.eNetworkTypeResults_WiFi) {
                SKApplication.setNetworkTypeResults(eNetworkTypeResults.eNetworkTypeResults_WiFi);
                bChanged = true;
              }
            } else { // if (which == 2) {
              if (SKApplication.getNetworkTypeResults() != eNetworkTypeResults.eNetworkTypeResults_Any) {
                SKApplication.setNetworkTypeResults(eNetworkTypeResults.eNetworkTypeResults_Any);
                bChanged = true;
              }
            }

            if (bChanged) {
              setNetworkTypeToggleButton();

              //Trace.beginSection("setTotalArchiveRecords");
              // Update all the graphs etc.!!!
              // Query average data, and update charts - this might make them invisible, if there is no data!
              setTotalArchiveRecords();
              //Trace.endSection();

              //Trace.beginSection("pagerAdapter");
              adapter = new MyPagerAdapter(SKAMainResultsActivity.this);
              //viewPager = (ViewPager) findViewById(R.id.viewPager);
              SKLogger.sAssert(getClass(), viewPager == (ViewPager) findViewById(R.id.viewPager));
              //Trace.beginSection("setAdapter");
              viewPager.setAdapter(adapter);
              //Trace.endSection();
              //Trace.endSection();

              //Trace.beginSection("queryAverageDataAndUpdateTheCharts");
              queryAverageDataAndUpdateTheCharts();
              //Trace.endSection();

//    	  					// Do NOT reload the grids, as this has been done already by the previous code.
//    	  					clearGrid(R.id.download_results_tablelayout);
//    	  					clearGrid(R.id.upload_results_tablelayout);
//    	  					clearGrid(R.id.latency_results_tablelayout);
//    	  					clearGrid(R.id.packetloss_results_tablelayout);
//    	  					clearGrid(R.id.jitter_results_tablelayout);
//    	  					adapter.loadGrids();
              //Trace.beginSection("setContinuousTestingButton");
              setContinuousTestingButton();
              //Trace.endSection();
            }
            //Trace.endSection();
          }
        });
        builder.setNegativeButton(getString(R.string.cancel),
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
              }
            });
        AlertDialog alert = builder.create();
        alert.show();
      }
    });

    // grid navigation buttons

    Button download_grid_right_button;
    download_grid_right_button = (Button) subview
        .findViewById(R.id.btn_download_grid_right);
    download_grid_right_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (download_page_index < total_download_archive_records
            - ITEMS_PER_PAGE) {
          download_page_index = download_page_index + ITEMS_PER_PAGE;
        }

        clearGrid(R.id.download_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.DOWNLOAD_TEST_ID,
            R.id.download_results_tablelayout, download_page_index,
            ITEMS_PER_PAGE);

        // And force a relayout, otherwise we get a huge chunk of white space on the screen!
        findViewById(R.id.download_panel).requestLayout();
      }
    });

    Button download_grid_left_button;
    download_grid_left_button = (Button) subview
        .findViewById(R.id.btn_download_grid_left);
    download_grid_left_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (download_page_index > 0) {
          download_page_index = download_page_index - ITEMS_PER_PAGE;
        }
        clearGrid(R.id.download_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.DOWNLOAD_TEST_ID,
            R.id.download_results_tablelayout, download_page_index,
            ITEMS_PER_PAGE);

      }
    });

    Button upload_grid_right_button;
    upload_grid_right_button = (Button) subview
        .findViewById(R.id.btn_upload_grid_right);
    upload_grid_right_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (upload_page_index < total_upload_archive_records
            - ITEMS_PER_PAGE) {
          upload_page_index = upload_page_index + ITEMS_PER_PAGE;
        }

        clearGrid(R.id.upload_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.UPLOAD_TEST_ID,
            R.id.upload_results_tablelayout, upload_page_index,
            ITEMS_PER_PAGE);

      }
    });

    Button upload_grid_left_button;
    upload_grid_left_button = (Button) subview
        .findViewById(R.id.btn_upload_grid_left);
    upload_grid_left_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (upload_page_index > 0) {
          upload_page_index = upload_page_index - ITEMS_PER_PAGE;
        }
        clearGrid(R.id.upload_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.UPLOAD_TEST_ID,
            R.id.upload_results_tablelayout, upload_page_index,
            ITEMS_PER_PAGE);

      }
    });

    Button latency_grid_right_button;
    latency_grid_right_button = (Button) subview
        .findViewById(R.id.btn_latency_grid_right);
    latency_grid_right_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (latency_page_index < total_latency_archive_records
            - ITEMS_PER_PAGE) {
          latency_page_index = latency_page_index + ITEMS_PER_PAGE;
        }

        clearGrid(R.id.latency_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.LATENCY_TEST_ID,
            R.id.latency_results_tablelayout, latency_page_index,
            ITEMS_PER_PAGE);

      }
    });

    Button latency_grid_left_button;
    latency_grid_left_button = (Button) subview
        .findViewById(R.id.btn_latency_grid_left);
    latency_grid_left_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (latency_page_index > 0) {
          latency_page_index = latency_page_index - ITEMS_PER_PAGE;
        }
        clearGrid(R.id.latency_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.LATENCY_TEST_ID,
            R.id.latency_results_tablelayout, latency_page_index,
            ITEMS_PER_PAGE);

      }
    });

    Button packetloss_grid_right_button;
    packetloss_grid_right_button = (Button) subview
        .findViewById(R.id.btn_packetloss_grid_right);
    packetloss_grid_right_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (packetloss_page_index < total_packetloss_archive_records
            - ITEMS_PER_PAGE) {
          packetloss_page_index = packetloss_page_index
              + ITEMS_PER_PAGE;
        }

        clearGrid(R.id.packetloss_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.PACKETLOSS_TEST_ID,
            R.id.packetloss_results_tablelayout, packetloss_page_index,
            ITEMS_PER_PAGE);

      }
    });

    Button packetloss_grid_left_button;
    packetloss_grid_left_button = (Button) subview
        .findViewById(R.id.btn_packetloss_grid_left);
    packetloss_grid_left_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (packetloss_page_index > 0) {
          packetloss_page_index = packetloss_page_index
              - ITEMS_PER_PAGE;
        }
        clearGrid(R.id.packetloss_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.PACKETLOSS_TEST_ID,
            R.id.packetloss_results_tablelayout, packetloss_page_index,
            ITEMS_PER_PAGE);

      }
    });

    Button jitter_grid_right_button;
    jitter_grid_right_button = (Button) subview
        .findViewById(R.id.btn_jitter_grid_right);
    jitter_grid_right_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (jitter_page_index < total_jitter_archive_records
            - ITEMS_PER_PAGE) {
          jitter_page_index = jitter_page_index + ITEMS_PER_PAGE;
        }

        clearGrid(R.id.jitter_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.JITTER_TEST_ID,
            R.id.jitter_results_tablelayout, jitter_page_index,
            ITEMS_PER_PAGE);

      }
    });

    Button jitter_grid_left_button;
    jitter_grid_left_button = (Button) subview
        .findViewById(R.id.btn_jitter_grid_left);
    jitter_grid_left_button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (jitter_page_index > 0) {
          jitter_page_index = jitter_page_index - ITEMS_PER_PAGE;
        }
        clearGrid(R.id.jitter_results_tablelayout);
        loadDownloadGrid(DETAIL_TEST_ID.JITTER_TEST_ID,
            R.id.jitter_results_tablelayout, jitter_page_index,
            ITEMS_PER_PAGE);

      }
    });

    // toggle buttons

    View button = findViewById(R.id.download_header);
    ImageView button_iv = (ImageView) findViewById(R.id.btn_download_toggle);

    button.setOnClickListener(this);
    button_iv.setOnClickListener(this);

    View button2 = subview.findViewById(R.id.upload_header);
    ImageView button2_iv = (ImageView) findViewById(R.id.btn_upload_toggle);

    button2.setOnClickListener(this);
    button2_iv.setOnClickListener(this);

    View button3 = subview.findViewById(R.id.latency_header);
    ImageView button3_iv = (ImageView) findViewById(R.id.btn_latency_toggle);

    button3.setOnClickListener(this);
    button3_iv.setOnClickListener(this);

    View button4 = subview.findViewById(R.id.packetloss_header);
    ImageView button4_iv = (ImageView) findViewById(R.id.btn_packetloss_toggle);

    button4.setOnClickListener(this);
    button4_iv.setOnClickListener(this);

    View button5 = subview.findViewById(R.id.jitter_header);
    ImageView button5_iv = (ImageView) findViewById(R.id.btn_jitter_toggle);

    button5.setOnClickListener(this);
    button5_iv.setOnClickListener(this);

  }

  private void addGridItemHeader(String timestamp, String location,
                                 String result, int grid) {
    TableLayout table = (TableLayout) findViewById(grid);
    TableLayout row = (TableLayout) LayoutInflater.from(
        SKAMainResultsActivity.this).inflate(
        R.layout.ska_main_results_activity_stat_grid_header, null);

    ((TextView) row.findViewById(R.id.stat_grid_timestamp)).setText(timestamp);
    ((TextView) row.findViewById(R.id.stat_grid_location)).setText(location);
    ((TextView) row.findViewById(R.id.stat_grid_result)).setText(result);

    table.addView(row);
  }

  private void addGridItem(String timestamp, String location, String result, String networkType,
                           int grid) {
    TableLayout table = (TableLayout) findViewById(grid);
    TableLayout row = (TableLayout) LayoutInflater.from(
        SKAMainResultsActivity.this).inflate(
        R.layout.ska_main_results_activity_stat_grid, null);

    ((TextView) row.findViewById(R.id.stat_grid_timestamp)).setText(timestamp);
    ((TextView) row.findViewById(R.id.stat_grid_location)).setText(location);
    ((TextView) row.findViewById(R.id.stat_grid_result)).setText(result);

    if (networkType.equals("mobile")) {
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setImageResource(R.drawable.cell_phone_icon);
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setVisibility(View.VISIBLE);
    } else if (networkType.equals("WiFi")) {
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setImageResource(R.drawable.wifiservice);
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setVisibility(View.VISIBLE);
    } else {
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setVisibility(View.INVISIBLE);
    }

    table.addView(row);
  }

  private void addGridItemFailed(String timestamp, String location,
                                 String result, String networkType, int grid) {
    TableLayout table = (TableLayout) findViewById(grid);
    TableLayout row = (TableLayout) LayoutInflater.from(
        SKAMainResultsActivity.this).inflate(
        R.layout.ska_main_results_activity_stat_grid_fail, null);

    ((TextView) row.findViewById(R.id.stat_grid_timestamp)).setText(timestamp);
    ((TextView) row.findViewById(R.id.stat_grid_location)).setText(location);
    ((TextView) row.findViewById(R.id.stat_grid_result)).setText(result);

    if (networkType.equals("mobile")) {
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setImageResource(R.drawable.cell_phone_icon);
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setVisibility(View.VISIBLE);
    } else if (networkType.equals("WiFi")) {
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setImageResource(R.drawable.wifiservice);
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setVisibility(View.VISIBLE);
    } else {
      ((ImageView) row.findViewById(R.id.networkTypeImage)).setVisibility(View.INVISIBLE);
    }

    table.addView(row);
  }

  private void clearGrid(int grid) {
    TableLayout table = (TableLayout) findViewById(grid);
//		table.setVisibility(View.GONE);
//		table.getLayoutParams().height = 0;

    int count = table.getChildCount();
    for (int i = 0; i < count; i++) {
      View child = table.getChildAt(i);
      ((ViewGroup) child).removeAllViews();
    }
  }

  private void graphsSetup() {

    //Trace.beginSection("graphsSetup");

    WebView graphDownload = (WebView) subview.findViewById(R.id.download_graph);
    // http://stackoverflow.com/questions/2527899/disable-scrolling-in-webview
    // disable scroll on touch
    graphDownload.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return (event.getAction() == MotionEvent.ACTION_MOVE);
      }
    });
    TextView downloadCaption = (TextView) subview.findViewById(R.id.downloadCaption);
    // disable scroll on touch
    WebView graphUpload = (WebView) subview.findViewById(R.id.upload_graph);
    // disable scroll on touch
    graphUpload.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return (event.getAction() == MotionEvent.ACTION_MOVE);
      }
    });
    TextView uploadCaption = (TextView) subview.findViewById(R.id.uploadCaption);
    WebView graphLatency = (WebView) subview.findViewById(R.id.latency_graph);
    // disable scroll on touch
    graphLatency.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return (event.getAction() == MotionEvent.ACTION_MOVE);
      }
    });
    TextView latencyCaption = (TextView) subview.findViewById(R.id.latencyCaption);
    WebView graphPacketLoss = (WebView) subview.findViewById(R.id.packetloss_graph);
    // disable scroll on touch
    graphPacketLoss.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return (event.getAction() == MotionEvent.ACTION_MOVE);
      }
    });
    TextView packetlossCaption = (TextView) subview.findViewById(R.id.packetlossCaption);
    WebView graphJitter = (WebView) subview.findViewById(R.id.jitter_graph);
    // disable scroll on touch
    graphJitter.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return (event.getAction() == MotionEvent.ACTION_MOVE);
      }
    });
    TextView jitterCaption = (TextView) subview.findViewById(R.id.jitterCaption);

    graphHandlerDownload = new SKGraphForResults(context, graphDownload, downloadCaption, "download");
    graphHandlerUpload = new SKGraphForResults(context, graphUpload, uploadCaption, "upload");
    graphHandlerLatency = new SKGraphForResults(context, graphLatency, latencyCaption, "latency");
    graphHandlerPacketLoss = new SKGraphForResults(context, graphPacketLoss, packetlossCaption, "packetloss");
    graphHandlerJitter = new SKGraphForResults(context, graphJitter, jitterCaption, "jitter");

    //Trace.endSection();
  }

  /**
   * Create the options menu that displays the refresh and about options
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.ska_main_results_activity_menu, menu);
    menu.findItem(R.id.menu_export_file).setVisible(SKApplication.getAppInstance().isExportMenuItemSupported());

    menu.findItem(R.id.menu_share_averages).setVisible(SKApplication.getAppInstance().isSocialMediaExportSupported());

    // postponeEnterTransition();

    return true;
  }

  private static Intent getEmailAttachmentIntent(Context context, File zipFile, String formattedDate) {

    final Intent intent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);

    // http://stephendnicholas.com/archives/974
    // The only way to get this to work, was to use a custom ContentProvider!
    // So, this gets the file from the cache - which is might be called export_xyz.zip...
    Uri uri = Uri.parse("content://" + ExportFileProvider.sGetAUTHORITY() + "/" + zipFile.getName());
    ArrayList<Uri> uris = new ArrayList<Uri>();
    uris.add(uri);
    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

    // need this to prompts email clients only
    intent.setType("message/rfc822");

    intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.menu_export_mail_subject) + " - " + formattedDate);
    intent.putExtra(Intent.EXTRA_TEXT, SKApplication.getAppInstance().getAppName() + " - " + context.getString(R.string.menu_export_mail_body) + "\n\n" + zipFile.getName());

    return intent;
  }

  /**
   * Handle menu options
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    boolean ret = false;
    int itemId = item.getItemId();
    if (R.id.menu_about == itemId) {
      Intent intent = new Intent(this, SKAAboutActivity.class);
      startActivity(intent);
      ret = true;
    } else if (R.id.menu_settings == itemId) {
      startActivity(new Intent(this, SKASettingsActivity.class));
      ret = true;
//		} else if (R.id.menu_map == itemId) {
//			// startActivity(new Intent(this, SamKnowsMapActivity.class));
//			startActivityForResult(new Intent(this, SKAMapActivity.class),
//					cRunTestActivityRequestCode);
//			ret = true;
    } else if (R.id.menu_terms_and_condition == itemId) {

      SKApplication.getAppInstance().showTermsAndConditions(this);

      ret = true;
    } else if (R.id.menu_export_file == itemId) {
      sExportMenuItemSelected(this, getCacheDir());
      ret = true;
    } else if (R.id.menu_share_averages == itemId) {

      if (SKApplication.getAppInstance().isSocialMediaExportSupported() == false) {
        // Invalid request!
        SKLogger.sAssert(getClass(), false);
      } else {

        if (on_aggregate_page == true) {
          // On main page!

          if (!mNetworkTypeToggleButton.getText().equals(getString(R.string.network_type_mobile_results))) {
            Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.social_media_Title_ShareUsingSocialMediaMobile);
            builder.setMessage(R.string.social_media_Message_ShareUsingSocialMediaMobile);
            builder.setPositiveButton(getString(R.string.ok_dialog), null);
            AlertDialog alert = builder.create();
            alert.show();
            return false;
          }

          String last_run_test = null;
          try {
            JSONObject summary = dbHelper.getArchiveDataSummary();
            last_run_test = summary.getString("enddate");
          } catch (JSONException e1) {
            SKLogger.sAssert(getClass(), false);
          }
          if (last_run_test == null) {
            last_run_test = "0";
          }
          long theTimeMilli = Long.parseLong(last_run_test);
          if (theTimeMilli == 0) {
            //initWithTitle:NSLocalizedString(@"Title_ShareUsingSocialMediaInfo",nil)
            //message:NSLocalizedString(@"RESULTS_Label_Data",nil)
            Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.social_media_Title_ShareUsingSocialMediaMobile);
            builder.setMessage(R.string.no_data_message);
            builder.setPositiveButton(getString(R.string.ok_dialog), null);
            AlertDialog alert = builder.create();
            alert.show();
            return false;
          }

          // Export average data!

          // Call this method in the base class (SKAPostToSocialMedia)

          TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
          String carrier = telManager.getNetworkOperatorName();
          SocialStrings socialStrings = getTextForSocialMediaAverage(carrier);
          promptUserToSelectSocialMediaAndThenPost(socialStrings);
        } else {
          // We're NOT on an aggregate page!

          int position = viewPager.getCurrentItem();

          StatRecord sr = adapter.statRecords.get(position);
          Log.d(getClass().getName(), "sr.active_network_type=(" + sr.active_network_type + ")");
          if ((SKApplication.getAppInstance().isSocialMediaExportSupported() == true) &&
              (sr.active_network_type.equals("(Mobile)"))
              ) {
            // Call this method in the base class (SKAPostToSocialMedia)
            promptUserToSelectSocialMediaAndThenPost(adapter.getTextForSocialMedia(current_page_view_position));
          } else {
            Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.social_media_Title_ShareUsingSocialMediaMobile);
            builder.setMessage(R.string.social_media_Message_ShareUsingSocialMediaMobile);
            builder.setPositiveButton(getString(R.string.ok_dialog), null);
            AlertDialog alert = builder.create();
            alert.show();
            return false;
          }
        }
      }

    } else {
      ret = super.onOptionsItemSelected(item);
    }
    return ret;
  }

  public static void sExportMenuItemSelected(final Activity fromActivity, final File cacheDir) { // getCacheDir()
    File[] files = ExportFile.getAllFiles();
    if (files.length == 0) {
      // No files to export!
      new AlertDialog.Builder(fromActivity)
          .setTitle(R.string.menu_export_nothing_to_export)
          .setMessage(R.string.menu_export_there_is_no_data)
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
          }).show();
      return;
    }

    try {
      Date now = new Date();
      java.text.DateFormat df = android.text.format.DateFormat.getLongDateFormat(fromActivity);
      java.text.DateFormat df2 = android.text.format.DateFormat.getTimeFormat(fromActivity);
      final String formattedDate = df.format(now) + " " + df2.format(now);
      String candidateZipFileName = fromActivity.getString(R.string.menu_export_default_file_name_no_extension) + "_" + formattedDate + ".zip";
      // Ensure that there are no :/, characters in the name!
      final String zipFileName = candidateZipFileName.replace(':', '_').replace('/', '_').replace(',', '_').replace(' ', '_').replace("__", "_");

      AlertDialog.Builder builder = new AlertDialog.Builder(fromActivity);
      builder.setMessage(R.string.menu_export_dialog_message)
          .setCancelable(true)
          .setPositiveButton(R.string.menu_export_dialog_email, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              // Extract to mail!
              // Write the file temporarily to cache folder, so it can be auto-purged...
              File zipFile = ExportFile.getZipOfAllExportJsonFilesToThisFolderFile(cacheDir, zipFileName);

              Intent intent = getEmailAttachmentIntent(fromActivity, zipFile, formattedDate);

              try {
                // Try to launch the email intent.
                // Note that this *might* throw an exception - on some systems - if the intent could not be found...
                fromActivity.startActivity(Intent.createChooser(intent, fromActivity.getString(R.string.menu_export_send_file_chooser_title)));
              } catch (ActivityNotFoundException ex) {
                new AlertDialog.Builder(fromActivity)
                    .setTitle(R.string.menu_export_emailapp_notfound_title)
                    .setMessage(R.string.menu_export_emailapp_notfound_body)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                      }
                    }).show();
              }
            }
          })
          .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              // Nothing to do!
            }
          });

      if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
        // Storage card available - ask use if they want to export there, or not!
        builder.setNeutralButton(R.string.menu_export_dialog_storage, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // TODO - export to file system!
            userRequestedZipExportToFileSystem(fromActivity, zipFileName);
          }
        });
      }

      AlertDialog alert = builder.create();
      alert.show();

    } catch (Exception e) {
      // All we can do is catch, and prevent the app crashing!
      SKLogger.sAssert(SKAMainResultsActivity.class, false);
    }
  }

  static public void sCopyFileFromTo(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);

    // Transfer bytes from in to out
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  private static void userRequestedZipExportToThisFileNameOnFileSystem(Activity fromActivity, String fileName) {
    File storage = android.os.Environment.getExternalStorageDirectory();
    String subFolderName = SKApplication.getAppInstance().getAppName();
    File storageSubFolder = new File(storage, subFolderName);
    storageSubFolder.mkdir();
    File writeHere = new File(storageSubFolder, fileName);

    if (writeHere.exists()) {
      if (writeHere.delete() == false) {
        new AlertDialog.Builder(fromActivity)
            .setTitle(R.string.menu_export_file_save_failed_title)
            .setMessage(R.string.menu_export_file_could_not_be_overwritten)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
              }
            }).show();
        return;
      }
    }

    // Extract the file!
    File file = ExportFile.getZipOfAllExportJsonFilesToThisFolderFile(storageSubFolder, fileName);
    if (file != null) {

      new AlertDialog.Builder(fromActivity)
          .setTitle(fromActivity.getString(R.string.menu_export_file_saved_title) + " (" + subFolderName + "/" + fileName + ")")
          .setMessage(R.string.menu_export_saved_ok)
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
          }).show();

    } else {

      new AlertDialog.Builder(fromActivity)
          .setTitle(R.string.menu_export_file_save_failed_title)
          .setMessage(R.string.menu_export_file_failed_to_save)
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
          }).show();
    }

  }

  private static void userRequestedZipExportToFileSystem(Activity fromActivity, String fileNameToUse) {
    userRequestedZipExportToThisFileNameOnFileSystem(fromActivity, fileNameToUse);
  }

  /**
   * Returns a response handler that displays a loading message
   *
   * @return SamKnowsResponseHandler
   */
  private SamKnowsResponseHandler getLoadingResponseHandler(String message) {
    final ProgressDialog dialog = getProgressDialog(message);
    return new SamKnowsResponseHandler() {
      public void onSuccess(JSONObject result, Date date,
                            String start_date) {
        dialog.dismiss();
        // setStartDate(start_date);
        // setData(result);
        // setDate(date);
      }

      public void onFailure(Throwable error) {
        SKLogger.e(SKAMainResultsActivity.class,
            "failed to get data", error);
        dialog.dismiss();
      }
    };
  }

  private ProgressDialog getProgressDialog(String message) {
    return ProgressDialog.show(SKAMainResultsActivity.this,
        "", message, true, true);
  }


  SocialStrings getStringsForSocialMediaForCarrierDownloadUpload(String carrier, String download_result, String upload_result, boolean inThisDataIsAveraged) {
    SocialStrings result = new SocialStrings();

    result.twitterString = getTextForSocialMediaForCarrierDownloadUpload(false, carrier, download_result, upload_result, inThisDataIsAveraged);
    result.facebookString = getTextForSocialMediaForCarrierDownloadUpload(true, carrier, download_result, upload_result, inThisDataIsAveraged);

    return result;
  }

  String getTextForSocialMediaForCarrierDownloadUpload(boolean longString, String carrier, String download_result, String upload_result, boolean inThisDataIsAveraged) {
    StringBuilder builder = new StringBuilder();

    if (carrier.length() > 0) {
      String withCarrier;

      if (inThisDataIsAveraged) {
        if (longString) {
          withCarrier = getString(R.string.socialmedia_header_long_carrier_average);
        } else {
          withCarrier = getString(R.string.socialmedia_header_short_carrier_average);
        }
      } else {
        if (longString) {
          withCarrier = getString(R.string.socialmedia_header_long_carrier);
        } else {
          withCarrier = getString(R.string.socialmedia_header_short_carrier);
        }
      }
      // Social media posting:
      // 	- Change e.g. "My-Network-Operator" to "MyNetworkOperator"
      // 	- Change e.g. "Network&Operator" to "NetworkOperator"
      // 	- Change e.g. "Network - Operator" to "Network Operator"
      builder.append(withCarrier);
      builder.append(carrier.replace("-", "").replace("&", "").replace("  ", " "));
    } else {
      String noCarrier;
      if (inThisDataIsAveraged) {
        if (longString) {
          noCarrier = getString(R.string.socialmedia_header_long_nocarrier_average);
        } else {
          noCarrier = getString(R.string.socialmedia_header_short_nocarrier_average);
        }
      } else {
        if (longString) {
          noCarrier = getString(R.string.socialmedia_header_long_nocarrier);
        } else {
          noCarrier = getString(R.string.socialmedia_header_short_nocarrier);
        }
      }
      builder.append(noCarrier);
    }

    boolean gotSomethingYet = false;
    if (download_result.length() > 0) {
      gotSomethingYet = true;
      String download = getString(R.string.socialmedia_download);
      builder.append(download);
      builder.append(download_result);
    }
    if (upload_result.length() > 0) {
      if (gotSomethingYet == true) {
        builder.append(",");
      }
      String upload = getString(R.string.socialmedia_upload);
      builder.append(upload);
      builder.append(upload_result);
    }

    String footer;
    if (longString) {
      footer = getString(R.string.socialmedia_footer_long);
    } else {
      footer = getString(R.string.socialmedia_footer_short);
    }
    builder.append(footer);

    String result = builder.toString();
    Log.d(getClass().getName(), "String:(" + result + ")");

    return result;
  }


  SocialStrings getTextForSocialMediaAverage(String carrier) {

    //"Now testing my actual #mobilebroadband speed Up:2, Down=3 using...";

    Pair<String, String> uploadDownload = getAverageDownloadAndUpload();

    String download_result = uploadDownload.first;
    String upload_result = uploadDownload.second;

    // The true means that this data is averaged!
    SocialStrings result = getStringsForSocialMediaForCarrierDownloadUpload(carrier, download_result, upload_result, true);
    return result;
  }

  /**
   * Returns a response handler that displays a loading message for the RECENT
   * api
   *
   * @return SamKnowsResponseHandler
   */
  private SamKnowsResponseHandler getRecentLoadingResponseHandler(
      String message) {
    final ProgressDialog dialog = getProgressDialog(message);
    return new SamKnowsResponseHandler() {
      public void onSuccess(JSONObject result, Date date,
                            String start_date) {
        dialog.dismiss();
        // setRecentData(result);
      }

      public void onFailure(Throwable error) {
        dialog.dismiss();
      }
    };
  }

  public class MyPagerAdapter extends PagerAdapter {

    private ArrayList<StatRecord> statRecords;

    @Override
    public void setPrimaryItem(ViewGroup container, int position,
                               Object object) {
      current_page_view = (View) object;
      current_page_view_position = position;
    }

    SKAMainResultsActivity mMainResultsActivity;

    public MyPagerAdapter(SKAMainResultsActivity inMainResultsActivity) {

      mMainResultsActivity = inMainResultsActivity;

      statRecords = new ArrayList<StatRecord>();
      statRecords.add(new StatRecord());
      // views.get(0) is the aggregate view

      JSONObject summary = dbHelper.getArchiveDataSummary();

      try {
        total_archive_records = summary.getInt("counter");
        String last_run_test = summary.getString("enddate");

        long theTimeMilli = Long.parseLong(last_run_test);
        if (theTimeMilli != 0) {
          last_run_test_formatted = new SKDateFormat(context).UITime(theTimeMilli);
        } else {
          last_run_test_formatted = "";
        }
      } catch (JSONException e1) {
        SKLogger.e(this, "Error in reading from JSONObject.", e1);
      }

      //Trace.beginSection("addRecords");
      for (int i = 0; i < total_archive_records; i++) {
        statRecords.add(new StatRecord());
        // load blank records ready for populating
      }
      //Trace.endSection();
    }

    public void readArchiveItem(int archiveItemIndex) {
      //Trace.beginSection("readArchiveItem");
      JSONObject archiveItem;
      try {

        archiveItem = dbHelper.getSingleArchiveDataItemAtIndex(archiveItemIndex);

      } catch (Exception e) {
        //Trace.endSection();
        SKLogger.e(this, "Error in reading archive item " + archiveItemIndex, e);
        SKLogger.sAssert(getClass(), false);
        return;
      }

      if (archiveItem == null) {
        //Trace.endSection();
        SKLogger.sAssert(getClass(), false);
        return;
      }

      // read headers of json
      String datetime = "";
      String dtime_formatted;
      try {
        datetime = archiveItem.getString("dtime");

        dtime_formatted = new SKDateFormat(context).UITime(Long
            .parseLong(datetime));
        statRecords.get(archiveItemIndex + 1).time_stamp = dtime_formatted;

      } catch (JSONException e1) {
        e1.printStackTrace();
      }

      // unpack activemetrics
      JSONArray results = null;

      try {
        results = archiveItem.getJSONArray("activemetrics");
      } catch (JSONException je) {
        SKLogger.e(this, "Exception in reading active metrics array: "
            + je.getMessage());
      }

      if (results != null) {
        int itemcount = 0;

        for (itemcount = 0; itemcount < results.length(); itemcount++) {

          JSONObject user = null;
          try {
            user = results.getJSONObject(itemcount);
          } catch (JSONException je) {
            SKLogger.e(
                this,
                "Exception in reading JSONObject: "
                    + je.getMessage());
          }

          if (user != null) {
            try {
              String testnumber = user.getString("test");
              String location = user.getString("location");
              String success = user.getString("success");
              String hrresult = user.getString("hrresult");

              if (success.equals("0")) {
                hrresult = getString(R.string.failed);
              }

              if (testnumber.equals("" + DETAIL_TEST_ID.UPLOAD_TEST_ID.getValueAsInt())) {
                statRecords.get(archiveItemIndex + 1).tests_location = location;
                statRecords.get(archiveItemIndex + 1).upload_location = location;
                statRecords.get(archiveItemIndex + 1).upload_result = hrresult;

              }
              if (testnumber.equals("" + DETAIL_TEST_ID.DOWNLOAD_TEST_ID.getValueAsInt())) {
                statRecords.get(archiveItemIndex + 1).tests_location = location;
                statRecords.get(archiveItemIndex + 1).download_location = location;
                statRecords.get(archiveItemIndex + 1).download_result = hrresult;
              }

              if (testnumber.equals("" + DETAIL_TEST_ID.LATENCY_TEST_ID.getValueAsInt())) {
                statRecords.get(archiveItemIndex + 1).tests_location = location;
                statRecords.get(archiveItemIndex + 1).latency_location = location;
                statRecords.get(archiveItemIndex + 1).latency_result = hrresult;
              }

              if (testnumber.equals("" + DETAIL_TEST_ID.PACKETLOSS_TEST_ID.getValueAsInt())) {
                statRecords.get(archiveItemIndex + 1).tests_location = location;
                statRecords.get(archiveItemIndex + 1).packetloss_location = location;
                statRecords.get(archiveItemIndex + 1).packetloss_result = hrresult;
              }

              if (testnumber.equals("" + DETAIL_TEST_ID.JITTER_TEST_ID.getValueAsInt())) {
                statRecords.get(archiveItemIndex + 1).tests_location = location;
                statRecords.get(archiveItemIndex + 1).jitter_location = location;
                statRecords.get(archiveItemIndex + 1).jitter_result = hrresult;
              }

            } catch (JSONException je) {
              SKLogger.e(
                  this,
                  "Exception in reading JSONObject: "
                      + je.getMessage());
            }
          }
        }
      }

      // Log.d(this.getClass().toString(), "*** SamKnowsAggregateStatViewerActivity:UNPACK PASSIVE METRICS!!");

      // unpack passivemetrics
      results = null;
      try {
        results = archiveItem.getJSONArray("passivemetrics");
      } catch (JSONException je) {
        SKLogger.e(this,
            "Exception in reading JSONObject: " + je.getMessage());
      }

      if (results != null) {
        int itemcount = 0;

        for (itemcount = 0; itemcount < results.length(); itemcount++) {
          JSONObject user = null;
          try {
            user = results.getJSONObject(itemcount);
          } catch (JSONException je) {
            SKLogger.e(
                this,
                "Exception in reading JSONObject: "
                    + je.getMessage());
            user = null;
          }

          if (user == null) {
            continue;
          }

          captureUserMetricAtArchiveItemIndex(archiveItemIndex, user);
        }
      }

      //Trace.endSection();
    }

    private void captureUserMetricAtArchiveItemIndex(int archiveItemIndex,
                                                     JSONObject user) {
      if (user == null) {
        Log.e(this.getClass().toString(), "captureUserMetricAtArchiveItemIndex - user == null");
        return;
      }


      try {
        String metric = user.getString("metric");
        String value = user.getString("value");
        String type = user.getString("type");

        //Log.d("*** SamKnowsAggregateStatViewerActivity:MyPagerAdapter", "INFO - metric (" + metric +"), type=(" + type + "), value=(" + value + ")");

        // There is a completed disconnect between the integer metric value
        // as considered by the PassiveMetric class, and the layout "passive metric"
        // identifiers, such as R.id.passivemetric20.
        // The only safe thing to do, is to use as String value to determine
        // which resource id to use.

        if (metric.equals("connected")) { // connected
          statRecords.get(archiveItemIndex + 1).passivemetric1 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric1_type = type;
        } else if (metric.equals("connectivitytype")) { // connectivity
          // type
          statRecords.get(archiveItemIndex + 1).passivemetric2 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric2_type = type;
        } else if (metric.equals("gsmcelltowerid")) { // cell tower id
          // TODO - Giancarlo says this isn't displayed in SamKnowsAggregateStatViewerActivity - "Archived Result" (archive_result)
          // METRIC_TYPE.GSMCID("gsmcelltowerid")
          statRecords.get(archiveItemIndex + 1).passivemetric3 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric3_type = type;
        } else if (metric.equals("gsmlocationareacode")) { // cell tower
          // location area
          statRecords.get(archiveItemIndex + 1).passivemetric4 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric4_type = type;
        } else if (metric.equals("gsmsignalstrength")) { // signal strength
          statRecords.get(archiveItemIndex + 1).passivemetric5 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric5_type = type;
        } else if (metric.equals("networktype")) { // bearer
          statRecords.get(archiveItemIndex + 1).passivemetric6 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric6_type = type;
        } else if (metric.equals("networkoperatorname")) { // network
          // operator
          statRecords.get(archiveItemIndex + 1).passivemetric7_networkoperatorname = value;
          statRecords.get(archiveItemIndex + 1).passivemetric7_networkoperatorname_type = type;
        } else if (metric.equals("latitude")) { // latitude
          statRecords.get(archiveItemIndex + 1).passivemetric8 =
              SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString(value);
          statRecords.get(archiveItemIndex + 1).passivemetric8_type = type;
        } else if (metric.equals("longitude")) { // longitude
          statRecords.get(archiveItemIndex + 1).passivemetric9 =
              SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString(value);
          statRecords.get(archiveItemIndex + 1).passivemetric9_type = type;
        } else if (metric.equals("accuracy")) { // accuracy
          statRecords.get(archiveItemIndex + 1).passivemetric10 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric10_type = type;
        } else if (metric.equals("locationprovider")) { // location
          // provider
          statRecords.get(archiveItemIndex + 1).passivemetric11 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric11_type = type;
        } else if (metric.equals("simoperatorcode")) { // sim operator code
          statRecords.get(archiveItemIndex + 1).passivemetric12 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric12_type = type;
        } else if (metric.equals("simoperatorname")) { // sim operator name
          statRecords.get(archiveItemIndex + 1).passivemetric13 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric13_type = type;
        } else if (metric.equals("imei")) { // imei
          statRecords.get(archiveItemIndex + 1).passivemetric14 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric14_type = type;
        } else if (metric.equals("imsi")) { // imsi
          statRecords.get(archiveItemIndex + 1).passivemetric15 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric15_type = type;
        } else if (metric.equals("manufactor")) { // manufacturer
          statRecords.get(archiveItemIndex + 1).passivemetric16 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric16_type = type;
        } else if (metric.equals("model")) { // model
          statRecords.get(archiveItemIndex + 1).passivemetric17 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric17_type = type;
        } else if (metric.equals("ostype")) { // os type
          statRecords.get(archiveItemIndex + 1).passivemetric18 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric18_type = type;
        } else if (metric.equals("osversion")) { // os version
          statRecords.get(archiveItemIndex + 1).passivemetric19 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric19_type = type;
        } else if (metric.equals("gsmbiterrorrate")) { // gsmbiterrorrate
          statRecords.get(archiveItemIndex + 1).passivemetric20 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric20_type = type;
        } else if (metric.equals("cdmaecio")) { // cdmaecio
          statRecords.get(archiveItemIndex + 1).passivemetric21 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric21_type = type;
        } else if (metric.equals("phonetype")) { // phone type
          statRecords.get(archiveItemIndex + 1).passivemetric22 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric22_type = type;
        } else if (metric.equals("activenetworktype")) { // active network
          // type
          if (value.length() > 0) {
            String new_value = value.substring(0, 1).toUpperCase() + value.substring(1);
            statRecords.get(archiveItemIndex + 1).active_network_type = "(" + new_value + ")";
          }

          //views.get(i + 1).passivemetric23 = value;
          //views.get(i + 1).passivemetric23_type = type;
        } else if (metric.equals("connectionstatus")) { // connection
          // status
          statRecords.get(archiveItemIndex + 1).passivemetric24 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric24_type = type;
        } else if (metric.equals("roamingstatus")) { // roaming status
          statRecords.get(archiveItemIndex + 1).passivemetric25 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric25_type = type;
        } else if (metric.equals("networkoperatorcode")) { // network
          // operator code
          statRecords.get(archiveItemIndex + 1).passivemetric26 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric26_type = type;
        } else if (metric.equals("cdmasignalstrength")) { // cdmasignalstrength
          statRecords.get(archiveItemIndex + 1).passivemetric27 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric27_type = type;
        } else if (metric.equals("cdmabasestationid")) { // cdmabasestationid
          statRecords.get(archiveItemIndex + 1).passivemetric28 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric28_type = type;
        } else if (metric.equals("cdmabasestationlatitude")) { // cdmabasestationlatitude
          statRecords.get(archiveItemIndex + 1).passivemetric29 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric29_type = type;
        } else if (metric.equals("cdmabasestationlongitude")) { // cdmabasestationlongitude
          statRecords.get(archiveItemIndex + 1).passivemetric30 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric30_type = type;
        } else if (metric.equals("cdmanetworkid")) { // cdmanetworkid
          statRecords.get(archiveItemIndex + 1).passivemetric31 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric31_type = type;
        } else if (metric.equals("cdmasystemid")) { // cdmasystemid
          statRecords.get(archiveItemIndex + 1).passivemetric32 = value;
          statRecords.get(archiveItemIndex + 1).passivemetric32_type = type;
        } else {
          Log.d("SamKnowsAggregateStatViewerActivity:MyPagerAdapter", "WARNING - unsupported metric (" + metric + ")");
        }

      } catch (JSONException je) {
        Log.d("SamKnowsAggregateStatViewerActivity:MyPagerAdapter", "ERROR - exception reading JSON object (" + je.getMessage() + ")");

        SKLogger.e(
            this,
            "Exception in reading JSONObject: "
                + je.getMessage());
      }
    }

    @Override
    public void destroyItem(View view, int arg1, Object object) {
      ((ViewPager) view).removeView((StatView) object);
    }

    @Override
    public void finishUpdate(View arg0) {

    }

    @Override
    public int getItemPosition(Object object) {
      return POSITION_NONE;
    }

    @Override
    public int getCount() {
      return statRecords.size();
    }

    public SocialStrings getTextForSocialMedia(int position) {

      //"Now testing my actual #mobilebroadband speed Up:2, Down=3 using...";

      StatRecord sr = statRecords.get(position);

      String carrier = sr.passivemetric7_networkoperatorname;
      String download_result = sr.download_result;
      String upload_result = sr.upload_result;
      // The false means that this data is NOT averaged!
      return getStringsForSocialMediaForCarrierDownloadUpload(carrier, download_result, upload_result, false);
    }

    @Override
    public Object instantiateItem(View view, final int position) {

      //Trace.beginSection("instantiateItem");

      StatView sc = new StatView(SKAMainResultsActivity.this);
      sc.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
          LayoutParams.WRAP_CONTENT));
      sc.setFillViewport(true);

      LayoutInflater inflater = (LayoutInflater) SKAMainResultsActivity.this
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      View subview_archive;

      // If position is zero take care of the visibility of the messages
      if (position == 0) {
        //Trace.beginSection("ska_main_results_activity_runnow_and_graphs");
        subview = inflater.inflate(R.layout.ska_main_results_activity_runnow_and_graphs, null);
        //Trace.endSection();

        subview.setTag(position);

        if (SKApplication.getAppInstance().hideJitter() == true) {
          // Hide some elements!
          subview.findViewById(R.id.jitter_panel).setVisibility(View.GONE);
        }

        if (SKApplication.getAppInstance().hideJitterLatencyAndPacketLoss() == true) {
          // Hide some elements!
          subview.findViewById(R.id.jitter_panel).setVisibility(View.GONE);
          subview.findViewById(R.id.latency_panel).setVisibility(View.GONE);
          subview.findViewById(R.id.packetloss_panel).setVisibility(View.GONE);
        }

        sc.addView(subview);

        ((ViewPager) view).addView(sc);
        //if there is a problem with with the Test Maagner, display the
        //appropriate message

        setContinuousTestingButton();

        if (total_archive_records == 0) {
          // No data!
          mMainResultsActivity.findViewById(R.id.test_last_run).setVisibility(View.INVISIBLE);
        } else {
          // Data!
          TextView tv = (TextView) subview.findViewById(R.id.no_data_message_text);
          ImageView iv = (ImageView) subview.findViewById(R.id.no_data_message_image);
          tv.setVisibility(View.GONE);
          iv.setVisibility(View.GONE);
          // data icon
          // & message
          mMainResultsActivity.findViewById(R.id.test_last_run).setVisibility(View.VISIBLE);
        }

        // If we have at least one test result, load the first item.
        if (total_archive_records > 0) {
          adapter.readArchiveItem(0);
        }

        String caption = getString(R.string.last_run) + " " + last_run_test_formatted;
        ((TextView) findViewById(R.id.test_last_run)).setText(caption);

        if (SKApplication.getAppInstance().hideJitter() == true) {
          // Hide some elements!
          subview.findViewById(R.id.jitter_panel).setVisibility(View.GONE);
        }

        if (SKApplication.getAppInstance().hideJitterLatencyAndPacketLoss() == true) {
          // Hide some elements!
          subview.findViewById(R.id.jitter_panel).setVisibility(View.GONE);
          subview.findViewById(R.id.latency_panel).setVisibility(View.GONE);
          subview.findViewById(R.id.packetloss_panel).setVisibility(View.GONE);
        }

        buttonSetup();

        loadAverage();
        graphsSetup();

        loadGrids();
      }

      //Trace.beginSection("ska_main_results_activity_single_result");
      subview_archive = inflater.inflate(R.layout.ska_main_results_activity_single_result, null);
      //Trace.endSection();

      if (SKApplication.getAppInstance().hideJitter() == true) {
        // Hide some elements!
        subview_archive.findViewById(R.id.jitter_archive_panel).setVisibility(View.GONE);
      }

      if (SKApplication.getAppInstance().hideJitterLatencyAndPacketLoss() == true) {
        // Hide some elements!
        subview_archive.findViewById(R.id.packetloss_archive_panel).setVisibility(View.GONE);
        subview_archive.findViewById(R.id.latency_archive_panel).setVisibility(View.GONE);
        subview_archive.findViewById(R.id.jitter_archive_panel).setVisibility(View.GONE);
      }

      //
      // Show or hide the passive results, depending on whether
      // we're looking at mobile (show) or WiFi (hide!)
      //
      LinearLayout passiveResultsLinearLayout = (LinearLayout) subview_archive.findViewById(R.id.passive_results_linearlayout);
      if (SKApplication.getNetworkTypeResults() == eNetworkTypeResults.eNetworkTypeResults_WiFi) {
        // Hide for WiFi results!
        passiveResultsLinearLayout.setVisibility(View.GONE);
      } else {
        // Show for Mobile results!
        passiveResultsLinearLayout.setVisibility(View.VISIBLE);
      }

      //
      // Prepare left/right buttons etc. ..
      //
      Button leftButton = (Button) subview_archive.findViewById(R.id.page_turn_left);
      Button rightButton = (Button) subview_archive.findViewById(R.id.page_turn_right);
      if (position == total_archive_records) {
        rightButton.setVisibility(View.INVISIBLE);
      } else {
        rightButton.setVisibility(View.VISIBLE);
      }

      leftButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          int currentPage = viewPager.getCurrentItem();
          //mMainResultsActivity.handleOnPageSelected(currentPage - 1);
          // This is how you scroll "left" to the previous page (we always
          // start on page "zero"...)
          viewPager.setCurrentItem(currentPage - 1);
        }
      });

      rightButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          int currentPage = viewPager.getCurrentItem();
          // mMainResultsActivity.handleOnPageSelected(currentPage + 1);
          // This is how you scroll "right" to the previous page (we always
          // start on page "zero"...)

          // https://code.google.com/p/android/issues/detail?id=27526
          int targetPage = currentPage + 1;
          int pageCount = viewPager.getChildCount();
          //SKLogger.sAssert(getClass(),  pageCount >= currentPage);

          viewPager.setCurrentItem(targetPage);
          //int testCurrentPage = viewPager.getCurrentItem();
          //SKLogger.sAssert(getClass(),  testCurrentPage == (currentPage+1));
        }
      });

      if (position > 0) {
        subview_archive.setTag(position);
        sc.addView(subview_archive);

        StatRecord sr = statRecords.get(position);

        sc.setData(sr);

        ((ViewPager) view).addView(sc);

        if (position == total_archive_records) {
          sc.setRightPageIndicator(false);
        }

        // http://stackoverflow.com/questions/9235374/confusion-on-position-in-instantiateitem-function-in-class-pageradapter-andro
        // Inside of instantiateItem, the position parameter is the position that is in need of rendering.
        // It is NOT the position of the currently focused item that the user would see.
        // The pages to the left and right of the currently displayed view need to be pre rendered in memory
        // so that the animations to those screens will be smooth.
        if (position < total_archive_records) {
          // Position (page number) is:
          //    0, 1, 2
          // Archive item index is:
          //    NA 0  1
          // It looks like we should pass (position-1) here, but if we do that, we get un-populated
          // archived results pages!
          // TODO - figure out why that is the case.
          adapter.readArchiveItem(position);
        }
      }

      Util.overrideFonts(SKAMainResultsActivity.this, sc);

      //Trace.endSection();

      return sc;
    }

    public void loadGrids() {

      loadDownloadGrid(DETAIL_TEST_ID.DOWNLOAD_TEST_ID,
          R.id.download_results_tablelayout, 0, ITEMS_PER_PAGE);
      loadDownloadGrid(DETAIL_TEST_ID.UPLOAD_TEST_ID,
          R.id.upload_results_tablelayout, 0, ITEMS_PER_PAGE);
      loadDownloadGrid(DETAIL_TEST_ID.LATENCY_TEST_ID,
          R.id.latency_results_tablelayout, 0, ITEMS_PER_PAGE);
      loadDownloadGrid(DETAIL_TEST_ID.PACKETLOSS_TEST_ID,
          R.id.packetloss_results_tablelayout, 0, ITEMS_PER_PAGE);
      loadDownloadGrid(DETAIL_TEST_ID.JITTER_TEST_ID,
          R.id.jitter_results_tablelayout, 0, ITEMS_PER_PAGE);

      LinearLayout l = (LinearLayout) findViewById(R.id.download_content);
      l.setVisibility(View.GONE);
      l.getLayoutParams().height = 0;

      l = (LinearLayout) findViewById(R.id.upload_content);
      l.setVisibility(View.GONE);
      l.getLayoutParams().height = 0;

      l = (LinearLayout) findViewById(R.id.latency_content);
      l.setVisibility(View.GONE);
      l.getLayoutParams().height = 0;

      l = (LinearLayout) findViewById(R.id.packetloss_content);
      l.setVisibility(View.GONE);
      l.getLayoutParams().height = 0;

      l = (LinearLayout) findViewById(R.id.jitter_content);
      l.setVisibility(View.GONE);
      l.getLayoutParams().height = 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
      return view == object;
    }

    @Override
    public void restoreState(Parcelable arg0, ClassLoader arg1) {

    }

    @Override
    public Parcelable saveState() {
      return null;
    }

    @Override
    public void startUpdate(View arg0) {

    }
  }

  //private int mWeeks = 1;
  private DATERANGE_1w1m3m1y mDateRange = DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_WEEK;

  private void SingleChoice() {
    Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.choose_time));
    // dropdown setup
    final String array_spinner[];

    if (SKApplication.getAppInstance().supportOneDayResultView() == true) {
      array_spinner = new String[5];
      array_spinner[0] = getString(R.string.time_period_1day);
      array_spinner[1] = getString(R.string.time_period_1week);
      array_spinner[2] = getString(R.string.time_period_1month);
      array_spinner[3] = getString(R.string.time_period_3months);
      array_spinner[4] = getString(R.string.time_period_1year);
    } else {
      array_spinner = new String[4];
      array_spinner[0] = getString(R.string.time_period_1week);
      array_spinner[1] = getString(R.string.time_period_1month);
      array_spinner[2] = getString(R.string.time_period_3months);
      array_spinner[3] = getString(R.string.time_period_1year);
    }

    builder.setItems(array_spinner, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

        String value = array_spinner[which];
        Button tvHeader = (Button) findViewById(R.id.btn_timeperiod);

        if (value.equals(getString(R.string.time_period_1week))) {
          tvHeader.setText(R.string.results_for_1_week);
          mDateRange = DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_WEEK;
        } else if (value.equals(getString(R.string.time_period_1month))) {
          tvHeader.setText(R.string.results_for_1_month);
          mDateRange = DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_MONTH;
        } else if (value.equals(getString(R.string.time_period_3months))) {
          tvHeader.setText(R.string.results_for_3_months);
          mDateRange = DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_THREE_MONTHS;
        } else if (value.equals(getString(R.string.time_period_1year))) {
          tvHeader.setText(R.string.results_for_1_year);
          mDateRange = DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_YEAR;
        } else if (value.equals(getString(R.string.time_period_1day))) {
          tvHeader.setText(R.string.results_for_1_day);
          mDateRange = DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_DAY;
        } else {
          tvHeader.setText(R.string.results_for_1_week);
          Log.e(this.getClass().toString(), "onClick - value out of range=" + value);
        }

        // Query average data, and update charts - this might make them invisible, if there is no data!
        queryAverageDataAndUpdateTheCharts();

        dialog.dismiss();
      }
    });
    builder.setNegativeButton(getString(R.string.cancel),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });
    AlertDialog alert = builder.create();
    alert.show();
  }

  // Query average data, and update charts - this might make them invisible, if there is no data!
  void queryAverageDataAndUpdateTheCharts() {
    loadAverage();

    // Update charts - this might make them invisible, if there is no data!
    setGraphDataForColumnIdAndHideIfNoResultsFound(DETAIL_TEST_ID.DOWNLOAD_TEST_ID);
    setGraphDataForColumnIdAndHideIfNoResultsFound(DETAIL_TEST_ID.UPLOAD_TEST_ID);
    setGraphDataForColumnIdAndHideIfNoResultsFound(DETAIL_TEST_ID.LATENCY_TEST_ID);
    setGraphDataForColumnIdAndHideIfNoResultsFound(DETAIL_TEST_ID.PACKETLOSS_TEST_ID);
    setGraphDataForColumnIdAndHideIfNoResultsFound(DETAIL_TEST_ID.JITTER_TEST_ID);
  }

  final int cRunTestActivityRequestCode = 99999999;

  private boolean checkIfIsConnectedAndIfNotShowAnAlert() {

    if (NetworkDataCollector.sGetIsConnected() == true) {
      return true;
    }

    // We're not connected - show an alert if possible, and return false!
    if (!isFinishing()) {
      new AlertDialog.Builder(this)
          .setMessage(R.string.Offline_message)
          .setPositiveButton(R.string.ok_dialog, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
          }).show();
    }

    return false;
  }

  private void RunChoice() {

    storage = CachingStorage.getInstance();
    config = storage.loadScheduleConfig();
    // if config == null the app is not been activated and
    // no test can be run
    if (config == null) {
      // TODO Add an alert that the app has not been initialised yet
      config = new ScheduleConfig();
    }

    // Only allow this to continue, if we're connected!
    if (checkIfIsConnectedAndIfNotShowAnAlert() == false) {
      return;
    }

    if (SKApplication.getAppInstance().allowUserToSelectTestToRun() == false) {
      //
      // Many app variants - always run all tests.
      // This is identified by setting testID to -1 ...!
      //
      Intent intent = new Intent(SKAMainResultsActivity.this, SKARunningTestActivity.class);
      Bundle b = new Bundle();
      b.putInt(SKARunningTestActivity.cTestIdToRunMinusOneMeansAll, -1);
      intent.putExtras(b);
      startActivityForResult(intent, cRunTestActivityRequestCode);
      overridePendingTransition(R.anim.transition_in, R.anim.transition_out);
      return;
    }

    //
    // User must select the test to run
    ///
    testList = config.manual_tests;
    SKLogger.sAssert(getClass(), testList.size() > 0);

    array_spinner.clear();
    array_spinner_int.clear();

    // Do NOT include a 'closest target' test in the list!
    int i;
    for (i = 0; i < testList.size(); i++) {
      if (testList.get(i).type.equals(SKConstants.TEST_TYPE_CLOSEST_TARGET)) {
        // Ignore it!
      } else {
        TestDescription td = testList.get(i);
        Log.d(TAG, "TEST NAME=" + td.displayName + ", testID=" + td.testId.getValueAsInt());
        array_spinner.add(td.displayName);
        array_spinner_int.add(td.testId);
      }
    }
    SKLogger.sAssert(getClass(), array_spinner.size() > 0);
    SKLogger.sAssert(getClass(), array_spinner_int.size() == array_spinner.size());

    Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.choose_test));
    // dropdown setup

    array_spinner.add(getString(R.string.all));
    array_spinner_int.add(SCHEDULE_TEST_ID.ALL_TESTS);

    String theItems[] = new String[array_spinner.size()];
    i = 0;
    for (String value : array_spinner) {
      theItems[i] = value;
      i++;
    }

    builder.setItems(theItems, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

        dialog.dismiss();

        Intent intent = new Intent(
            SKAMainResultsActivity.this,
            SKARunningTestActivity.class);
        Bundle b = new Bundle();
        b.putInt(SKARunningTestActivity.cTestIdToRunMinusOneMeansAll, array_spinner_int.get(which).getValueAsInt());
        intent.putExtras(b);
        startActivityForResult(intent, cRunTestActivityRequestCode);
        overridePendingTransition(R.anim.transition_in,
            R.anim.transition_out);
      }
    });
    builder.setNegativeButton(getString(R.string.cancel),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });
    AlertDialog alert = builder.create();
    alert.show();
  }

  private ContinuousTestRunner mContinuousTestRunner = null;
  private ContinuousTestRunner.TestRunnerState mContinuousState = ContinuousTestRunner.TestRunnerState.STOPPED;
  private Handler mContinuousHandler;

  private void continuousStateChanged(ContinuousTestRunner.TestRunnerState newState) {

    mContinuousState=newState;

    setContinuousTestingButton();

    if(mContinuousState== ContinuousTestRunner.TestRunnerState.STOPPED)
    {
      adapter = new MyPagerAdapter(SKAMainResultsActivity.this);
      setTotalArchiveRecords();
      //viewPager = (ViewPager) findViewById(R.id.viewPager);
      SKLogger.sAssert(getClass(), viewPager == (ViewPager) findViewById(R.id.viewPager));
      viewPager.setAdapter(adapter);
    }
  }

	void startContinuousTestAfterCheckingForDataCap() {

		Button b = (Button) findViewById(R.id.btnRunContinuousTests);

    SKTestRunner.SKTestRunnerObserver observer = new SKTestRunner.SKTestRunnerObserver() {
      @Override
      public void onTestProgress(JSONObject pm) {

      }

      @Override
      public void onTestResult(JSONObject pm) {

      }

      @Override
      public void onPassiveMetric(JSONObject o) {

      }

      @Override
      public void OnChangedStateTo(SKTestRunner.TestRunnerState state) {
        SKAMainResultsActivity.this.continuousStateChanged(state);
      }

      @Override
      public void OnUDPFailedSkipTests() {

      }

      @Override
      public void OnClosestTargetSelected(String closestTarget) {

      }

      @Override
      public void OnCurrentLatencyCalculated(long latencyMilli) {

      }
    };

    mContinuousTestRunner = new ContinuousTestRunner(observer);
    mContinuousTestRunner.startTestRunning_RunInBackground();

		b.setText(R.string.starting_continuous);
	}
	
	private void ContinuousToggle(View v){

		//CONTINUOUS TESTING

		if (mContinuousState == ContinuousTestRunner.TestRunnerState.STOPPED) {
		
			// Only allow this to continue, if we're connected!
    		if (checkIfIsConnectedAndIfNotShowAnAlert() == false) {
    			return;
	    	}
		
			String showWithMessage = "";

			if (SKApplication.getAppInstance().getIsDataCapEnabled() == true) {
				if (SK2AppSettings.getSK2AppSettingsInstance().isDataCapReached()) {
					Log.d(TAG, "Data cap exceeded");
					showWithMessage = getString(R.string.data_cap_exceeded);
				} else {

					// Create a manual test (which we do NOT run!), to allow us to measure if we might
					// exceed the test limit!
					StringBuilder errorDescription = new StringBuilder();

          SKTestRunner.SKTestRunnerObserver observerNull = null;
					ManualTestRunner mt = ManualTestRunner.create(observerNull, errorDescription);
					if (mt == null) {
						String theErrorString = errorDescription.toString();
						if (theErrorString.length() == 0) {
							theErrorString = getString(R.string.unexpected_error);
						}

						Log.d(TAG,
								"Impossible to run continuous tests");
						new AlertDialog.Builder(this)
						.setMessage(theErrorString)
						.setPositiveButton(R.string.ok_dialog,
								new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
							}
						}).show();
						return;
					}
					
					if (SK2AppSettings.getSK2AppSettingsInstance().isDataCapLikelyToBeReached(mt.getNetUsage())) {

						// Data cap exceeded - but only ask the user if they want to continue, if the app is configured
						// to work like that...
						Log.d(TAG, "Data cap likely to be exceeded");
						showWithMessage = getString(R.string.data_cap_might_be_exceeded);
					}
				}
			}
			
			if (showWithMessage.length() > 0) {
		
				// Data cap exceeded limit hit, or will be hit!

				Log.d(TAG, "Data cap exceeded");
				new AlertDialog.Builder(this)
				.setMessage(showWithMessage)
				.setPositiveButton(R.string.ok_dialog,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int id) {
						startContinuousTestAfterCheckingForDataCap();
					}
				})
				.setNegativeButton(R.string.no_dialog,
						new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog,
							int which) {
					}
				}).show();
				return;
			} else {
				startContinuousTestAfterCheckingForDataCap();
			}


		} else if (mContinuousState == ContinuousTestRunner.TestRunnerState.EXECUTING) {

      if (mContinuousTestRunner == null) {
        SKLogger.sAssert(false);
      } else {
        mContinuousTestRunner.stopTestRunning();
        mContinuousTestRunner = null;
      }

			((Button) v).setText(R.string.stopping_continuous);
		}

	}
	

	
//	private boolean mContinuousRequested = false;
//	private MainService mMainService = null;
//	private ServiceConnection mConnection;
	
	public boolean forceBackToAllowClose() {
		if (on_aggregate_page) {
			return true;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
	
		// This screen has strange behaviour on the "Back" button being pressed.
		// It is the task root - so, normally, pressing back would terminate the application.
		// However, pressing back actually "changes page" until we reach the "aggregate page";
		// at which point, we can try to close the app if required.

		if (on_aggregate_page) {
			// The app can be closed from this page - we expect to be the task root.
			if (this.wouldBackButtonReturnMeToTheHomeScreen()) {
				super.onBackPressed();
				return;
			}
		} else {
			// The app must NOT be closed from this page - change "viewed page" instead.
			//viewPager = (ViewPager) findViewById(R.id.viewPager);
			// viewPager.setAdapter(adapter);
			viewPager.setCurrentItem(0, true); // The true means to perform a smooth scroll!
			// overridePendingTransition(0, 0);
			on_aggregate_page = true;
		}
	}

	// This method will query the data synchronously, for the specified column.

	private JSONObject fetchGraphDataForColumnId(DETAIL_TEST_ID PColumnId) {
		Calendar fromCal = Calendar.getInstance();
		
		lookBackwardInTime(fromCal);
		
		long startTime = fromCal.getTimeInMillis();

		Calendar upToCal = Calendar.getInstance();
		long upToTime = upToCal.getTimeInMillis();
		
		if (!(startTime < upToTime)) {
			Log.e(this.getClass().toString(), "getDataForColumnId - startTime/upToTime out of range mis-matched");
		}
		
		JSONObject data = dbHelper.fetchGraphData(PColumnId, startTime, upToTime, mDateRange);	
		return data;
	}
	

    // This method will set the graph data and update, based on the specified column;
	// and will make the container layout invisible if there are no results in that data.
	//
	// GIVEN: the response data has been queried synchronously from the local database for the specified column
	// WHEN: there are NO results in the response data
	// THEN: the graph layout will be made invisible (GONE)
	
	private boolean setGraphDataForColumnIdAndHideIfNoResultsFound(DETAIL_TEST_ID PColumnId) {
		boolean buttonFound = false;
		
		JSONObject data = null;
		try {
			data = fetchGraphDataForColumnId(PColumnId);
			if (data.getJSONArray("results").length() > 0) {
				buttonFound = true;
			}
		} catch (JSONException e1) {
		}
		
		LinearLayout l = null;

		switch (PColumnId) {
		case DOWNLOAD_TEST_ID:
			l = (LinearLayout) findViewById(R.id.download_content);
			graphHandlerDownload.updateGraphWithTheseResults(data, mDateRange);
			break;
		case UPLOAD_TEST_ID:
			l = (LinearLayout) findViewById(R.id.upload_content);
			graphHandlerUpload.updateGraphWithTheseResults(data, mDateRange);
			break;
		case LATENCY_TEST_ID:
			l = (LinearLayout) findViewById(R.id.latency_content);
			graphHandlerLatency.updateGraphWithTheseResults(data, mDateRange);
			break;
		case PACKETLOSS_TEST_ID:
			l = (LinearLayout) findViewById(R.id.packetloss_content);
			graphHandlerPacketLoss.updateGraphWithTheseResults(data, mDateRange);
			break;
		case JITTER_TEST_ID:
			l = (LinearLayout) findViewById(R.id.jitter_content);
			graphHandlerJitter.updateGraphWithTheseResults(data, mDateRange);
			break;
		default:
			Log.e(this.getClass().toString(), "setGraphDataForColumnIdAndHideIfNoResultsFound - unexpected result");
			return buttonFound;
		}
		
		if (l == null) {
			Log.e(this.getClass().toString(), "setGraphDataForColumnIdAndHideIfNoResultsFound - l == null");
		}

		// Changed to NO LONGER HIDE if not visible; as that is unhelpful
		// behaviour now that we have a network type results toggle!
//		if (buttonFound) {
////			if (PForceVisibleIfThereIsData == false) {
////				// Button found, but do NOT set visibility to VISIBLE.
////			} else {
////				// Set button visibility to VISIBLE.
////				l.setVisibility(View.VISIBLE);
////			}
//		} else {
//		    // Not found!
//   		    l.setVisibility(View.GONE);
//		}
		
		return buttonFound;
	}

	@Override
	public void onClick(View v) {
		// Toast.makeText(this,"clicked ..."+v.getId(),3000).show();

		int grid = 0;
		boolean buttonfound = false;
		ImageView button = null;
		LinearLayout l = null;
		DETAIL_TEST_ID testid = DETAIL_TEST_ID.DOWNLOAD_TEST_ID;

		int id = v.getId();
		if (id == R.id.download_header || id == R.id.btn_download_toggle) {
   		testid = DETAIL_TEST_ID.DOWNLOAD_TEST_ID;
			buttonfound = setGraphDataForColumnIdAndHideIfNoResultsFound(testid);
			button = (ImageView) findViewById(R.id.btn_download_toggle);
			l = (LinearLayout) findViewById(R.id.download_content);
			grid = R.id.download_results_tablelayout;
		}

		if (id == R.id.upload_header || id == R.id.btn_upload_toggle) {
   		testid = DETAIL_TEST_ID.UPLOAD_TEST_ID;
			buttonfound = setGraphDataForColumnIdAndHideIfNoResultsFound(testid);
			button = (ImageView) findViewById(R.id.btn_upload_toggle);
			l = (LinearLayout) findViewById(R.id.upload_content);
			grid = R.id.upload_results_tablelayout;
		}

		if (id == R.id.latency_header || id == R.id.btn_latency_toggle) {
   		testid = DETAIL_TEST_ID.LATENCY_TEST_ID;
			buttonfound = setGraphDataForColumnIdAndHideIfNoResultsFound(testid);
			button = (ImageView) findViewById(R.id.btn_latency_toggle);
			l = (LinearLayout) findViewById(R.id.latency_content);
			grid = R.id.latency_results_tablelayout;
		}

		if (id == R.id.packetloss_header || id == R.id.btn_packetloss_toggle) {
   		testid = DETAIL_TEST_ID.PACKETLOSS_TEST_ID;
			buttonfound = setGraphDataForColumnIdAndHideIfNoResultsFound(testid);
			button = (ImageView) findViewById(R.id.btn_packetloss_toggle);
			l = (LinearLayout) findViewById(R.id.packetloss_content);
			grid = R.id.packetloss_results_tablelayout;
		}

		if (id == R.id.jitter_header || id == R.id.btn_jitter_toggle) {
   		testid = DETAIL_TEST_ID.JITTER_TEST_ID;
			buttonfound = setGraphDataForColumnIdAndHideIfNoResultsFound(testid);
			button = (ImageView) findViewById(R.id.btn_jitter_toggle);
			l = (LinearLayout) findViewById(R.id.jitter_content);
			grid = R.id.jitter_results_tablelayout;
		}

		// actions

		if (l.getVisibility() != View.VISIBLE) {

			button.setBackgroundResource(R.drawable.btn_open_selector);
			button.setContentDescription(getString(R.string.close_panel));
			// graphHandler1.update();
			clearGrid(grid);
			loadDownloadGrid(testid, grid, 0, ITEMS_PER_PAGE);

			l.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
			l.setVisibility(View.VISIBLE);

		} else {

			l.setVisibility(View.GONE);

			button.setBackgroundResource(R.drawable.btn_closed_selector);
			button.setContentDescription(getString(R.string.open_panel));
		}

	}

	public class MyAnimationListener implements AnimationListener {
		View view;

		public void setView(View view) {
			this.view = view;
		}

		public void onAnimationEnd(Animation animation) {
			view.setVisibility(View.INVISIBLE);
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
		}
	}
	
    // http://stackoverflow.com/questions/7175873/click-effect-on-button-in-android?lq=1
    private static AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.7F);
    
    public static void sSetButtonEffect(View button){
    	
        button.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                    	buttonClick.setDuration(500);
                    	v.startAnimation(buttonClick);
                        //v.getBackground().setColorFilter(0xe0f47521,android.graphics.PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
//                    case MotionEvent.ACTION_UP: {
//                        //v.getBackground().clearColorFilter();
//                        v.invalidate();
//                        break;
//                    }
                }
                return false;
            }
        });
    }
}
