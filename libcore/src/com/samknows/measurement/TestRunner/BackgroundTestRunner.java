package com.samknows.measurement.TestRunner;

import android.content.Context;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.statemachine.State;
import com.samknows.measurement.statemachine.StateResponseCode;
import com.samknows.measurement.statemachine.Transition;
import com.samknows.measurement.statemachine.state.BaseState;
import com.samknows.measurement.util.OtherUtils;

public class BackgroundTestRunner  extends SKTestRunner  {
  private Context ctx;

  public BackgroundTestRunner(SKTestRunnerObserver observer) {
    super(observer);
    this.ctx = SKApplication.getAppInstance().getApplicationContext();
  }

  // Returns the number of test bytes!
  public long startTestRunning_RunToEndBlocking_ReturnNumberOfTestBytes() {
    setStateChangeToUIHandler(TestRunnerState.EXECUTING);

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
        SKLogger.e(this, "fail to startTestRunning_RunToEndBlocking state: " + state + ", reschedule");
        OtherUtils.rescheduleRTC(ctx, appSettings.rescheduleTime);
        setStateChangeToUIHandler(TestRunnerState.STOPPED);
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

    setStateChangeToUIHandler(TestRunnerState.STOPPED);
    return accumulatedTestBytes;
  }
}
