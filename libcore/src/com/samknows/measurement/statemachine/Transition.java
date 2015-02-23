package com.samknows.measurement.statemachine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.samknows.measurement.MainService;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.statemachine.state.ActivateState;
import com.samknows.measurement.statemachine.state.AssociateState;
import com.samknows.measurement.statemachine.state.BaseState;
import com.samknows.measurement.statemachine.state.CheckConfigVersionState;
import com.samknows.measurement.statemachine.state.DownloadConfigAnonymousState;
import com.samknows.measurement.statemachine.state.DownloadConfigState;
import com.samknows.measurement.statemachine.state.ExecuteScheduledTestQueueState;
import com.samknows.measurement.statemachine.state.InitialiseAnonymousState;
import com.samknows.measurement.statemachine.state.InitialiseState;
import com.samknows.measurement.statemachine.state.NoneState;
import com.samknows.measurement.statemachine.state.RunInitTestsState;
import com.samknows.measurement.statemachine.state.SubmitResultsAnonymousState;
import com.samknows.measurement.statemachine.state.SubmitResultsState;

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
	
	public static BaseState createState(State state, MainService ctx) {
		switch (state) {
		case NONE: return new NoneState(ctx);
		case INITIALISE_ANONYMOUS: return new InitialiseAnonymousState(ctx);
		case RUN_INIT_TESTS : return new RunInitTestsState(ctx);
		case EXECUTE_QUEUE : return new ExecuteScheduledTestQueueState(ctx);
		case SUBMIT_RESULTS : return new SubmitResultsState(ctx);
		case SUBMIT_RESULTS_ANONYMOUS : return new SubmitResultsAnonymousState(ctx);
		case SHUTDOWN:
		}
		throw new RuntimeException("unimplemented state: " + state);
	}

  private static final Map<State, String[]> transitionFunction;
  static {
    Map<State, String[]> tmp= new HashMap<State, String[]>();
    tmp.put(State.NONE, new String[] {"OK:RUN_INIT_TESTS"});
    tmp.put(State.RUN_INIT_TESTS, new String[] {"OK:EXECUTE_QUEUE"});
    tmp.put(State.EXECUTE_QUEUE, new String[] {"OK:SUBMIT_RESULTS_ANONYMOUS"});
    tmp.put(State.SUBMIT_RESULTS_ANONYMOUS, new String[] {"OK:SHUTDOWN"});
    tmp.put(State.SHUTDOWN, new String[] {"OK:DOWNLOAD_CONFIG_ANONYMOUS"});
    transitionFunction = Collections.unmodifiableMap(tmp);
  }

  protected Map<State, String[]> getTransition() {
    return transitionFunction;
  }
}

