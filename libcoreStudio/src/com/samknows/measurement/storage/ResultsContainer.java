package com.samknows.measurement.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.schedule.TestDescription;
import com.samknows.measurement.schedule.condition.Condition;
import com.samknows.tests.Param;
import com.samknows.tests.TestFactory;

public class ResultsContainer {

	public static final String JSON_TESTS = "tests";
	public static final String JSON_CONDITIONS = "conditions";
	public static final String JSON_METRICS = "metrics";
	public static final String JSON_REQUESTED_TESTS = "requested_tests";
	public static final String JSON_CONDITION_BREACHES = "condition_breaches";
	private List<JSONObject> mTests = new ArrayList<JSONObject>();
	private List<JSONObject> mConditions = new ArrayList<JSONObject>();
	private List<JSONObject> mMetrics = new ArrayList<JSONObject>();
	private Map<String, Object> mExtra = new LinkedHashMap<String, Object>();
	private JSONArray mRequestedTests = new JSONArray();
	private Map<String, String> mConditionBreaches = new HashMap<String,String>();
	
	public ResultsContainer(){
		
	}
	
	public JSONObject getJSONArrayForTestId(String testId) {
		for (JSONObject test : mTests) {
			try {
				if (test.get("type").equals(testId)) {
					return test;
				}
			} catch (JSONException e) {
			}
		}
		
		return null;
	}
	
	public ResultsContainer(Map<String, String> extra){
		mExtra.putAll(extra);
	}
	
	public void addTest(JSONObject test){
		mTests.add(test);
	}
	
	public void addTest(List<JSONObject> tests){
		mTests.addAll(tests);
	}
	
	public void addCondition(JSONObject condition){
		mConditions.add(condition);
	}
	
	public void addCondition(List<JSONObject> conditions){
		mConditions.addAll(conditions);
	}
	
	public void addMetric(JSONObject metric){
		mMetrics.add(metric);
	}
	
	public void addMetric(List<JSONObject> metrics){
		mMetrics.addAll(metrics);
	}

/*	private int getNumberOfThreadsForHttpTestDescription(
			TestDescription testDescription) {
		int numberOfThreads = 1;
		for (Param curr : testDescription.params) {
			String paramName = curr.getName();
			// The parameter value is "numberofthreads", the internal value is "numberOfThreads"
			if (paramName.equalsIgnoreCase(TestFactory.NTHREADS)) {
				String paramValue = curr.getValue();
				numberOfThreads = Integer.parseInt(paramValue);
			}
		}
		return numberOfThreads;
	}*/
	
	public void addFailedCondition(String condition) {
		mConditionBreaches.put(condition, condition);
	}
	
	public void addFailedCondition(Condition condition) {
		
		// "condition_breach"

		// get e.g. "NETACTIVITY"
		String theString = condition.getConditionStringForReportingFailedCondition();
		// Using a map, prevents duplicates!
		mConditionBreaches.put(theString, theString);
	}
	
	public void addRequestedTest(TestDescription td){
	
		mRequestedTests.put(td.getTypeString());
	}

	public void addExtra(String key, String value){
		mExtra.put(key, value);
	}
	
	public JSONObject getJSON(){
		mExtra.putAll(SK2AppSettings.getSK2AppSettingsInstance().getJSONExtra());
		JSONObject ret = new JSONObject(mExtra);
		JSONArray tests = new JSONArray();
		JSONArray conditions = new JSONArray();
		JSONArray metrics = new JSONArray();
		
		for(JSONObject t: mTests){
			tests.put(t);
		}
		for(JSONObject c: mConditions){
			conditions.put(c);
		}
		for(JSONObject m: mMetrics){
			metrics.put(m);
		}
		
		try{
			if(mRequestedTests.length() > 0){
				ret.put(JSON_REQUESTED_TESTS, mRequestedTests);
			}
			if (mConditionBreaches.size() > 0) {
            	JSONArray conditionBreaches = new JSONArray();
            
                Iterator<Entry<String, String>> it = mConditionBreaches.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String,String> pairs = it.next();
                    conditionBreaches.put(pairs.getKey());
                }
            	
				ret.put(JSON_CONDITION_BREACHES, conditionBreaches);
			}
			ret.put(JSON_TESTS, tests);
			ret.put(JSON_METRICS, metrics);
			ret.put(JSON_CONDITIONS, conditions);
		}catch(JSONException je){
			SKLogger.e(this, "Error in creating a JSONObject: " + je.getMessage() );
			ret = null;
		}
		return ret;
	}
	
}

