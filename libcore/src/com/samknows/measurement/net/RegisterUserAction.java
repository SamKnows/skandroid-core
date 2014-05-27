package com.samknows.measurement.net;

import com.samknows.measurement.SK2AppSettings;

public class RegisterUserAction extends NetAction{

	public RegisterUserAction(String name, String pass) {
		super();
//		setPost(true);
		setRequest(SK2AppSettings.getInstance().reportingServerPath + "user/create?" + "email=" + name + "&password=" + pass);
	}
}
