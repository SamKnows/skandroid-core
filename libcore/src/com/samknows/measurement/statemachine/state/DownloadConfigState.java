package com.samknows.measurement.statemachine.state;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.MainService;
import com.samknows.libcore.R;
import com.samknows.measurement.Storage;
import com.samknows.measurement.net.RequestScheduleAction;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.statemachine.StateResponseCode;

public class DownloadConfigState extends BaseState{

	public DownloadConfigState(MainService c) {
		super(c);
	}

	@Override
	public StateResponseCode executeState() {
		try {
		ScheduleConfig config = null;
		Storage storage = CachingStorage.getInstance();
		RequestScheduleAction action = new RequestScheduleAction(ctx);
		action.execute();
		if (action.isSuccess() == false) {
			SKLogger.sAssert(this.getClass(), "schedule parsing", false);
		} else {
//			if (SKConstants.USE_LOCAL_CONFIG) {
//				SKLogger.w(this.getClass(), "Using local config file");
//				config = ScheduleConfig.parseXml(ctx.getResources()
//						.openRawResource(R.raw.schedule_example));
//			} else {
			{
				config = ScheduleConfig.parseXml(action.content);
			}
			storage.saveScheduleConfig(config);
			SKLogger.d(this, "obtained config from server and saved");
			
			storage.dropExecutionQueue();
			storage.dropParamsManager();
			SK2AppSettings.getSK2AppSettingsInstance().setConfig(config);
			
			return StateResponseCode.OK;
		}
		} catch (Exception e) {
			SKLogger.e(this, "failed to download config", e);
		}
		return StateResponseCode.FAIL;
	}

}
