package com.samknows.measurement.TestRunner;

import android.content.Context;
import android.os.Looper;
import com.samknows.libcore.SKLogger;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.environment.PassiveMetricCollector;
import com.samknows.measurement.schedule.condition.ConditionGroupResult;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.schedule.TestDescription;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.statemachine.state.StateEnum;
import com.samknows.measurement.statemachine.StateResponseCode;
import com.samknows.measurement.statemachine.Transition;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.TestBatch;
import com.samknows.measurement.Storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;



/*  
 * This class is used to run the the tests when they are executed manually
 * as a continuous test.
 * The class is simply created, started, and stopped.
 * Test results are not reported while the tests are running.
 */
public class ContinuousTestRunner  extends SKTestRunner  implements Runnable {
  private static final String JSON_SUBMISSION_TYPE = "continuous_testing";
  private Context mContext;
  private StateEnum mPreviousState;

  PassiveMetricCollector mPassiveMetricCollector;

  private TestContext mTestContext;
  private ScheduleConfig mConfig;
  private DBHelper mDBHelper;

  public ContinuousTestRunner(SKTestRunnerObserver observer) {
    super(observer);

    mTestContext = TestContext.createBackgroundTestContext(mContext);
    mContext = SKApplication.getAppInstance().getApplicationContext();
    mPassiveMetricCollector = new PassiveMetricCollector(mContext, mTestContext);
    mDBHelper = new DBHelper(mContext);
  }

  // Start continuous testing
  public void startTestRunning_RunInBackground() {
    new Thread(this).start();
  }

  public void stopTestRunning() {
    super.setStateChangeToUIHandler(TestRunnerState.STOPPING);
    isExecutingContinuous = false;
  }

  /*
   * Execute continuous testing:
   * check for config file
   * start data collectors
   * run init tests
   * while(not stopped)
   * 		run tests
   * 		submit data
   */
  @Override
  public void run() {

    super.setStateChangeToUIHandler(TestRunnerState.STARTING);

    Looper.prepare();

    StateResponseCode response;
    SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
    mPreviousState = appSettings.getState();
    StateEnum state = StateEnum.EXECUTE_QUEUE;
    appSettings.saveState(state);
    try {
      response = Transition.createState(state, mContext).executeState();
    } catch (Exception e) {
      SKLogger.e(this, "fail to execute " + state + " ");
    }
    Storage storage = CachingStorage.getInstance();
    mConfig = storage.loadScheduleConfig();
    if (mConfig == null) {
      onEnd();
      throw new NullPointerException("null schedule config!");
    }

    isExecutingContinuous = true;

    super.setStateChangeToUIHandler(TestRunnerState.EXECUTING);


    //
    // Change required 29/04/2014 - always enforce that we run a closest target test FIRST for a continuous test!
    // Code very similar to this is in ManualTestRunner.java.
    // If we don't do this, then the continuous tests always fail unless you have *first*
    // already remembered to run a manual test!
    mConfig.forManualOrContinuousTestEnsureClosestTargetIsRunAtStart(mConfig.continuous_tests);

    mPassiveMetricCollector.startCollectors(mConfig.dataCollectors);

    while (isExecutingContinuous()) {

      executeBatch();

      try {
        state = StateEnum.SUBMIT_RESULTS_ANONYMOUS;
        response = Transition.createState(state, mContext).executeState();
      } catch (Exception e) {
        SKLogger.e(this, "fail to execute " + state + " ");
      }

//			// And after first pass-through, remove the closest target test so we don't
//			// keep running it - this is different to iOS, where we run it every time.
//			if (mConfig.continuous_tests.size() > 0) {
//				if (mConfig.continuous_tests.get(0).type.equals(SKConstants.TEST_TYPE_CLOSEST_TARGET)) {
//					mConfig.continuous_tests.remove(0);
//				}
//			}
    }
    mPassiveMetricCollector.stopCollectors();

    super.setStateChangeToUIHandler(TestRunnerState.STOPPED);

    onEnd();
  }

  private void executeBatch() {
    TestContext tc = mTestContext;
    long startTime = System.currentTimeMillis();

    List<JSONObject> testsResults = new ArrayList<>();

    HashMap<String, Object> batch = new HashMap<>();
    TestExecutor te = new TestExecutor(tc);
    batch.put(TestBatch.JSON_DTIME, startTime);
    batch.put(TestBatch.JSON_RUNMANUALLY, "0");

    ConditionGroupResult tr = new ConditionGroupResult();
    for (TestDescription td : mConfig.continuous_tests) {
      te.executeTest(td, tr);

      List<JSONObject> theResult = com.samknows.measurement.storage.StorageTestResult.testOutput(te.getExecutingTest(), td.type);
      if (theResult != null) {
        testsResults.addAll(theResult);
      }
    }

    List<JSONObject> passiveMetrics = mPassiveMetricCollector.collectMetricsIntoResultsContainer(te.getResultsContainer());

    long batchId = mDBHelper.insertTestBatch(new JSONObject(batch), testsResults, passiveMetrics);

    te.save(JSON_SUBMISSION_TYPE, batchId);
  }



  private void onEnd() {
    SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
    appSettings.saveState(mPreviousState);
  }


  //
  // Properties and methods to get and modify the execution status of continuous testing
  //
  private static boolean isExecutingContinuous = false;

  public boolean isExecutingContinuous() {
    return isExecutingContinuous;
  }

}
