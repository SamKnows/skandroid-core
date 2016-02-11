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

public class ResultsContainer {

	public static final String JSON_TESTS = "tests";
	public static final String JSON_CONDITIONS = "conditions";
	public static final String JSON_METRICS = "metrics";
	public static final String JSON_REQUESTED_TESTS = "requested_tests";
	public static final String JSON_CONDITION_BREACHES = "condition_breaches";
	private List<JSONObject> mTests = new ArrayList<>();
	private List<JSONObject> mConditions = new ArrayList<>();
	private List<JSONObject> mMetrics = new ArrayList<>();
	private Map<String, Object> mExtra = new LinkedHashMap<>();
	private JSONArray mRequestedTests = new JSONArray();
	private Map<String, String> mConditionBreaches = new HashMap<>();
	
	public ResultsContainer(){
	}

	public void addTestJSONObject(JSONObject test){
		mTests.add(test);
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
          Entry<String, String> pairs = it.next();
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

