package com.samknows.measurement;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.statemachine.State;
import com.samknows.measurement.util.TimeUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PowerReceiver extends BroadcastReceiver{

	@Override
	//Simply checks if the next scheduled event is in past
	//drop the schedule config and start the mainService
	//
	public void onReceive(Context context, Intent intent) {
		SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
		Long nextEvent = appSettings.getNextRunTime();
		SKLogger.d(this, "next event due to :"+TimeUtils.logString(nextEvent));
		if(nextEvent == SKConstants.NO_NEXT_RUN_TIME){
			SKLogger.d(this, "App is not activated yet");
			return;
		}
		if(nextEvent <= System.currentTimeMillis() && ! MainService.isExecuting()){
			SKLogger.e(this,"Next event is in the past, starting the main server again now.");
			appSettings.saveState(State.NONE);
			MainService.poke(context);
		}
		
		
	}

}
