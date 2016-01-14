package com.samknows.measurement.schedule.condition;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.samknows.libcore.SKConstants;
import com.samknows.measurement.util.DCSStringBuilder;
import com.samknows.measurement.util.SKDateFormat;

public class ConditionResult {
	public boolean isSuccess;
	public String outString;
	public static final String JSON_TYPE = "type";
	public static final String JSON_TIMESTAMP = "timestamp";
	public static final String JSON_DATETIME = "datetime";
	public static final String JSON_SUCCESS = "success";
	public static final String JSON_CRASH   = "crash";
	private String[] json_fields = null;
	public JSONObject outJSON;
	private boolean failQuiet = false;
	private String type = "";
	
	public ConditionResult(boolean isSuccess) {
		super();
		this.isSuccess = isSuccess;
	}
	
	public ConditionResult(boolean isSuccess, boolean failQuiet){
		super();
		this.isSuccess = isSuccess;
		this.failQuiet = failQuiet;
	}
	
	public ConditionResult(boolean isSuccess, String outString) {
		super();
		this.isSuccess = isSuccess;
		this.outString = outString;
	}

	public void generateOut(String id, Object... data) {
		type = id;
		DCSStringBuilder b = new DCSStringBuilder();
		b.append(id);
		long time = System.currentTimeMillis();
		b.append(time);
		if (isSuccess) {
			b.append(SKConstants.RESULT_OK);
		} else {
			b.append(SKConstants.RESULT_FAIL);
		}
		
		for (Object s : data) {
			if (s != null) {
				b.append(s.toString());
			}
		}
		
		outString = b.build();
		
		Map<String, Object> j = new HashMap<>();
		j.put(JSON_TYPE, id);
		j.put(JSON_TIMESTAMP, time/1000);
		j.put(JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(time)));
		j.put(JSON_SUCCESS, isSuccess);
		if(json_fields != null && json_fields.length == data.length){
			for(int i =0; i<json_fields.length ; i++){
				j.put(json_fields[i], data[i]);
				}
		}
		outJSON = new JSONObject(j);
		
	}

	public String getType(){return type;}

	public boolean isFailQuiet() {
		return failQuiet;
	}

	public void setFailQuiet(boolean failQuiet) {
		this.failQuiet = failQuiet;
	}
	
	public void setJSONFields(String... fields){
		json_fields= fields ;
	}
}
