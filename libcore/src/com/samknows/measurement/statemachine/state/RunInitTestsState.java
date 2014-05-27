package com.samknows.measurement.statemachine.state;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.MainService;
import com.samknows.measurement.Storage;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.schedule.TestDescription;
import com.samknows.measurement.schedule.condition.ConditionGroupResult;
import com.samknows.measurement.statemachine.StateResponseCode;
import com.samknows.measurement.test.TestContext;
import com.samknows.measurement.test.TestExecutor;

public class RunInitTestsState extends BaseState{

	public RunInitTestsState(MainService c) {
		super(c);
	}

	@Override
	public StateResponseCode executeState() {
		Storage storage = CachingStorage.getInstance();
		ScheduleConfig sc = storage.loadScheduleConfig();
		if(sc  == null){
			SKLogger.e(this, "There is no schedule config");
			return StateResponseCode.FAIL;
		}
		TestContext testContext = TestContext.createBackgroundTestContext(ctx);
		TestExecutor initTestExecutor = new TestExecutor(testContext);
		initTestExecutor.startInBackGround();
		for (String type : sc.initTestTypes) {
			TestDescription td = sc.findTestForType(type);
			if (td == null) {
				SKLogger.e(this, "no init test for type: " + type);
			} else {
				ConditionGroupResult result = initTestExecutor.execute(td.id);
				if (!result.isSuccess) {
					return StateResponseCode.FAIL;
				}
			}
		}
		initTestExecutor.stop();
		initTestExecutor.save("init_test");
		storage.saveTestParamsManager(testContext.paramsManager);
		
		return StateResponseCode.OK;
	}

}
