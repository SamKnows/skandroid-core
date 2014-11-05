package com.samknows.ui2.activity;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.MainService;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SKApplication;
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

            String crashReportingId = SKApplication.getAppInstance().getCrashManagerId();

			CrashManager.register(this, crashReportingId, new CrashManagerListener()
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

		final Activity ctx = this;
		
		LoginHelper.openMainScreenWithNoTransitionAnimation(ctx, FragmentActivityMain.class);
	}
}
