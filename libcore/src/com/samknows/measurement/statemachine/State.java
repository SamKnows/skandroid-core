package com.samknows.measurement.statemachine;

import com.samknows.libcore.R;


public enum State {
	NONE(R.string.state_none), 
	INITIALISE(R.string.state_initialise), 
	INITIALISE_ANONYMOUS(R.string.state_initialise),
	ACTIVATE(R.string.state_activate),
	ASSOCIATE(R.string.state_associate),
	CHECK_CONFIG_VERSION(R.string.state_check_config),
	DOWNLOAD_CONFIG_ANONYMOUS(R.string.state_download_config),
	DOWNLOAD_CONFIG(R.string.state_download_config),
	RUN_INIT_TESTS(R.string.state_run_init),
	EXECUTE_QUEUE(R.string.state_execute_queue),
	SUBMIT_RESULTS(R.string.state_submit_results),
	SUBMIT_RESULTS_ANONYMOUS(R.string.state_submit_results),
	SHUTDOWN(R.string.state_shutdown);
	
	public int sId;
	private State(int stringId){
		sId = stringId;
	}
}
