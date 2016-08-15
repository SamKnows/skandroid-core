package com.samknows.measurement.statemachine.state;

import com.samknows.libcore.R;


public enum StateEnum {
	NONE(R.string.state_none), 
	EXECUTE_QUEUE(R.string.state_execute_queue),
	SUBMIT_RESULTS_ANONYMOUS(R.string.state_submit_results),
	SHUTDOWN(R.string.state_shutdown);
	
	public final int sId;
	StateEnum(int stringId){
		sId = stringId;
	}
}
