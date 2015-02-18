package com.samknows.measurement.statemachine.state;

import com.samknows.measurement.MainService;
import com.samknows.measurement.statemachine.StateResponseCode;

public class BaseState {
	protected MainService ctx;
	
	public BaseState(MainService c) {
		super();
		this.ctx = c;
	}
	
	public StateResponseCode executeState() throws Exception {
		return StateResponseCode.FAIL;
	}
	
	public long getAccumulatedTestBytes() {
		return 0L;
	}
}
