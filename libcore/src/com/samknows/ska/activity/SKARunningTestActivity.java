package com.samknows.ska.activity;

import org.json.JSONException;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.TestRunner.ManualTestRunner;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.Storage;
import com.samknows.libcore.R;
import com.samknows.measurement.activity.BaseLogoutActivity;
import com.samknows.measurement.activity.components.FontFitTextView;
import com.samknows.measurement.activity.components.ProgressWheel;
import com.samknows.measurement.activity.components.Util;
import com.samknows.measurement.environment.NetworkDataCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.storage.StorageTestResult.*;
import com.samknows.measurement.schedule.TestDescription.*;
import com.samknows.measurement.storage.StorageTestResult;
import com.samknows.tests.ClosestTarget;

public class SKARunningTestActivity extends BaseLogoutActivity {

	private Context cxt;
	public Handler handler;
	private ProgressWheel pw;
	private ManualTestRunner mt;
	int page;
	int result = 0;
	
	private String mActiveNetworkType = "mobile";

	Storage storage;
	ScheduleConfig config;

  TextView runningTestWithClosestTarget = null;

  public final static String cTestIdToRunMinusOneMeansAll = "cTestIdToRunMinusOneMeansAll";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		cxt = this;

		Bundle b = getIntent().getExtras();
		SCHEDULE_TEST_ID testIdToRunMinusOneMeansAll = SCHEDULE_TEST_ID.ALL_TESTS;

		if (b != null) {
			int testIdAsInt = b.getInt(cTestIdToRunMinusOneMeansAll);
      testIdToRunMinusOneMeansAll = SCHEDULE_TEST_ID.sGetTestIdForInt(testIdAsInt);
		}

		storage = CachingStorage.getInstance();
		config = storage.loadScheduleConfig();
		if (config == null) {
			config = new ScheduleConfig();
		}
		
		this.setTitle(R.string.running_test);

		//
		// Load the layout!
		//
		setContentView(R.layout.ska_running_test_activity);

		LinearLayout passiveMetricsLayout = (LinearLayout) findViewById(R.id.passive_metrics);
		TextView activeMetricsTextView = (TextView) findViewById(R.id.active_metrics_textview);
		runningTestWithClosestTarget = (TextView) findViewById(R.id.running_test_with_closest_target);

		if (SKApplication.getAppInstance().hideJitter() == true) {
			// Hide some elements!
			findViewById(R.id.jitter_test_panel).setVisibility(View.GONE);	
		}

		if (SKApplication.getAppInstance().hideJitterLatencyAndPacketLoss() == true) {
			// Hide some elements!
			findViewById(R.id.latency_test_panel).setVisibility(View.GONE);
			findViewById(R.id.packetloss_test_panel).setVisibility(View.GONE);
		}

		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		if ((networkInfo == null) || (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
			// Show for Mobile results, or if not known!
			passiveMetricsLayout.setVisibility(View.VISIBLE);
			activeMetricsTextView.setText(getString(R.string.active_metrics_mobile));
		} else {
			// Hide for WiFi results!
			passiveMetricsLayout.setVisibility(View.GONE);
			activeMetricsTextView.setText(getString(R.string.active_metrics_wifi));
		}

		Util.initializeFonts(this);
		Util.overrideFonts(this, findViewById(android.R.id.content));
		try {

			handler = new Handler() {

				@Override
				public void handleMessage(Message msg) {
          doHandleMessage(msg);
				}

      };

			launchTest(testIdToRunMinusOneMeansAll);
		} catch (Throwable t) {
			SKLogger.e(this, "handler or test failure", t);
		}
	}

  private void doHandleMessage(Message msg) {
    TextView tv = null;
    FontFitTextView fftv = null;

    JSONObject message_json;
    message_json = (JSONObject) msg.obj;
    String value;
    int success;
    int testnameAsInt;
    int status_complete;
    int metric;

    String hostUrl = ClosestTarget.sGetClosestTarget();
    if (hostUrl.length() != 0) {
      String name = config.hosts.get(hostUrl);
      if (name == null) {
        name = hostUrl;
      }

      if (name.length() <= 1) {
        // For now, just show the "Finding..." message as the text.
        runningTestWithClosestTarget.setText(R.string.TEST_Label_Finding_Best_Target);
      } else if (SKApplication.getAppInstance().getDoesAppDisplayClosestTargetInfo()) {
        // Show "Best Target - myserver" as the text.
        runningTestWithClosestTarget.setText(getString(R.string.running_test_closest_target) + name);
      } else {
// Show "Running Test"
        runningTestWithClosestTarget.setText(R.string.running_test);
      }

    }

    try {

      String type = message_json.getString(StorageTestResult.JSON_TYPE_ID);

      if (type == "completed") {

        result = 1;

        if (SKARunningTestActivity.this.checkIfIsConnectedAndIfNotShowAnAlertThenFinish() == false)
        {
          // The alert that is shown, handles the "finish()"
        } else {
          SKARunningTestActivity.this.finish();
          overridePendingTransition(0, 0);
        }
      }

      if (type == "test") {
        testnameAsInt = message_json .getInt(StorageTestResult.JSON_TESTNUMBER);
        status_complete = message_json
            .getInt(StorageTestResult.JSON_STATUS_COMPLETE);
        value = message_json.getString(StorageTestResult.JSON_HRRESULT);
        if (status_complete == 100 && message_json.has(StorageTestResult.JSON_SUCCESS)) {

          success = message_json
              .getInt(StorageTestResult.JSON_SUCCESS);
          if (success == 0) {
            value = getString(R.string.failed);
          }
        }

        DETAIL_TEST_ID testId = DETAIL_TEST_ID.sGetTestIdForInt(testnameAsInt);

        switch (testId) {
          // active metrics
          case DOWNLOAD_TEST_ID:
            pw = (ProgressWheel) findViewById(R.id.ProgressWheel1);
            fftv = (FontFitTextView) findViewById(R.id.download_result);
            pw.setProgress((int) (status_complete * 3.6));
            pw.setContentDescription("Status "
                + status_complete + "%");
            if (status_complete == 100) {
              pw.setVisibility(View.GONE);
              fftv.setText(value);
              fftv.setContentDescription(getString(R.string.download)
                  + " " + value);
              fftv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            } else {
              pw.setVisibility(View.VISIBLE);
              fftv.setText("");
            }
            break;
          case UPLOAD_TEST_ID:
            pw = (ProgressWheel) findViewById(R.id.ProgressWheel2);
            fftv = (FontFitTextView) findViewById(R.id.upload_result);
            pw.setProgress((int) (status_complete * 3.6));
            pw.setContentDescription("Status "
                + status_complete + "%");
            if (status_complete == 100) {
              pw.setVisibility(View.GONE);

              fftv.setText(value);
              fftv.setContentDescription(getString(R.string.upload)
                  + " " + value);
              fftv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            } else {
              pw.setVisibility(View.VISIBLE);
              fftv.setText("");
            }
            break;
          case PACKETLOSS_TEST_ID:
            pw = (ProgressWheel) findViewById(R.id.ProgressWheel3);
            fftv = (FontFitTextView) findViewById(R.id.packetloss_result);
            pw.setProgress((int) (status_complete * 3.6));
            pw.setContentDescription("Status " + status_complete + "%");
            if (status_complete == 100) {
              // We MUST restore the packet loss result field after the progress spinner has finished,
              // otherwise, we can see "Perda de pacote" truncated to just "Perda da"...
              fftv.setVisibility(View.VISIBLE);
              pw.setVisibility(View.GONE);
              fftv.setText(value);
              fftv.setContentDescription(getString(R.string.packet_loss)
                  + " " + value);
              fftv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            } else {
              pw.setVisibility(View.VISIBLE);
              // We MUST hide the packet loss result field while the progress spinner is present,
              // otherwise, we can see "Perda de pacote" truncated to just "Perda da"
              fftv.setVisibility(View.GONE);
              fftv.setText("");
            }
            break;
          case LATENCY_TEST_ID:
            pw = (ProgressWheel) findViewById(R.id.ProgressWheel4);
            fftv = (FontFitTextView) findViewById(R.id.latency_result);
            pw.setProgress((int) (status_complete * 3.6));
            pw.setContentDescription("Status "
                + status_complete + "%");
            if (status_complete == 100) {
              pw.setVisibility(View.GONE);
              fftv.setText(value);
              fftv.setContentDescription(getString(R.string.latency)
                  + " " + value);
              fftv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            } else {
              pw.setVisibility(View.VISIBLE);
              fftv.setText("");
            }
            break;
          case JITTER_TEST_ID:
            pw = (ProgressWheel) findViewById(R.id.JitterProgressWheel);
            fftv = (FontFitTextView) findViewById(R.id.jitter_result);
            pw.setProgress((int) (status_complete * 3.6));
            pw.setContentDescription("Status "
                + status_complete + "%");
            if (status_complete == 100) {
              pw.setVisibility(View.GONE);
              fftv.setText(value);
              fftv.setContentDescription(getString(R.string.jitter)
                  + " " + value);
              fftv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            } else {
              pw.setVisibility(View.VISIBLE);
              fftv.setText("");
            }
            break;

        }
      }

      if (type == "passivemetric") {
        metric = message_json.getInt("metric");
        String metricString = message_json.getString("metricString");
        value = message_json.getString("value");

        if (metricString.equals("invisible")) {
        } else {
          // There is a complete disconnect between the integer metric value
          // returned from the PassiveMetric class, and the layout "passive metric" identifiers
          // such as R.id.passivemetric20.
          // The only safe thing to do, is to look at the metricString value,
          // to determine which resource id to use.
          Log.d(this.getClass().getName(), "metric=" + metric + ", metricString=" + metricString + ", value=" + value);
          if (metricString.equals("connected")) { // connected
            metric = 1;
          } else if (metricString.equals("connectivitytype")) { // connectivity
            metric = 2;
          } else if (metricString.equals("gsmcelltowerid")) { // cell tower id
            metric = 3;
          } else if (metricString.equals("gsmlocationareacode")) { // cell tower
            metric = 4;
          } else if (metricString.equals("gsmsignalstrength")) { // signal strength
            metric = 5;
          } else if (metricString.equals("networktype")) { // bearer
            metric = 6;
          } else if (metricString.equals("networkoperatorname")) { // network operator name
            metric = 7;
          } else if (metricString.equals("latitude")) { // latitude
            metric = 8;
          } else if (metricString.equals("longitude")) { // longitude
            metric = 9;
          } else if (metricString.equals("accuracy")) { // accuracy
            metric = 10;
          } else if (metricString.equals("locationprovider")) { // location
            metric = 11;
          } else if (metricString.equals("simoperatorcode")) { // sim operator code
            metric = 12;
          } else if (metricString.equals("simoperatorname")) { // sim operator name
            metric = 13;
          } else if (metricString.equals("imei")) { // imei
            metric = 14;
          } else if (metricString.equals("imsi")) { // imsi
            metric = 15;
          } else if (metricString.equals("manufactor")) { // manufacter
            metric = 16;
          } else if (metricString.equals("model")) { // model
            metric = 17;
          } else if (metricString.equals("ostype")) { // os type
            metric = 18;
          } else if (metricString.equals("osversion")) { // os version
            metric = 19;
          } else if (metricString.equals("gsmbiterrorrate")) { // gsmbiterrorrate
            metric = 20;
          } else if (metricString.equals("cdmaecio")) { // cdmaecio
            metric = 21;
          } else if (metricString.equals("phonetype")) { // phone type
            metric = 22;
          } else if (metricString.equals("activenetworktype")) { // active network
            metric = 23;
            mActiveNetworkType = value;
          } else if (metricString.equals("connectionstatus")) { // connection
            metric = 24;
          } else if (metricString.equals("roamingstatus")) { // roaming status
            metric = 25;
          } else if (metricString.equals("networkoperatorcode")) { // network
            metric = 26;
          } else if (metricString.equals("cdmasignalstrength")) { // cdmasignalstrength
            metric = 27;
          } else if (metricString.equals("cdmabasestationid")) { // cdmabasestationid
            metric = 28;
          } else if (metricString.equals("cdmabasestationlatitude")) { // cdmabasestationlatitude
            metric = 29;
          } else if (metricString.equals("cdmabasestationlongitude")) { // cdmabasestationlongitude
            metric = 30;
          } else if (metricString.equals("cdmanetworkid")) { // cdmanetworkid
            metric = 31;
          } else if (metricString.equals("cdmasystemid")) { // cdmasystemid
            metric = 32;
          } else {
            Log.d(this.getClass().getName(), "WARNING - unsupported metric (" + metric +")");
          }

          // Prevent them display at ALL in the test view; to prevent
          // weird flickering!
          metric = -99;
        }


        switch (metric) {

          // passive metrics
          case 1:
            tv = (TextView) findViewById(R.id.passivemetric1);
            tv.setText(value);
            break;

          case 2:
            tv = (TextView) findViewById(R.id.passivemetric2);
            tv.setText(value);
            break;

          case 3:
            tv = (TextView) findViewById(R.id.passivemetric3);
            tv.setText(value);
            break;

          case 4:
            tv = (TextView) findViewById(R.id.passivemetric4);
            tv.setText(value);
            break;

          case 5:
            tv = (TextView) findViewById(R.id.passivemetric5);
            tv.setText(value);
            break;

          case 6:
            tv = (TextView) findViewById(R.id.passivemetric6);
            tv.setText(value);
            break;

          case 7:
            tv = (TextView) findViewById(R.id.passivemetric7_networkoperatorname);
            tv.setText(value);
            break;

          case 8:
            tv = (TextView) findViewById(R.id.passivemetric8);
            tv.setText(value);
            break;

          case 9:
            tv = (TextView) findViewById(R.id.passivemetric9);
            tv.setText(value);
            break;

          case 10:
            tv = (TextView) findViewById(R.id.passivemetric10);
            tv.setText(value);
            break;

          case 11:
            tv = (TextView) findViewById(R.id.passivemetric11);
            tv.setText(value);
            break;
          case 12:
            tv = (TextView) findViewById(R.id.passivemetric12);
            tv.setText(value);
            break;
          case 13:
            tv = (TextView) findViewById(R.id.passivemetric13);
            tv.setText(value);
            break;
          case 14:
            tv = (TextView) findViewById(R.id.passivemetric14);
            tv.setText(value);
            break;
          case 15:
            tv = (TextView) findViewById(R.id.passivemetric15);
            tv.setText(value);
            break;
          case 16:
            tv = (TextView) findViewById(R.id.passivemetric16);
            tv.setText(value);
            break;
          case 17:
            tv = (TextView) findViewById(R.id.passivemetric17);
            tv.setText(value);
            break;
          case 18:
            tv = (TextView) findViewById(R.id.passivemetric18);
            tv.setText(value);
            break;
          case 19:
            tv = (TextView) findViewById(R.id.passivemetric19);
            tv.setText(value);
            break;
          case 20:
            tv = (TextView) findViewById(R.id.passivemetric20);
            tv.setText(value);
            break;
          case 21:
            tv = (TextView) findViewById(R.id.passivemetric21);
            tv.setText(value);
            break;
          case 22:
            tv = (TextView) findViewById(R.id.passivemetric22);
            tv.setText(value);
            break;
          case 23:
            tv = (TextView) findViewById(R.id.passivemetric23);
            tv.setText(value);
            break;
          case 24:
            tv = (TextView) findViewById(R.id.passivemetric24);
            tv.setText(value);
            break;
          case 25:
            tv = (TextView) findViewById(R.id.passivemetric25);
            tv.setText(value);
            break;
          case 26:
            tv = (TextView) findViewById(R.id.passivemetric26);
            tv.setText(value);
            break;
          case 27:
            tv = (TextView) findViewById(R.id.passivemetric27);
            tv.setText(value);
            break;
          case 28:
            tv = (TextView) findViewById(R.id.passivemetric28);
            tv.setText(value);
            break;
          case 29:
            tv = (TextView) findViewById(R.id.passivemetric29);
            tv.setText(value);
            break;
          case 30:
            tv = (TextView) findViewById(R.id.passivemetric30);
            tv.setText(value);
            break;
          case 31:
            tv = (TextView) findViewById(R.id.passivemetric31);
            tv.setText(value);
            break;
          case 32:
            tv = (TextView) findViewById(R.id.passivemetric32);
            tv.setText(value);
            break;
          default:
            //

        }
        if (!value.equals("") && tv != null) {

          TableLayout tl1 = (TableLayout) findViewById(R.id.passive_metrics_status);
          tl1.setVisibility(View.GONE);
          TableLayout tl = (TableLayout) tv.getParent()
              .getParent();
          tl.setVisibility(View.VISIBLE);
        }

        if (value.equals("") && tv != null) {
          TableLayout tl = (TableLayout) tv.getParent()
              .getParent();
          tl.setVisibility(View.GONE);
        }

      }

    } catch (JSONException e) {
      SKLogger.e(this, e.getMessage());
    }
  }

	private boolean checkIfIsConnectedAndIfNotShowAnAlertThenFinish() {
		
		if (NetworkDataCollector.sGetIsConnected() == true) {
			return true;
		}
	
		// We're not connected - show an alert - if possible - and return false!
		if (!isFinishing()) {
			new AlertDialog.Builder(this)
			.setMessage(R.string.Offline_message)
			.setPositiveButton(R.string.ok_dialog, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					SKARunningTestActivity.this.finish();
					overridePendingTransition(0, 0);
				}
			}).show();
		}
		
		return false;
	}

	// https://stackoverflow.com/questions/3947641/android-equivalent-to-nsnotificationcenter
	
	// Our handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcasted.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			String message = intent.getStringExtra("message");
			Log.d("receiver", "Got message: " + message);

			// Mark the tests as "UDP blocked"
			//SKARunningTestActivity.this.
			FontFitTextView fftv = (FontFitTextView) findViewById(R.id.latency_result);
			if (fftv != null) {
				fftv.setText(getString(R.string.udp_blocked_not_running));
			}
			fftv = (FontFitTextView) findViewById(R.id.packetloss_result);
			if (fftv != null) {
				fftv.setText(getString(R.string.udp_blocked_not_running));
			}
			fftv = (FontFitTextView) findViewById(R.id.jitter_result);
			if (fftv != null) {
				fftv.setText(getString(R.string.udp_blocked_not_running));
			}
		}
	};

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		  // Register to receive messages.
		  // This is just like [[NSNotificationCenter defaultCenter] addObserver:...]
		  // We are registering an observer (mMessageReceiver) to receive Intents
		  // with actions named "custom-event-name".
		  LocalBroadcastManager.getInstance(SKApplication.getAppInstance().getApplicationContext()).registerReceiver(mMessageReceiver,
		      new IntentFilter(ManualTestRunner.kManualTest_UDPFailedSkipTests));
	}

    @Override
	public void onDestroy() {
      // Unregister since the activity is about to be closed.
      // This is somewhat like [[NSNotificationCenter defaultCenter] removeObserver:name:object:] 
      LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
      super.onDestroy();
    }

	@Override
	public void finish() {

		Intent returnIntent = new Intent();
		returnIntent.putExtra("activeneworktype", mActiveNetworkType);

		if (result == 0) {
			// setResult(RESULT_OK,returnIntent);
		} else {
			setResult(RESULT_OK, returnIntent);
		}
		super.finish();

	}

	@Override
	public void onStart() {
		super.onStart();

		// make passive metrics invisible

		for (int x = 1; x < 33; x = x + 1) {
			Message msg = new Message();
			JSONObject jtc = new JSONObject();
			try {
				jtc.put("type", "passivemetric");
				jtc.put("metric", "" + x);
				jtc.put("metricString", "invisible");
				jtc.put("value", "");
				msg.obj = jtc;
			} catch (JSONException je) {
				SKLogger.e(this,
						"Error in creating JSONObject:" + je.getMessage());
			}
			handler.sendMessage(msg);
		}

	}

	private void launchTest(SCHEDULE_TEST_ID testIdToRunMinusOneMeansAll) {

		StringBuilder errorDescription = new StringBuilder();

    // Create the test runner.
    // Note that this will always include a closest target test that precedes the test(s)
    mt = ManualTestRunner.create(this, handler, testIdToRunMinusOneMeansAll, errorDescription);
    if (mt == null) {
      String theErrorString = errorDescription.toString();
      if (theErrorString.length() == 0) {
        theErrorString = getString(R.string.manual_test_error);
      }

      SKLogger.d(SKARunningTestActivity.class,
          "Impossible to run manual tests");
      new AlertDialog.Builder(this)
          .setMessage(theErrorString)
          .setPositiveButton(R.string.ok_dialog,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int id) {
                  result = 0;
                  SKARunningTestActivity.this.finish();
                  overridePendingTransition(0, 0);
                }
              }).show();

      return;
    }

		if (testIdToRunMinusOneMeansAll == SCHEDULE_TEST_ID.ALL_TESTS) {
      // Nothing special to do!
    } else  if (testIdToRunMinusOneMeansAll == SCHEDULE_TEST_ID.DOWNLOAD_TEST) { // download
			// hide others
			TableLayout tl = (TableLayout) findViewById(R.id.upload_test_panel);
			tl.setVisibility(View.GONE);
			TableLayout tl2 = (TableLayout) findViewById(R.id.latency_test_panel);
			tl2.setVisibility(View.GONE);
			TableLayout tl3 = (TableLayout) findViewById(R.id.packetloss_test_panel);
			tl3.setVisibility(View.GONE);
		} else if (testIdToRunMinusOneMeansAll == SCHEDULE_TEST_ID.LATENCY_TEST) { // loss / latency
			// hide others
			TableLayout tl = (TableLayout) findViewById(R.id.download_test_panel);
			tl.setVisibility(View.GONE);
			TableLayout tl2 = (TableLayout) findViewById(R.id.upload_test_panel);
			tl2.setVisibility(View.GONE);
		} else if (testIdToRunMinusOneMeansAll == SCHEDULE_TEST_ID.UPLOAD_TEST) { // upload
			// hide others
			TableLayout tl = (TableLayout) findViewById(R.id.download_test_panel);
			tl.setVisibility(View.GONE);
			TableLayout tl2 = (TableLayout) findViewById(R.id.latency_test_panel);
			tl2.setVisibility(View.GONE);
			TableLayout tl3 = (TableLayout) findViewById(R.id.packetloss_test_panel);
			tl3.setVisibility(View.GONE);
		} else {
      SKLogger.sAssert(false);
		}

		if (SKApplication.getAppInstance().getIsDataCapEnabled() == true) {

			String showWithMessage = "";

		  if (SK2AppSettings.getSK2AppSettingsInstance().isDataCapAlreadyReached()) {
			  SKLogger.d(SKARunningTestActivity.class, "Data cap exceeded");
    		showWithMessage = getString(R.string.data_cap_exceeded);
	    } else if (SK2AppSettings.getSK2AppSettingsInstance().isDataCapLikelyToBeReached(mt.getNetUsage())) {
				// Data cap exceeded - but only ask the user if they want to continue, if the app is configured
				// to work like that...
				SKLogger.d(SKARunningTestActivity.class, "Data cap likely to be exceeded");
				showWithMessage = getString(R.string.data_cap_might_be_exceeded);
			}
			
			if (showWithMessage.length() > 0) {

				SKLogger.d(SKARunningTestActivity.class, "Data cap exceeded");
				new AlertDialog.Builder(this)
				.setMessage(showWithMessage)
				.setPositiveButton(R.string.ok_dialog,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int id) {
            mt.startTestRunning_RunInBackground();
					}
				})
				.setNegativeButton(R.string.no_dialog, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						result = 0;
						SKARunningTestActivity.this.finish();
						overridePendingTransition(0, 0);
					}
				}).show();

				return;
			}
		}

    mt.startTestRunning_RunInBackground();
	}

	@Override
	public void onBackPressed() {
		
		if (this.wouldBackButtonReturnMeToTheHomeScreen()) {
			super.onBackPressed();
			return;
		}
		
		new AlertDialog.Builder(this)
				.setMessage(getString(R.string.cancel_test_question))
				.setCancelable(true)
				.setPositiveButton(getString(R.string.yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								result = 0;
								if (mt == null) {
					              // Log.d(this.getClass().getName(), "Avoided null pointer exception!");
								} else {
									mt.stopTestRunning();
								}
								SKARunningTestActivity.this.finish();
								overridePendingTransition(0, 0);
							}
						})
				.setNegativeButton(getString(R.string.no_dialog),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

							}
						}).show();
	}

}
