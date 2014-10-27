package com.samknows.ui2.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samknows.libui2.R;

/**
 * This activity is responsible for the select network activity. It's started from different places in the application with startActivityForResult.
 * The returned values are 0,1 or 2 for All, WiFi and Mobile.
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */


public class ActivitySelectNetworkType extends Activity
{
	// *** VARIABLES *** //
	// UI elements	
	private RelativeLayout layout_rl_main;									// Main layout
	private LinearLayout layout_ll_network_wifi, layout_ll_network_mobile;	// Buttons representing the "WiFi" and "Mobile" options
	private TextView tv_network_wifi, tv_network_mobile;					// Text views representing the "WiFi" and "Mobile" options
	private Button button_network_all;										// Button representing the "All" option
	private Typeface typeface_Roboto_Light;										// Type face
	
	
	// *** ACTIVITY LIFECYCLE *** //
	// Called when the activity is starting.
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_network_type);
		
		// Bind resources and set up them
		setUpResources();
	}
	
	// *** CUSTOM METHODS *** //
	/**
	 * Create, bind and set up the resources
	 */
	private void setUpResources()
	{
		layout_rl_main = (RelativeLayout)findViewById(R.id.activity_select_network_type_relativelayout_main);
		layout_ll_network_wifi = (LinearLayout)findViewById(R.id.ll_select_network_wifi);
		layout_ll_network_mobile = (LinearLayout)findViewById(R.id.ll_select_network_mobile);
		button_network_all = (Button)findViewById(R.id.button_select_network_all);
		tv_network_wifi = (TextView)findViewById(R.id.tv_select_network_wifi);
		tv_network_mobile = (TextView)findViewById(R.id.tv_select_network_mobile);
		
		// Initialise fonts
		typeface_Roboto_Light = Typeface.createFromAsset(getAssets(), "fonts/roboto_light.ttf");
		
		// Set up fonts
		tv_network_wifi.setTypeface(typeface_Roboto_Light);
		tv_network_mobile.setTypeface(typeface_Roboto_Light);
		button_network_all.setTypeface(typeface_Roboto_Light);
		
		
		// Switch taking actions depending on the fragment this activity was called from
		switch (getIntent().getIntExtra("currentFragment", 0))
		{
			// Case we this activity was called from Archived Results fragment
			case 0:
				// Set the background of the activity layout
				layout_rl_main.setBackgroundResource(R.drawable.background_gradient_nonmain_with_border);
				
				// Recover the last network type selected
				switch (getSharedPreferences(getString(R.string.sharedPreferencesIdentifier),Context.MODE_PRIVATE).getInt("networkTypeArchivedTests", 0))
				{
					// Case "All"
					case 0:
						button_network_all.setSelected(true);
						break;
					// Case "WiFi"
					case 1:
						layout_ll_network_wifi.setSelected(true);
						break;
					// Case "Mobile"
					case 2:
						layout_ll_network_mobile.setSelected(true);
						break;
					// Case default
					default:
						break;
				}
				break;
			// Case this activity was called from Summary fragment
			case 2:
				// Set the background of the activity layout
				layout_rl_main.setBackgroundResource(R.drawable.background_gradient_main_with_border);
				
				// Recover the last network type selected
				switch (getSharedPreferences(getString(R.string.sharedPreferencesIdentifier),Context.MODE_PRIVATE).getInt("networkTypeSummary", 0))
				{
					// Case "All"
					case 0:
						button_network_all.setSelected(true);
						break;
					// Case "WiFi"
					case 1:
						layout_ll_network_wifi.setSelected(true);
						break;
					// Case "Mobile"
					case 2:
						layout_ll_network_mobile.setSelected(true);
						break;
					// Case default
					default:
						break;
				}
				break;
	
			// Case default
			default:
				break;
		}
		
		// Button listener when "All" button is pressed
		button_network_all.setOnClickListener(new OnClickListener()
		{				
			@Override
			public void onClick(View v)
			{
				Intent intent_network_type_result = new Intent();			// Create the intent to return back
				intent_network_type_result.putExtra("networkType", 0);		// Set as extra the network type selected
				
				setResult(0, intent_network_type_result);					// Set the result code and the data to propagate back to the originating fragment
				
				finish();													// Finish this activity
			}
		});
		
		// Button listener when WiFi button is pressed
		layout_ll_network_wifi.setOnClickListener(new OnClickListener()
		{				
			@Override
			public void onClick(View v)
			{
				Intent intent_network_type_result = new Intent();			// Create the intent to return back
				intent_network_type_result.putExtra("networkType", 1);		// Set as extra the network type selected
				
				setResult(0, intent_network_type_result);					// Set the result code and the data to propagate back to the originating fragment
				
				finish();													// Finish this activity
			}
		});
		
		// Button listener when Mobile button is pressed
		layout_ll_network_mobile.setOnClickListener(new OnClickListener()
		{				
			@Override
			public void onClick(View v)
			{
				Intent intent_network_type_result = new Intent();			// Create the intent to return back
				intent_network_type_result.putExtra("networkType", 2);		// Set as extra the network type selected					
				
				setResult(0, intent_network_type_result);					// Set the result code and the data to propagate back to the originating fragment
				
				finish();													// Finish this activity
			}
		});
		
	}
}
