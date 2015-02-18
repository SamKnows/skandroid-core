package com.samknows.measurement;

import java.util.Date;

import org.json.JSONObject;

public class SamKnowsResponseHandler {
	public void onSuccess(JSONObject object) {};
	public void onSuccess(JSONObject object, Date date) {};
	public void onSuccess(JSONObject object, Date date, String start_date) {};
	public void onFailure(Throwable error) {};
}
