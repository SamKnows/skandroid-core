package com.samknows.measurement.statemachine.state;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.MainService;
import com.samknows.measurement.net.GetVersionNumberAction;
import com.samknows.measurement.statemachine.StateResponseCode;

public class CheckConfigVersionState extends BaseState{

	public CheckConfigVersionState(MainService c) {
		super(c);
	}

	@Override
	public StateResponseCode executeState() {
		String currentVersion = SK2AppSettings.getInstance().getConfigVersion();
		SKLogger.d(this, "current config version: " + currentVersion);
		
		GetVersionNumberAction action = new GetVersionNumberAction(ctx,	currentVersion);
		action.execute();
		if (action.isSuccess()) {
			SKLogger.d(this, "obtained config version from server: "
					+ action.version);
			if (action.version.equals(currentVersion)) {
				return StateResponseCode.OK;
			} else {
				SK2AppSettings.getInstance().saveConfigVersion(action.version); 
				SK2AppSettings.getInstance().saveConfigPath(action.path);
				return StateResponseCode.NOT_OK; //we will download config at next state
			}
		} else {
			return StateResponseCode.FAIL;
		}
	}

}
