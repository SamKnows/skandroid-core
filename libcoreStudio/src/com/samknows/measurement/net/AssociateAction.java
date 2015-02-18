package com.samknows.measurement.net;

import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.util.LoginHelper;

public class AssociateAction extends NetAction{
	
	public AssociateAction(String unitId, String imei) {
		super();
		addHeader("Authorization", "Basic " + LoginHelper.getCredentialsEncoded());
		setRequest(SK2AppSettings.getInstance().reportingServerPath + "unit/associate" + "?unit_id=" + unitId + "&mac=" + imei);
	}
}
