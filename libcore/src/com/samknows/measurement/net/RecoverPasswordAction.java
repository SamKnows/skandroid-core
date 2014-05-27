package com.samknows.measurement.net;

import com.samknows.measurement.SK2AppSettings;

public class RecoverPasswordAction extends NetAction{

	public RecoverPasswordAction(String name, String pass) {
		super();
		setRequest(SK2AppSettings.getInstance().reportingServerPath + "user/request_secret?email=" + name);
	}
}
