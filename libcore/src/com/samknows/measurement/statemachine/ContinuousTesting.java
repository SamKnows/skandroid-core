package com.samknows.measurement.statemachine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.MainService;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.Storage;
import com.samknows.measurement.environment.CellTowersDataCollector;
import com.samknows.measurement.environment.DCSData;
import com.samknows.measurement.environment.NetworkDataCollector;
import com.samknows.measurement.environment.PhoneIdentityDataCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.schedule.TestDescription;
import com.samknows.measurement.environment.BaseDataCollector;
import com.samknows.measurement.schedule.condition.ConditionGroupResult;
import com.samknows.measurement.schedule.datacollection.LocationDataCollector;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.ResultsContainer;
import com.samknows.measurement.storage.TestBatch;
import com.samknows.measurement.test.TestContext;
import com.samknows.measurement.test.TestExecutor;


public class ContinuousTesting {
	private static final String JSON_SUBMISSION_TYPE = "continuous_testing";
	private MainService mContext;
	private State mPreviousState;
	List<BaseDataCollector> mCollectors;
	LocationDataCollector mLocationDataCollector;
	List<DCSData> mListDCSData;
	TestContext mTestContext ;
	ScheduleConfig mConfig;
	ResultsContainer mResultsContainer;
	DBHelper mDBHelper;
	public ContinuousTesting(MainService ctx){
		mContext = ctx;
		mListDCSData = new ArrayList<DCSData>();
		mTestContext = TestContext.createBackgroundTestContext(mContext);
		mResultsContainer = new ResultsContainer();
		mDBHelper = new DBHelper(mContext);
	}
	/*
	 * execute the continuous testing
	 * these steps are followed
	 * check for config file
	 * start data collectors
	 * run init tests
	 * while(not stopped)
	 * 		run tests
	 * 		submit data
	 * 		
	 */
	public void execute(){
		StateResponseCode response;
		SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
		mPreviousState = appSettings.getState();
		State state  = State.RUN_INIT_TESTS;
		appSettings.saveState(state);
		try {
			response = Transition.createState(state, mContext).executeState();
		} catch (Exception e) {
			SKLogger.e(this, "fail to execute " + state + " ");
		}
		Storage storage = CachingStorage.getInstance();
		mConfig = storage.loadScheduleConfig();
		if( mConfig == null){
			onEnd();
			throw new NullPointerException("null schedule config!");
		}
		
		
		//
		// Change required 29/04/2014 - always enforce that we run a closest target test FIRST for a continuous test!
		// Code very similar to this is in ManualTest.java.
		// If we don't do this, then the continuous tests always fail unless you have *first*
		// already remembered to run a manual test!
		mConfig.forManualOrContinuousTestEnsureClosestTargetIsRunAtStart(mConfig.continuous_tests);
		
		startCollectors();
		
		while(mContext.isExecutingContinuous()){
		
			executeBatch();
			try {
				state = State.SUBMIT_RESULTS_ANONYMOUS;
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
		stopCollectors();
			
		
	}
	
	private void executeBatch(){
		long batchTime = System.currentTimeMillis();
		List<JSONObject> testsResults = new ArrayList<JSONObject>();
		List<JSONObject> passiveMetrics = new ArrayList<JSONObject>();
		HashMap<String, Object> batch = new HashMap<String, Object>();
		TestExecutor te = new TestExecutor(mTestContext);
		batch.put(TestBatch.JSON_DTIME, batchTime);
		batch.put(TestBatch.JSON_RUNMANUALLY, "0");
		ConditionGroupResult tr = new ConditionGroupResult();
		for(TestDescription td: mConfig.continuous_tests){
			te.executeTest(td, tr);
		}
		for(String out: tr.results){
			List<JSONObject> theResult = com.samknows.measurement.storage.StorageTestResult.testOutput(out, te);
			if (theResult != null) {
			    testsResults.addAll(theResult);
			}
		}
		collectData();
		for(DCSData d:mListDCSData){
			passiveMetrics.addAll(d.getPassiveMetric());
			te.getResultsContainer().addMetric(d.convertToJSON());
		}
		mListDCSData.clear();
		
		long batchId = mDBHelper.insertTestBatch(new JSONObject(batch), testsResults, passiveMetrics);
		
		te.save(JSON_SUBMISSION_TYPE, batchId);
	}
	
	
	
	private void startCollectors(){
		mCollectors = new  ArrayList<BaseDataCollector>();
		mCollectors.add(new NetworkDataCollector(mContext));
		mCollectors.add(new CellTowersDataCollector(mContext));
		
		for(com.samknows.measurement.schedule.datacollection.BaseDataCollector c: mConfig.dataCollectors){
			if( c instanceof LocationDataCollector){
				mLocationDataCollector = (LocationDataCollector)c;
			}
		}
		
		for(BaseDataCollector c: mCollectors){
			c.start();
		}
		mLocationDataCollector.start(mTestContext);
	}
	
	private void stopCollectors(){
		for(BaseDataCollector c:mCollectors){
			c.stop();
		}
		if( mLocationDataCollector != null){
			mLocationDataCollector.stop(mTestContext);
		}
	}
	
	private void collectData(){
		
		mListDCSData.add(new PhoneIdentityDataCollector(mContext).collect());
		for(BaseDataCollector c: mCollectors){
			mListDCSData.addAll(c.collectPartialData());
		}
		mListDCSData.addAll(mLocationDataCollector.getPartialData());
	}
	
	private void onEnd(){
		SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
		appSettings.saveState(mPreviousState)
;	}
}
