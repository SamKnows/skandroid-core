package com.samknows.ui2.activity;

import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samknows.ska.activity.PointElement;
import com.samknows.libcore.SKLogger;
import com.samknows.libui2.R;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.SKApplication.eNetworkTypeResults;
import com.samknows.measurement.activity.components.FontFitTextView;
import com.samknows.measurement.activity.components.SKGraphForResults;
import com.samknows.measurement.activity.components.SKGraphForResults.DATERANGE_1w1m3m1y;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.StorageTestResult;
import com.samknows.measurement.storage.SummaryResult;

/**
 * This fragment is responsible for:
 * * Show average results
 * * Show best results
 * * Filter results by time period and network type 
 * * Show graphs
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */


public class FragmentSummary extends Fragment
{
	// *** CONSTANTS *** //
	private final static String C_TAG_FRAGMENT_SUMMARY = "Fragment Summary";		// Tag for this fragment
	
	// *** VARIABLES *** //
	private int summary_section_height, header_section_height;
	// The initial position of each or the rows
	private float headerPositionX, headerPositionY, initialPositionUploadX, initialPositionUploadY, initialPositionLatencyX, initialPositionLatencyY, initialPositionLossX, initialPositionLossY,
					initialPositionJitterX, initialPositionJitterY;
	private boolean rowsHidden = false;		// If true, some of the rows are hidden (we are showing the graph), if false we are showing all the rows (not showing the graphs)
	private long startTime;

	private ArrayList<SummaryResult> aList_SummaryResults = new ArrayList<SummaryResult>();	// List of the summary results
	
	// UI elements
	private Typeface typeface_Din_Condensed_Cyrillic, typeface_Roboto_Light, typeface_Roboto_Thin, typeface_Roboto_Regular;
	private FontFitTextView tv_summary_result_average_download;
	private FontFitTextView tv_summary_result_best_download;
	private FontFitTextView tv_summary_result_average_upload;
	private FontFitTextView tv_summary_result_best_upload;
	private FontFitTextView tv_summary_result_average_latency;
	private FontFitTextView tv_summary_result_best_latency;
	private FontFitTextView tv_summary_result_average_loss;
	private FontFitTextView tv_summary_result_best_loss;
	private FontFitTextView tv_summary_result_average_jitter;
	private FontFitTextView tv_summary_result_best_jitter;
	private TextView tv_label_average, tv_label_best;
	private LinearLayout mShowingThisSection = null;
	private LinearLayout layout_ll_summary_section_download, layout_ll_summary_section_upload, layout_ll_summary_section_latency, layout_ll_summary_section_packet_loss, layout_ll_summary_section_jitter, layout_ll_summary_result_average_download,
							layout_ll_summary_result_best_download, layout_ll_summary_result_average_upload, layout_ll_summary_result_best_upload, layout_ll_summary_result_average_latency, layout_ll_summary_result_best_latency, layout_ll_summary_result_average_packet_loss,
								layout_ll_summary_result_best_packet_loss, layout_ll_summary_result_average_jitter, layout_ll_summary_result_best_jitter;
	private LinearLayout layout_ll_chart, layout_ll_header, layout_ll_summary_main;
	TextView mChartCaption;
	
	// Container for the graph
	private WebView graphContainer;
	
	private MenuItem menu_item_seletcNetworkType, menu_Item_Select_Time_Period, menu_Item_Refresh_Spinner;
	private boolean asyncTask_RetrieveData_Running = false;		// Whether or not the asynchronous task is running
	private boolean asyncTask_PrepareChart_Running = false;		// Whether or not the asynchronous task is running
	private boolean aList_SummaryResults_No_Empty_For_Download, aList_SummaryResults_No_Empty_For_Upload, aList_SummaryResults_No_Empty_For_Latency, aList_SummaryResults_No_Empty_For_Packet_Loss, aList_SummaryResults_No_Empty_For_Jitter;
	
	// Database
	private DBHelper dbHelper;

	// *** FRAGMENT LIFECYCLE METHODS *** //
    // Called to have the fragment instantiate its user interface view.
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_summary, container, false);	// Create the view
		setUpResources(view);	// Bind the resources with the elements in this class and set up them
		
		new RetrieveAverageAndBestResults().execute();		// Background process retrieving the data for the first time
		new PrepareChartData().execute();		// Prepare the chart data. This needs to be done here because a filter was triggered
		
		return view;						// Inflate the layout for this fragment
	}
	
	// Called when the Fragment is no longer resumed. This is generally tied to Activity.onPause of the containing Activity's life cycle
	@Override
	public void onResume()
	{
		super.onResume();
		
		registerBackButtonHandler();
		
		// Register the local broadcast receiver listener to receive messages when the UI data needs to be refreshed.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(updateScreenMessageReceiver, new IntentFilter("refreshUIMessage"));
    }
	
	// Receive the result from a previous call to startActivityForResult(Intent, int)
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		// Check if the data is different from null. Is null for instance when the user leaves the activity with the back button		
		if (data != null)
		{
			// Check what activity was launched as startActivityForResults
			switch (requestCode)
			{
				// The activity that we came from is ActivitySelectNetworkType
				case 0:
					int networkType = data.getIntExtra("networkType", 0);
		        	
					// If the network selection is different from the current network selection
		        	if (networkType != getNetworkTypeSelection())
		        	{
		        		saveNetworkTypeSelection(networkType);		// Save the new network selection
		        		new RetrieveAverageAndBestResults().execute();				// Retrieve the data with the current network type
		        		new PrepareChartData().execute();				// Prepare the chart data. This needs to be done here because a filter was triggered
					}
					break;

				// The activity that we came from is ActivitySelectTimePeriod
				case 1:
					int time_period = data.getIntExtra("timePeriod", 0);

					// If the time period selection is different from the current time period selection
					if (time_period != getTimePeriodSelection())
					{
						saveTimePeriodSelection(time_period);					// Save the new time period selection
						new RetrieveAverageAndBestResults().execute();			// Retrieve the data with the current time period
						new PrepareChartData().execute();						// Prepare the chart data. This needs to be done here because a filter was triggered
					}
					break;

				default:
					break;
			}						
		}        
	}
	
	// Called when the fragment is visible to the user and actively running
	@Override
	public void onPause()
	{
		super.onPause();
		
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(updateScreenMessageReceiver);
	}
	
	// *** BROADCAST RECEIVERS *** //
	// Our handler for received Intents. This will be called whenever an Intent with an action named "refreshUIMessage" is broadcasted.
	private BroadcastReceiver updateScreenMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{		    
		    // Refresh the UI data. This it's need because the list view data was modified
		    new RetrieveAverageAndBestResults().execute();
		    new PrepareChartData().execute();		// Prepare the chart data. This needs to be done here because a filter was triggered
		}
	};
	
	// *** INNER CLASSES *** //	
	/**
	 * Retrieve the data from the database and update the UI
	 */
	private class RetrieveAverageAndBestResults extends AsyncTask<Void, Void, Void>
	{
		// Runs on the UI thread before doInBackground
		@Override
		protected void onPreExecute()
		{		
			super.onPreExecute();
			
			asyncTask_RetrieveData_Running = true;
			
			aList_SummaryResults_No_Empty_For_Download = false;
			aList_SummaryResults_No_Empty_For_Upload = false;
			aList_SummaryResults_No_Empty_For_Latency = false;
			aList_SummaryResults_No_Empty_For_Packet_Loss = false;
			aList_SummaryResults_No_Empty_For_Jitter = false;
			
			// Restore all the fields before set the new values
			// This is made because sometimes not all values are refreshed and they will show an slash
			getActivity().runOnUiThread(new Runnable()
			{							
				@Override
				public void run()
				{
					layout_ll_summary_result_average_download.setGravity(Gravity.CENTER);
					layout_ll_summary_result_best_download.setGravity(Gravity.CENTER);
					layout_ll_summary_result_average_upload.setGravity(Gravity.CENTER);
					layout_ll_summary_result_best_upload.setGravity(Gravity.CENTER);
					layout_ll_summary_result_average_latency.setGravity(Gravity.CENTER);
					layout_ll_summary_result_best_latency.setGravity(Gravity.CENTER);
					layout_ll_summary_result_average_packet_loss.setGravity(Gravity.CENTER);
					layout_ll_summary_result_best_packet_loss.setGravity(Gravity.CENTER);
					layout_ll_summary_result_average_jitter.setGravity(Gravity.CENTER);
					layout_ll_summary_result_best_jitter.setGravity(Gravity.CENTER);
					
					tv_summary_result_average_download.setText(getString(R.string.slash));								
					tv_summary_result_best_download.setText((R.string.slash));
					tv_summary_result_average_upload.setText(getString(R.string.slash));								
					tv_summary_result_best_upload.setText((R.string.slash));
					tv_summary_result_average_latency.setText(getString(R.string.slash));								
					tv_summary_result_best_latency.setText((R.string.slash));
					tv_summary_result_average_loss.setText(getString(R.string.slash));								
					tv_summary_result_best_loss.setText((R.string.slash));
					tv_summary_result_average_jitter.setText(getString(R.string.slash));								
					tv_summary_result_best_jitter.setText((R.string.slash));
				}
			});
		
			// TODO - this could fail with random SQL error when run in an async task!
    		ExtractSummaryValues ();
		}
		
		private void ExtractSummaryValues () {
			// Get the summary values from the data base
			aList_SummaryResults = dbHelper.getSummaryValues(getNetworkTypeSelection(), calculateTimePeriodStart(getTimePeriodSelection()));
			
			final FormattedValues formattedValues = new FormattedValues();		// Class to format the values
			
			// Iterate the summary results list
			for (final SummaryResult summaryResult : aList_SummaryResults)
			{
				switch (summaryResult.getTestType())
				{
					// Summary result is download type
					case 0:
						aList_SummaryResults_No_Empty_For_Download = true;
						
						getActivity().runOnUiThread(new Runnable()		// UI modifications must be executed in the UI thread
						{							
							@Override
							public void run()
							{
								layout_ll_summary_result_average_download.setGravity(Gravity.LEFT);
								layout_ll_summary_result_best_download.setGravity(Gravity.RIGHT);
								
								tv_summary_result_average_download.setText(formattedValues.getFormattedSpeedValue(summaryResult.getAverage()));
								tv_summary_result_best_download.setText(formattedValues.getFormattedSpeedValue(summaryResult.getMax()));
							}
						});						
						break;
					// Summary result is upload type
					case 1:						
						aList_SummaryResults_No_Empty_For_Upload = true;
						
						getActivity().runOnUiThread(new Runnable()		// UI modifications must be executed in the UI thread
						{							
							@Override
							public void run()
							{
								layout_ll_summary_result_average_upload.setGravity(Gravity.LEFT);
								layout_ll_summary_result_best_upload.setGravity(Gravity.RIGHT);
								
								tv_summary_result_average_upload.setText(formattedValues.getFormattedSpeedValue(summaryResult.getAverage()));
								tv_summary_result_best_upload.setText(formattedValues.getFormattedSpeedValue(summaryResult.getMax()));
							}
						});
						break;
					// Summary result is latency type
					case 2:
						aList_SummaryResults_No_Empty_For_Latency = true;
						
						getActivity().runOnUiThread(new Runnable()		// UI modifications must be executed in the UI thread
						{							
							@Override
							public void run()
							{
								layout_ll_summary_result_average_latency.setGravity(Gravity.LEFT);
								layout_ll_summary_result_best_latency.setGravity(Gravity.RIGHT);
								
								tv_summary_result_average_latency.setText(String.format("%.0f", summaryResult.getAverage()));								
								tv_summary_result_best_latency.setText(String.format("%.0f", summaryResult.getMin()));								
							}
						});
						break;
					// Summary result is packet loss type
					case 3:
						aList_SummaryResults_No_Empty_For_Packet_Loss = true;
						
						getActivity().runOnUiThread(new Runnable()		// UI modifications must be executed in the UI thread
						{							
							@Override
							public void run()
							{
								layout_ll_summary_result_average_packet_loss.setGravity(Gravity.LEFT);
								layout_ll_summary_result_best_packet_loss.setGravity(Gravity.RIGHT);
								
								tv_summary_result_average_loss.setText(String.format("%.0f", summaryResult.getAverage()));								
								tv_summary_result_best_loss.setText(String.format("%.0f", summaryResult.getMin()));								
							}
						});
						break;
						
					// Summary result is jitter type
					case 4:
						aList_SummaryResults_No_Empty_For_Jitter = true;
						
						getActivity().runOnUiThread(new Runnable()		// UI modifications must be executed in the UI thread
						{							
							@Override
							public void run()
							{
								layout_ll_summary_result_average_jitter.setGravity(Gravity.LEFT);
								layout_ll_summary_result_best_jitter.setGravity(Gravity.RIGHT);
								
								tv_summary_result_average_jitter.setText(String.format("%.0f", summaryResult.getAverage()));
								tv_summary_result_best_jitter.setText(String.format("%.0f", summaryResult.getMin()));
							}
						});
						break;
	
					default:
						break;
				}
			}
		}

		// Override this method to perform a computation on a background thread
		@Override
		protected Void doInBackground(Void... params)
		{
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result)
		{		
			super.onPostExecute(result);
			
			asyncTask_RetrieveData_Running = false;
			
			if (menu_Item_Refresh_Spinner != null & asyncTask_PrepareChart_Running == false)
			{
				// Make invisible the progress spinner
				menu_Item_Refresh_Spinner.setVisible(false);				
			}
		}
	}
	
	
	/**
	 * Prepare the chart data
	 */
	private class PrepareChartData extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected void onPreExecute()
		{		
			super.onPreExecute();
			
			asyncTask_PrepareChart_Running = true;
			
			// Set the possibility to display the graphs to true
			chartsAvailable(false);
			
			if (menu_Item_Refresh_Spinner != null)
			{
				// Make visible the progress spinner
				menu_Item_Refresh_Spinner.setVisible(true);				
			}
		
			// TODO - this CANNOT be run in the background - achartengine simply doesn't allow for it!
			// TODO - 0 is the DOWNLOAD etc. test id!
			prepareChartEnvironment(0, getTimePeriodSelection());
		}
		// Override this method to perform a computation on a background thread
		@Override
		protected Void doInBackground(Void... params)
		{
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);
			
			asyncTask_PrepareChart_Running = false;
			
			if (menu_Item_Refresh_Spinner != null & asyncTask_RetrieveData_Running == false)
			{
				// Make invisible the progress spinner
				menu_Item_Refresh_Spinner.setVisible(false);				
			}
			
			// Set the possibility to display the graphs to true
			chartsAvailable(true);			
		}
	}

	private boolean hide_ll_chart_generic() {
		if (mShowingThisSection == layout_ll_summary_section_download) {
			hide_ll_chart_download();
		} else if (mShowingThisSection == layout_ll_summary_section_upload) {
			hide_ll_chart_upload();
		} else if (mShowingThisSection == layout_ll_summary_section_latency) {
			hide_ll_chart_latency();
		} else if (mShowingThisSection == layout_ll_summary_section_packet_loss) {
			hide_ll_chart_packetloss();
		} else if (mShowingThisSection == layout_ll_summary_section_jitter) {
			hide_ll_chart_jitter();
		} else {
			SKLogger.sAssert(getClass(),  false);
			return false;
		}
		
		return true;
	}

	private void hide_ll_chart_download() {
		// Hide the chart
		mChartCaption.setAlpha(0.0F);
		layout_ll_chart.animate().setDuration(300).alpha(0.0f).setListener(new AnimatorListenerAdapter()
		{
			// Executed at the end of the animation
			@Override
			public void onAnimationEnd(Animator animation)
			{						
				super.onAnimationEnd(animation);
				layout_ll_chart.animate().setListener(null);
        		layout_ll_chart.setVisibility(View.GONE);
				
				// Set the position of the other rows to the initial position with an animation.
				layout_ll_summary_section_upload.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
				layout_ll_summary_section_latency.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
				layout_ll_summary_section_jitter.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
				layout_ll_summary_section_packet_loss.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f)).setListener(new AnimatorListenerAdapter()								
				{
					// Executed at the end of the animation
					@Override
					public void onAnimationEnd(Animator animation)
					{						
						super.onAnimationEnd(animation);
						
						// Enable click events
						setLayoutsClickable(true);
						// Enable action bar menu filters
						getActivity().invalidateOptionsMenu();
					}
				});
			}
		});
	}
	
	private void hide_ll_chart_upload() {
		// Hide the chart
		mChartCaption.setAlpha(0.0F);
		layout_ll_chart.animate().setDuration(300).alpha(0.0f).setListener(new AnimatorListenerAdapter()
		{
			// Executed at the end of the animation
			@Override
			public void onAnimationEnd(Animator animation)
			{						
				super.onAnimationEnd(animation);
				
				layout_ll_chart.animate().setListener(null);	// Delete the listener to avoid side effects							
        		layout_ll_chart.setVisibility(View.GONE);
				layout_ll_summary_section_upload.animate().setDuration(300).x(initialPositionUploadX).y(initialPositionUploadY).setInterpolator(new OvershootInterpolator(1.2f)).setListener(new AnimatorListenerAdapter()
				{
					// Executed at the end of the animation
					@Override
					public void onAnimationEnd(Animator animation)
					{						
						super.onAnimationEnd(animation);
						// Set the position of the other rows to the initial position with an animation.
						layout_ll_summary_section_download.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_latency.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_packet_loss.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_jitter.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						
						// Delete the listener of the row to avoid side effects
						layout_ll_summary_section_upload.animate().setListener(null);
						
						// Enable click events
						setLayoutsClickable(true);
						// Enable action bar menu filters
						getActivity().invalidateOptionsMenu();
					}
				});
			}
		});	
	}
	
	private void hide_ll_chart_latency() {
		// Hide the chart
		mChartCaption.setAlpha(0.0F);
		layout_ll_chart.animate().setDuration(300).alpha(0.0f).setListener(new AnimatorListenerAdapter()
		{
			// Executed at the end of the animation
			@Override
			public void onAnimationEnd(Animator animation)
			{						
				super.onAnimationEnd(animation);
				layout_ll_chart.animate().setListener(null);
        		layout_ll_chart.setVisibility(View.GONE);
				
				layout_ll_summary_section_latency.animate().setDuration(300).x(initialPositionLatencyX).y(initialPositionLatencyY).setInterpolator(new OvershootInterpolator(1.2f)).setListener(new AnimatorListenerAdapter()
				{
					// Executed at the end of the animation
					@Override
					public void onAnimationEnd(Animator animation)
					{
						// Set the position of the other rows to the initial position with an animation.
						layout_ll_summary_section_upload.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_download.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_packet_loss.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_jitter.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						
						// Delete the listener of the row to avoid side effects
						layout_ll_summary_section_latency.animate().setListener(null);
						
						// Enable click events
						setLayoutsClickable(true);
						// Enable action bar menu filters
						getActivity().invalidateOptionsMenu();
					};
				});
			}
		});
	}
	
	private void hide_ll_chart_packetloss() {
		// Hide the chart
		mChartCaption.setAlpha(0.0F);
		layout_ll_chart.animate().setDuration(300).alpha(0.0f).setListener(new AnimatorListenerAdapter()
		{
			// Executed at the end of the animation
			@Override
			public void onAnimationEnd(Animator animation)
			{						
				super.onAnimationEnd(animation);
				layout_ll_chart.animate().setListener(null);		// Delete animation listener to avoid side effects
        		layout_ll_chart.setVisibility(View.GONE);
				
				layout_ll_summary_section_packet_loss.animate().setDuration(300).x(initialPositionLossX).y(initialPositionLossY).setInterpolator(new OvershootInterpolator(1.2f)).setListener(new AnimatorListenerAdapter()
				{
					// Executed at the end of the animation
					@Override
					public void onAnimationEnd(Animator animation)
					{
						// Set the position of the other rows to the initial position with an animation.
						layout_ll_summary_section_download.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_upload.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_latency.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_jitter.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						
						// Delete the listener of the row to avoid side effects
						layout_ll_summary_section_packet_loss.animate().setListener(null);
						
						// Enable click events
						setLayoutsClickable(true);
						// Enable action bar filters									
						getActivity().invalidateOptionsMenu();
					}
				});	
			}
		});
	}
	
	private void hide_ll_chart_jitter() {
		// Hide the chart
		mChartCaption.setAlpha(0.0F);
		layout_ll_chart.animate().setDuration(300).alpha(0.0f).setListener(new AnimatorListenerAdapter()
		{
			// Executed at the end of the animation
			@Override
			public void onAnimationEnd(Animator animation)
			{						
				super.onAnimationEnd(animation);
				
				layout_ll_chart.animate().setListener(null);	// Delete the listener to avoid side effects							
        		layout_ll_chart.setVisibility(View.GONE);
				layout_ll_summary_section_jitter.animate().setDuration(300).x(initialPositionJitterX).y(initialPositionJitterY).setInterpolator(new OvershootInterpolator(1.2f)).setListener(new AnimatorListenerAdapter()
				{
					// Executed at the end of the animation
					@Override
					public void onAnimationEnd(Animator animation)
					{						
						super.onAnimationEnd(animation);
						// Set the position of the other rows to the initial position with an animation.
						layout_ll_summary_section_download.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_upload.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_latency.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						layout_ll_summary_section_packet_loss.animate().x(headerPositionX).setDuration(300).alpha(1.0f).setInterpolator(new OvershootInterpolator(1.2f));
						
						// Delete the listener of the row to avoid side effects
						layout_ll_summary_section_jitter.animate().setListener(null);
						
						// Enable click events
						setLayoutsClickable(true);
						// Enable action bar menu filters
						getActivity().invalidateOptionsMenu();
					}
				});
			}
		});
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
						if (rowsHidden == false) {
							// Don't handle it...
							return false;
						}
						
						// Handle the back button event directly.
		    			// The gauge elements are invisible - show them and hide the passive metrics.
						if (hide_ll_chart_generic() == false) {
							SKLogger.sAssert(getClass(),  false);
							return false;
						}
						
		            	return true;
		            } else {
		            	// Don't handle it...
		            	return false;
		            }
		        }
		    });
	}
	
	
	// *** CUSTOM METHODS *** //
	/**
	 * Bind the resources of the layout with the objects in this class and set up them
	 * 
	 * @param pView View to inflate
	 */
	private void setUpResources(View pView)
	{
		// Create the dbHelper to query the data base
		dbHelper = new DBHelper(getActivity());
		
		// Report that this fragment would like to participate in populating the options menu by receiving a call to onCreateOptionsMenu(Menu, MenuInflater) and related methods.
    	setHasOptionsMenu(true);
    	
    	// Main layout
    	layout_ll_summary_main = (LinearLayout)pView.findViewById(R.id.ll_summary_main);
    	
		// Header fields
    	layout_ll_header = (LinearLayout)pView.findViewById(R.id.ll_header);
		tv_label_average = (TextView)pView.findViewById(R.id.tv_label_average);
		tv_label_best = (TextView)pView.findViewById(R.id.tv_label_best);
		
		// Set up the on click listener to perform a shake animation		
		OnClickListener shakeOnClickListener = new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{				
				v.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.shake));
			}			
		};

		// Assign the on click listener to the header fields
		tv_label_average.setOnClickListener(shakeOnClickListener);		
		tv_label_best.setOnClickListener(shakeOnClickListener);		

		// Main sections of the layout
		layout_ll_summary_section_download = (LinearLayout)pView.findViewById(R.id.ll_summary_section_download);
		layout_ll_summary_section_upload = (LinearLayout)pView.findViewById(R.id.ll_summary_section_upload);
		layout_ll_summary_section_latency = (LinearLayout)pView.findViewById(R.id.ll_summary_section_latency);
		layout_ll_summary_section_packet_loss = (LinearLayout)pView.findViewById(R.id.ll_summary_section_loss);
		layout_ll_summary_section_jitter = (LinearLayout)pView.findViewById(R.id.ll_summary_section_jitter);

		// Results layouts
		layout_ll_summary_result_average_download = (LinearLayout)pView.findViewById(R.id.fragment_summary_download_ll_average);
		layout_ll_summary_result_best_download = (LinearLayout)pView.findViewById(R.id.fragment_summary_download_ll_best);
		layout_ll_summary_result_average_upload = (LinearLayout)pView.findViewById(R.id.fragment_summary_upload_ll_average);
		layout_ll_summary_result_best_upload = (LinearLayout)pView.findViewById(R.id.fragment_summary_upload_ll_best);
		layout_ll_summary_result_average_latency = (LinearLayout)pView.findViewById(R.id.fragment_summary_latency_ll_average);
		layout_ll_summary_result_best_latency = (LinearLayout)pView.findViewById(R.id.fragment_summary_latency_ll_best);
		layout_ll_summary_result_average_packet_loss = (LinearLayout)pView.findViewById(R.id.fragment_summary_packet_loss_ll_average);
		layout_ll_summary_result_best_packet_loss = (LinearLayout)pView.findViewById(R.id.fragment_summary_packet_loss_ll_best);
		layout_ll_summary_result_average_jitter = (LinearLayout)pView.findViewById(R.id.fragment_summary_jitter_ll_average);
		layout_ll_summary_result_best_jitter = (LinearLayout)pView.findViewById(R.id.fragment_summary_jitter_ll_best);
		
		// Result fields
		tv_summary_result_average_download = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_average_download);
		tv_summary_result_best_download = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_best_download);
		tv_summary_result_average_upload = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_average_upload);
		tv_summary_result_best_upload = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_best_upload);
		tv_summary_result_average_latency = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_average_latency);
		tv_summary_result_best_latency = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_best_latency);
		tv_summary_result_average_loss = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_average_packet_loss);
		tv_summary_result_best_loss = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_best_packet_loss);
		tv_summary_result_average_jitter = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_average_jitter);
		tv_summary_result_best_jitter = (FontFitTextView)pView.findViewById(R.id.tv_summary_result_best_jitter);
		tv_summary_result_average_download.setText(R.string.slash);
		tv_summary_result_best_download.setText(R.string.slash);
		tv_summary_result_average_upload.setText(R.string.slash);
		tv_summary_result_best_upload.setText(R.string.slash);
		tv_summary_result_average_latency.setText(R.string.slash);
		tv_summary_result_best_latency.setText(R.string.slash);
		tv_summary_result_average_loss.setText(R.string.slash);
		tv_summary_result_best_loss.setText(R.string.slash);
		tv_summary_result_average_jitter.setText(R.string.slash);
		tv_summary_result_best_jitter.setText(R.string.slash);
		
		// Set up the fonts to be used
		typeface_Din_Condensed_Cyrillic = Typeface.createFromAsset(getActivity().getAssets(), "fonts/roboto_condensed_regular.ttf");
		typeface_Roboto_Light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/roboto_light.ttf");
		typeface_Roboto_Thin = Typeface.createFromAsset(getActivity().getAssets(), "fonts/roboto_thin.ttf");
		typeface_Roboto_Regular = Typeface.createFromAsset(getActivity().getAssets(), "fonts/roboto_regular.ttf");
		
		// Assign fonts the the layout text fields
		tv_summary_result_average_download.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_summary_result_best_download.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_summary_result_average_upload.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_summary_result_best_upload.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_summary_result_average_latency.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_summary_result_best_latency.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_summary_result_average_loss.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_summary_result_best_loss.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_summary_result_average_jitter.setTypeface(typeface_Din_Condensed_Cyrillic);
		tv_summary_result_best_jitter.setTypeface(typeface_Din_Condensed_Cyrillic);
		
		((TextView) pView.findViewById(R.id.tv_label_average)).setTypeface(typeface_Roboto_Regular);
		((TextView) pView.findViewById(R.id.tv_label_best)).setTypeface(typeface_Roboto_Regular);
		((FontFitTextView) pView.findViewById(R.id.tv_summary_label_Mbps_1)).setTypeface(typeface_Roboto_Regular);
		((FontFitTextView) pView.findViewById(R.id.tv_summary_label_Mbps_2)).setTypeface(typeface_Roboto_Regular);		
		((FontFitTextView) pView.findViewById(R.id.tv_summary_label_Mbps_3)).setTypeface(typeface_Roboto_Regular);
		((FontFitTextView) pView.findViewById(R.id.tv_summary_label_Mbps_4)).setTypeface(typeface_Roboto_Regular);
		((FontFitTextView) pView.findViewById(R.id.tv_summary_label_ms_1)).setTypeface(typeface_Roboto_Regular);
		((FontFitTextView) pView.findViewById(R.id.tv_summary_label_ms_2)).setTypeface(typeface_Roboto_Regular);
		((FontFitTextView) pView.findViewById(R.id.tv_summary_label_ms_3)).setTypeface(typeface_Roboto_Regular);
		((FontFitTextView) pView.findViewById(R.id.tv_summary_label_ms_4)).setTypeface(typeface_Roboto_Regular);
		((TextView) pView.findViewById(R.id.tv_summary_download_label)).setTypeface(typeface_Roboto_Regular);
		((TextView) pView.findViewById(R.id.tv_summary_upload_label)).setTypeface(typeface_Roboto_Regular);
		((TextView) pView.findViewById(R.id.tv_summary_latency_label)).setTypeface(typeface_Roboto_Regular);
		((TextView) pView.findViewById(R.id.tv_summary_packet_loss_label)).setTypeface(typeface_Roboto_Regular);
		((TextView) pView.findViewById(R.id.tv_summary_jitter_label)).setTypeface(typeface_Roboto_Regular);
		
		// Chart elements
		layout_ll_chart = (LinearLayout)pView.findViewById(R.id.fragment_summary_ll_chart);
		mChartCaption = (TextView)pView.findViewById(R.id.download_caption);
		
		graphsSetup(pView);
	
		// Hide the chart layout for now!
        mChartCaption.setAlpha(0.0F);
		layout_ll_chart.setAlpha(0.0f);
		layout_ll_chart.setVisibility(View.GONE);
		
		// Get the width of the screen in pixels
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		final int screenWidth = size.x;
		
		// Get the height of the header layout
		layout_ll_header.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{			
			@Override
			public void onGlobalLayout()
			{
				header_section_height = layout_ll_header.getHeight();
				
				layout_ll_header.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
			
			
		});
		
		// Get the position of the header row when it's already drawn		
		layout_ll_summary_section_download.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{			
			@Override
			public void onGlobalLayout()
			{
				headerPositionX = layout_ll_summary_section_download.getLeft();
				headerPositionY = layout_ll_summary_section_download.getTop();
				
				summary_section_height = layout_ll_summary_section_download.getHeight();
				
				layout_ll_summary_section_download.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
		
		// Get the initial position of the upload row when it's already drawn
		layout_ll_summary_section_upload.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{			
			@Override
			public void onGlobalLayout()
			{
				initialPositionUploadX = layout_ll_summary_section_upload.getLeft();
				initialPositionUploadY = layout_ll_summary_section_upload.getTop();
				
				layout_ll_summary_section_upload.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
		
		// Get the initial position of the latency row when it's already drawn
		layout_ll_summary_section_latency.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{			
			@Override
			public void onGlobalLayout()
			{
				initialPositionLatencyX = layout_ll_summary_section_latency.getLeft();
				initialPositionLatencyY = layout_ll_summary_section_latency.getTop();
				
				layout_ll_summary_section_latency.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
		
		// Get the initial position of the packet loss row when it's already drawn
		layout_ll_summary_section_packet_loss.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{			
			@Override
			public void onGlobalLayout()
			{
				initialPositionLossX = layout_ll_summary_section_packet_loss.getLeft();
				initialPositionLossY = layout_ll_summary_section_packet_loss.getTop();
				
				layout_ll_summary_section_packet_loss.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
		
		// Get the initial position of the packet loss row when it's already drawn
				layout_ll_summary_section_jitter.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
				{			
					@Override
					public void onGlobalLayout()
					{
						initialPositionJitterX = layout_ll_summary_section_jitter.getLeft();
						initialPositionJitterY = layout_ll_summary_section_jitter.getTop();
						
						layout_ll_summary_section_jitter.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					}
				});

		// Get the width of the linear layout hosting the charts
		layout_ll_chart.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				layout_ll_chart.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
		
		// Set the click listener for the download layout element (Download average and best result)
		layout_ll_summary_section_download.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(!aList_SummaryResults_No_Empty_For_Download)
				{
					Toast.makeText(getActivity(), getString(R.string.no_results_for_period_and_network), Toast.LENGTH_SHORT).show();
				}
				else
				{
					// Disable click events to avoid side effects
					setLayoutsClickable(false);
					
					// If the rows are hidden (we are showing the graph)
					if (rowsHidden)
					{
						// Hide the chart
						hide_ll_chart_download();
                    	//hide_ll_chart_generic(layout_ll_summary_section_download);
					}
					// If all the rows are showed (we are not showing the graph)
					else
					{
						// Prepare chart for download test type
						prepareChartEnvironment(0, getTimePeriodSelection());
						
						// Disable action bar filters
						menu_item_seletcNetworkType.setVisible(false);
						menu_Item_Select_Time_Period.setVisible(false);
						// Move out of the screen the other rows
						layout_ll_summary_section_upload.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_latency.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_jitter.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_packet_loss.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null).setListener(new AnimatorListenerAdapter()
						{
							// Enable click events on the rows when the animation ends.
							@Override
							public void onAnimationEnd(Animator animation)
							{
								super.onAnimationEnd(animation);

								// Enable click events
								setLayoutsClickable(true);
								// Show the chart
                        		mChartCaption.setAlpha(1.0F);
								layout_ll_chart.animate().setDuration(300).alpha(1.0f);
								layout_ll_chart.setVisibility(View.VISIBLE);
								
								mShowingThisSection = layout_ll_summary_section_download;
							}
							
						});					
					}				
					// Change the value of the variable to the opposite. This is if we were showing the graphs set to not showing them and reverse.
					rowsHidden = !rowsHidden;					
				}
			}

		});
		
		// Set the click listener for the upload layout element (Upload average and best result)
		layout_ll_summary_section_upload.setOnClickListener(new OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if(!aList_SummaryResults_No_Empty_For_Upload)
				{
					Toast.makeText(getActivity(), getString(R.string.no_results_for_period_and_network), Toast.LENGTH_SHORT).show();
				}
				else
				{
					// Disable click events to avoid side effects
					setLayoutsClickable(false);
					
					// If the rows are hidden (we are showing the graph)
					if (rowsHidden)
					{
						// Hide the chart
						hide_ll_chart_upload();
					}
					// If all the rows are showed (we are not showing the graph)
					else
					{
						// Prepare chart for upload test type
						prepareChartEnvironment(1, getTimePeriodSelection());
						
						// Disable action bar filters
						menu_item_seletcNetworkType.setVisible(false);
						menu_Item_Select_Time_Period.setVisible(false);
						// Move out of the screen the other rows
						layout_ll_summary_section_download.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_latency.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_jitter.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_packet_loss.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null).setListener(new AnimatorListenerAdapter()
						{
							// Executed at the end of the animation
							@Override
							public void onAnimationEnd(Animator animation)
							{						
								super.onAnimationEnd(animation);
								
								layout_ll_summary_section_upload.animate().setDuration(300).x(headerPositionX).y(headerPositionY).setInterpolator(new OvershootInterpolator(1.2f));
								
								// Delete the listener of the row to avoid side effects
								layout_ll_summary_section_packet_loss.animate().setListener(null);
								
								// Enable click events
								setLayoutsClickable(true);
								// Show the chart
                        		mChartCaption.setAlpha(1.0F);
								layout_ll_chart.animate().setDuration(300).alpha(1.0f);
								layout_ll_chart.setVisibility(View.VISIBLE);
								
								mShowingThisSection = layout_ll_summary_section_upload;
							}
						});					
					}				
					// Change the value of the variable to the opposite. This is if we were showing the graphs set to not showing them and reverse.
					rowsHidden = !rowsHidden;
				}
			}
		});
		
		// Set the click listener for the latency layout element (Latency average and best result)
		layout_ll_summary_section_latency.setOnClickListener(new OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if(!aList_SummaryResults_No_Empty_For_Latency)
				{
					Toast.makeText(getActivity(), getString(R.string.no_results_for_period_and_network), Toast.LENGTH_SHORT).show();
				}
				else
				{
					// Disable click events to avoid side effects
					setLayoutsClickable(false);
					
					// If the rows are hidden (we are showing the graph)
					if (rowsHidden)
					{
						// Hide the chart
						hide_ll_chart_latency();
					}
					// If all the rows are showed (we are not showing the graph)
					else
					{
						// Prepare chart for latency test type
						prepareChartEnvironment(2, getTimePeriodSelection());
						
						// Disable action bar filters
						menu_item_seletcNetworkType.setVisible(false);
						menu_Item_Select_Time_Period.setVisible(false);
						// Move out of the screen the other rows
						layout_ll_summary_section_download.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_upload.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);					
						layout_ll_summary_section_jitter.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_packet_loss.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null).setListener(new AnimatorListenerAdapter()
						{
							// Executed at the end of the animation
							@Override
							public void onAnimationEnd(Animator animation)
							{						
								super.onAnimationEnd(animation);
								
								layout_ll_summary_section_latency.animate().setDuration(300).x(headerPositionX).y(headerPositionY).setInterpolator(new OvershootInterpolator(1.2f));
								
								layout_ll_summary_section_packet_loss.animate().setListener(null);		// Delete the listener of the row to avoid side effects
								
								// Enable click events
								setLayoutsClickable(true);
								// Show the chart
                        		mChartCaption.setAlpha(1.0F);
								layout_ll_chart.animate().setDuration(300).alpha(1.0f);
								layout_ll_chart.setVisibility(View.VISIBLE);
								
								mShowingThisSection = layout_ll_summary_section_latency;
							}
						});					
					}				
					// Change the value of the variable to the opposite. This is if we were showing the graphs set to not showing them and reverse.
					rowsHidden = !rowsHidden;					
				}
			}
		});
		
		// Set the click listener for the packet loss layout element (Packet loss average and best result)
		layout_ll_summary_section_packet_loss.setOnClickListener(new OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if(!aList_SummaryResults_No_Empty_For_Packet_Loss)
				{
					Toast.makeText(getActivity(), getString(R.string.no_results_for_period_and_network), Toast.LENGTH_SHORT).show();
				}
				else
				{
					// Disable click events to avoid side effects
					setLayoutsClickable(false);
					
					// If the rows are hidden (we are showing the graph)
					if (rowsHidden)
					{
						// Hide the chart
						hide_ll_chart_packetloss();
					}
					// If all the rows are showed (we are not showing the graph)
					else
					{
						// Prepare chart for packet loss test type
						prepareChartEnvironment(3, getTimePeriodSelection());
						
						// Disable action bar filters
						menu_item_seletcNetworkType.setVisible(false);
						menu_Item_Select_Time_Period.setVisible(false);
						// Move out of the screen the other rows
						layout_ll_summary_section_download.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_upload.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_jitter.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_latency.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null).setListener(new AnimatorListenerAdapter()
						{
							// Executed at the end of the animation
							@Override
							public void onAnimationEnd(Animator animation)
							{
								layout_ll_summary_section_packet_loss.animate().setDuration(300).x(headerPositionX).y(headerPositionY).setInterpolator(new OvershootInterpolator(1.2f));
								
								// Delete the listener of the row to avoid side effects
								layout_ll_summary_section_latency.animate().setListener(null);
								
								// Enable click events
								setLayoutsClickable(true);
								// Show the chart
                        		mChartCaption.setAlpha(1.0F);
								layout_ll_chart.animate().setDuration(300).alpha(1.0f);
								layout_ll_chart.setVisibility(View.VISIBLE);
								
								mShowingThisSection = layout_ll_summary_section_packet_loss;
							};						
						});									
					}			
					// Change the value of the variable to the opposite. This is if we were showing the graphs set to not showing them and reverse.
					rowsHidden = !rowsHidden;					
				}
			}
		});
		
		// Set the click listener for the jitter layout element (Jitter average and best result)
		layout_ll_summary_section_jitter.setOnClickListener(new OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if(!aList_SummaryResults_No_Empty_For_Jitter)
				{
					Toast.makeText(getActivity(), getString(R.string.no_results_for_period_and_network), Toast.LENGTH_SHORT).show();
				}
				else
				{
					// Disable click events to avoid side effects
					setLayoutsClickable(false);
					
					// If the rows are hidden (we are showing the graph)
					if (rowsHidden)
					{
						// Hide the chart
						hide_ll_chart_jitter();

					}
					// If all the rows are showed (we are not showing the graph)
					else
					{
						// Prepare chart for upload test type
						prepareChartEnvironment(4, getTimePeriodSelection());
						
						// Disable action bar filters
						menu_item_seletcNetworkType.setVisible(false);
						menu_Item_Select_Time_Period.setVisible(false);
						// Move out of the screen the other rows
						layout_ll_summary_section_download.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_upload.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_latency.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null);
						layout_ll_summary_section_packet_loss.animate().x(-screenWidth).setDuration(300).alpha(0.0f).setInterpolator(null).setListener(new AnimatorListenerAdapter()
						{
							// Executed at the end of the animation
							@Override
							public void onAnimationEnd(Animator animation)
							{						
								super.onAnimationEnd(animation);
								
								layout_ll_summary_section_jitter.animate().setDuration(300).x(headerPositionX).y(headerPositionY).setInterpolator(new OvershootInterpolator(1.2f));
								
								// Delete the listener of the row to avoid side effects
								layout_ll_summary_section_packet_loss.animate().setListener(null);
								
								// Enable click events
								setLayoutsClickable(true);
								// Show the chart
                        		mChartCaption.setAlpha(1.0F);
								layout_ll_chart.animate().setDuration(300).alpha(1.0f);
								layout_ll_chart.setVisibility(View.VISIBLE);
								
								mShowingThisSection = layout_ll_summary_section_jitter;
							}
						});					
					}				
					// Change the value of the variable to the opposite. This is if we were showing the graphs set to not showing them and reverse.
					rowsHidden = !rowsHidden;
				}
			}
		});
	}
	
	private void graphsSetup(View pView) {

		//Trace.beginSection("graphsSetup");

		graphContainer =  (WebView)pView.findViewById(R.id.download_graph);
		// http://stackoverflow.com/questions/2527899/disable-scrolling-in-webview
		// disable scroll on touch
		graphContainer.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return (event.getAction() == MotionEvent.ACTION_MOVE);
			}
		});

		Context context = SKApplication.getAppInstance().getApplicationContext();
		
		graphHandlerDownload = new SKGraphForResults(context, graphContainer, mChartCaption, "download");
		//graphContainer.setBackgroundColor(Color.RED); // TODO - remove me, set transparent?
		//graphContainer.setBackgroundColor(Color.TRANSPARENT); // TODO - remove me, set transparent?
	}
	
	private	SKGraphForResults graphHandlerDownload;
	
	private void setGraphDataForColumnIdAndHideIfNoResultsFound(int PColumnId) {

		// Switch depending on the network type currently selected - this affects the underlying query.
		switch (getNetworkTypeSelection())
		{
		case 0: // Network type: All
			SKApplication.setNetworkTypeResults(eNetworkTypeResults.eNetworkTypeResults_Any);
			break;
		case 1:				// Network type: WiFi
			SKApplication.setNetworkTypeResults(eNetworkTypeResults.eNetworkTypeResults_WiFi);
			break;
		case 2: // Network type: Mobile
			SKApplication.setNetworkTypeResults(eNetworkTypeResults.eNetworkTypeResults_Mobile);
			break;
		default:
			break;
		}

		JSONObject data = null;
		//try {
		data = fetchGraphDataForColumnId(PColumnId);
		//} catch (JSONException e1) {
		//}

		// When using achartengine, the background CANNOT be TRANSPARENT.
		// Otherwise, the fill area *below the y axis* will be filled with the fill colour - which is ugly!
		
		graphHandlerDownload.updateGraphWithTheseResults(data, mDateRange, sGetGraphColourBackground(), sGetGraphColourTopAreaFill());
		//graphHandlerDownload.updateGraphWithTheseResults(data, mDateRange);
	}
	
	public static int sGetSamKnowsBlue() {
		// "#009fe3"
		return Color.rgb( 0, 159, 227);
	}	
	
	public static int sGetGraphColourBackground() {
		return SKApplication.getAppInstance().getApplicationContext().getResources().getColor(R.color.GraphColourBackground);
	}
	
	public static int sGetGraphColourAverageFill() {
		return SKApplication.getAppInstance().getApplicationContext().getResources().getColor(R.color.GraphColourTopAreaFill);
	}

	public static int sGetGraphColourTopAreaFill() {
		return SKApplication.getAppInstance().getApplicationContext().getResources().getColor(R.color.GraphColourTopAreaFill);
	}

	public static int sGetGraphColourBottomAreaFill() {
		return SKApplication.getAppInstance().getApplicationContext().getResources().getColor(R.color.GraphColourBottomAreaFill);
	}

	private JSONObject fetchGraphDataForColumnId(int PColumnId) {
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
	
	private DATERANGE_1w1m3m1y mDateRange = DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_WEEK;
	
	private DATERANGE_1w1m3m1y convertDataPeriodToDateRange(int pDataPeriod)
	{
		switch (pDataPeriod)
		{
			// 1 Day
			case 0:
				return DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_DAY;
			// 1 Week
			case 1:
				return DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_WEEK;
			// 1 Month
			case 2:
				return DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_MONTH;
			// 3 Months
			case 3:
				return DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_THREE_MONTHS;
			// 1 Year
			case 4:
				return DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_YEAR;
			// Default: 1 Week
			default:
				SKLogger.sAssert(getClass(),  false);
				return DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_WEEK;
		}
	}

	
	private void lookBackwardInTime(Calendar fromCal) {
		
		// Convert the "new app" time period, into a usable enumerated value.
		int dataPeriod = getTimePeriodSelection();
    	mDateRange = convertDataPeriodToDateRange(dataPeriod);
    			
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
    	default:
			SKLogger.sAssert(getClass(),  false);
    		fromCal.add(Calendar.WEEK_OF_YEAR, -1);
    		break;
		}
	}
		
	/**
	 * Save the state of the network type filter in shared preferences
	 * 
	 * @param pNetworkType is the network type value stored in shared preferences for the Summary fragment
	 */
	private void saveNetworkTypeSelection(int pNetworkType)
	{
		// Get shared preferences editor
		SharedPreferences.Editor editor = getActivity().getSharedPreferences(getString(R.string.sharedPreferencesIdentifier),Context.MODE_PRIVATE).edit();

		editor.putInt("networkTypeSummary", pNetworkType);	// Save the state of network type filter	
		editor.commit();	// Commit changes
	}
		
	/**
	 * Get the state of the network type filter from shared preferences. 0 is All, 1 is Wifi and 2 is mobile.
	 * 
	 * @return networkTypeSummary is the value stored in shared preferences for the network type in the Summary fragment
	 */
	private int getNetworkTypeSelection()
	{
		// Get the shared preferences
		SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.sharedPreferencesIdentifier),Context.MODE_PRIVATE);
    	// Return the state of network type selection		
    	return prefs.getInt("networkTypeSummary", 0);
	}	
	
	/**
	 * Save the state of the time period filter in shared preferences
	 * 
	 * @param pTimePeriod is the value stored in shared preferences for the time period in the Summary fragment
	 */
	private void saveTimePeriodSelection(int pTimePeriod)
	{
		// Get the shared preferences editor		 
		SharedPreferences.Editor editor = getActivity().getSharedPreferences(getString(R.string.sharedPreferencesIdentifier),Context.MODE_PRIVATE).edit();
		
		editor.putInt("timePeriodSummary", pTimePeriod);		// Save the state of the time period filter	
		editor.commit();		// Commit changes
	}
		
	/**
	 * Get the state of the network type filter from shared preferences
	 * 
	 * @return timePeriodSummary is the value stored in shared preferences for the time period in the Summary fragment
	 */
	private int getTimePeriodSelection()
	{
		// Get the shared preferences
		SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.sharedPreferencesIdentifier),Context.MODE_PRIVATE);
    	// Return the state of the time period selection. The default value is 4 (1 week)		
    	return prefs.getInt("timePeriodSummary", 4);
	}
	
	/**
	 * Calculate the initial date of the period of time in milliseconds
	 * 
	 * @param pTimePeriodSelection time to subtract to the current time
	 * @return the time in milliseconds equals to the current time minus the time passed as parameter
	 */
	private long calculateTimePeriodStart(int pTimePeriodSelection)
	{		
		switch (pTimePeriodSelection)
		{
			// 1 day ago
			case 0:
				return System.currentTimeMillis()/1000 - 3600 * 24;
	    	// 1 week ago
			case 1:
				return System.currentTimeMillis()/1000 - 3600 * 24 * 7;
	    	// 4 weeks ago
			case 2:
				return System.currentTimeMillis()/1000 - 3600 * 24 * 31;
	    	// 12 weeks ago
			case 3:
				return System.currentTimeMillis()/1000 - 3600 * 24 * 31 * 3;
	    	// 1 year ago
			case 4:
				return System.currentTimeMillis()/1000 - 3600 * 24 * 365;
			// Default case: 1 week ago
			default:			
				return System.currentTimeMillis()/1000 - 3600 * 24 * 7;
		}		
	}
	
	/**
	 * Set the linear layouts clickable property to true or false. This is to disable click events while transitions are happening in the screen.
	 * 
	 * @param pClickable that is true or false
	 */
	private void setLayoutsClickable(boolean pClickable)
	{
		layout_ll_summary_section_download.setClickable(pClickable);
		layout_ll_summary_section_upload.setClickable(pClickable);
		layout_ll_summary_section_latency.setClickable(pClickable);
		layout_ll_summary_section_packet_loss.setClickable(pClickable);
		layout_ll_summary_section_jitter.setClickable(pClickable);
	}
	
	/**
	 * Prepare the chart for the selected test type
	 * 
	 * @param pChartType
	 * @param pDataPeriod
	 */
	private void prepareChartEnvironment(int pChartType, int pDataPeriod)
	{
		// Prepare data for the download chart
		int chartType = StorageTestResult.DOWNLOAD_TEST_ID;
		switch (pChartType)
		{
		case 0:
			// Case download
    		chartType = StorageTestResult.DOWNLOAD_TEST_ID;
    		break;
		case 1:
			// Case upload
    		chartType = StorageTestResult.UPLOAD_TEST_ID;
    		break;
		case 2:
			// Case latency
    		chartType = StorageTestResult.LATENCY_TEST_ID;
    		break;
		case 3:
			// Case packet loss
    		chartType = StorageTestResult.PACKETLOSS_TEST_ID;
    		break;
		case 4:
			// Case jitter
    		chartType = StorageTestResult.JITTER_TEST_ID;
    		break;
		default:
			SKLogger.sAssert(getClass(),  false);

		}
		
		setGraphDataForColumnIdAndHideIfNoResultsFound(chartType);
	}
	
	/**
	 * Set the possibility of display the charts true or false
	 * 
	 * @param pAvailable that is true or false
	 */
	private void chartsAvailable(boolean pAvailable)
	{
		layout_ll_summary_section_download.setClickable(pAvailable);
		layout_ll_summary_section_upload.setClickable(pAvailable);
		layout_ll_summary_section_latency.setClickable(pAvailable);
		layout_ll_summary_section_packet_loss.setClickable(pAvailable);
		layout_ll_summary_section_jitter.setClickable(pAvailable);
	}
	
	// *** MENUS *** //
	// Initialise the contents of the Activity's standard options menu.
    // You should place your menu items in to menu. For this method to be called, you must have first called setHasOptionsMenu(boolean).
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    	inflater.inflate(R.menu.menu_fragment_summary, menu);
    	super.onCreateOptionsMenu(menu, inflater);
    	
    	// Identify the action bar menu filters
    	menu_item_seletcNetworkType = menu.findItem(R.id.menu_item_fragment_summary_select_network);
    	menu_Item_Select_Time_Period = menu.findItem(R.id.menu_item_fragment_summary_select_time_period);
    	// Identify and initialise the refresh spinner in the action bar
    	menu_Item_Refresh_Spinner = menu.findItem(R.id.menu_item_fragment_summary_refreshSpinner);
    	menu_Item_Refresh_Spinner.setVisible(asyncTask_RetrieveData_Running || asyncTask_PrepareChart_Running);
    	menu_Item_Refresh_Spinner.setActionView(R.layout.actionbar_indeterminate_progress);

    	// If the rows are hidden, hide the menu filters
    	if (rowsHidden)
    	{
    		menu_item_seletcNetworkType.setVisible(false);
    		menu_Item_Select_Time_Period.setVisible(false);
		}
    }

    // This hook is called whenever an item in your options menu is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	int itemId = item.getItemId();

    	if (itemId == R.id.menu_item_fragment_summary_select_network) {
    		// Case select network
    		Intent intent_select_network = new Intent(getActivity(),ActivitySelectNetworkType.class);	// Intent launching the activity to select the network type
    		intent_select_network.putExtra("currentFragment", 2);	// Set the current fragment. This will determine the background of the activity
    		startActivityForResult(intent_select_network, 0);		// Activity is started with requestCode 0

    		return true;
    	}

    	if (itemId == R.id.menu_item_fragment_summary_select_time_period) {
    		// Case select time period
    		Intent intent_select_time_period = new Intent(getActivity(),ActivitySelectTimePeriod.class);	// Intent launching the activity to select the time period
    		intent_select_time_period.putExtra("currentFragment", 2);		// Set the current fragment. This will determine the background of the activity
    		startActivityForResult(intent_select_time_period, 1); 			// Activity is started with requestCode 1

    		return true;
    	}

    	// Default case
    	return true;
    }
}