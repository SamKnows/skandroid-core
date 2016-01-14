package com.samknows.measurement.statemachine;

import com.samknows.libcore.R;


public enum State {
	NONE(R.string.state_none), 
	EXECUTE_QUEUE(R.string.state_execute_queue),
	SUBMIT_RESULTS_ANONYMOUS(R.string.state_submit_results),
	SHUTDOWN(R.string.state_shutdown);
	
	public int sId;
	State(int stringId){
		sId = stringId;
	}
}
