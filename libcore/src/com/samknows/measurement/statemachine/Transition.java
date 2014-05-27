package com.samknows.measurement.statemachine;

import java.util.Map;

import android.content.Context;

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

public abstract class Transition {
	
	public static Transition create(SK2AppSettings as){
		//if(as.anonymous){} else {}
		
		// Always return an instance of TransitionAnonymous...
		return new TransitionAnonymous();
	}
	
	public abstract String getType();
	protected abstract Map<State, String[]> getTransition();
	
	public State getNextState(State state, StateResponseCode code) {
		String[] state_transiction = getTransition().get(state);
		if(state_transiction == null){
			throw new RuntimeException(getType() +" state machine doesn't define state: "+ state);
		}
		for (String s : state_transiction) {
			if (s.startsWith(code.toString())) {
				return State.valueOf(s.split(":")[1]);
			}
		}
		throw new RuntimeException(getType() +" does not define " +state +" ->" + code);
		
	}
	
	public static BaseState createState(State state, MainService ctx) {
		switch (state) {
		case NONE: return new NoneState(ctx);
		case INITIALISE : return new InitialiseState(ctx);
		case INITIALISE_ANONYMOUS: return new InitialiseAnonymousState(ctx);
		case ACTIVATE : return new ActivateState(ctx);
		case ASSOCIATE : return new AssociateState(ctx);
		case CHECK_CONFIG_VERSION : return new CheckConfigVersionState(ctx);
		case DOWNLOAD_CONFIG : return new DownloadConfigState(ctx);
		case DOWNLOAD_CONFIG_ANONYMOUS: return new DownloadConfigAnonymousState(ctx);
		case RUN_INIT_TESTS : return new RunInitTestsState(ctx);
		case EXECUTE_QUEUE : return new ExecuteScheduledTestQueueState(ctx);
		case SUBMIT_RESULTS : return new SubmitResultsState(ctx);
		case SUBMIT_RESULTS_ANONYMOUS : return new SubmitResultsAnonymousState(ctx);
		case SHUTDOWN:
		}
		throw new RuntimeException("unimplemented state: " + state);
	}
}
