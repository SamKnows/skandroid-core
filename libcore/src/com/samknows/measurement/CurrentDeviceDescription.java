package com.samknows.measurement;

public class CurrentDeviceDescription extends DeviceDescription{

	public CurrentDeviceDescription(String imei) {
		super(null, imei);
	}

	@Override
	public String getId() {
		return SK2AppSettings.getInstance().getUnitId();
	}
}
