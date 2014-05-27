package com.samknows.measurement.statemachine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TransitionUser extends Transition {
	private static final String TYPE = TransitionUser.class.getName();
	private static final Map<State, String[]> transitionFunction;
	static {
		Map<State, String[]> tmp= new HashMap<State, String[]>();
		tmp.put(State.NONE, new String[] {"OK:INITIALISE"});
		tmp.put(State.INITIALISE, new String[] {"OK:ACTIVATE"});
		tmp.put(State.ACTIVATE, new String[] {"OK:ASSOCIATE"});
		tmp.put(State.ASSOCIATE, new String[] {"OK:CHECK_CONFIG_VERSION"});
		tmp.put(State.CHECK_CONFIG_VERSION, new String[] {"OK:EXECUTE_QUEUE","NOT_OK:DOWNLOAD_CONFIG"});
		tmp.put(State.DOWNLOAD_CONFIG, new String[] {"OK:RUN_INIT_TESTS"});
		tmp.put(State.RUN_INIT_TESTS, new String[] {"OK:EXECUTE_QUEUE"});
		tmp.put(State.EXECUTE_QUEUE, new String[] {"OK:SUBMIT_RESULTS"});
		tmp.put(State.SUBMIT_RESULTS, new String[] {"OK:SHUTDOWN"});
		tmp.put(State.SHUTDOWN, new String[] {"OK:CHECK_CONFIG_VERSION"});
		transitionFunction = Collections.unmodifiableMap(tmp);
		
	}
	
	@Override
	public String getType(){
		return TYPE;
	}
	
	@Override
	protected Map<State,String[]> getTransition(){
		return transitionFunction;
	}
	

}
