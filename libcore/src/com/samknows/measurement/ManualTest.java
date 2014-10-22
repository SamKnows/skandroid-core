package com.samknows.measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONException;
import org.json.JSONObject;


import com.samknows.libcore.R;
import com.samknows.libcore.SKConstants;
import com.samknows.libcore.SKLogger;
import com.samknows.measurement.net.SubmitTestResultsAction;
import com.samknows.measurement.net.SubmitTestResultsAnonymousAction;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.schedule.TestDescription;
import com.samknows.measurement.schedule.condition.ConditionGroupResult;
import com.samknows.measurement.schedule.datacollection.BaseDataCollector;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.PassiveMetric;
import com.samknows.measurement.storage.TestBatch;
import com.samknows.measurement.storage.StorageTestResult;
import com.samknows.measurement.test.TestContext;
import com.samknows.measurement.test.TestExecutor;
import com.samknows.tests.ClosestTarget;
import com.samknows.tests.Test;
import com.samknows.tests.TestFactory;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

/*  
 * This class is used to run the the tests when they are executed manually
 * implements a runnable interface and and uses an Handler in order to publish
 * the tests results to the interface
 */
public class ManualTest implements Runnable {
	private Handler mHandler;
	private List<TestDescription> mTestDescription;
	private Context ctx;
	private AtomicBoolean run = new AtomicBoolean(true);
	public static boolean isExecuting = false;

	ManualTest(Context ctx, Handler handler, List<TestDescription> td) {
		mHandler = handler;
		mTestDescription = td;
		this.ctx = ctx;
	}

	/*
	 * Returns a ManualTest object that runs only the test with id test_id
	 */

	public static ManualTest create(Context ctx, Handler handler, int test_id, StringBuilder errorDescription) {
		ManualTest ret = create(ctx, handler, errorDescription);
		if (ret == null) {
			return ret;
		}
		
		// We must ALWAYS add the closest target test - 29/04/2014 ...
		ArrayList<TestDescription> filteredArrayOfTestDescriptions = new ArrayList<TestDescription>();
		SKLogger.sAssert(ManualTest.class, ret.mTestDescription.get(0).type.equals(SKConstants.TEST_TYPE_CLOSEST_TARGET));
		filteredArrayOfTestDescriptions.add(ret.mTestDescription.get(0));
		
		boolean bFound = false;
		
		for (TestDescription td : ret.mTestDescription) {
			if (td.testId == test_id) {
        		bFound = true;
				filteredArrayOfTestDescriptions.add(td);
			}
		}
		
		if (bFound == false) {
			SKLogger.e(ManualTest.class,
					"ManualTest cannot be initialized because there is no manual test with id: "
							+ test_id);
			return null;
		}
		
		ret.mTestDescription = filteredArrayOfTestDescriptions;
		
		return ret;
	}
	
	/*
     * Pablo's modifications
     * Returns a ManualTest object that runs only the tests in the list
     */

    public static ManualTest create(Context ctx, Handler handler, List<Integer> test_ids, StringBuilder errorDescription)
    {
        ManualTest ret = create(ctx, handler, errorDescription);
        
        if (ret == null)
        {
            return ret;
        }    
        
        // Add the closest target test
        List<TestDescription> listOfTestDescriptions = new ArrayList<TestDescription>();
        listOfTestDescriptions.add(ret.mTestDescription.get(0));
        
        for (TestDescription td : ret.mTestDescription)
        {            
            if (test_ids.contains(td.testId))
            {    
                listOfTestDescriptions.add(td);                                                                
            }
        }
        
        ret.mTestDescription = listOfTestDescriptions;
        
        return ret;        
    }
    // End's Pablo's modification

	/*
	 * Returns a ManualTest object if the manual_tests list of the schedule
	 * config is not empty and the MainService is not executing
	 */
	public static ManualTest create(Context ctx, Handler handler, StringBuilder RErrorDescription) {
		Storage storage = CachingStorage.getInstance();
		ScheduleConfig config = storage.loadScheduleConfig();
		if (config == null) {
			RErrorDescription.append(ctx.getString(R.string.manual_test_create_failed_1));
			SKLogger.e( ManualTest.class, RErrorDescription.toString());
			return null;
		}
		if (config.manual_tests.size() == 0) {
			RErrorDescription.append(ctx.getString(R.string.manual_test_create_failed_2));
			SKLogger.e( ManualTest.class, RErrorDescription.toString());
			return null;
		}
		if (MainService.isExecuting()) {
			RErrorDescription.append(ctx.getString(R.string.manual_test_create_failed_3));
			SKLogger.e(ManualTest.class, RErrorDescription.toString());
			return null;
		}
		
		//
		// Change required 29/04/2014 - always enforce that we run a closest target test FIRST for a manual test!
		// Code very similar to this is in ContinuousTesting.java.
		//
		config.forManualOrContinuousTestEnsureClosestTargetIsRunAtStart(config.manual_tests);
		
		return new ManualTest(ctx, handler, config.manual_tests);
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

	/*
	 * Runs all the test in manual tests
	 */
	private boolean mbUdpClosestTargetTestSucceeded = false;
	public static final String kManualTest_UDPFailedSkipTests = "kManualTest_UDPFailedSkipTests";

	@Override
	public void run() {
		DBHelper db = new DBHelper(ctx);
	
		mbUdpClosestTargetTestSucceeded  = false;
		
		sSetIsExecuting(true);
		
		// Start collectors for the passive metrics
		// Start tests
		long startTime = System.currentTimeMillis();
		JSONObject batch = new JSONObject();
		TestContext tc = TestContext.createManualTestContext(ctx);
		TestExecutor manualTestExecutor = new TestExecutor(tc);
		List<JSONObject> testsResults = new ArrayList<JSONObject>();
		List<JSONObject> passiveMetrics = new ArrayList<JSONObject>();
		manualTestExecutor.startInBackGround();
		Message msg;
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

			manualTestExecutor.addRequestedTest(td);
			// check if a stop command has been received
			if (!run.get()) {
				manualTestExecutor.cancelNotification();
				SKLogger.d(this, "Manual test interrupted by the user.");
				break;
			}
			ConditionGroupResult tr = new ConditionGroupResult();
			ObservableExecutor oe = new ObservableExecutor(manualTestExecutor, td, tr);
			Thread t = new Thread(oe);
			t.start();
			while (true) {
				try {
					t.join(100);
					if (!t.isAlive())
						break;
				} catch (InterruptedException ie) {
					SKLogger.e(this, ie.getMessage());
				}
				for (JSONObject pm : progressMessage(td, manualTestExecutor)) {
					msg = new Message();
					msg.obj = pm;
					mHandler.sendMessage(msg);
				}

			}
			
			if (td.type.equals(SKConstants.TEST_TYPE_CLOSEST_TARGET)) {
				SKLogger.sAssert(getClass(), manualTestExecutor.getExecutingTest() != null);
				if (manualTestExecutor.getExecutingTest() != null) {
					SKLogger.sAssert(getClass(), manualTestExecutor.getExecutingTest().getClass() == ClosestTarget.class);

					ClosestTarget closestTargetTest = (ClosestTarget)manualTestExecutor.getExecutingTest();
					mbUdpClosestTargetTestSucceeded = closestTargetTest.getUdpClosestTargetTestSucceeded();
				}
			}
			
			testsBytes += manualTestExecutor.getLastTestByte();
			
//			Test theTestThatWasRun = oe.getTheExecutedTestPostRun();
//			if (theTestThatWasRun != null) {
//				if (theTestThatWasRun instanceof ClosestTarget) {
//
//				}
//			}

			List<JSONObject> currResults = new ArrayList<JSONObject>();
			for (String out : tr.results) {
				List<JSONObject> theResult = StorageTestResult.testOutput(out, manualTestExecutor);
				if (theResult != null) {
					currResults.addAll(theResult);
				}
			}
			for (JSONObject cr : currResults) {
				// publish results
				msg = new Message();
				msg.obj = cr;
				mHandler.sendMessage(msg);
			}
			testsResults.addAll(currResults);
		}
		SKLogger.d(this, "bytes used by the tests: " + testsBytes);
		SK2AppSettings.getInstance().appendUsedBytes(testsBytes);
		// stops collectors
		manualTestExecutor.stop();

		manualTestExecutor.save("manual_test");

		// Gather data from collectors
		for (BaseDataCollector collector : tc.config.dataCollectors) {
			if (collector.isEnabled) {
				for (JSONObject o : collector.getPassiveMetric()) {
					// update interface
					msg = new Message();
					msg.obj = PassiveMetric.passiveMetricToCurrentTest(o);
					mHandler.sendMessage(msg);
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
		if (run.get()) {
			db.insertTestBatch(batch, testsResults, passiveMetrics);
		}
		// Send completed message to the interface
		msg = new Message();
		JSONObject jtc = new JSONObject();
		try {
			Thread.sleep(1000);
			jtc.put(StorageTestResult.JSON_TYPE_ID, "completed");
			msg.obj = jtc;

		} catch (JSONException je) {
			SKLogger.e(this, "Error in creating json object: " + je.getMessage());
		} catch (InterruptedException e) {
			SKLogger.e(
					this,
					"Sleep interrupted in the manual test view: "
							+ e.getMessage());
		}
		mHandler.sendMessage(msg);

		try {
			// Submitting test results
			new SubmitTestResultsAnonymousAction(ctx).execute();
		} catch (Throwable t) {
			SKLogger.e(this, "Submit result. ", t);
		}
	
		if(!SKApplication.getAppInstance().getIsBackgroundTestingEnabledInUserPreferences()){
			MainService.force_poke(ctx);
		}
		SKLogger.d(this, "Exiting manual test");
		
		sSetIsExecuting(false);
	}
	
	private static void sSetIsExecuting(boolean bValue) {
		isExecuting = bValue;
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

	static private List<JSONObject> progressMessage(TestDescription td,
			TestExecutor te) {
		List<JSONObject> ret = new ArrayList<JSONObject>();
		List<String> tests = new ArrayList<String>();

		if (td.type.equals(TestFactory.DOWNSTREAMTHROUGHPUT)) {
			tests.add("" + StorageTestResult.DOWNLOAD_TEST_ID);
		} else if (td.type.equals(TestFactory.UPSTREAMTHROUGHPUT)) {
			tests.add("" + StorageTestResult.UPLOAD_TEST_ID);
		} else if (td.type.equals(TestFactory.LATENCY)) {
			tests.add("" + StorageTestResult.LATENCY_TEST_ID);
			tests.add("" + StorageTestResult.PACKETLOSS_TEST_ID);
			tests.add("" + StorageTestResult.JITTER_TEST_ID);
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
					ManualTest.class,
					"Error in creating JSON progress object: "
							+ je.getMessage());
		}
		return ret;
	}

	// It stops the test from being executed
	public void stop() {
		
		run.set(false);
	}
}
