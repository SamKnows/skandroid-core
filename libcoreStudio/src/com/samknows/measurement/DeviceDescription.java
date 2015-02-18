package com.samknows.measurement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.samknows.libcore.SKLogger;

public class DeviceDescription implements Serializable{
	private String id;
	private String mac;
	
	public DeviceDescription(String id, String mac) {
		super();
		this.id = id;
		this.mac = mac;
	}

	public static List<DeviceDescription> parce(String json) {
		List<DeviceDescription> result = new ArrayList<DeviceDescription>();
		try {
			if (json != null) {
				JSONArray array = new JSONArray(json);
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObj = array.getJSONObject(i);
					DeviceDescription dev = new DeviceDescription(jsonObj.getString("id"), jsonObj.getString("mac"));
					result.add(dev);
				}
			}
		} catch (Exception e) {
			SKLogger.e(DeviceDescription.class, "failed to parce devices json: " + json, e);
		}
		return result;
	}
	
	public boolean isCurrentDevice(String imei) {
		return imei.equals(getMac());
	}

	public String getId() {
		return id;
	}

	public String getMac() {
		return mac;
	}
}
