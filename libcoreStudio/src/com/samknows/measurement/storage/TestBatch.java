package com.samknows.measurement.storage;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.samknows.libcore.SKLogger;

public class TestBatch extends JSONObject {
	
	public static final String JSON_DTIME = "dtime";
	public static final String JSON_RUNMANUALLY = "run_manually";
	private long _starttime;
	private boolean _run_manually;
	
	private Vector<JSONObject> tests = new Vector<JSONObject>();
	private Vector<JSONObject> metrics = new Vector<JSONObject>();

	public TestBatch(){
		_starttime = System.currentTimeMillis();
	}

	public TestBatch(boolean run_manually){
		_starttime = System.currentTimeMillis();
		_run_manually = run_manually;
	}

	public TestBatch(long starttime, boolean run_manually){
		_starttime = starttime;
		_run_manually = run_manually;
	}
	
	public void setRunManually(boolean manually){
		_run_manually = manually;
	}
	
	public void addTest(JSONObject test){
		tests.add(test);
	}
	
	public void addMetric(JSONObject metric){
		metrics.add(metric);
	}
	
	public void insert(Context ctx){
		JSONObject test_batch = new JSONObject();
		try{
		test_batch.put(JSON_DTIME, _starttime);
		test_batch.put(JSON_RUNMANUALLY, _run_manually ? "1" : "0");
		}catch(JSONException je){
			SKLogger.e(TestBatch.class, "Error in creating the JSONObject for creating a new test batch in the DB: " + je.getMessage());
		}
		DBHelper db = new DBHelper(ctx);
		
		db.insertTestBatch(test_batch, getTests(), getMetrics());
		
	}
	
	private JSONArray getTests(){
		JSONArray json_tests = new JSONArray();
		for(JSONObject test: tests){
			json_tests.put(test);
		}
		return json_tests;
	}
	
	private JSONArray getMetrics(){
		JSONArray json_metrics = new JSONArray();
		for(JSONObject metric: metrics){
			json_metrics.put(metric);
		}
		return json_metrics;
	}

}
