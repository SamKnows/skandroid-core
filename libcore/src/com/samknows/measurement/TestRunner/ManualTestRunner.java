package com.samknows.measurement.TestRunner;

import android.content.Context;
import android.util.Log;

import com.samknows.libcore.R;
import com.samknows.libcore.SKConstants;
import com.samknows.libcore.SKLogger;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.MainService;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.schedule.TestDescription.*;
import com.samknows.measurement.net.SubmitTestResultsAnonymousAction;
import com.samknows.measurement.schedule.condition.ConditionGroupResult;
import com.samknows.measurement.environment.BaseDataCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.schedule.TestDescription;
import com.samknows.measurement.storage.StorageTestResult.*;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.StorageTestResult;
import com.samknows.measurement.storage.TestBatch;
import com.samknows.measurement.Storage;
import com.samknows.measurement.test.TestContext;
import com.samknows.measurement.test.TestExecutor;
import com.samknows.tests.ClosestTarget;
import com.samknows.tests.Test;
import com.samknows.tests.TestFactory;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/*  
 * This class is used to run the the tests when they are executed manually
 * The class is created, started, and stopped.
 * Test results are not reported through a handler, as messages, while the tests are running.
 */

public class ManualTestRunner extends SKTestRunner implements Runnable {

  static final String TAG = "ManualTestRunner";


  private List<TestDescription> mTestDescription;
  private Context ctx;
  private AtomicBoolean run = new AtomicBoolean(true);

  private ManualTestRunner(SKTestRunnerObserver observer, List<TestDescription> td) {
    super(observer);

    mTestDescription = td;
    this.ctx = SKApplication.getAppInstance().getApplicationContext();
  }

	/*
   * Returns a ManualTestRunner object that runs only the test with id test_id
	 */

  public static ManualTestRunner create(SKTestRunnerObserver observer, SCHEDULE_TEST_ID test_id, StringBuilder errorDescription) {
    ManualTestRunner ret = create(observer, errorDescription);
    if (ret == null) {
      return ret;
    }

    // We must ALWAYS start with the closest target test - 29/04/2014 ...
    SKLogger.sAssert(ManualTestRunner.class, ret.mTestDescription.get(0).type.equals(SKConstants.TEST_TYPE_CLOSEST_TARGET));

    if (test_id == SCHEDULE_TEST_ID.ALL_TESTS) {
      return ret;
    }

    //
    // We're told to run just a specific test... find it, and use it.
    //
    ArrayList<TestDescription> filteredArrayOfTestDescriptions = new ArrayList<TestDescription>();
    // We must ALWAYS start with the closest target test - 29/04/2014 ...
    filteredArrayOfTestDescriptions.add(ret.mTestDescription.get(0));

    boolean bFound = false;
    for (TestDescription td : ret.mTestDescription) {
      if (td.testId == test_id) {
        bFound = true;
        filteredArrayOfTestDescriptions.add(td);
      }
    }

    if (bFound == false) {
      SKLogger.sAssert(false);
      SKLogger.e(ManualTestRunner.class, "ManualTestRunner cannot be initialized because there is no manual test with id: "
              + test_id);
      return null;
    }

    ret.mTestDescription = filteredArrayOfTestDescriptions;

    return ret;
  }
	
	/*
   * Return a ManualTestRunner object that runs only the tests in the list
   */

  public static ManualTestRunner create(SKTestRunnerObserver observer, List<SCHEDULE_TEST_ID> test_ids, StringBuilder errorDescription) {
    ManualTestRunner ret = create(observer, errorDescription);

    if (ret == null) {
      return ret;
    }

    // Add the closest target test
    List<TestDescription> listOfTestDescriptions = new ArrayList<TestDescription>();
    listOfTestDescriptions.add(ret.mTestDescription.get(0));

    for (TestDescription td : ret.mTestDescription) {
      if (test_ids.contains(td.testId)) {
        listOfTestDescriptions.add(td);
      }
    }

    ret.mTestDescription = listOfTestDescriptions;

    return ret;
  }

  /*
   * Return a ManualTestRunner object if the manual_tests list of the schedule
   * config is not empty and the MainService is not executing
   */
  public static ManualTestRunner create(SKTestRunnerObserver observer, StringBuilder RErrorDescription) {

    Context ctx = SKApplication.getAppInstance().getApplicationContext();

    Storage storage = CachingStorage.getInstance();
    ScheduleConfig config = storage.loadScheduleConfig();
    if (config == null) {
      RErrorDescription.append(ctx.getString(R.string.manual_test_create_failed_1));
      SKLogger.e(ManualTestRunner.class, RErrorDescription.toString());
      return null;
    }
    if (config.manual_tests.size() == 0) {
      RErrorDescription.append(ctx.getString(R.string.manual_test_create_failed_2));
      SKLogger.e(ManualTestRunner.class, RErrorDescription.toString());
      return null;
    }
    if (MainService.isExecuting()) {
      RErrorDescription.append(ctx.getString(R.string.manual_test_create_failed_3));
      SKLogger.e(ManualTestRunner.class, RErrorDescription.toString());
      return null;
    }

    //
    // Change required 29/04/2014 - always enforce that we run a closest target test FIRST for a manual test!
    // Code very similar to this is in ContinuousTestRunner.java.
    //
    config.forManualOrContinuousTestEnsureClosestTargetIsRunAtStart(config.manual_tests);

    return new ManualTestRunner(observer, config.manual_tests);
  }

  public void startTestRunning_RunInBackground() {
    new Thread(this).start();
  }

  // It stops the test from being executed
  public void stopTestRunning() {

    run.set(false);
  }

  /*
   * Runs all the test in manual tests
   */
  private boolean mbUdpClosestTargetTestSucceeded = false;

  @Override
  public void run() {
    setStateChangeToUIHandler(TestRunnerState.STARTING);
    setStateChangeToUIHandler(TestRunnerState.EXECUTING);

    DBHelper db = new DBHelper(ctx);

    mbUdpClosestTargetTestSucceeded = false;

    // Start collectors for the passive metrics
    // Start tests
    TestContext tc = TestContext.createManualTestContext(ctx);
    long startTime = System.currentTimeMillis();

    List<JSONObject> testsResults = new ArrayList<JSONObject>();
    List<JSONObject> passiveMetrics = new ArrayList<JSONObject>();

    JSONObject batch = new JSONObject();
    TestExecutor te = new TestExecutor(tc);

    te.startInBackGround();
    long testsBytes = 0;
    for (TestDescription td : mTestDescription) {
      // GIVEN:
      // - running manual test
      // - if latency/loss/jitter
      // WHEN:
      // - if the closest target UDP test had failed (NB: this is ALWAYS run first in manual testing)
      // THEN:
      // - skip this test

      if (td.type.equals(SKConstants.TEST_TYPE_LATENCY)) {
        if (mbUdpClosestTargetTestSucceeded == false) {
          continue;
        }
      }

      te.addRequestedTest(td);
      // check if a stop command has been received
      if (!run.get()) {
        te.cancelNotification();
        Log.d(TAG, "Manual test interrupted by the user.");
        break;
      }
      ConditionGroupResult tr = new ConditionGroupResult();
      ObservableExecutor oe = new ObservableExecutor(te, td, tr);
      Thread t = new Thread(oe);
      t.start();
      while (true) {
        try {
          t.join(100);
          if (!t.isAlive())
            break;
        } catch (InterruptedException ie) {
          SKLogger.sAssert(false);
        }

//        if (td.type.equals(SKConstants.TEST_TYPE_UPLOAD)) {
//          // Upload test!
//          // Could do, say, this...
//          //HttpTest theTest = (HttpTest)te.getExecutingTest();
//          //theTest.doSomethingSpecificToUploadTest...()
//        }

        // For all test results, we send a progress percentage update Message instance...
        // the JSON_STATUS_COMPLETE field contains the value from te.getProgress())
        // Typically returns just 1 value - might be up to 3 for latency/loss/jitter!
        List<JSONObject> testProgressList = progressMessage(td, te);
        for (JSONObject pm : testProgressList) {
          super.sendTestProgressToUIHandler(pm);
        }

      }

      if (td.type.equals(SKConstants.TEST_TYPE_CLOSEST_TARGET)) {
        SKLogger.sAssert(getClass(), te.getExecutingTest() != null);
        if (te.getExecutingTest() != null) {
          SKLogger.sAssert(getClass(), te.getExecutingTest().getClass() == ClosestTarget.class);

          ClosestTarget closestTargetTest = (ClosestTarget) te.getExecutingTest();
          mbUdpClosestTargetTestSucceeded = closestTargetTest.getUdpClosestTargetTestSucceeded();
        }
      }

      testsBytes += te.getLastTestByte();

//			Test theTestThatWasRun = oe.getTheExecutedTestPostRun();
//			if (theTestThatWasRun != null) {
//				if (theTestThatWasRun instanceof ClosestTarget) {
//
//				}
//			}

      List<JSONObject> currResults = new ArrayList<JSONObject>();
      for (String out : tr.results) {
        List<JSONObject> theResult = StorageTestResult.testOutput(out, te);
        if (theResult != null) {
          currResults.addAll(theResult);
        }
      }

      // For all test results, we send a Message instance...
      // the JSON_HRESULT field contains the value of interest!
      for (JSONObject cr : currResults) {
        super.sendTestResultToUIHandler(cr);
      }
      testsResults.addAll(currResults);
    }
    Log.d(TAG, "bytes used by the tests: " + testsBytes);
    SK2AppSettings.getInstance().appendUsedBytes(testsBytes);
    // stops collectors
    te.stop();

    // Gather data from collectors
    for (BaseDataCollector collector : tc.config.dataCollectors) {
      if (collector.isEnabled) {
        for (JSONObject o : collector.getPassiveMetric()) {
          // update interface
          super.sendPassiveMetricToUIHandler(o);
          // save metric
          passiveMetrics.add(o);
        }
      }
    }
    // insert batch in the database
    try {
      batch.put(TestBatch.JSON_DTIME, startTime);
      batch.put(TestBatch.JSON_RUNMANUALLY, "1");
    } catch (JSONException je) {
      SKLogger.e(this,
          "Error in creating test batch object: " + je.getMessage());
    }

    // insert the results in the database only if we didn't receive a stop
    // command
    long batchId = -1;
    if (run.get()) {
      batchId = db.insertTestBatch(batch, testsResults, passiveMetrics);
    }

    // And now upload the test (this will get a submission id etc., so *must* have a batch id to save to the database...)
    te.save("manual_test", batchId);

    try {
      // Submitting test results
      new SubmitTestResultsAnonymousAction(ctx).execute();
    } catch (Throwable t) {
      SKLogger.e(this, "Submit result. ", t);
    }

    Log.d(TAG, "Exiting manual test");

    // Send completed message to the interface - after a short delay
    super.sendCompletedMessageToUIHandlerWithMilliDelay(1000); // TestRunnerState.STOPPED
  }

  private class ObservableExecutor implements Runnable {
    public TestExecutor te;
    private TestDescription td;
    private ConditionGroupResult tr;
    private Test theTestThatWasRun = null;

    public ObservableExecutor(TestExecutor te, TestDescription td,
                              ConditionGroupResult tr) {
      this.te = te;
      this.td = td;
      this.tr = tr;
    }

    @Override
    public void run() {
      theTestThatWasRun = te.executeTest(td, tr);
    }

    public Test getTheExecutedTestPostRun() {
      return theTestThatWasRun;
    }

  }

  // Typically returns just 1 value - might be up to 3 for latency/loss/jitter!
  static private List<JSONObject> progressMessage(TestDescription td,
                                                  TestExecutor te) {
    List<JSONObject> ret = new ArrayList<JSONObject>();
    List<String> tests = new ArrayList<String>();

    if (td.type.equals(TestFactory.DOWNSTREAMTHROUGHPUT)) {
      tests.add("" + DETAIL_TEST_ID.DOWNLOAD_TEST_ID.getValueAsInt());
    } else if (td.type.equals(TestFactory.UPSTREAMTHROUGHPUT)) {
      tests.add("" + DETAIL_TEST_ID.UPLOAD_TEST_ID.getValueAsInt());
    } else if (td.type.equals(TestFactory.LATENCY)) {
      tests.add("" + DETAIL_TEST_ID.LATENCY_TEST_ID.getValueAsInt());
      tests.add("" + DETAIL_TEST_ID.PACKETLOSS_TEST_ID.getValueAsInt());
      tests.add("" + DETAIL_TEST_ID.JITTER_TEST_ID.getValueAsInt());
    }
    try {
      for (String t : tests) {
        JSONObject c = new JSONObject();
        c.put(StorageTestResult.JSON_TYPE_ID, "test");
        c.put(StorageTestResult.JSON_TESTNUMBER, t);
        c.put(StorageTestResult.JSON_STATUS_COMPLETE, te.getProgress());
        c.put(StorageTestResult.JSON_HRRESULT, "");

        ret.add(c);
      }
    } catch (JSONException je) {
      SKLogger.e(
          ManualTestRunner.class,
          "Error in creating JSON progress object: "
              + je.getMessage());
    }
    return ret;
  }

  // returns the maximum amount of bytes used by the manual test
  // This value is generally *MUCH* higher than the *actually* used value.
  // e.g. 40+MB, compared to 4MB. The reason is that the value is from SCHEDULE.xml, and specifies the absolute
  // maximum that a test is allowed to use; in practise, the test runs for a capped amount of time (also in the schedule data),
  // and processes far less data that the defined maximum number of bytes to use.
  public long getNetUsage() {
    long ret = 0;
    for (TestDescription td : mTestDescription) {
      ret += td.maxUsageBytes;
    }
    return ret;
  }
}
