package com.samknows.measurement.statemachine.state;

import com.samknows.measurement.MainService;
import com.samknows.measurement.statemachine.StateResponseCode;

public class InitialiseAnonymousState extends BaseState {

	public InitialiseAnonymousState(MainService c){
		super(c);
	}

	@Override
	public StateResponseCode executeState() {
		return StateResponseCode.OK;
	}

}
