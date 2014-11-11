package com.samknows.measurement.activity;

import android.os.Bundle;
import android.view.Window;

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
