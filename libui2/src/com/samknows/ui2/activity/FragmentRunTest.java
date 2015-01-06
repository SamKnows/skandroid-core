package com.samknows.ui2.activity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samknows.libcore.SKLogger;
import com.samknows.libui2.R;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.MainService;
import com.samknows.measurement.ManualTest;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.SKApplication.eNetworkTypeResults;
import com.samknows.measurement.activity.components.FontFitTextView;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.StorageTestResult;

/**
 * This fragment is responsible for running the tests and managing the home screen.
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */


public class FragmentRunTest extends Fragment
{
	// *** CONSTANTS *** //
	private final static String C_TAG_FRAGMENT_SPEED_TEST = "Fragment SpeedTest";	// Tag for this fragment
	private final static int C_UPDATE_INTERVAL_IN_MS = 250;							// Time threshold in milliseconds to refresh the UI data
	
	// *** VARIABLES *** //	
	private int numberOfTestsToBePerformed;											// Number of tests to be performed (minimum 1, maximum 3)
	private int heightInPixels;														// Height in pixels
	private eNetworkTypeResults connectivityType = eNetworkTypeResults.eNetworkTypeResults_WiFi;
	private int results_Layout_Position_Y;											// Position in the Y axis of the results layout	
	private float testProgressDownload, testProgressUpload,							// Variables to control the background progress bar
					testProgressLatencyPacketLossJitter, progressPercent;
	private float screenDensity;													// Screen density. This allows us to calculate density points to pixels
	private long lastTimeMillisCurrentSpeed = 0;									// Last the the current speed was updated
	private long lastTimeMillisProgressBar = 0;										// Last time progress bar was updated
	private long testTime;
	private boolean testsRunning = false;											// If true, the tests are been performed	
	
	// Whether a test was selected to perform or not
	private boolean test_selected_download = true;
	private boolean test_selected_upload = true;
	private boolean test_selected_latency_and_packet_loss_and_jitter = true;
	
	private boolean onChangeLabelTextSemaphore = false;								// Semaphore to allow or not to enter a loop
	private boolean onContextualInformationLabelAnimationSemaphore = false;			// Semaphore to allow or not to enter a loop	
	private boolean gaugeVisible = true;											// Whether the gauge is visible or not
	private boolean executingLatencyTest = false;

	// UI elements
	private RelativeLayout layout_layout_Shining_Labels;
	private LinearLayout layout_ll_Speed_Test_Layout, layout_ll_Main_Progress_Bar;
	private LinearLayout layout_ll_passive_metrics, layout_ll_results, layout_ll_passive_metrics_divider_sim_and_network_operators, layout_ll_passive_metrics_divider_signal, layout_ll_passive_metrics_divider_location;
	private FrameLayout run_results_panel_frame_to_animate;
	// Text views showing warnings
	private TextView mUnitText;	
	private View initial_warning_text;
	// Text views showing the test result labels
	private TextView tv_Label_Download, tv_Label_Upload; //  tv_Label_Mbps_1, tv_Label_Mbps_2;
	private TextView tv_Label_Latency, tv_Label_Loss, tv_Label_Jitter;
	private TextView tv_TopTextNetworkType;
	// Text views showing the test result information
	private TextView tv_Result_Download, tv_Result_Upload, tv_Result_Latency, tv_Result_Packet_Loss, tv_Result_Jitter;
	private TextView tv_DownloadUnits, tv_UploadUnits;
	private TextView tv_Result_DateDay;
	private TextView tv_Result_DateTime;
	// Text views showing the passive metric headers
	private TextView tv_header_label_sim_and_network_operators, tv_header_label_signal, tv_header_label_device, tv_header_label_location;
	// Text views showing the passive metric labels
	private TextView tv_label_sim_operator, tv_label_sim_operator_code, tv_label_network_operator, tv_label_network_operator_code, tv_label_roaming_status,
						tv_label_cell_tower_ID, tv_label_cell_tower_area_location_code, tv_label_signal_strength, tv_label_bearer,
							tv_label_manufacturer, tv_label_model, tv_label_OS, tv_label_OS_version, tv_label_phone_type, tv_label_latitude, tv_label_longitude,
								tv_label_accuracy, tv_label_provider;
	// Text views that show the passive metric results
	private TextView tv_result_sim_operator, tv_result_sim_operator_code, tv_result_network_operator, tv_result_network_operator_code, tv_result_roaming_status,
						tv_result_cell_tower_ID, tv_result_cell_tower_area_location_code, tv_result_signal_strength, tv_result_bearer,
							tv_result_manufacturer, tv_result_model, tv_result_OS, tv_result_OS_version, tv_result_phone_type, tv_result_latitude, tv_result_longitude,
								tv_result_accuracy, tv_result_provider;
	// Text views showing another additional information
	private TextView tv_Advice_Message, tv_Gauge_TextView_PsuedoButton, tv_Status_Label_1, tv_Status_Label_2;
	private ImageView iv_Result_NetworkType;															// Image showing the network type icon (Mobile or WiFi)
	private Typeface typeface_Din_Condensed_Cyrillic, typeface_Roboto_Light, typeface_Roboto_Thin;		// Type faces to be used in this fragment UI
	private Typeface typeface_Roboto_Bold;
	private MenuItem menuItem_SelectTests, menuItem_ShareResult;

	// Other class objects
    private FrameLayout gaugeViewContainer;
    private GaugeView gaugeView;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
	
	// Background tasks   
    private Thread threadRunningTests;				// Thread that run the tests
    private ManualTest manualTest;					// Object containing the ManualTest class object
    public Handler testResultsHandler;				// Handler that listen for the test results
    private ScheduleConfig config;

	private	TextView publicIp;
	private	TextView submissionId;

    // *** FRAGMENT LIFECYCLE METHODS *** //
    // Called to have the fragment instantiate its user interface view.
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{	
		View view = inflater.inflate(R.layout.fragment_run_container, container, false);
		
		// Bind and initialise the resources
		setUpResources(view);
	
		// Inflate the layout for this fragment
		return view;		
	}
	
	// Called immediately after onCreateView but before any saved state has been restored in to the view
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{		
		super.onViewCreated(view, savedInstanceState);		
	}
	
	// Called when the fragment is visible to the user and actively running
	@Override
    public void onResume()
    {
		// Restore any previously saved data...
    	restoreWhichTestsToRun();
    	
		// Register back button handler...
		registerBackButtonHandler();
		
        // Register the broadcast receiver to listen for connectivity changes from the system
        IntentFilter mIntentFilter = new IntentFilter();               
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);        
        
    	Context context = SKApplication.getAppInstance().getApplicationContext();
        context.registerReceiver(broadcastReceiverConnectivityChanges, mIntentFilter);
        
        // Register the local broadcast receiver listener to receive messages within the application (Listen for current values while performing tests)
        LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiverCurrentClosestTarget, new IntentFilter("currentClosestTarget"));			// Current closest target server
        LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiverCurrentLatency, new IntentFilter("currentLatencyIntent"));			// Current latency value
        
		// Start the periodic timer!
		startTimer();

        // Add the listener to the telephonyManager to listen for changes in the data connectivity
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
        super.onResume();
    }

	// Called when the fragment is no longer resumed
    @Override
    public void onPause()
    {
    	super.onPause();

        // Unregister the broadcast receiver listener. The listener listen for connectivity changes
    	Context context = SKApplication.getAppInstance().getApplicationContext();
    	context.unregisterReceiver(broadcastReceiverConnectivityChanges);

    	// Unregister the local broadcasts
    	LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiverCurrentClosestTarget);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiverCurrentpeed);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiverCurrentLatency);

		// Stop the periodic timer!
		stopTimer();

        //Remove the telephonyManager listener
        telephonyManager.listen(null, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
    }

	/// *** BROADCASTER RECEIVERS **** ///
    // Broadcast receiver listening for connectivity changes to make the application connectivity aware.
    private BroadcastReceiver broadcastReceiverConnectivityChanges = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
        	// On connectivity changes the data cap message might be modified because this message is only shown on mobile connectivity (3G, Edge, HSPA...)
        	checkOutDataCap();
        	// Check out connectivity status
        	checkConnectivity(intent);        	
        }
    };
    
    // Broadcast receiver listening for changes in the closest target (server) value
    private BroadcastReceiver messageReceiverCurrentClosestTarget = new BroadcastReceiver()
    {
    	@Override
    	public void onReceive(Context context, Intent intent)
    	{
    		if (testsRunning)
    		{
    			String hostUrl = intent.getStringExtra("currentClosestTarget");	// Get extra data included in the Intent
        	    
    			if (hostUrl.length() != 0)
    			{
    				String nameOfTheServer = config.hosts.get(hostUrl);
    				
    				if (nameOfTheServer == null)
    				{
    					nameOfTheServer = hostUrl;					
    				}				
    				changeFadingTextViewValue(mUnitText, nameOfTheServer, getResources().getColor(R.color.MainColourDialUnitText));		// Update the current result closest target
    			}				
			}    	    			
    	}    	
    };
    
    // Broadcast receiver listening for changes in current download / upload speed measurement
    private BroadcastReceiver messageReceiverCurrentpeed = new BroadcastReceiver()
    {
    	@Override
    	public void onReceive(Context context, Intent intent)
    	{
    		//if (testsRunning)
    		{
    			// Update the UI data only few times a second
        		//if (System.currentTimeMillis() - lastTimeMillisCurrentSpeed > C_UPDATE_INTERVAL_IN_MS)
        		{    			
            	    //String message = intent.getStringExtra("currentSpeedValue");			// Get extra data included in the Intent
            	    //updateCurrentTestSpeed(message);										// Update the current result meter for download/upload
            	    //gaugeView.setResult(Double.valueOf(message)*0.000008);					// Update the gauge colour indicator (in Megabytes)
        	    	//lastTimeMillisCurrentSpeed = System.currentTimeMillis();				// Register the time of the last UI update
    			}				
			}    		    		
    	}    	

    };
    	
    Handler mHandler = new Handler();
    Timer myTimer = null;
   
    private void stopTimer() {
    	if (myTimer != null) {
    		myTimer.cancel();
    		myTimer = null;
    	}
    }
   
    static double lastPolledSpeedValueMbps = 0;
    
    private void startTimer() {
    	
       Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
            	// For each timer "tick", *IF* the tests are running, we must see if the
            	// measured speed has changed since last time... and if so, update the UI.
            	// Otherwise, do nothing!
            	if (testsRunning) {
            		mHandler.post(new Runnable() {

            			@Override
            			public void run() {
            				Pair<Double,String> value = com.samknows.tests.HttpTest.sGetLatestSpeedForExternalMonitorAsMbps();
//Log.d("MPCMPCMPC", "gotResult for timer, =" + value.first + " Mbps (" + value.second + ")");
            				if (value.first.doubleValue() != lastPolledSpeedValueMbps) {
            					lastPolledSpeedValueMbps = value.first.doubleValue();
            					String message = String.valueOf(value);
            					updateCurrentTestSpeedMbps(value.first.doubleValue());										// Update the current result meter for download/upload
            					gaugeView.setAngleByValue(value.first.doubleValue());					// Update the gauge colour indicator (in Megabytes)
            					lastTimeMillisCurrentSpeed = System.currentTimeMillis();				// Register the time of the last UI update
            				}
            			}});
            	}
            }

        }, 0, C_UPDATE_INTERVAL_IN_MS);
    }
    

    
    // Broadcast receiver listening for changes in current latency value measurement
    private BroadcastReceiver messageReceiverCurrentLatency = new BroadcastReceiver()
    {
    	@Override
    	public void onReceive(Context context, Intent intent)
    	{
    		if (testsRunning)
    		{
    			// Update the UI data only few times a second
        		if (executingLatencyTest && System.currentTimeMillis() - lastTimeMillisCurrentSpeed > C_UPDATE_INTERVAL_IN_MS)
        		{    			
            	    String message = intent.getStringExtra("currentLatencyValue");	// Get extra data included in the Intent
            	    updateCurrentLatencyValue(message);								// Update the current result meter for latency
            	    gaugeView.setAngleByValue(Double.valueOf(message));					// Update the gauge colour indicator
            	    
            	    lastTimeMillisCurrentSpeed = System.currentTimeMillis();		// Register the time of the last UI update
    			}    							
			}    		    		
    	}    	
    };
    
    // *** INNER CLASSES *** //
    /**
     * @author pablo
     *
     * Just when the button is clicked it checks if we have real internet connection before start the tests.
     */
    private class InitTestAsyncTask extends AsyncTask<Void, Void, Boolean>
    {
    	@Override
    	protected void onPreExecute()
    	{
    		changeFadingTextViewValue(tv_Gauge_TextView_PsuedoButton, getString(R.string.gauge_message_starting),0);	// Set the gauge main text to STARTING
    		super.onPreExecute();
    	}

		@Override
		protected Boolean doInBackground(Void... params)
		{			
			return isInternetAvailable();
		}
		
		@Override
		protected void onPostExecute(Boolean result)
		{
			if (getActivity() == null) {
				SKLogger.sAssert(getClass(), false);
				
			} else {
				
				if (result) {
					testsRunning = true;																		// Make it notice that tests are running
					resetValueFields();																			// Set the value fields to a initial state
					menuItem_SelectTests.setVisible(false);
					changeFadingTextViewValue(tv_Gauge_TextView_PsuedoButton, getString(R.string.gauge_message_starting),0);	// Set the gauge main text to STARTING
					changeAdviceMessageTo(getString(R.string.advice_message_running));							// Change the advice message to "Running tests"
					fadeInProgressBar();																		// Initiate the progress bar to let the user know the progress of the tests
					launchTests();																				// Launch tests
					layout_ll_results.animate().setDuration(300).alpha(1.0f);									// Make the results layout visible						
					setTestTime();																				// Set the time of the current test
					//setTestConnectivity();																	// Set the connectivity of the current test
					layout_ll_results.setClickable(false);														// The results layout is not clickable from here							
				}
				else {
					changeFadingTextViewValue(tv_Gauge_TextView_PsuedoButton, getString(R.string.gauge_message_start),0);	// Set the gauge main text to START

					Context context = SKApplication.getAppInstance().getApplicationContext();
					Toast.makeText(context, getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();							
				}
			}
			
			super.onPostExecute(result);
		}
    }
    
    
    // *** CUSTOM METHODS *** //
    /**
     * Bind the resources of the layout with the elements in this class and set up them
     * 
     * @param pView
     */
    private void setUpResources(View pView)	
    {
    	// Passive metrics fields
    	// Header labels
    	tv_header_label_sim_and_network_operators = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_sim_and_network_operators); 
    	tv_header_label_signal = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_signal);
    	tv_header_label_device = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_device);
    	tv_header_label_location = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_location);

    	//Dividers
    	layout_ll_passive_metrics_divider_sim_and_network_operators = (LinearLayout)pView.findViewById(R.id.fragment_speed_test_passive_metrics_ll_divider_sim_and_network_operators);
    	layout_ll_passive_metrics_divider_signal = (LinearLayout)pView.findViewById(R.id.fragment_speed_test_passive_metrics_ll_divider_signal);
    	layout_ll_passive_metrics_divider_location = (LinearLayout)pView.findViewById(R.id.fragment_speed_test_passive_metric_divider_location);

    	// Labels
    	tv_label_sim_operator = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_sim_operator);
    	tv_label_sim_operator_code = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_sim_operator_code);
    	tv_label_network_operator = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_network_operator);
    	tv_label_network_operator_code = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_network_operator_code);
    	tv_label_roaming_status = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_roaming_status);
		tv_label_cell_tower_ID = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_cell_tower_ID);
		tv_label_cell_tower_area_location_code = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_cell_tower_area_location_code); 
		tv_label_signal_strength = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_signal_strength);
		tv_label_bearer = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_bearer);
		tv_label_manufacturer = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_manufacturer);
		tv_label_model = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_model);
		tv_label_OS = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_OS);
		tv_label_OS_version = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_OS_vesion);
		tv_label_phone_type = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_phone_type);
		tv_label_latitude = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_latitude);
		tv_label_longitude = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_longitude);
		tv_label_accuracy = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_accuracy);
		tv_label_provider = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_label_location_provider);

		// Results
		tv_result_sim_operator = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_sim_operator_name);
		tv_result_sim_operator_code = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_sim_operator_code);
		tv_result_network_operator = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_network_operator_name);
		tv_result_network_operator_code = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_network_operator_code);
		tv_result_roaming_status = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_roaming_status);
		tv_result_cell_tower_ID = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_cell_tower_id);
		tv_result_cell_tower_area_location_code = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_cell_tower_area_location_code);
		tv_result_signal_strength = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_signal_strength);
		tv_result_bearer = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_bearer);
		tv_result_manufacturer = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_manufacturer);
		tv_result_model = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_detail_model);
		tv_result_OS = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_detail_OS);
		tv_result_OS_version = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_OS_version);
		tv_result_phone_type = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_phone_type);
		tv_result_latitude = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_latitude);
		tv_result_longitude = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_longitude);
		tv_result_accuracy = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_accuracy);
		tv_result_provider = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_location_provider);
	
		publicIp = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_your_ip_value);
		SKLogger.sAssert(getClass(), publicIp != null);
		submissionId = (TextView)pView.findViewById(R.id.fragment_speed_test_passive_metric_result_reference_number_value);
		SKLogger.sAssert(getClass(), submissionId != null);
		publicIp.setText("");
		submissionId.setText("");

		// Identify and hide the passive metrics layout
		layout_ll_passive_metrics = (LinearLayout) pView.findViewById(R.id.fragment_speed_test_ll_passive_metrics);
    	layout_ll_passive_metrics.setVisibility(View.GONE);
    	layout_ll_passive_metrics.setAlpha(0.0f);
		
		// Now - what items to show?
		LinearLayout ip_and_reference_metrics = (LinearLayout)pView.findViewById(R.id.ip_and_reference_metrics2);
		LinearLayout network_operator_metrics = (LinearLayout)pView.findViewById(R.id.network_operator_metrics2);
		LinearLayout signal_metrics = (LinearLayout)pView.findViewById(R.id.signal_metrics2);
		LinearLayout device_metrics = (LinearLayout)pView.findViewById(R.id.device_metrics2);
		LinearLayout location_metrics = (LinearLayout)pView.findViewById(R.id.location_metrics2);
		
		if (SKApplication.getAppInstance().getPassiveMetricsJustDisplayPublicIpAndSubmissionId() == true) {
    		network_operator_metrics.setVisibility(View.GONE);
	    	signal_metrics.setVisibility(View.GONE);
		    device_metrics.setVisibility(View.GONE);
    		location_metrics.setVisibility(View.GONE);
		} else {
			ip_and_reference_metrics.setVisibility(View.GONE);
		}
		

    	// Get the screen density. This is use to transform from dips to pixels
		Context context = SKApplication.getAppInstance().getApplicationContext();
    	screenDensity = context.getResources().getDisplayMetrics().density;

    	// Initialise the telephony manager
    	telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

    	// Set up the listener for the mobile connectivity changes
    	phoneStateListener = new PhoneStateListener()
    	{
    		public void onDataConnectionStateChanged(int state, int networkType)
    	    {
    	       // We have changed protocols, for example we have gone from HSDPA to GPRS
    	       // HSDPA is an example of a 3G connection, GPRS is an example of a 2G connection
    			checkConnectivityStatus();
    	    }

    		public void onCellLocationChanged(CellLocation location)
    		{
    	       // We have changed to a different Tower/Cell
    	    }
    	};    	
    	
    	// Report that this fragment would like to participate in populating the options menu by receiving a call to onCreateOptionsMenu(Menu, MenuInflater) and related methods.
    	setHasOptionsMenu(true);

		// Gauge view container
		gaugeViewContainer = (FrameLayout)pView.findViewById(R.id.fragment_speed_gauge_container);		
		// Gauge view
		gaugeView = (GaugeView)pView.findViewById(R.id.fragment_speed_gauge_view);		
		
		// Text view showing the measurement values
		tv_Gauge_TextView_PsuedoButton = (TextView)pView.findViewById(R.id.fragment_speed_test_gauge_textview_pseudobutton);		
		tv_Gauge_TextView_PsuedoButton.setText(getString(R.string.gauge_message_start));
		
		// Initialise and hide the results layout
		layout_ll_results = (LinearLayout)pView.findViewById(R.id.fragment_speed_test_results_ll2);
		layout_ll_results.setAlpha(0.0f);
     	run_results_panel_frame_to_animate = (FrameLayout)pView.findViewById(R.id.run_results_panel_frame_to_animate);
		
		// Get the position of the header row when it's already drawn		
     	run_results_panel_frame_to_animate.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{			
			@Override
			public void onGlobalLayout()
			{
				if (run_results_panel_frame_to_animate != null) {
					results_Layout_Position_Y = run_results_panel_frame_to_animate.getTop();

					ViewTreeObserver observer = run_results_panel_frame_to_animate.getViewTreeObserver();
					if (observer != null) {
						// http://stackoverflow.com/questions/15162821/why-does-removeongloballayoutlistener-throw-a-nosuchmethoderror
						try {
							observer.removeOnGlobalLayoutListener(this);
						} catch (NoSuchMethodError x) {
							observer.removeGlobalOnLayoutListener(this);
						}
					}
				}
			}			
		});
	
		// Set a listener to the results layout to move it to the top and show the passive metrics
		layout_ll_results.setOnClickListener(new OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if (gaugeVisible)
				{
    				// If the gauge elements are visible, hide them and show the passive metrics.
					hideGaugeShowPassiveMetricsPanel();
				}
				else
				{
    				// The gauge elements are invisible - show them and hide the passive metrics.
					showGaugeHidePassiveMetricsPanel();
				}
			}
		});
		
     	// Now: immediately disable clicking on the panel, until the first result has arrived (in onDidDetectTestCompleted() ...)
  		layout_ll_results.setClickable(false);
		
		// Elements in the results layout
		tv_Result_Download = (TextView)pView.findViewById(R.id.archiveResultsListItemDownload);
		tv_DownloadUnits = (TextView)pView.findViewById(R.id.mbps_label_1);
		tv_Result_Download.setText(R.string.slash);
		tv_Result_Upload = (TextView)pView.findViewById(R.id.archiveResultsListItemUpload);
		tv_UploadUnits = (TextView)pView.findViewById(R.id.mbps_label_2);
		tv_Result_Upload.setText(R.string.slash);
		tv_Result_Latency = (TextView)pView.findViewById(R.id.archiveResultsListItemLatency);
		tv_Result_Latency.setText(R.string.slash);
		tv_Result_Packet_Loss = (TextView)pView.findViewById(R.id.archiveResultsListItemPacketLoss);
		tv_Result_Packet_Loss.setText(R.string.slash);
		tv_Result_Jitter = (TextView)pView.findViewById(R.id.archiveResultsListItemJitter);
		tv_Result_Jitter.setText(R.string.slash);
		
		tv_Label_Download = (TextView)pView.findViewById(R.id.downloadLabel);
		tv_Label_Upload = (TextView)pView.findViewById(R.id.uploadLabel);
		tv_Label_Latency = (TextView)pView.findViewById(R.id.latency_label);
		tv_Label_Loss = (TextView)pView.findViewById(R.id.loss_label);
		tv_Label_Jitter = (TextView)pView.findViewById(R.id.jitter_label);
		//tv_Label_Mbps_1 = (TextView)pView.findViewById(R.id.mbps_label_1);
		//tv_Label_Mbps_2 = (TextView)pView.findViewById(R.id.mbps_label_2);
		tv_Result_DateDay = (TextView)pView.findViewById(R.id.archiveResultsListItemDateDay);
		tv_Result_DateTime = (TextView)pView.findViewById(R.id.archiveResultsListItemDateTime);

		tv_TopTextNetworkType = (TextView)pView.findViewById(R.id.top_text_network_type_label);
		tv_TopTextNetworkType.setText(""); // Clear this immediately, until we know what the network type is!
		
		iv_Result_NetworkType = (ImageView)pView.findViewById(R.id.archiveResultsListItemNetworkType);		
		
        // Layouts that contains the shining labels
        layout_layout_Shining_Labels = (RelativeLayout)pView.findViewById(R.id.status_label_layout_1);   
        
        // Label showing the closest server
        mUnitText = (TextView)pView.findViewById(R.id.unit_text_label);
        mUnitText.setTextColor(getResources().getColor(R.color.MainColourDialUnitText));
        mUnitText.setText("");
        
        initial_warning_text = pView.findViewById(R.id.initial_warning_text);

        
        // Text views in the shining labels
        tv_Status_Label_1 = (TextView)pView.findViewById(R.id.status_label_1);
		tv_Status_Label_2 = (TextView)pView.findViewById(R.id.status_label_2);
		
		// Initialise the first text in the shining labels
		tv_Status_Label_1.setText(getString(R.string.label_message_ready_to_run));
		tv_Status_Label_2.setText(getString(R.string.label_message_ready_to_run));		
		
		// Starts the "shining animation" of the shining labels
		startShiningLabelsAnimation();
		
        // Label showing an information/advice text
		tv_Advice_Message = (TextView)pView.findViewById(R.id.press_the_start_button_label);
		
		// Initialise fonts
		typeface_Din_Condensed_Cyrillic = Typeface.createFromAsset(context.getAssets(), "fonts/roboto_condensed_regular.ttf");
		typeface_Roboto_Light = Typeface.createFromAsset(context.getAssets(), "fonts/roboto_light.ttf");
		typeface_Roboto_Thin = Typeface.createFromAsset(context.getAssets(), "fonts/roboto_thin.ttf");
		typeface_Roboto_Bold = Typeface.createFromAsset(context.getAssets(), "fonts/roboto_bold.ttf");

		// Assign fonts
		// Passive metrics headers
		tv_header_label_sim_and_network_operators.setTypeface(typeface_Roboto_Thin); 
    	tv_header_label_signal.setTypeface(typeface_Roboto_Thin);
    	tv_header_label_device.setTypeface(typeface_Roboto_Thin);
    	tv_header_label_location.setTypeface(typeface_Roboto_Thin);

		// Passive metrics labels
    	tv_label_sim_operator.setTypeface(typeface_Roboto_Light);
    	tv_label_sim_operator_code.setTypeface(typeface_Roboto_Light);
    	tv_label_network_operator.setTypeface(typeface_Roboto_Light);
    	tv_label_network_operator_code.setTypeface(typeface_Roboto_Light);
    	tv_label_roaming_status.setTypeface(typeface_Roboto_Light);
		tv_label_cell_tower_ID.setTypeface(typeface_Roboto_Light);
		tv_label_cell_tower_area_location_code.setTypeface(typeface_Roboto_Light); 
		tv_label_signal_strength.setTypeface(typeface_Roboto_Light);
		tv_label_bearer.setTypeface(typeface_Roboto_Light);
		tv_label_manufacturer.setTypeface(typeface_Roboto_Light);
		tv_label_model.setTypeface(typeface_Roboto_Light);
		tv_label_OS.setTypeface(typeface_Roboto_Light);
		tv_label_OS_version.setTypeface(typeface_Roboto_Light);
		tv_label_phone_type.setTypeface(typeface_Roboto_Light);
		tv_label_latitude.setTypeface(typeface_Roboto_Light);
		tv_label_longitude.setTypeface(typeface_Roboto_Light);
		tv_label_accuracy.setTypeface(typeface_Roboto_Light);
		tv_label_provider.setTypeface(typeface_Roboto_Light);

		// Passive metrics results
		tv_result_sim_operator.setTypeface(typeface_Roboto_Light);
		tv_result_sim_operator_code.setTypeface(typeface_Roboto_Light);
		tv_result_network_operator.setTypeface(typeface_Roboto_Light);
		tv_result_network_operator_code.setTypeface(typeface_Roboto_Light);
		tv_result_roaming_status.setTypeface(typeface_Roboto_Light);
		tv_result_cell_tower_ID.setTypeface(typeface_Roboto_Light);
		tv_result_cell_tower_area_location_code.setTypeface(typeface_Roboto_Light);
		tv_result_signal_strength.setTypeface(typeface_Roboto_Light);
		tv_result_bearer.setTypeface(typeface_Roboto_Light);
		tv_result_manufacturer.setTypeface(typeface_Roboto_Light);
		tv_result_model.setTypeface(typeface_Roboto_Light);
		tv_result_OS.setTypeface(typeface_Roboto_Light);
		tv_result_OS_version.setTypeface(typeface_Roboto_Light);
		tv_result_phone_type.setTypeface(typeface_Roboto_Light);
		tv_result_latitude.setTypeface(typeface_Roboto_Light);
		tv_result_longitude.setTypeface(typeface_Roboto_Light);
		tv_result_accuracy.setTypeface(typeface_Roboto_Light);
		tv_result_provider.setTypeface(typeface_Roboto_Light);

//		// Test result fields
//		tv_Result_Download.setTypeface(typeface_Din_Condensed_Cyrillic);
//		tv_Result_Upload.setTypeface(typeface_Din_Condensed_Cyrillic);
//		tv_Result_Latency.setTypeface(typeface_Din_Condensed_Cyrillic);
//		tv_Result_Packet_Loss.setTypeface(typeface_Din_Condensed_Cyrillic);
//		tv_Result_Jitter.setTypeface(typeface_Din_Condensed_Cyrillic);
//		
//		// Test result labels
//		tv_Label_Download.setTypeface(typeface_Roboto_Light);
//		tv_Label_Upload.setTypeface(typeface_Roboto_Light);
//		tv_Label_Latency.setTypeface(typeface_Roboto_Light);
//		tv_Label_Loss.setTypeface(typeface_Roboto_Light);
//		tv_Label_Jitter.setTypeface(typeface_Roboto_Light);
//		tv_Label_Mbps_1.setTypeface(typeface_Roboto_Thin);
//		tv_Label_Mbps_2.setTypeface(typeface_Roboto_Thin);
//		tv_Result_Date.setTypeface(typeface_Roboto_Light);		
		
		if (SKApplication.getAppInstance().hideJitter()) {
			tv_Label_Jitter.setVisibility(View.GONE);
    		pView.findViewById(R.id.jitter_panel).setVisibility(View.GONE);
		}
		if (SKApplication.getAppInstance().hideLoss()) {
			tv_Label_Loss.setVisibility(View.GONE);
    		pView.findViewById(R.id.loss_panel).setVisibility(View.GONE);
		}
		
		// Other labels		
		tv_TopTextNetworkType.setTypeface(typeface_Roboto_Bold);
		tv_Gauge_TextView_PsuedoButton.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_Advice_Message.setTypeface(typeface_Roboto_Light);
		mUnitText.setTypeface(typeface_Roboto_Light);		
		
		// Initialise the type face of the shining labels
		tv_Status_Label_1.setTypeface(typeface_Roboto_Light);
		tv_Status_Label_2.setTypeface(typeface_Roboto_Light);
		
		// Layout containing the main screen
		layout_ll_Speed_Test_Layout = (LinearLayout)pView.findViewById(R.id.new_speed_test_layout);
		
		// Set the message label about the current network
    	setNetworkTypeInformation();

		// Get the layout height in pixels just after the layout is drawn
		final ViewTreeObserver observer = layout_ll_Speed_Test_Layout.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{			
			@Override
			public void onGlobalLayout()
			{
				heightInPixels = layout_ll_Speed_Test_Layout.getHeight();				
				
				ViewTreeObserver observer = layout_ll_Speed_Test_Layout.getViewTreeObserver();
				if (observer != null) {
					// http://stackoverflow.com/questions/15162821/why-does-removeongloballayoutlistener-throw-a-nosuchmethoderror
					try {
						observer.removeOnGlobalLayoutListener(this);
					} catch (NoSuchMethodError x) {
						observer.removeGlobalOnLayoutListener(this);
					}
				}
			}
		});
		
		// Background layout showing a progress animation		
		layout_ll_Main_Progress_Bar = (LinearLayout)getActivity().findViewById(R.id.main_Fragment_Progress_Bar);
		
		// Define the behaviour when the tests are started
		tv_Gauge_TextView_PsuedoButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onStartButtonPressed();		
			}

		});

		// Handler that is listening for test results and updates the UI
	    testResultsHandler = new Handler()
	    {
	    	// Subclasses must implement this to receive messages
	    	@Override
			public void handleMessage(Message msg)
	    	{
	    		if (isAdded() == false) {
	    			// This fragment is NOT attached to the activity.
	    			// Don't do anything with the message, or we're likely to crash!
	    			SKLogger.sAssert(getClass(), false);
	    			return;
	    		}
	    		
	    		FormattedValues formattedValues = new FormattedValues();
	    		JSONObject message_JSON = (JSONObject) msg.obj;
				int success, testName, statusComplete;
				String value;
				
	    		try
	    		{
	    			String messageType = message_JSON.getString(StorageTestResult.JSON_TYPE_ID);

	    			// Tests are on progress       			
	    			if (messageType == "test")
	    			{
	    				statusComplete = message_JSON.getInt(StorageTestResult.JSON_STATUS_COMPLETE);
	    				testName = message_JSON.getInt(StorageTestResult.JSON_TESTNUMBER);						
						value = message_JSON.getString(StorageTestResult.JSON_HRRESULT);						
						
						
						if (statusComplete == 100) {
							if (value.length() == 0) {
								// MPC - we're not *really* complete...
								// we're never complete until we have a result, whatever
								// the progress report might claim!
								statusComplete = 99;
							}
						}
						
						if (statusComplete == 100 && message_JSON.has(StorageTestResult.JSON_SUCCESS))
						{
							success = message_JSON.getInt(StorageTestResult.JSON_SUCCESS);
							
							if (success == 0)
							{
								value = getString(R.string.failed);
							}
						}
						
						Pair<Float,String> valueUnits =	null;
						
						switch (testName)
						{
							// Case download test
							case StorageTestResult.DOWNLOAD_TEST_ID:
								// Download test results are processed															
								updateProgressBar(statusComplete, 0);
								changeLabelText(getString(R.string.label_message_download_test));
								gaugeView.setKindOfTest(0);								
								changeUnitsInformationLabel(getString(R.string.units_Mbps));
								
								valueUnits = FormattedValues.getFormattedSpeedValue(value);
								
								if (valueUnits.second.length() > 0) {
Log.d(getClass().getName(), "gotResult for Download test ... at the end of the test - TODO - do this at the start!");
    								tv_DownloadUnits.setText(valueUnits.second);
								}
								
								if (statusComplete == 100)
								{
									updateCurrentTestSpeedMbps(0.0);
									gaugeView.setAngleByValue(0.0);
									changeFadingTextViewValue(tv_Result_Download, String.valueOf(FormattedValues.sGet3DigitsNumber(valueUnits.first)),0);																	
								}							
								break;

							// Case upload test
							case StorageTestResult.UPLOAD_TEST_ID:
								// Upload test results are processed															
								updateProgressBar(statusComplete, 1);								
								changeLabelText(getString(R.string.label_message_upload_test));								
								gaugeView.setKindOfTest(1);
								changeUnitsInformationLabel(getString(R.string.units_Mbps));
								
								valueUnits = FormattedValues.getFormattedSpeedValue(value);
								if (valueUnits.second.length() > 0) {
Log.d(getClass().getName(), "gotResult for Upload test ... at the end of the test - TODO - do this at the start!");
    								tv_UploadUnits.setText(valueUnits.second);
								}
								
								if (statusComplete == 100)
								{									
									updateCurrentTestSpeedMbps(0.0);
									gaugeView.setAngleByValue(0.0);
									
									changeFadingTextViewValue(tv_Result_Upload, String.valueOf(FormattedValues.sGet3DigitsNumber(valueUnits.first)),0);																	
								}							
								break;

							// Case latency test
							case StorageTestResult.LATENCY_TEST_ID:
								// Latency test results are processed	
								executingLatencyTest = true;
								updateProgressBar(statusComplete, 2);								
								changeLabelText(getString(R.string.label_message_latency_loss_jitter_test));
								gaugeView.setKindOfTest(2);								
								changeUnitsInformationLabel(getString(R.string.units_ms));
								
								valueUnits = FormattedValues.getFormattedSpeedValue(value);
								
								if (statusComplete == 100)
								{									
                    				tv_Gauge_TextView_PsuedoButton.setText("");
									//updateCurrentLatencyValue("0");
									gaugeView.setAngleByValue(0.0);									
									changeFadingTextViewValue(tv_Result_Latency, value,0);
									executingLatencyTest = false;
								}									
								break;

							// Case packet loss test
							case StorageTestResult.PACKETLOSS_TEST_ID:
								// Loss test results are processed
								
								if (statusComplete == 100)
								{									
									// Display as Integer % (rather than Float %)
									Pair<Integer, String> theResult = FormattedValues.getFormattedPacketLossValue(value);
									changeFadingTextViewValue(tv_Result_Packet_Loss, String.valueOf(theResult.first) + " " + theResult.second, 0);
								}									
								break;
								
							case StorageTestResult.JITTER_TEST_ID:
								// Jitter test results are processed

								if (statusComplete == 100)
								{
									changeFadingTextViewValue(tv_Result_Jitter, value,0);
								}
							}
	    				}
	    				// Passive metric data process
	    				else if (messageType == "passivemetric")
	        			{        				
							String metricString = message_JSON.getString("metricString");
							value = message_JSON.getString("value");
							
	        				if (!metricString.equals("invisible"))
	        				{            					
	        					if (metricString.equals("simoperatorname"))
	        					{						
	        						tv_result_sim_operator.setText(value);
	        					}
	        					else if(metricString.equals("simoperatorcode"))
	        					{						
	        						tv_result_sim_operator_code.setText(value);
	        					}
	        					else if(metricString.equals("networkoperatorname"))
	        					{						
	        						tv_result_network_operator.setText(value);
	        					}
	        					else if(metricString.equals("networkoperatorcode"))
	        					{						
	        						tv_result_network_operator_code.setText(value);
	        					}
	        					else if(metricString.equals("roamingstatus"))
	        					{						
	        						tv_result_roaming_status.setText(value);
	        					}
	        					else if (metricString.equals("gsmcelltowerid"))
	        					{						
	        						tv_result_cell_tower_ID.setText(value);
	        					}
	        					else if(metricString.equals("gsmlocationareacode"))
	        					{						
	        						tv_result_cell_tower_area_location_code.setText(value);
	        					}
	        					else if(metricString.equals("gsmsignalstrength"))
	        					{						
	        						tv_result_signal_strength.setText(value);
	        					}					
	        					else if(metricString.equals("manufactor"))
	        					{						
	        						tv_result_manufacturer.setText(value);
	        					}
	        					else if(metricString.equals("networktype"))
	        					{						
	        						tv_result_bearer.setText(value);
	        					}
	        					else if(metricString.equals("model"))
	        					{						
	        						tv_result_model.setText(value);
	        					}
	        					else if(metricString.equals("ostype"))
	        					{						
	        						tv_result_OS.setText(value);
	        					}
	        					else if(metricString.equals("osversion"))
	        					{						
	        						tv_result_OS_version.setText(value);
	        					}
	        					else if(metricString.equals("phonetype"))
	        					{						
	        						tv_result_phone_type.setText(value);
	        					}
	        					else if (metricString.equals("latitude"))
	        					{
	        						tv_result_latitude.setText(value);
	        					}
	        					else if (metricString.equals("longitude"))
	        					{						
	        						tv_result_longitude.setText(value);
	        					}
	        					else if (metricString.equals("accuracy"))
	        					{	
	        						tv_result_accuracy.setText(value);
	        					}
	        					else if (metricString.equals("locationprovider"))
	        					{
	        						tv_result_provider.setText(value);
	        					}
	        					else if (metricString.equals("public_ip"))
	        					{
	        						if (publicIp != null) {
	        							publicIp.setText(value);
	        						}
	        					}
	        					else if (metricString.equals("submission_id")) 
	        					{
	        						if (submissionId != null) {
	        							submissionId.setText(value);
	        						}
	        					}
	        					else if (metricString.equals("activenetworktype"))
	        					{
	        						if (value.equals("WiFi"))
	        						{					
	        							setTestConnectivity(eNetworkTypeResults.eNetworkTypeResults_WiFi);
	        							//testResult.setNetworkType(eNetworkTypeResults.eNetworkTypeResults_WiFi);
	        						}
	        						else if (value.equals("mobile"))
	        						{					
	        							setTestConnectivity(eNetworkTypeResults.eNetworkTypeResults_Mobile);
	        							//testResult.setNetworkType(eNetworkTypeResults.eNetworkTypeResults_Mobile);
	        						}				
	        						else
	        						{
	        							SKLogger.sAssert(getClass(), false);
	        						}
	        					}
	        				
	        					else {
	        						//SKLogger.sAssert(getClass(),  false);
	        					}
							}        				
	        			}
	    			    // The tests are completed
						else if (messageType == "completed")
	        			{
	                        onDidDetectTestCompleted();
						}
	    			}
	        		catch (JSONException e)
	        		{        			
						Log.e(C_TAG_FRAGMENT_SPEED_TEST, "There was an error within the handler. " + e.getMessage());
					}        		
	    	}        	
	    };
	    
		config = CachingStorage.getInstance().loadScheduleConfig();
		if (config == null)
		{
			config = new ScheduleConfig();
		}
	}    
    
    private void safeRunOnUiThread(Runnable runnable) {
    	if (getActivity() == null) {
			SKLogger.sAssert(getClass(), false);
    		return;
    	}
    	
    	getActivity().runOnUiThread(runnable);
    }
    
    /**
     * Update the UI current speed indicator (in Megabytes)
     * 
     * @param pCurrentSpeed
     */
    private void updateCurrentTestSpeedMbps(final double pCurrentSpeed)
    {
    	final DecimalFormat df;
    	
    	if (pCurrentSpeed < 10)
    	{
    		df = new DecimalFormat("#.##");			
		}
    	else
    	{
    		df = new DecimalFormat("##.#");    		
    	}
    	
    	if (getActivity() == null) {
			SKLogger.sAssert(getClass(), false);
    		return;
    	}
    	
    	safeRunOnUiThread(new Runnable()
	    {				
			@Override
			public void run()
			{
				tv_Gauge_TextView_PsuedoButton.setText(String.valueOf(df.format(pCurrentSpeed)));					
			}
		});    	
    }
    
    /**
     * Update the UI current latency indicator (in milliseconds)
     * 
     * @param pLatencyValue
     */
    private void updateCurrentLatencyValue(final String pLatencyValue)
    {
    	final double formattedValue = Integer.valueOf(pLatencyValue);
    	final DecimalFormat df = new DecimalFormat("#.##");
    	
    	safeRunOnUiThread(new Runnable()
	    {				
			@Override
			public void run()
			{
				tv_Gauge_TextView_PsuedoButton.setText(String.valueOf(df.format(formattedValue)));					
			}
		});      	
    }
    
    /**
     * Make the progress bar visible. It is made invisible when it reaches the top in the fadeOutProgressBar method 
     */
    private void fadeInProgressBar()
	{		
		layout_ll_Main_Progress_Bar.setAlpha(0.3f);		
		layout_ll_Main_Progress_Bar.setVisibility(View.VISIBLE);		
	}
    
    /**
     * Update the UI progress bar. The bar goes up while performing the tests
     * 
     * @param pProgress
     * @param pNumOfTest
     */
	private void updateProgressBar(int pProgress, int pNumOfTest)
	{
		// Update the UI data only few times a second or when the progress is 100%
		if ((System.currentTimeMillis() - lastTimeMillisProgressBar > C_UPDATE_INTERVAL_IN_MS) || pProgress == 100)
		{
			switch (pNumOfTest)
			{
				// Case the update comes from download test
				case 0:
					testProgressDownload = pProgress;
					break;

				// Case the update comes from upload test	
				case 1:					
					testProgressUpload = pProgress;
					break;

				// Case the update comes from latency test
				case 2:					
					testProgressLatencyPacketLossJitter = pProgress;
					break;

				// Default case
				default:
					break;
			}

			// Update the total progress
			progressPercent = (testProgressDownload + testProgressUpload + testProgressLatencyPacketLossJitter) / numberOfTestsToBePerformed;

			// Modify the progress layout height
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) layout_ll_Main_Progress_Bar.getLayoutParams();
			layoutParams.height = (int) (progressPercent * heightInPixels / 100);
			layout_ll_Main_Progress_Bar.setLayoutParams(layoutParams);

			lastTimeMillisProgressBar = System.currentTimeMillis();			// Register the time of the last UI update
		}
	}

	/**
	 * Restore the progress bat to the initial state. It's called when the bar reaches the top and the tests are finished
	 */
	private void resetProgressBar()
	{
		// Set the height of the progress layout to 0. This performs an animation because we have set the animations to true in this layout		
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) layout_ll_Main_Progress_Bar.getLayoutParams();
		
		layoutParams.height = 0;						
		layout_ll_Main_Progress_Bar.setLayoutParams(layoutParams);		
		
		testProgressDownload = 0;
		testProgressUpload = 0;
		testProgressLatencyPacketLossJitter = 0;
		progressPercent = 0;
	}
	
	/**
	 * Create the test and launches it
	 */
    public void launchTests()
    {    	
    	createManualTest();								// Create the manual test object storing which tests will be performed    	
    	threadRunningTests = new Thread(manualTest);	// Create the thread with the Manual Test Object (Runnable Object) 
		changeLabelText(getString(R.string.label_message_starting_tests));
		threadRunningTests.start();    					// Run the thread		
    }
    
    /**
     * Start the shining effect on the labels. We have 4 text views and we play with the alpha value of them
     */
    private void startShiningLabelsAnimation()
    {
    	// Animation to make the label invisible
    	tv_Status_Label_2.animate().alpha(0.0f).setDuration(1500).setListener(new AnimatorListenerAdapter()
		{
    		// When the animation ends, we call another method to make this labels visible again
            @Override
            public void onAnimationEnd(Animator animation)
            {
            	fadeInLabelAnimation();            	            	
            }
        });    	
    }
    
    /**
     * Makes the labels visible after making them invisible in the startShiningLabelsAnimation. This is called when the startShiningLabelsAnimation methods ends.
     */
    private void fadeInLabelAnimation()
    {
    	tv_Status_Label_2.animate().alpha(1.0f).setDuration(1500).setListener(new AnimatorListenerAdapter()
		{
    		// When the animation ends, we call another method to make this labels invisible again
            @Override
            public void onAnimationEnd(Animator animation)
            {
            	startShiningLabelsAnimation();            	            	
            }
        }); 
    }
    
    /**
     * Change the text in the labels with a subtle animation (cross fading)
     * 
     * @param pLabelText
     */
    private void changeLabelText(final String pLabelText)
    {
    	// If the text is different and we are not during an animation, perform the text change animation
    	if (!pLabelText.equals(tv_Status_Label_1.getText()) && !onChangeLabelTextSemaphore)
    	{
    		onChangeLabelTextSemaphore = true;
    		layout_layout_Shining_Labels.animate().alpha(0.0f).setDuration(300).setListener(new AnimatorListenerAdapter()
        	{
        		@Override
        		public void onAnimationEnd(Animator animation)
        		{
        			tv_Status_Label_1.setText(pLabelText);
        			tv_Status_Label_2.setText(pLabelText);
        			layout_layout_Shining_Labels.animate().alpha(1.0f).setDuration(300).setListener(new AnimatorListenerAdapter()
        			{
        				@Override
        				public void onAnimationEnd(Animator animation)
        				{
        					onChangeLabelTextSemaphore = false;
        				}
					});        			
        		}
    		});			
		}    	
    }

    /**
     * Change the text in the advice message with a transition animation
     * 
     * @param pAdviceMessageRunning
     */
    private void changeAdviceMessageTo(final String pAdviceMessageRunning)
    {
    	tv_Advice_Message.animate().alpha(0.0f).setDuration(300).setListener(new AnimatorListenerAdapter()
    	{
    		@Override
    		public void onAnimationEnd(Animator animation)
    		{
    			tv_Advice_Message.setText(pAdviceMessageRunning);
    	    	tv_Advice_Message.animate().alpha(1.0f).setDuration(300);
    	    	tv_Advice_Message.animate().setListener(null);
    		}
		});    	
    }
    
    /**
     * Restores all selected tests from storage.
     */
    private void restoreWhichTestsToRun()
    {
		Context context = SKApplication.getAppInstance().getApplicationContext();
    	SharedPreferences prefs = context.getSharedPreferences(getString(R.string.sharedPreferencesIdentifier),Context.MODE_PRIVATE);
    	
    	if (SKApplication.getAppInstance().allowUserToSelectTestToRun() == false) {
    		SharedPreferences.Editor editor = prefs.edit();
    		editor.putBoolean("downloadTestState", true);
    		editor.putBoolean("uploadTestState", true);
    		editor.putBoolean("latencyAndLossTestState", true);
	    	editor.commit();
	    	
    		// User is not allowed to specify which tests to run.
        	test_selected_download = true;
        	test_selected_upload = true;
        	test_selected_latency_and_packet_loss_and_jitter = true;
    		
    	} else {

    		// Get the selected tests from shared preferences

    		test_selected_download = prefs.getBoolean("downloadTestState", false);
    		test_selected_upload = prefs.getBoolean("uploadTestState", false);
    		test_selected_latency_and_packet_loss_and_jitter = prefs.getBoolean("latencyAndLossTestState", false);
    	}
    }
    
    /**
     * Checks if at least one test is selected. True if so, false if any test is selected
     * 
     * @return true or false
     */
    private boolean atLeastOneTestSelected()
    {
    	restoreWhichTestsToRun();
    	
    	return test_selected_download || test_selected_upload || test_selected_latency_and_packet_loss_and_jitter;
    }
    
    /**
     * Create the ManualTest object depending on which tests are selected
     */
    private void createManualTest()
    {
    	List<Integer> testIDs;
    	StringBuilder errorDescription = new StringBuilder();    	
    	
		testIDs = findOutTestIDs();		
		
		if (getActivity() == null) {
			SKLogger.sAssert(getClass(), false);
			return;
		}
		
		// Perform the selected tests
    	if (!testIDs.contains(-1))
    	{
    		manualTest = ManualTest.create(getActivity(), testResultsHandler, testIDs, errorDescription);			
		}
    	// Perform all tests
    	else
    	{
    		manualTest = ManualTest.create(getActivity(), testResultsHandler, errorDescription);
    		
    		if (manualTest == null) {
    			if (errorDescription.toString().contains(getString(R.string.manual_test_create_failed_3))) {
    				showAlertCannotRunTestAsBackgroundTaskIsRunning();
    			} else {
    				showAlertForTestWithMessageBody (getString(R.string.unexpected_error));
    			}
    		}
    	}    	
    }
   
    // Shown when the background test is already running, so we cannot run the manual
    // test at the moment...
    void showAlertCannotRunTestAsBackgroundTaskIsRunning () {
    	// The App is busy
      showAlertForTestWithMessageBody (getString(R.string.manual_test_error)); // Unable to launch test, a background task is running. Please retry shortly.
    }
    
    
    void showAlertForTestWithMessageBody (String bodyMessage) {
    	
    	if (getActivity() == null) {
    		SKLogger.sAssert(getClass(), false);
    		return;
    	}

    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(R.string.tests_running_title); // The app is busy
    	builder.setMessage(bodyMessage)
    	.setCancelable(false)
    	.setPositiveButton(R.string.ok_dialog, new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int id) {
    			dialog.dismiss();
    		}
    	});
    	builder.create().show();

    }
    
    
    /**
     * Checks which tests are selected
     * 
     * @return a list of tests
     */
    private List<Integer> findOutTestIDs()
    {    	
    	// Get the selected tests from shared preferences
    	restoreWhichTestsToRun();
    	
    	List<Integer> testIDs = new ArrayList<Integer>();
    	
    	// Create the list of tests to be performed
    	// All tests
    	if (test_selected_download && test_selected_upload && test_selected_latency_and_packet_loss_and_jitter)
    	{
    		testIDs.add(0);
    		testIDs.add(-1);
    		numberOfTestsToBePerformed = 3;
    		return testIDs;
		}
    	// Just Download test
    	if (test_selected_download && !test_selected_upload && !test_selected_latency_and_packet_loss_and_jitter)
    	{    		
    		testIDs.add(2);
    		numberOfTestsToBePerformed = 1;
    		return testIDs;
		}
    	// Just Upload test
    	if (!test_selected_download && test_selected_upload && !test_selected_latency_and_packet_loss_and_jitter)
    	{
    		testIDs.add(3);
    		numberOfTestsToBePerformed = 1;
    		return testIDs;
		}
    	// Just latency and loss
    	if (!test_selected_download && !test_selected_upload && test_selected_latency_and_packet_loss_and_jitter)
    	{
    		testIDs.add(4);
    		numberOfTestsToBePerformed = 1;
    		return testIDs;
		}    	
    	// Download and upload
    	if (test_selected_download && test_selected_upload && !test_selected_latency_and_packet_loss_and_jitter)
    	{
    		testIDs.add(2);
    		testIDs.add(3);
    		numberOfTestsToBePerformed = 2;
    		return testIDs;
		}
    	// Download and latency and loss
    	if (test_selected_download && !test_selected_upload && test_selected_latency_and_packet_loss_and_jitter)
    	{
    		testIDs.add(2);
    		testIDs.add(4);
    		numberOfTestsToBePerformed = 2;
    		return testIDs;
		}
    	// Upload and latency and loss
    	if (!test_selected_download && test_selected_upload && test_selected_latency_and_packet_loss_and_jitter)
    	{
    		testIDs.add(3);
    		testIDs.add(4);
    		numberOfTestsToBePerformed = 2;
    		return testIDs;
		}
    	
    	// Default case
    	testIDs.add(-1);
    	numberOfTestsToBePerformed = 3;
    	
    	return testIDs;
    }
    
    /**
     * Checks if the data cap has been or might be exceeded
     */
    private void checkOutDataCap()
    {
    	// If the data cap is enabled
    	if (SKApplication.getAppInstance().getIsDataCapEnabled() == true)
    	{
    		String warningMessage = "";

    		// If the data cap was already reached, set the message that was already exceeded
    	    if (SK2AppSettings.getSK2AppSettingsInstance().isDataCapAlreadyReached())
    	    {    	    	
    	    	warningMessage = getString(R.string.data_cap_warning_already_exceeded);
        	}
    	    else	// If the data cap wasn't reached, check if could be reached in the next run
    	    {
    	    	createManualTest();
    	    	
    	    	if (manualTest != null && SK2AppSettings.getSK2AppSettingsInstance().isDataCapLikelyToBeReached(manualTest.getNetUsage()))
    	    	{    	    		
    	    		warningMessage = getString(R.string.data_cap_warning_might_be_exceeded);
    	    	}
    	    }
    	    
    	    // If the data cap was already reached or could be reached in the next run, show a message warning the user
    		if (warningMessage.length() > 0)
    		{
    			Context context = SKApplication.getAppInstance().getApplicationContext();
    			Toast.makeText(context, warningMessage, Toast.LENGTH_LONG).show();							
    		}
    		else	// If the data cap wasn't reached and won't be reached in the next run, hide the warning (maybe is not showed anyway)
    		{
    		}
    	}    	
    }
    
    /**
     * Set the time of the test in an specific format
     */
    private void setTestTime()
    {    	
    	testTime = System.currentTimeMillis();
    	//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    	//String currentDateandTime = sdf.format(new Date());
        String currentDateandTimeDay = new FormattedValues().getDate(testTime, "dd/MM/yyyy");
        String currentDateandTimeTime = new FormattedValues().getDate(testTime, "HH:mm:ss");
    	//tv_Result_Date.setText(currentDateandTime);    	
		changeFadingTextViewValue(tv_Result_DateDay, currentDateandTimeDay,0);	// Set the gauge main text to STARTING
		changeFadingTextViewValue(tv_Result_DateTime, currentDateandTimeTime,0);	// Set the gauge main text to STARTING
    }
    
    /**
     * Set the connectivity icon depending on the connectivity (which comes from the passive metrics!)
     */
    private void setTestConnectivity(eNetworkTypeResults pNetworkType)
    {
    	//if (Connectivity.isConnectedWifi(getActivity()))
    	if (pNetworkType == eNetworkTypeResults.eNetworkTypeResults_WiFi)
    	{
    		iv_Result_NetworkType.setImageResource(R.drawable.ic_swifi);
    		connectivityType = eNetworkTypeResults.eNetworkTypeResults_WiFi;
		}
    	else
    	{
    		iv_Result_NetworkType.setImageResource(R.drawable.ic_sgsm);
    		connectivityType = eNetworkTypeResults.eNetworkTypeResults_Mobile;
    	}
    }
    
    /**
     * Check connectivity status to show or hide the warning messages (no connectivity, slow connectivity)
     * 
     * @param pIntent
     */
    private void checkConnectivity(Intent pIntent)
    {
    	// Set the network type information to update the changes in the network
    	setNetworkTypeInformation();
    	
		final Context context = SKApplication.getAppInstance().getApplicationContext();
		
    	// Show connectivity warning if there's no connectivity
    	if (pIntent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY,false))
        {                
    		safeRunOnUiThread(new Runnable()
    		{					
				@Override
				public void run()
				{
					if (testsRunning == false)
					{						
						Toast.makeText(context, getString(R.string.no_connectivity), Toast.LENGTH_LONG).show();							
						tv_Gauge_TextView_PsuedoButton.setClickable(false);						
					}											
				}
			});        		
        }
    	// Show slow connection warning if we are on slow connectivity
        else if (!Connectivity.isConnectedFast(context))
    	{
        	safeRunOnUiThread(new Runnable()
        	{				
				@Override
				public void run()
				{	
					Toast.makeText(context, getString(R.string.slow_connectivity), Toast.LENGTH_LONG).show();							
					tv_Gauge_TextView_PsuedoButton.setClickable(true);
				}
			});        	    					
		}
        else
        {
            // Hide connectivity warning if there is connectivity
        	safeRunOnUiThread(new Runnable()
        	{				
				@Override
				public void run()
				{
					if (!testsRunning)
					{		
						tv_Gauge_TextView_PsuedoButton.setClickable(true);
					}
				}
			});
        }    	    	    	
    }
    
    /**
     * Check which is the current connectivity status: whether is connected or not, is fast or not.
     */
    private void checkConnectivityStatus()
    {
    	// Set the network type information to update the changes in the network
    	setNetworkTypeInformation();
    	
		final Context context = SKApplication.getAppInstance().getApplicationContext();
    	if (Connectivity.isConnected(context) == false)
    	{
    		// No connectivity!
    		safeRunOnUiThread(new Runnable()
    		{					
				@Override
				public void run()
				{
					if (testsRunning == false)
					{
						Toast.makeText(context, getString(R.string.no_connectivity), Toast.LENGTH_LONG).show();							
						tv_Gauge_TextView_PsuedoButton.setClickable(false);
					}											
				}
			});    					
		}
    	else if (Connectivity.isConnectedFast(context) == false)
    	{
    		// Slow connectivity!
    		safeRunOnUiThread(new Runnable()
    		{				
				@Override
				public void run()
				{
            		Toast.makeText(context, getString(R.string.slow_connectivity), Toast.LENGTH_LONG).show();							
				}
			});
		}
    	else
    	{
    		// Good connectivity!
    		safeRunOnUiThread(new Runnable()
    		{				
				@Override
				public void run()
				{
					tv_Gauge_TextView_PsuedoButton.setClickable(true);
				}
			});
    		
    	}
    }
    
    /**
     * 
     * @return true if we have internet conection, false otherwise
     */
    private boolean isInternetAvailable()
    {
    	try {
            URL url = new URL("http://www.google.com/");
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
            urlc.setRequestProperty("User-Agent", "test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(1000); // mTimeout is in seconds
            urlc.connect();
            
            if (urlc.getResponseCode() == 200)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    	catch (IOException e)
    	{
            Log.i("warning", "Error checking internet connection", e);
            return false;
        }    	
    }
    
    /**
     * Get the type of network and set it on the information label
     */
    private void setNetworkTypeInformation()
    {
    	if (!testsRunning)
    	{
    		final Context context = SKApplication.getAppInstance().getApplicationContext();
    		String networkType = Connectivity.getConnectionType(context);
    		
    		if (networkType == null) {
    			// Unexpected, but defend against it.
    			networkType = SKApplication.getAppInstance().getApplicationContext().getString(R.string.unknown);
    			SKLogger.sAssert(getClass(), false);
    		}
    		
    		final String theNetworkType = networkType;
    		
    		if (!networkType.equals(tv_TopTextNetworkType.getText()))
        	{
        		if (gaugeVisible)
        		{
        			tv_TopTextNetworkType.animate().alpha(0.0f).setDuration(300).setListener(new AnimatorListenerAdapter()
                	{
                		@Override
                		public void onAnimationEnd(Animator animation)
                		{        			
                			tv_TopTextNetworkType.setText(theNetworkType);
                			tv_TopTextNetworkType.animate().alpha(1.0f).setDuration(300);
                			tv_TopTextNetworkType.animate().setListener(null);
                		}    		
            		});					
				}
        		else
        		{
        			tv_TopTextNetworkType.setText(networkType);        			
        		}
    		}
		}    	    	    	    	
    }
    
    /**
     * Set the contextual information label with an animation
     * 
     * @param second
     */
    private void changeUnitsInformationLabel(final String value)
    {    	
		if (testsRunning == false) 
		{
			if (value.length() > 0)
			{
				// Do NOT update the unit text to non-empty string, if the test isn't running!
				return;
			}
		}
		
    	if (!onContextualInformationLabelAnimationSemaphore && !mUnitText.getText().equals(value))
    	{
    		onContextualInformationLabelAnimationSemaphore = true;
    		
    		mUnitText.animate().alpha(0.0f).setDuration(300).setListener(new AnimatorListenerAdapter()
        	{
        		@Override
        		public void onAnimationEnd(Animator animation)
        		{        			
        			String useValue = value;
        			if (testsRunning == false) 
        			{
        				useValue = "";
        			}
        			
        			mUnitText.setText(useValue);
        			mUnitText.animate().alpha(1.0f).setDuration(300);
        			onContextualInformationLabelAnimationSemaphore = false;
        		}    		
    		});			
		}    	  	
    }
    
    /**
     * Send message to the other fragments to refresh the UI data
     */
    private void sendRefreshUIMessage()
    {
    	final Context context = SKApplication.getAppInstance().getApplicationContext();
    	LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("refreshUIMessage"));
    }
    
    /**
     * Set the result fields value with an animation
     * 
     * @param pTextView
     * @param pValue
     */
    private void changeFadingTextViewValue(final TextView pTextView, final String pValue, final int pColor)
    {
    	if (!pTextView.getText().toString().equals(pValue))
    	{
    		pTextView.animate().setDuration(300).alpha(0.0f).setListener(new AnimatorListenerAdapter()
        	{
        		@Override
        		public void onAnimationEnd(Animator animation)
        		{    		
        			super.onAnimationEnd(animation);
        			
        			if (pColor != 0)
        			{
        				pTextView.setTextColor(pColor);					
    				}
        			pTextView.setText(pValue);
        			pTextView.animate().setDuration(300).alpha(1.0f);
        			
        			pTextView.animate().setListener(null);		// Set animation listener to null to avoid side effects
        		}
    		});			
		}    	    	
    }
    
    /**
     * Convert dips to pixels
     * 
     * @param pDP
     * @return
     */
    private int dpToPx(int pDP)
    {        
        return Math.round((float)pDP * screenDensity);
    }
    
    /**
     * Show or hide the visibility of the passive metrics related to mobile network
     * 
     * @param pNetworkType
     */
    private void setUpPassiveMetricsLayout(eNetworkTypeResults pNetworkType)
    {
    	int visibility;
    	
    	if (pNetworkType == eNetworkTypeResults.eNetworkTypeResults_WiFi)
    	{
    		visibility = View.GONE;   					
		}
    	else
    	{
    		visibility = View.VISIBLE;    		
    	}
    	
    	// Header labels
    	tv_header_label_sim_and_network_operators.setVisibility(visibility); 
    	tv_header_label_signal.setVisibility(visibility);    	
    	//Dividers
    	layout_ll_passive_metrics_divider_sim_and_network_operators.setVisibility(visibility);
    	layout_ll_passive_metrics_divider_signal.setVisibility(visibility);
    	// Labels
    	tv_label_sim_operator.setVisibility(visibility);
    	tv_label_sim_operator_code.setVisibility(visibility);
    	tv_label_network_operator.setVisibility(visibility);
    	tv_label_network_operator_code.setVisibility(visibility);
    	tv_label_roaming_status.setVisibility(visibility);
		tv_label_cell_tower_ID.setVisibility(visibility);
		tv_label_cell_tower_area_location_code.setVisibility(visibility); 
		tv_label_signal_strength.setVisibility(visibility);
		tv_label_bearer.setVisibility(visibility);		
		// Results
		tv_result_sim_operator.setVisibility(visibility);
		tv_result_sim_operator_code.setVisibility(visibility);
		tv_result_network_operator.setVisibility(visibility);
		tv_result_network_operator_code.setVisibility(visibility);
		tv_result_roaming_status.setVisibility(visibility);
		tv_result_cell_tower_ID.setVisibility(visibility);
		tv_result_cell_tower_area_location_code.setVisibility(visibility);
		tv_result_signal_strength.setVisibility(visibility);
		tv_result_bearer.setVisibility(visibility);
		
		// If the location metrics are not available, just hide those labels
		int visibilityOfLocation = tv_result_longitude.getText().equals("") ? View.GONE : View.VISIBLE;
		
		tv_header_label_location.setVisibility(visibilityOfLocation);
		layout_ll_passive_metrics_divider_location.setVisibility(visibilityOfLocation);
		tv_label_latitude.setVisibility(visibilityOfLocation);
		tv_label_longitude.setVisibility(visibilityOfLocation);
		tv_label_accuracy.setVisibility(visibilityOfLocation);
		tv_label_provider.setVisibility(visibilityOfLocation);
		tv_result_latitude.setVisibility(visibilityOfLocation);
		tv_result_longitude.setVisibility(visibilityOfLocation);
		tv_result_accuracy.setVisibility(visibilityOfLocation);
		tv_result_provider.setVisibility(visibilityOfLocation);
    }
    
    /**
     * Set the value fields to a initial position
     */
    private void resetValueFields()
    {
    	changeFadingTextViewValue(tv_Result_Download, getString(R.string.slash), 0);
    	changeFadingTextViewValue(tv_Result_Upload, getString(R.string.slash), 0);
    	changeFadingTextViewValue(tv_Result_Latency, getString(R.string.slash), 0);
    	changeFadingTextViewValue(tv_Result_Packet_Loss, getString(R.string.slash), 0);
    	changeFadingTextViewValue(tv_Result_Jitter, getString(R.string.slash), 0);
    	changeFadingTextViewValue(tv_Result_DateDay, getString(R.string.slash), 0);
    	changeFadingTextViewValue(tv_Result_DateTime, getString(R.string.slash), 0);
    }

    // *** MENUS *** //
    // Initialise the contents of the Activity's standard options menu.
    // You should place your menu items in to menu. For this method to be called, you must have first called setHasOptionsMenu(boolean).
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    	inflater.inflate(R.menu.menu_fragment_run_test, menu); 
    	
    	menuItem_SelectTests = menu.findItem(R.id.menu_item_fragment_run_test_select_tests);
    	menuItem_ShareResult = menu.findItem(R.id.menu_item_fragment_run_test_share_result);
    	
		menuItem_ShareResult.setVisible((!gaugeVisible) && (connectivityType == eNetworkTypeResults.eNetworkTypeResults_Mobile));
		
		if (menuItem_SelectTests != null) {
			menuItem_SelectTests.setVisible(SKApplication.getAppInstance().allowUserToSelectTestToRun());
		}
    }

    // This hook is called whenever an item in your options menu is selected.
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	int itemId = item.getItemId();
    	
    	if (itemId == R.id.menu_item_fragment_run_test_select_tests) {
			if (SKApplication.getAppInstance().allowUserToSelectTestToRun() == false) {
    			menuItem_SelectTests.setVisible(SKApplication.getAppInstance().allowUserToSelectTestToRun());
    			SKLogger.sAssert(getClass(), false);
    			return true;
			}
    		// Case select tests
    		Intent intent_select_tests = new Intent(getActivity(),ActivitySelectTests.class);
    		startActivity(intent_select_tests);

    		return true;
    	}
    	
    	if (itemId == R.id.menu_item_fragment_run_test_share_result) {
     		if (connectivityType == eNetworkTypeResults.eNetworkTypeResults_WiFi) {
     			SKLogger.sAssert(getClass(), false);
     			
        		menuItem_ShareResult.setVisible((!gaugeVisible) && (connectivityType == eNetworkTypeResults.eNetworkTypeResults_Mobile));
     		} else {
     			FormattedValues formattedValues = new FormattedValues();

     			try {
     				Intent intent_share_result_activity = new Intent(getActivity(), ActivityShareResult.class);
     				intent_share_result_activity.putExtra("downloadResult", tv_Result_Download.getText() + " " + tv_DownloadUnits.getText());
     				intent_share_result_activity.putExtra("uploadResult", tv_Result_Upload.getText() + " " + tv_UploadUnits.getText());
     				intent_share_result_activity.putExtra("latencyResult", String.valueOf(formattedValues.getFormattedLatencyValue(tv_Result_Latency.getText().toString()).first));
     				intent_share_result_activity.putExtra("packetLossResult", String.valueOf(formattedValues.getFormattedPacketLossValue(tv_Result_Packet_Loss.getText().toString()).first));
     				intent_share_result_activity.putExtra("jitterResult", String.valueOf(formattedValues.getFormattedJitter(tv_Result_Jitter.getText().toString()).first));
     				intent_share_result_activity.putExtra("networkType", 2); // 2 mobile
     				intent_share_result_activity.putExtra("dateResult", testTime); 				

     				startActivity(intent_share_result_activity);
     			} catch (Exception e) {
     				// Don't let this crash the app!
     				SKLogger.sAssert(getClass(), false);
     			}
     		}

    		return true;
    	}
    	
    	if (itemId == R.id.menu_force_background_test) {
    		MainService.sForceBackgroundTest(getActivity());
    		return true;
    	}
    			
		return true;
    }

    //
    // This method is called when the activity detects that the running test has completed.
    //
	private void onDidDetectTestCompleted() {
	    checkOutDataCap();												// Check out if we have reach the data cap to show the warning
	    
		testsRunning = false;											// Indicate that we are not running tests
		manualTest = null;
		threadRunningTests = null;
		
		menuItem_SelectTests.setVisible(SKApplication.getAppInstance().allowUserToSelectTestToRun());
		changeAdviceMessageTo(getString(R.string.advice_message_press_the_button_run_again));
		resetProgressBar();
		changeLabelText(getString(R.string.label_message_tests_executed));
		changeFadingTextViewValue(tv_Gauge_TextView_PsuedoButton, getString(R.string.gauge_message_start),0);
		// tv_Gauge_Message.setText(getString(R.string.gauge_message_start));
		gaugeView.setKindOfTest(-1);
		setNetworkTypeInformation();
		sendRefreshUIMessage();
		layout_ll_results.setClickable(true);
		setUpPassiveMetricsLayout(connectivityType);	                        
		//changeFadingTextViewValue(tv_Closest_Server, getString(R.string.TEST_Label_Finding_Best_Target), getResources().getColor(R.color.grey_light));
        mUnitText.setText("");
	}

	private void registerBackButtonHandler() {
		View view = getView();
		view.setFocusableInTouchMode(true);
		view.requestFocus();
		view.setOnKeyListener(new View.OnKeyListener() {
		        @Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
		            if( keyCode == KeyEvent.KEYCODE_BACK ) {
		            	// TODO - should we handle this ourselves, or not?!
						if (gaugeVisible == true) {
							// Don't handle it...
							return false;
						}
						
						// Handle the back button event directly.
		    			// The gauge elements are invisible - show them and hide the passive metrics.
						showGaugeHidePassiveMetricsPanel();
		            	return true;
		            } else {
							// Don't handle it...
		                return false;
		            }
		        }
		    });
	}
	

	private void hideGaugeShowPassiveMetricsPanel() {
		
		// Find the public_ip and submission_id!
        publicIp.setText(SKApplication.getAppInstance().mLastPublicIp);
        submissionId.setText(SKApplication.getAppInstance().mLastSubmissionId);
				
		tv_Advice_Message.animate().setDuration(300).alpha(0.0f);
		tv_TopTextNetworkType.animate().setDuration(300).alpha(0.0f);
		tv_Gauge_TextView_PsuedoButton.animate().setDuration(300).alpha(0.0f);
		layout_layout_Shining_Labels.animate().setDuration(300).alpha(0.0f);
		mUnitText.animate().setDuration(300).alpha(0.0f);
		gaugeViewContainer.animate().setDuration(300).alpha(0.0f).setListener(new AnimatorListenerAdapter()
		{
			// Executed at the end of the animation
			@Override
			public void onAnimationEnd(Animator animation)
			{					
				super.onAnimationEnd(animation);
				
				gaugeViewContainer.animate().setListener(null);		// Remove listener to avoid side effects
				
				// Hide all the gauge elements
				tv_Advice_Message.setVisibility(View.GONE);
				tv_TopTextNetworkType.setVisibility(View.GONE);
				tv_Gauge_TextView_PsuedoButton.setVisibility(View.GONE);
				layout_layout_Shining_Labels.setVisibility(View.GONE);
				gaugeViewContainer.setVisibility(View.GONE);
				mUnitText.setVisibility(View.GONE);
				
				gaugeVisible = false;
				
        		menuItem_ShareResult.setVisible((!gaugeVisible) && (connectivityType == eNetworkTypeResults.eNetworkTypeResults_Mobile));

				// Move the results layout to the top of the screen
				run_results_panel_frame_to_animate.animate().setDuration(300).y(dpToPx(8)).setInterpolator(new OvershootInterpolator(1.2f));
				// Make the passive metrics layout visible
				layout_ll_passive_metrics.animate().setDuration(300).alpha(1.0f);
				layout_ll_passive_metrics.setVisibility(View.VISIBLE);
			}
		});
	}

	private void showGaugeHidePassiveMetricsPanel() {
		layout_ll_passive_metrics.animate().setDuration(300).alpha(0.0f).setListener(new AnimatorListenerAdapter()
		{
			// Executed at the end of the animation
			@Override
			public void onAnimationEnd(Animator animation)
			{
				super.onAnimationEnd(animation);
				
				layout_ll_passive_metrics.animate().setListener(null);		// Remove listeners to avoid side effects
				// Move the results layout to the default position
				run_results_panel_frame_to_animate.animate().setDuration(300).y(results_Layout_Position_Y).setInterpolator(new OvershootInterpolator(1.2f)).setListener(new AnimatorListenerAdapter()
				{
					@Override
					public void onAnimationEnd(Animator animation)
					{
						// Executed at the end of the animation
						super.onAnimationEnd(animation);
						
						run_results_panel_frame_to_animate.animate().setListener(null);				// Remove listeners to avoid side effects
						
						// Make gauge elements visible
						tv_Advice_Message.setVisibility(View.VISIBLE);
						tv_TopTextNetworkType.setVisibility(View.VISIBLE);
						tv_Gauge_TextView_PsuedoButton.setVisibility(View.VISIBLE);
						layout_layout_Shining_Labels.setVisibility(View.VISIBLE);					
						gaugeViewContainer.setVisibility(View.VISIBLE);
						mUnitText.setVisibility(View.VISIBLE);
						
						gaugeViewContainer.animate().setDuration(300).alpha(1.0f);
						tv_Advice_Message.animate().setDuration(300).alpha(1.0f);
						tv_TopTextNetworkType.animate().setDuration(300).alpha(1.0f);
						tv_Gauge_TextView_PsuedoButton.animate().setDuration(300).alpha(1.0f);
						layout_layout_Shining_Labels.animate().setDuration(300).alpha(1.0f);
						mUnitText.animate().setDuration(300).alpha(1.0f);
						
        				layout_ll_passive_metrics.setVisibility(View.GONE);
						gaugeVisible = true;
						
						menuItem_ShareResult.setVisible(false);
					}
				});
			}
		});
	}


	private void testRunningAskUserIfTheyWantToCancelIt() {
		//
		// Tests are running - stop the tests, if the user agrees!
		//
    	if (getActivity() == null) {
    		SKLogger.sAssert(getClass(), false);
    		return;
    	}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.tests_running_title);
		builder.setMessage(R.string.tests_running_message)
		.setCancelable(false)
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		})
		.setPositiveButton(R.string.ok_dialog, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();

				// Note that if the test has auto-stopped since we started asking
				// the user, the manualTest value will be null; so, we need to defend
				// against that.
				if (manualTest != null) {
					changeAdviceMessageTo(getString(R.string.advice_message_stopping));
					manualTest.stop();
				}
			}
		});
		builder.create().show();
	}
	
	private void onStartButtonPressed() {
		if (MainService.isExecuting()) {
			showAlertCannotRunTestAsBackgroundTaskIsRunning();
		}
		else if (testsRunning == true) {
			if (manualTest == null) {
				SKLogger.sAssert(getClass(), false);
				// Should not happen - force a tidy-up!
				onDidDetectTestCompleted();
			} else if (threadRunningTests == null) {
				SKLogger.sAssert(getClass(), false);
				// Should not happen - force a tidy-up!
				onDidDetectTestCompleted();
			} else {
				//
				// Tests are running - stop the tests, if the user agrees!
				//
				testRunningAskUserIfTheyWantToCancelIt();
			}
		} else {
			//
			// Tests are not running - start the test!
			//

			// If at least one test is selected
			if (atLeastOneTestSelected())
			{
				initial_warning_text.setVisibility(View.GONE);
				SKApplication.getAppInstance().mLastPublicIp = "";
				SKApplication.getAppInstance().mLastSubmissionId = "";
				publicIp.setText(SKApplication.getAppInstance().mLastPublicIp);
				submissionId.setText(SKApplication.getAppInstance().mLastSubmissionId);

				// Make sure that we display the correct image (for now!) in the Test results panel.
				// This will be overridden by incoming passive metric data.
				final Context context = SKApplication.getAppInstance().getApplicationContext();
				if (Connectivity.isConnectedWifi(context)) {
					setTestConnectivity(eNetworkTypeResults.eNetworkTypeResults_WiFi);
				} else {
					setTestConnectivity(eNetworkTypeResults.eNetworkTypeResults_Mobile);
				}

				if (SKApplication.getAppInstance().getDoesAppDisplayClosestTargetInfo()) {
					mUnitText.setText(R.string.TEST_Label_Finding_Best_Target);        
				}
				new InitTestAsyncTask().execute();												
			}
			else
			{
				// No tests are selected: show the activity to select tests
				if (getActivity() == null) {
					SKLogger.sAssert(getClass(), false);
					return;
				}
				Intent intent_select_tests = new Intent(getActivity(),ActivitySelectTests.class);
				startActivity(intent_select_tests);
			}
		}
	}
}