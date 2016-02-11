package com.samknows.measurement.schedule.condition;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class ConditionGroupResult extends ConditionResult{

	private List<JSONObject> json_results = new ArrayList<>();
  public List<JSONObject> getJsonResultArray() {
    return json_results;
  }

	public ConditionGroupResult() {
		super(true);
		//setFailQuiet(true); //TODO refactor FailQuiet functionality
	}

	public void add(ConditionResult cr) {

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
		json_results.addAll(cr.json_results);
		if (!cr.isSuccess) {
			isSuccess = false;
			if (!cr.isFailQuiet()) {
				setFailQuiet(false);
			}
		}
	}
}
