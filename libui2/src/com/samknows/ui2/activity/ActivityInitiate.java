package com.samknows.ui2.activity;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.samknows.measurement.MainService;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.activity.BaseLogoutActivity;
import com.samknows.measurement.util.LoginHelper;
import com.samknows.measurement.util.OtherUtils;

/**
 * This activity is in charge of:
 * Start the application deciding between activation or not
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */

public class ActivityInitiate extends BaseLogoutActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.d(this.getClass().toString(), "*** onCreate ***");

		if (OtherUtils.isDebuggable(this))
		{
			Log.d(this.getClass().toString(), "OtherUtils.isDebuggable(), not using crash reporting");
		}
		else
		{
			Log.d(this.getClass().toString(), "This app is NOT debuggable, so setting-up crash reporting!");
			CrashManager.register(this, "3d13669fc03f8ace6693934bc9922c65", new CrashManagerListener()
			{
				@Override
				public boolean shouldAutoUploadCrashes()
				{
					return true;
				}
				
				@Override
				public void onConfirmedCrashesFound()
				{
					Log.d(this.getClass().toString(), "*** CrashManagerListener onConfirmedCrashesFound ***");
				}
				
				@Override
				public void onCrashesNotSent()
				{
					Log.d(this.getClass().toString(), "*** CrashManagerListener onCrashesNotSent ***");
				}
				
				@Override
				public void onCrashesSent()
				{
					Log.d(this.getClass().toString(), "*** CrashManagerListener onCrashesSent ***");
				}
				
				@Override
				public void onNewCrashesFound()
				{
					Log.d(this.getClass().toString(), "*** CrashManagerListener onNewCrashesFound ***");
				}
			});
		}

		final SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
		final Activity ctx = this;
		
		if (appSettings.isServiceActivated())
		{
			LoginHelper.openMainScreenWithNoTransitionAnimation(ctx, FragmentActivityMain.class);
		}
		else
		{
			MainService.poke(ctx);
			startActivity(new Intent(ctx, ActivityActivation.class));
		}
	}
}
