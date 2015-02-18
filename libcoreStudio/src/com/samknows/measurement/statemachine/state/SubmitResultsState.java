package com.samknows.measurement.statemachine.state;

import com.samknows.measurement.MainService;
import com.samknows.measurement.net.SubmitTestResultsAction;
import com.samknows.measurement.statemachine.StateResponseCode;

public class SubmitResultsState extends BaseState{

	public SubmitResultsState(MainService c) {
		super(c);
	}

	@Override
	public StateResponseCode executeState() {
		new SubmitTestResultsAction(ctx).execute();
		return StateResponseCode.OK;
	}

}
