package com.samknows.measurement.statemachine.state;

import android.content.Context;

import com.samknows.measurement.statemachine.StateResponseCode;

public class BaseState {
	protected final Context ctx;
	
	public BaseState(Context c) {
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
