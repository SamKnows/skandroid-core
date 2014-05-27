package com.samknows.measurement.statemachine.state;

import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.MainService;
import com.samknows.measurement.Storage;
import com.samknows.measurement.statemachine.StateResponseCode;

public class NoneState extends BaseState{
	
	public NoneState(MainService ctx) {
		super(ctx);
	}

	@Override
	public StateResponseCode executeState() {
		Storage storage = CachingStorage.getInstance();
		//dropping the config file because we either are here after login/first launch
		//or recovering after an error
		storage.dropScheduleConfig();
		return StateResponseCode.OK;
	}

}
