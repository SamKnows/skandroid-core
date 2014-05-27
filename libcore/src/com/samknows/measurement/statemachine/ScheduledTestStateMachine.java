package com.samknows.measurement.statemachine;


import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.MainService;
import com.samknows.measurement.activity.components.UIUpdate;
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
import com.samknows.measurement.util.OtherUtils;

public class ScheduledTestStateMachine {
	private MainService ctx;
	
	public ScheduledTestStateMachine(MainService ctx) {
		super();
		this.ctx = ctx;
	}

	// Returns the number of test bytes!
	public long executeRoutine() { 
		SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
		Transition t = Transition.create(appSettings);
		State state = appSettings.getState();
		SKLogger.d(this, "starting routine from state: " + state);
		ctx.publish(UIUpdate.machineState(state));
		
		long accumulatedTestBytes = 0;
		
		while (state != State.SHUTDOWN) {
			SKLogger.d(this, "executing state: " + state);
			StateResponseCode code;
			try {
			  //code = Transition.createState(state, ctx).executeState();
				BaseState baseState = Transition.createState(state, ctx);
     			code = baseState.executeState();
         		accumulatedTestBytes += baseState.getAccumulatedTestBytes();
			} catch (Exception e) {
				SKLogger.d(this, "+++++DEBUG+++++ error calling executeState !" + e.toString());
				// do NOT rethrow the exception!
    			code = StateResponseCode.FAIL;
			}
			SKLogger.d(this, "finished state, code: " + code); 
			if (code == StateResponseCode.FAIL) {
				appSettings.saveState(State.NONE);
				appSettings.stateMachineFailure();
				SKLogger.e(this, "fail to execute state: " + state + ", reschedule");
				OtherUtils.rescheduleRTC(ctx, appSettings.rescheduleTime);
				ctx.publish(UIUpdate.stateFailure());
				return accumulatedTestBytes;
			} else {
				appSettings.stateMachineSuccess();
				state = t.getNextState(state, code);
				appSettings.saveState(state);
				SKLogger.d(this, "change service state to: " + state);
				activation(state);
			}
			ctx.publish(UIUpdate.progress(state));
			ctx.publish(UIUpdate.machineState(state));
		}
		
		state = t.getNextState(state, StateResponseCode.OK);
		appSettings.saveState(state);
		SKLogger.d(this, "shutdown state, stop execution and setup state for next time: " + state);
	
		return accumulatedTestBytes;
	}
	
	
	
	//used to set the service activate according to the state
	private void activation(State state){
		switch(state){
		case RUN_INIT_TESTS:
			SK2AppSettings.getInstance().setServiceActivated(true);
			default:
		}
	}
	
	
}
