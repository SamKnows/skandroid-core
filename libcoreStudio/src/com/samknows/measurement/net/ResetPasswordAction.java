package com.samknows.measurement.net;

import com.samknows.measurement.SK2AppSettings;

public class ResetPasswordAction extends NetAction{

	public ResetPasswordAction(String name, String pass, String code) {
		super();
		setRequest(SK2AppSettings.getInstance().reportingServerPath + "user/change_password?email="+name+"&secret="+code+"&password=" + pass);
	}
}
