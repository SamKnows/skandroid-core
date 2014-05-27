package com.samknows.measurement.statemachine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TransitionAnonymous extends Transition {
	private static final String TYPE = TransitionAnonymous.class.getName();
	private static final Map<State, String[]> transitionFunction;
	static {
		Map<State, String[]> tmp= new HashMap<State, String[]>();
		tmp.put(State.NONE, new String[] {"OK:INITIALISE_ANONYMOUS"});
		tmp.put(State.INITIALISE_ANONYMOUS, new String[] {"OK:DOWNLOAD_CONFIG_ANONYMOUS"});
		tmp.put(State.DOWNLOAD_CONFIG_ANONYMOUS, new String[] {"NOT_OK:RUN_INIT_TESTS","OK:EXECUTE_QUEUE"});
		tmp.put(State.RUN_INIT_TESTS, new String[] {"OK:EXECUTE_QUEUE"});
		tmp.put(State.EXECUTE_QUEUE, new String[] {"OK:SUBMIT_RESULTS_ANONYMOUS"});
		tmp.put(State.SUBMIT_RESULTS_ANONYMOUS, new String[] {"OK:SHUTDOWN"});
		tmp.put(State.SHUTDOWN, new String[] {"OK:DOWNLOAD_CONFIG_ANONYMOUS"});
		transitionFunction = Collections.unmodifiableMap(tmp);
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	protected Map<State, String[]> getTransition() {
		return transitionFunction;
	}

}
