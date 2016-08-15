package com.samknows.measurement;

import java.util.Calendar;
import java.util.Date;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.samknows.libcore.SKPorting;
import com.samknows.libcore.SKServiceDataCache;
import com.samknows.libcore.SKServiceDataCache.CachedValue;
import com.samknows.measurement.net.SamKnowsClient;


/*
 * Service that does all the remoting bits with the server of SamKnows
 */

public class SamKnowsLoginService {
	private static final String TAG = SamKnowsLoginService.class.getSimpleName();
	
	private SamKnowsClient client;
	private final SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance();
	private boolean FORCE = false;
	
	public static final SKServiceDataCache cache = new SKServiceDataCache();
	public static final int RECENT = 0;
	public static final int WEEK = 1;
	public static final int MONTH = 2;
	public static final int THREE_MONTHS = 3;
	public static final int SIX_MONTHS = 4;
	public static final int YEAR = 5;
	
//	public class SamKnowsBinder extends Binder {
//		public SamKnowsLoginService getService(){
//			return SamKnowsLoginService.this;
//		}
//	}
//	private final IBinder mBinder = new SamKnowsBinder();
//	@Override
//	public IBinder onBind(Intent intent){
//		return mBinder;
//	}
	
	public void createClient(String username, String password, String device){
		client = new SamKnowsClient(appSettings.getUsername(), 
				appSettings.getPassword(),
				device);
	}	
	

	private JsonHttpResponseHandler getHandler(final SamKnowsResponseHandler handler){
		return new JsonHttpResponseHandler(){

			@Override
			public void onSuccess(int statusCode, Header[] headers,
					JSONObject responseString) {
				
				JSONObject devices = responseString;
				handler.onSuccess(devices);
				
				super.onSuccess(statusCode, headers, responseString);
			}
			

			@Override
			public void onSuccess(int statusCode, Header[] headers,
					JSONArray response) {
				SKPorting.sAssert(getClass(),  false);
				super.onSuccess(statusCode, headers, response);
			}

			@Override
			public void onSuccess(int statusCode, Header[] headers,
					String responseString) {
				SKPorting.sAssert(getClass(),  false);
				super.onSuccess(statusCode, headers, responseString);
			}

			@Override
			public void onFailure(int statusCode, Header[] headers,
					Throwable throwable, JSONObject errorResponse) {
				handler.onFailure(throwable);
			}

			@Override
			public void onFailure(int statusCode, Header[] headers,
					Throwable throwable, JSONArray errorResponse) {
				handler.onFailure(throwable);
			}

			@Override
			public void onFailure(int statusCode, Header[] headers,
					String responseString, Throwable throwable) {
				handler.onFailure(throwable);
			}

		};		
	}
	
	private AsyncHttpResponseHandler getHandler(final SamKnowsResponseHandler handler, final int TYPE){
		return new AsyncHttpResponseHandler(){
			@Override
			public void onSuccess(int statusCode, Header[] headers,
					byte[] inResponseBody) {
				String response = String.valueOf(inResponseBody);
				try{
					JSONObject jsonResponse = new JSONObject(response);
					Date cachedTime = new Date();
					Calendar startDate = getStartDate(TYPE);
					String startDateStr = client.dateToString(startDate);
					cache.put(client.getDevice(), TYPE, response, startDateStr);
					handler.onSuccess(jsonResponse, cachedTime, startDateStr);
				} catch(JSONException e){
					handler.onFailure(e);
				} catch(Exception e){
					e.printStackTrace();
				}
			}
			@Override
			public void onFailure(int statusCode, Header[] headers,
					byte[] responseBody, Throwable error) {
				handler.onFailure(error);
			}
		};
	}
	
	public void checkLogin(String username, String password, SamKnowsResponseHandler handler){
		SamKnowsClient c = new SamKnowsClient(username, password);
		c.getDevices(getHandler(handler));
	}
	
	public void force(){
		FORCE = true;
	}
	
	public void unforce(){
		FORCE = false;
	}
	
	/**
	 * Get the data. This either returns stuff from the cache, if expiry is not
	 * reached yet - or returns stuff from the webservice if the cache is empty,
	 * expired or a FORCE is set.
	 * 
	 */
	public void get(int type, SamKnowsResponseHandler handler){
		CachedValue cached = cache.get(client.getDevice(), type);
		
		if (cached == null || cached.isExpired() || FORCE){
			AsyncHttpResponseHandler _handler = getHandler(handler, type);
			switch (type){
			case RECENT: client.getRecent(_handler); break;
			case WEEK: client.getWeek(_handler); break;
			case MONTH: client.getMonth(_handler); break;
			case THREE_MONTHS: client.getThreeMonths(_handler); break;
			case SIX_MONTHS: client.getSixMonths(_handler); break;
			case YEAR: client.getYear(_handler); break;
			}
		}else{
			try{
				handler.onSuccess(new JSONObject(cached.responce), new Date(cached.cachedTime), cached.cachedStart);
			}catch (JSONException e){
				handler.onFailure(e);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Get the start date for the data. I'm using this to fill in the blanks if
	 * not enough date is available.
	 */
	private Calendar getStartDate(int TYPE){
		Calendar date = null;
		switch(TYPE){
		case RECENT: date = client.getStartDate(1); break;
		case WEEK: date = client.getStartDate(7); break;
		case MONTH: date = client.getStartDate(30); break;
		case THREE_MONTHS: date = client.getStartDate(3*30); break;
		case SIX_MONTHS: date = client.getStartDate(6*30); break;
		case YEAR: date = client.getStartDate(365); break;		
		}
		return date;
	}
}
