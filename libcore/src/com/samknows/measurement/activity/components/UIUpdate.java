package com.samknows.measurement.activity.components;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.statemachine.State;
import com.samknows.measurement.storage.StorageTestResult;
import com.samknows.tests.ClosestTarget;

/*
 * Utility class that translates several events in JSONObject in order 
 * to update the UI  
 */
public class UIUpdate {
	//TYPE entry and possible values
	public static final String JSON_TYPE = "type";
	public static final String JSON_VALUE = "value";
	public static final String JSON_MAINPROGRESS = "mainprogress";
	public static final String JSON_ACTIVATED = "activated";
	public static final String JSON_DOWNLOADED = "downloaded";
	public static final String JSON_INITTESTS = "inittests";
	public static final String JSON_COMPLETED = "completed";
	
	//type inittest
	public static final String JSON_TOTAL = "total";
	public static final String JSON_FINISHED = "finished";
	public static final String JSON_CURRENTBEST = "currentbest";
	public static final String JSON_BESTTIME = "besttime";

	public UIUpdate(){}

	public static JSONObject completed(){
		JSONObject ret = new JSONObject();
		try{
			ret.put(JSON_TYPE, JSON_COMPLETED);
		}catch(JSONException je){
			SKLogger.e(UIUpdate.class, "Error in createing JSONObject: "+ je.getMessage());
		}
		return ret;
	}

}
