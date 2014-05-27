package com.samknows.measurement.net;

import android.content.Context;

import com.samknows.measurement.SK2AppSettings;


public class RequestScheduleAction extends RequestScheduleAnonymousAction {
	//public InputStream content;
	public RequestScheduleAction(Context c) {
		super(c);
		addHeader("X-Unit-ID", SK2AppSettings.getInstance().getUnitId());
	}

}
