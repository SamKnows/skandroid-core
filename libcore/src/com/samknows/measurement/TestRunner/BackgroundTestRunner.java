package com.samknows.measurement.TestRunner;

import android.content.Context;

import com.samknows.libcore.SKPorting;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.statemachine.state.StateEnum;
import com.samknows.measurement.statemachine.StateResponseCode;
import com.samknows.measurement.statemachine.Transition;
import com.samknows.measurement.statemachine.state.BaseState;
import com.samknows.measurement.util.OtherUtils;

public class BackgroundTestRunner  extends SKTestRunner  {
  private final Context mContext;

  public BackgroundTestRunner(SKTestRunnerObserver observer) {
    super(observer);

    // if this is NOT null, then we will need to put in place code that prevents us emitting
    // lots of unwanted latency observer calls; we'd also want any such calls not to be made
    // in the main user interface thread (via the private Handler instance owned by the
    // SKTestRunner superclass)...
    SKPorting.sAssert(observer == null);

    mContext = SKApplication.getAppInstance().getApplicationContext();
  }

  // Returns the number of test bytes!
  public long startTestRunning_RunToEndBlocking_ReturnNumberOfTestBytes() {
    setStateChangeToUIHandler(TestRunnerState.STARTING);
    setStateChangeToUIHandler(TestRunnerState.EXECUTING);

    SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
    Transition t = Transition.create(appSettings);
    StateEnum state = appSettings.getState();
    SKPorting.sLogD(this, "starting routine from state: " + state);

    long accumulatedTestBytes = 0;

    while (state != StateEnum.SHUTDOWN) {
      SKPorting.sLogD(this, "executing state: " + state);
      StateResponseCode code;
      try {
        //code = Transition.createState(state, mContext).executeState();
        BaseState baseState = Transition.createState(state, mContext);
        code = baseState.executeState();
        accumulatedTestBytes += baseState.getAccumulatedTestBytes();
      } catch (Exception e) {
        SKPorting.sLogD(this, "+++++DEBUG+++++ error calling executeState !" + e.toString());
        // do NOT rethrow the exception!
        code = StateResponseCode.FAIL;
      }
      SKPorting.sLogD(this, "finished state, code: " + code);
      if (code == StateResponseCode.FAIL) {
        appSettings.saveState(StateEnum.NONE);
        SKPorting.sAssertE(this, "fail to startTestRunning_RunToEndBlocking state: " + state + ", reschedule");
        OtherUtils.rescheduleRTC(mContext, appSettings.rescheduleTime);
        setStateChangeToUIHandler(TestRunnerState.STOPPED);
        return accumulatedTestBytes;
      } else {
        state = t.getNextState(state, code);
        appSettings.saveState(state);
        SKPorting.sLogD(this, "change service state to: " + state);
      }
    }

    state = t.getNextState(state, StateResponseCode.OK);
    appSettings.saveState(state);
    SKPorting.sLogD(this, "shutdown state, stop execution and setup state for next time: " + state);

    setStateChangeToUIHandler(TestRunnerState.STOPPED);
    return accumulatedTestBytes;
  }
}
