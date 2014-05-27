package com.samknows.measurement;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.environment.NetUsageCollector;
import com.samknows.measurement.statemachine.State;
import com.samknows.measurement.util.OtherUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		SK2AppSettings a = SK2AppSettings.getSK2AppSettingsInstance();
		long nextRunTime = a.getNextRunTime();
		//if the nextRunTime is in the past or too close restart the state machne
		if(nextRunTime < System.currentTimeMillis() - a.getTestStartWindow()){
			a.saveState(State.NONE);
			SKLogger.d(this, "State saved, None");
			MainService.poke(context);
		}else{
			//in this case we just set the alarm
			OtherUtils.reschedule(context, nextRunTime - System.currentTimeMillis());
		}
		//if the traffic data has to be collected by calling this method we 
		// refresh the object in cache
		if(a.collect_traffic_data){
			new NetUsageCollector(context).collect();
		}
		
		
	}
	
	

}
