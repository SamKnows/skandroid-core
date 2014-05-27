package com.samknows.measurement.statemachine.state;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.MainService;
import com.samknows.measurement.environment.PhoneIdentityData;
import com.samknows.measurement.environment.PhoneIdentityDataCollector;
import com.samknows.measurement.net.DCSInitAction;
import com.samknows.measurement.statemachine.StateResponseCode;

public class InitialiseState extends BaseState{

	public InitialiseState(MainService c) {
		super(c);
	}

	@Override
	public StateResponseCode executeState() {
		PhoneIdentityData data = new PhoneIdentityDataCollector(ctx).collect();
		DCSInitAction action = new DCSInitAction(data);
		action.execute();
		if (action.isSuccess()) {
			SKLogger.d(this, "retrived server base url: " + action.serverBaseUrl);
			SK2AppSettings.getInstance().saveServerBaseUrl(action.serverBaseUrl);
			SKLogger.d(this, "save server base url: " + action.serverBaseUrl);

			return StateResponseCode.OK;
		}
		return StateResponseCode.FAIL;
	}

}
