package com.samknows.measurement.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SamKnowsResponseHandler;
import com.samknows.measurement.test.TestResultsManager;

import android.content.Context;
import android.net.ParseException;
import android.net.Uri;

public class SubmitTestResultsAnonymousAction {
	protected Context context;
	protected boolean isSuccess = false;

	public SubmitTestResultsAnonymousAction(Context _context) {
		context = _context;
	}
	
	public void execute() {
		String[] results = TestResultsManager.getJSONDataAsStringArray(context);
		List<Integer> fail = new ArrayList<Integer>();
		for (int i=0; i<results.length; i++) {
			byte[] data = results[i].getBytes();
			if (data == null) {
				SKLogger.d(SubmitTestResultsAnonymousAction.class,
						"no results to be submitted");
				break;
			}
			HttpContext httpContext = new BasicHttpContext();
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(buildUrl());
			httpPost.setEntity(new ByteArrayEntity(data));
			httpContext.setAttribute("Content-Length", data.length);
			try {
				HttpResponse httpResponse = httpClient.execute(httpPost);
				StatusLine sl = httpResponse.getStatusLine();
				isSuccess = sl.getStatusCode() == HttpStatus.SC_OK
						&& sl.getReasonPhrase().equals("OK");
				int code = sl.getStatusCode();
				SKLogger.d(this, "submiting test results to server: " + isSuccess);
			
				// http://stackoverflow.com/questions/15704715/getting-json-response-android
				HttpEntity entity = httpResponse.getEntity();
				if (entity != null) {
					try {
						String result = EntityUtils.toString(entity); 
						// e.g. {"public_ip":"89.105.103.193","submission_id":"58e80db491ee3f7a893aee307dc7f5e1"}
						SKLogger.d(this, "TODO: process response from server as string, to extract data from the JSON!: " + result);
					} catch (ParseException e) {
					} catch (IOException e) {
					}
				}
			} catch (Exception e) {
				SKLogger.e(this, "failed to submit results to server", e);
				isSuccess = false;
			}

			if (!isSuccess) {
				fail.add(i);
				TestResultsManager.clearResults(context);
				TestResultsManager.saveSumbitedLogs(context, data);
			}
		}
		TestResultsManager.clearResults(context);
		for(int i:fail){
			TestResultsManager.saveResult(context, results[i]);
		}
	}

	public String buildUrl() {
		SK2AppSettings settings = SK2AppSettings.getSK2AppSettingsInstance();
		return new Uri.Builder().scheme(settings.protocol_scheme)
				.authority(settings.getServerBaseUrl())
				.path(settings.submit_path).build().toString();
	}
}
