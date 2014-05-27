package com.samknows.measurement.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Window;

import com.samknows.libcore.SKConstants;

public class BaseLogoutActivity extends SamKnowsBaseActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
