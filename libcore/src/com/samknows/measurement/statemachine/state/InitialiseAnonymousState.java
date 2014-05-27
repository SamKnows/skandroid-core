package com.samknows.measurement.statemachine.state;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.MainService;
import com.samknows.measurement.net.DCSInitAnonymousAction;
import com.samknows.measurement.statemachine.StateResponseCode;

public class InitialiseAnonymousState extends BaseState {

	public InitialiseAnonymousState(MainService c){
		super(c);
	}

	@Override
	public StateResponseCode executeState() {
		DCSInitAnonymousAction action = new DCSInitAnonymousAction();
		action.execute();
		if (action.isSuccess()) {
			SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
			SKLogger.d(this, "retrived server base url: " + action.serverBaseUrl);
			appSettings.saveServerBaseUrl(action.serverBaseUrl);
			SKLogger.d(this, "save server base url: " + action.serverBaseUrl);
			String config_path = appSettings.protocol_scheme+"://"+action.serverBaseUrl+"/"+appSettings.download_config_path;
			appSettings.saveConfigPath(config_path);
			SKLogger.d(this, "save config file url: " + config_path);
			return StateResponseCode.OK;
		}
		return StateResponseCode.FAIL;
	}

}
