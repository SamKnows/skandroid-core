package com.samknows.measurement.schedule.condition;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.samknows.measurement.util.SKDateFormat;

/*
 * It is used to add a data cap results in the conditions
 */
public class DatacapCondition {
	public static final String JSON_DATACAP = "DATACAP";
	private boolean mSuccess;
	private long mTimemillis;
	
	
	public DatacapCondition(boolean success){
		mSuccess = success;
		mTimemillis = System.currentTimeMillis();
	}
	
	public boolean isSuccess(){
		return mSuccess;
	}
	
	
	public JSONObject getCondition(){
		Map<String, Object> ret = new HashMap<String,Object>();
		ret.put(ConditionResult.JSON_TYPE, JSON_DATACAP);
		ret.put(ConditionResult.JSON_TIMESTAMP, mTimemillis/1000);
		ret.put(ConditionResult.JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(mTimemillis)));
		ret.put(ConditionResult.JSON_SUCCESS, mSuccess);
		return new JSONObject(ret);
	}
	
}
