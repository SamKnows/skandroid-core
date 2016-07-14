package com.samknows.measurement.net;

import java.io.InputStream;

import android.content.Context;

import com.samknows.libcore.SKPorting;
import com.samknows.measurement.SK2AppSettings;

public class RequestScheduleAnonymousAction extends NetAction {
	public InputStream content;

	public RequestScheduleAnonymousAction(Context c){
		super();
		SK2AppSettings settings = SK2AppSettings.getSK2AppSettingsInstance();
		setRequest(settings.getConfigPath());
		addHeader("X-App-Version", settings.app_version_code+"");
		addHeader("X-Enterprise-ID",settings.enterprise_id);
	}
	
	@Override
	public boolean isSuccess() {
		return content != null;
	}

	@Override
	protected void onActionFinished() {
		try {
			content = response.getEntity().getContent();
		} catch (Exception e) {
			SKPorting.sAssertE(this, "failed to parse response", e);
		}
	}

}
