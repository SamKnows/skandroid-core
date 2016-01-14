package com.samknows.measurement.statemachine.state;

import android.content.Context;
import android.util.Log;

import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.environment.Reachability;
import com.samknows.measurement.net.SubmitTestResultsAnonymousAction;
import com.samknows.measurement.statemachine.StateResponseCode;

public class SubmitResultsAnonymousState extends BaseState{
  static final String TAG = "SubmitResultsAnonymousS";

	public SubmitResultsAnonymousState(Context c){
		super(c);
	}
	
	@Override
	public StateResponseCode executeState(){
		if ( (SK2AppSettings.getInstance().isDataCapReached() == true) &&
		  	 (SKApplication.getAppInstance().getIsDataCapEnabled() == true) &&
		  	 (Reachability.sGetIsNetworkWiFi() == false)
			 )
    {
			Log.d(TAG, "Results have not been submitted because the data cap is reached");
		} else {
			new SubmitTestResultsAnonymousAction(ctx).execute();
		}
		return StateResponseCode.OK;
	}

}
