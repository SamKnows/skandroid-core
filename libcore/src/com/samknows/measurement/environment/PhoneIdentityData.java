package com.samknows.measurement.environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.storage.PassiveMetric;
import com.samknows.measurement.util.DCSStringBuilder;
import com.samknows.measurement.util.SKDateFormat;

public class PhoneIdentityData implements DCSData{
	public static final String JSON_TYPE_PHONE_IDENTITY = "phone_identity";
	public static final String JSON_IMEI = "imei";
	public static final String JSON_IMSI = "imsi";
	public static final String JSON_MANUFACTURER = "manufacturer";
	public static final String JSON_MODEL = "model";
	public static final String JSON_OSTYPE = "os_type";
	public static final String JSON_OSVERSION = "os_version";
	
	
	public String imei;
	public long time;
	public String imsi;
	public String manufacturer;
	public String model;
	public String osType;
	public int osVersion;
	
	@Override
	public List<String> convert() {
		List<String> list = new ArrayList<String>();
		
		DCSStringBuilder builder = new DCSStringBuilder();
		builder.append("PHONEIDENTITY");
		builder.append(time/1000);
		builder.append(imei);
		builder.append(imsi);
		builder.append(manufacturer);
		builder.append(model);
		builder.append(osType);
		builder.append(osVersion);
		list.add(builder.build());
		
		return list;
	}

	@Override
	public List<JSONObject> getPassiveMetric() {
		List<JSONObject> ret = new ArrayList<JSONObject>();
		long time = System.currentTimeMillis();
		if(!SK2AppSettings.getSK2AppSettingsInstance().anonymous){
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.IMEI, time, imei));
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.IMSI, time, imsi));
		}
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.MANUFACTOR, time, manufacturer));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.MODEL, time, model));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.OSTYPE, time, osType ));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.OSVERSION, time, osVersion));
		return ret;
	}
	
	@Override
	public List<JSONObject> convertToJSON() {
		List<JSONObject> ret = new ArrayList<JSONObject>();
		Map<String, Object> j = new HashMap<String, Object>();
		j.put(JSON_TYPE, JSON_TYPE_PHONE_IDENTITY);
		collectSensitiveData(j);
		j.put(JSON_TIMESTAMP, time/1000);
		j.put(JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(time)));
		j.put(JSON_MANUFACTURER, manufacturer);
		j.put(JSON_MODEL, model);
		j.put(JSON_OSTYPE, osType);
		j.put(JSON_OSVERSION, osVersion);
		ret.add(new JSONObject(j));
		
		return ret;
	}
	
	private void collectSensitiveData(Map<String, Object> j){
		if(!SK2AppSettings.getSK2AppSettingsInstance().anonymous){
			j.put(JSON_IMEI, imei);
			j.put(JSON_IMSI, imsi);
		}
		
	}
}
