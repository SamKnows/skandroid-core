package com.samknows.measurement.statemachine;


import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.MainService;
import com.samknows.measurement.activity.components.UIUpdate;
import com.samknows.measurement.statemachine.state.BaseState;
import com.samknows.measurement.test.ScheduledTestExecutionQueue;
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
				SKLogger.e(this, "fail to execute state: " + state + ", reschedule");
				OtherUtils.rescheduleRTC(ctx, appSettings.rescheduleTime);
				return accumulatedTestBytes;
			} else {
				state = t.getNextState(state, code);
				appSettings.saveState(state);
				SKLogger.d(this, "change service state to: " + state);
			}
		}
		
		state = t.getNextState(state, StateResponseCode.OK);
		appSettings.saveState(state);
		SKLogger.d(this, "shutdown state, stop execution and setup state for next time: " + state);
	
		return accumulatedTestBytes;
	}
}
