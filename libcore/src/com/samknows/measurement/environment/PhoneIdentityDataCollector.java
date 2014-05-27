package com.samknows.measurement.environment;

import com.samknows.measurement.SK2AppSettings;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

public class PhoneIdentityDataCollector {
	private Context context;

	public PhoneIdentityDataCollector(Context context) {
		super();
		this.context = context;
	}
	
	public PhoneIdentityData collect() {
		PhoneIdentityData data = new PhoneIdentityData();
		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		data.time = System.currentTimeMillis();
		if(!SK2AppSettings.getSK2AppSettingsInstance().anonymous){
			data.imei = manager.getDeviceId();
			data.imsi = manager.getSubscriberId();
		}
		data.manufacturer = Build.MANUFACTURER;
		data.model = Build.MODEL;
		data.osType = "android";
		data.osVersion = Build.VERSION.SDK_INT;
		return data;
	}
	
	public static String getImei(Context ctx) {
		TelephonyManager manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		return manager.getDeviceId();
	}
}
