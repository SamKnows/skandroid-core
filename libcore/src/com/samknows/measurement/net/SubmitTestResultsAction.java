package com.samknows.measurement.net;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.environment.PhoneIdentityDataCollector;
import com.samknows.measurement.test.TestResultsManager;
import com.samknows.measurement.util.DCSStringBuilder;

public class SubmitTestResultsAction extends SubmitTestResultsAnonymousAction{
	private String imei;
	
	public SubmitTestResultsAction(Context context) {
		super(context);
		this.imei = PhoneIdentityDataCollector.getImei(context);
	}

	public void execute() {
		byte[] data = TestResultsManager.getJSONDataAsByteArray(context);
		int code = -1;
		if (data != null) {
			HttpContext httpContext = new BasicHttpContext();
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(buildUrl());
			httpPost.addHeader("X-Unit-ID", SK2AppSettings.getInstance().getUnitId());
			httpPost.addHeader("X-Encrypted", "false");
			httpPost.addHeader("X-IMEI", imei);
			httpPost.setEntity(new ByteArrayEntity(data));
			httpContext.setAttribute("Content-Length", data.length);
			try {
				HttpResponse httpResponse = httpClient.execute(httpPost);
				StatusLine sl = httpResponse.getStatusLine();
				isSuccess = sl.getStatusCode() == HttpStatus.SC_OK && sl.getReasonPhrase().equals("OK");
				code = sl.getStatusCode();
				SKLogger.d(this, "submiting test results to server: " + isSuccess);
			} catch(Exception e){
				SKLogger.e(this, "failed to submit results to server", e);
			}
			
			if (isSuccess) {
				TestResultsManager.clearResults(context);
				TestResultsManager.saveSumbitedLogs(context, data);
			} else {
				//TestResultsManager.saveResult(context, getFailedString(code));
			}
		}
	}
	
	public String getFailedString(int code) {
		return new DCSStringBuilder().append("SUBMITRESULT").append(System.currentTimeMillis()/1000).append("FAIL").append(code).build();
	}
	
}
