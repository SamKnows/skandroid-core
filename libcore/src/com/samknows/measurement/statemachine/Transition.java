package com.samknows.measurement.statemachine;

import android.content.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.statemachine.state.BaseState;
import com.samknows.measurement.statemachine.state.ExecuteScheduledTestQueueState;
import com.samknows.measurement.statemachine.state.NoneState;
import com.samknows.measurement.statemachine.state.SubmitResultsAnonymousState;

public class Transition {
	
	public static Transition create(SK2AppSettings as){
		//if(as.anonymous){} else {}
		
		return new Transition();
	}
	
	public State getNextState(State state, StateResponseCode code) {
		String[] state_transition = getTransition().get(state);
		if(state_transition == null){
			throw new RuntimeException("Transition: state machine doesn't define state: "+ state);
		}
		for (String s : state_transition) {
			if (s.startsWith(code.toString())) {
				return State.valueOf(s.split(":")[1]);
			}
		}
		throw new RuntimeException("Transition does not define " +state +" ->" + code);
		
	}
	
	public static BaseState createState(State state, Context ctx) {
		switch (state) {
		case NONE: return new NoneState(ctx);
		case EXECUTE_QUEUE : return new ExecuteScheduledTestQueueState(ctx);
		case SUBMIT_RESULTS_ANONYMOUS : return new SubmitResultsAnonymousState(ctx);
		case SHUTDOWN:
		}
		throw new RuntimeException("unimplemented state: " + state);
	}

  private static final Map<State, String[]> transitionFunction;
  static {
    Map<State, String[]> tmp= new HashMap<>();
    tmp.put(State.NONE, new String[] {"OK:EXECUTE_QUEUE"});
    tmp.put(State.EXECUTE_QUEUE, new String[] {"OK:SUBMIT_RESULTS_ANONYMOUS"});
    tmp.put(State.SUBMIT_RESULTS_ANONYMOUS, new String[] {"OK:SHUTDOWN"});
    tmp.put(State.SHUTDOWN, new String[] {"OK:NONE"});
    transitionFunction = Collections.unmodifiableMap(tmp);
  }

  protected Map<State, String[]> getTransition() {
    return transitionFunction;
  }
}

