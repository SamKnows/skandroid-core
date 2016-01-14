package com.samknows.measurement.statemachine.state;


import android.content.Context;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.Storage;
import com.samknows.measurement.statemachine.StateResponseCode;
import com.samknows.measurement.test.ScheduledTestExecutionQueue;
import com.samknows.measurement.test.TestContext;
import com.samknows.measurement.util.OtherUtils;

public class ExecuteScheduledTestQueueState extends BaseState{

	private long accumulatedTestBytes = 0L;
	
	public ExecuteScheduledTestQueueState(Context ctx) {
		super(ctx);
	}

	@Override
	public StateResponseCode executeState() {
		SKLogger.sAssert(getClass(),  (accumulatedTestBytes == 0L));
		
		if(!SKApplication.getAppInstance().getIsBackgroundTestingEnabledInUserPreferences()){
			return StateResponseCode.OK;
		}
		
		Storage storage = CachingStorage.getInstance();
		TestContext tc = TestContext.createBackgroundTestContext(ctx);
		
    ScheduledTestExecutionQueue queue = storage.loadQueue();

    if (queue == null) {
      Log.w(getClass().getName(), "fail to load execution queue, creating new...");
      queue = new ScheduledTestExecutionQueue(tc);
    } else {
      queue.setTestContext(tc);
    }

		long testRun = queue.executeReturnRescheduleDurationMilliseconds();
		accumulatedTestBytes = queue.getAccumulatedTestBytes();

    storage.saveExecutionQueue(queue);
    storage.saveTestParamsManager(tc.paramsManager);
		
		// schedule from Queue or from config refresh
		OtherUtils.reschedule(ctx, testRun);
		
		return StateResponseCode.OK;
	}

	@Override
	public long getAccumulatedTestBytes() {
		return accumulatedTestBytes;
	}
}
