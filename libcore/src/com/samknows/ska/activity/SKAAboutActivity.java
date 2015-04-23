package com.samknows.ska.activity;

import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View;
import android.widget.TextView;

import com.samknows.libcore.R;
import com.samknows.libcore.SKTypeface;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.activity.BaseLogoutActivity;
import com.samknows.libcore.SKLogger;

public class SKAAboutActivity extends BaseLogoutActivity {

	@Override
	public void onStart(){
		super.onStart();
		//getActionBar().setDisplayHomeAsUpEnabled(true);
		this.setTitle(SKApplication.getAppInstance().getAboutScreenTitle());
		setContentView(R.layout.ska_about_activity);
		String versionName="";
		try {
			versionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			SKLogger.sAssert(getClass(), false);
		}
		
		TextView tv=(TextView) findViewById(R.id.version);
		tv.setText(getString(R.string.version)+ " " + versionName);

		// Hide some elements!

		
		if (SKApplication.getAppInstance().hideJitterLatencyAndPacketLoss()) {
			findViewById(R.id.TextViewLatency1).setVisibility(View.GONE);
			findViewById(R.id.TextViewLatency2).setVisibility(View.GONE);
			findViewById(R.id.TextViewPacketLoss1).setVisibility(View.GONE);
			findViewById(R.id.TextViewPacketLoss2).setVisibility(View.GONE);
			findViewById(R.id.TextViewJitter1).setVisibility(View.GONE);
			findViewById(R.id.TextViewJitter2).setVisibility(View.GONE);
		}
		
		if (SKApplication.getAppInstance().hideJitter()) {
			findViewById(R.id.TextViewJitter1).setVisibility(View.GONE);
			findViewById(R.id.TextViewJitter2).setVisibility(View.GONE);
		}
		
		SKTypeface.initializeFonts();
		SKTypeface.overrideFonts(this, findViewById(android.R.id.content));
	}
}
