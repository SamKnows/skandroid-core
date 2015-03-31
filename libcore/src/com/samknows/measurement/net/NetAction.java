package com.samknows.measurement.net;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;


public class NetAction {
  static final String TAG = "NetAction";

	private String request, errorString;
	private List<Header> headers = new ArrayList<Header>();
	private HttpParams params = new BasicHttpParams();
	private String body;
	protected HttpResponse response;
	
	private boolean isPost = false;
	private boolean isSuccess = false;
	
	public void setPost(boolean isPost) {
		this.isPost = isPost;
	}
	
	public void execute() {
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, SKConstants.CONNECTION_TIMEOUT_30_SECONDS_IN_MILLIS);
		HttpConnectionParams.setSoTimeout(httpParameters, SKConstants.CONNECTION_TIMEOUT_30_SECONDS_IN_MILLIS);
		
		
		HttpClient httpclient = new DefaultHttpClient(httpParameters);
		HttpRequestBase mess = null;
		if (isPost) {
			mess = new HttpPost(request);
			if (body != null) {
				try {
					((HttpPost)mess).setEntity(new StringEntity(body));
				} catch (UnsupportedEncodingException e) {
					SKLogger.e(this, "error creating http message", e);
				}
			}
		} else {
			mess = new HttpGet(request);
		}
		mess.setParams(params);
		for (Header h : headers) {
			mess.addHeader(h);
		}
		
		try{
			Log.d(TAG, "net request: " + request);
			response = httpclient.execute(mess);
		} catch (Exception e) {
			Log.e("NetAction", "failed to execute request: " + request + ", exception=" + e.toString());
			SKLogger.sAssert(getClass(), false);
		}
		
		if (isResponseOk()) {
			onActionFinished();
		} else if (response != null && response.getStatusLine() != null){
			isSuccess = false;
			SKLogger.e(this, "failed request, response code: " + response.getStatusLine().getStatusCode());
			try {
			InputStream content = response.getEntity().getContent();
			List<String> lines = IOUtils.readLines(content);
			errorString = "";
			for (String s : lines) {
				errorString += "\n" + s;
			}
			} catch (Exception e) {}
			SKLogger.e(this, errorString);
		}
	};
	
	public String getErrorString() {
		return errorString;
	}
	
	protected boolean isResponseOk() {
		return response != null && response.getStatusLine() != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
	}
	
	public boolean isSuccess() {
		return isSuccess;
	}

	protected void onActionFinished() {isSuccess = true;}
	
	public void addHeader(Map<String,String> _headers){
		for(String name: _headers.keySet()){
			addHeader(name, _headers.get(name));
		}
	}
	
	public void addHeader(String name, String value) {
		headers.add(new BasicHeader(name, value));
	}
	
	public void addParam(String name, String value) {
		params.setParameter(name, value);
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}
	
	public void setBodyString(String body) {
		this.body = body;
	}
}
