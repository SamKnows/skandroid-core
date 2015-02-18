package com.samknows.measurement.environment;

import java.util.List;
import org.json.JSONObject;


public interface DCSData {
	public static final String JSON_TYPE = "type";
	public static final String JSON_TIMESTAMP = "timestamp";
	public static final String JSON_DATETIME = "datetime";
	public List<String> convert();
	public List<JSONObject> getPassiveMetric();
	public List<JSONObject> convertToJSON();
}
