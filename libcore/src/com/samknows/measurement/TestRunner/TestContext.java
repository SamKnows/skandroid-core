package com.samknows.measurement.TestRunner;

import android.content.Context;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.Storage;
import com.samknows.measurement.TestParamsManager;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.environment.BaseDataCollector;
import com.samknows.measurement.environment.LocationDataCollector;
import com.samknows.measurement.storage.ResultsContainer;

public class TestContext {
	private Context ctx;
	public ScheduleConfig config;
	public TestParamsManager paramsManager;
	
	public ResultsContainer resultsContainer = null;
	
	private static TestContext create(Context ctx, boolean PbIsManualTest) { 
		Storage storage = CachingStorage.getInstance();
		ScheduleConfig config = storage.loadScheduleConfig();
		if (config == null) {
			throw new NullPointerException("null schedule config!");
		}
		
		TestParamsManager paramsManager = storage.loadParamsManager();
		if (paramsManager == null) {
			paramsManager = new TestParamsManager();
		}
		return new TestContext(ctx, config, paramsManager, PbIsManualTest);
	}
	
	public static TestContext createManualTestContext(Context ctx) {
		boolean bIsManualTestTrue = true;
		return create(ctx, bIsManualTestTrue);
	}

	public static TestContext createBackgroundTestContext(Context ctx) {
		boolean bIsManualTestFalse = false;
		return create(ctx, bIsManualTestFalse);
	}
	
	private TestContext(Context ctx, ScheduleConfig config, TestParamsManager manager, boolean PbIsManualTest) {
		super();
		this.ctx = ctx;
		this.config = config;
		this.paramsManager = manager;
		this.mbIsManualTest = PbIsManualTest;
	}
	
    private boolean mbIsManualTest = false;	
    public boolean getIsManualTest() {
    	return mbIsManualTest;
    }
	
	
	public Object getSystemService(String name) {
		return ctx.getSystemService(name);
	}

	public String getString(int rid) {
		return ctx.getString(rid);
	}
	
	public Context getContext() {
		return ctx;
	}
	
	public LocationDataCollector findLocationDataCollector() {
		for (BaseDataCollector d : config.dataCollectors) {
			if (d instanceof LocationDataCollector) return (LocationDataCollector) d;
		}
		return null;
	}
}
