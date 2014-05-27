package com.samknows.measurement.schedule.condition;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class ConditionGroupResult extends ConditionResult{
	public List<String> results = new ArrayList<String>();
	public List<JSONObject> json_results = new ArrayList<JSONObject>();
	public ConditionGroupResult() {
		super(true);
		//setFailQuiet(true); //TODO refactor FailQuiet functionality
	}

	public void add(ConditionResult cr) {
		if (cr.outString != null && !cr.outString.equals("")) {
			results.add(cr.outString);
		}
		
		if(cr.outJSON != null){
			json_results.add(cr.outJSON);
		}
		
		if (!cr.isSuccess) {
			isSuccess = false;
			if (!cr.isFailQuiet()) {
				setFailQuiet(false);
			}
		}
	}
	
	public void add(ConditionGroupResult cr) {
		results.addAll(cr.results);
		json_results.addAll(cr.json_results);
		if (!cr.isSuccess) {
			isSuccess = false;
			if (!cr.isFailQuiet()) {
				setFailQuiet(false);
			}
		}
	}

	public void addTestString(String outputString) {
		results.add(outputString);
	}
	
	public void addTestString(List<String> outputString) {
		results.addAll(outputString);
	}

}
