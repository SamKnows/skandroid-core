package com.samknows.measurement.environment;

import java.util.List;
import org.json.JSONObject;


public interface DCSData {
	String JSON_TYPE = "type";
	String JSON_TIMESTAMP = "timestamp";
	String JSON_DATETIME = "datetime";
	List<String> convert();
	List<JSONObject> getPassiveMetric();
	List<JSONObject> convertToJSON();
}
