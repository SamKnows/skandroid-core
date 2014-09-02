package com.samknows.ui2.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samknows.libui2.R;
import com.samknows.libcore.SKLogger;
import com.samknows.measurement.MainService;
import com.samknows.measurement.activity.BaseLogoutActivity;
import com.samknows.measurement.activity.components.UIUpdate;
import com.samknows.measurement.util.LoginHelper;

/**
 * This activity is in charge of:
 * The activatin process
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */

public class ActivityActivation extends BaseLogoutActivity
{
	// *** VARIABLES *** //
	public Handler handler;
	private boolean messageComplete = false;

	// *** ACTIVITY LIFECYCLE *** //
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_activation);
		
		((TextView)findViewById(R.id.activity_activation_tv_label_main)).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/roboto_light.ttf"));

		handler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				JSONObject message_json;
				
				if (msg.obj == null)
				{
					return;
				}
				
				message_json = (JSONObject) msg.obj;

				try
				{
					String type = message_json.getString(UIUpdate.JSON_TYPE);

					if (type == UIUpdate.JSON_MAINPROGRESS)
					{
						((ProgressBar)findViewById(R.id.activity_activation_progress_bar)).setProgress(Integer.parseInt(message_json.getString(UIUpdate.JSON_VALUE)));						
					}
					/*else if (type == UIUpdate.JSON_ACTIVATED)
					{
						ProgressBar pb = (ProgressBar) findViewById(R.id.activating_progress);
						pb.setVisibility(View.GONE);
						ImageView iv = (ImageView) findViewById(R.id.activating_complete);
						iv.setVisibility(View.VISIBLE);
					}
					else if (type == UIUpdate.JSON_DOWNLOADED)
					{
						ProgressBar pb = (ProgressBar) findViewById(R.id.download_progress);
						pb.setVisibility(View.GONE);
						ImageView iv = (ImageView) findViewById(R.id.download_complete);
						iv.setVisibility(View.VISIBLE);
					}
					else if (type == UIUpdate.JSON_INITTESTS)
					{

					}*/
					else if (type == UIUpdate.JSON_COMPLETED)
					{
						((ProgressBar)findViewById(R.id.activity_activation_progress_bar)).setProgress(100);
						
						// We receive several messages of this type						
						if(!messageComplete)
						{
							LoginHelper.openMainScreen(ActivityActivation.this, FragmentActivityMain.class);
							messageComplete = true;
						}						
					}
				}
				catch (JSONException e)
				{
					SKLogger.e(ActivityActivation.class, "Error in parsing JSONObject: " + e.getMessage());
				}
			}
		};
		
		if (MainService.registerActivationHandler(this, handler))
		{
			SKLogger.d(this, "activation handler registered");
		}
		else
		{
			SKLogger.d(this, "MainService is not executing");
			LoginHelper.openMainScreen(ActivityActivation.this, FragmentActivityMain.class);
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
		MainService.unregisterActivationHandler();
	}
}
