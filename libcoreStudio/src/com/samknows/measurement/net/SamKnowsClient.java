package com.samknows.measurement.net;

import java.util.Calendar;

import org.apache.http.client.HttpClient;
import org.apache.http.params.HttpParams;

import android.text.format.DateFormat;
import android.util.Base64;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.samknows.measurement.SK2AppSettings;

public class SamKnowsClient{
	private static final String TAG = Connection.class.getSimpleName();
	
	private String username;
	private String password;
	private String device;
	
	private AsyncHttpClient client = new AsyncHttpClient();
	private HttpClient httpClient = client.getHttpClient();
	private HttpParams httpParams = httpClient.getParams();
	
	public final String ALLOWED_UNITS = SK2AppSettings.getInstance().reportingServerPath + "user/getAllowedUnits";
	public final String REPORTS = SK2AppSettings.getInstance().reportingServerPath + "reports/getResults";
	
	public SamKnowsClient(String _username, String _password){
		username = _username;
		password = _password;
		setParams();
		
	}
	
	public SamKnowsClient(String _username, String _password, String _device){
		username = _username;
		password = _password;
		device = _device;
		setParams();
	}
	
	private void setParams(){
		client.addHeader("Authorization", "Basic " + getCredentials());
	}
	
	private String getCredentials(){
		return Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
	}
	
	public void getDevices(AsyncHttpResponseHandler responseHandler){
		client.get(ALLOWED_UNITS, responseHandler);
	}
	
	public void getRecent(AsyncHttpResponseHandler responseHandler){
		client.get(recent(), responseHandler);
	}
	
	public void getWeek(AsyncHttpResponseHandler responseHandler){
		client.get(week(), responseHandler);
	}
	
	public void getMonth(AsyncHttpResponseHandler responseHandler){
		client.get(month(), responseHandler);
	}
	
	public void getThreeMonths(AsyncHttpResponseHandler responseHandler){
		client.get(three_months(), responseHandler);
	}
	
	public void getSixMonths(AsyncHttpResponseHandler responseHandler){
		client.get(six_months(), responseHandler);
	}
	
	public void getYear(AsyncHttpResponseHandler responseHandler){
		client.get(year(), responseHandler);
	}
	
	
	// URL Assemblage
	private String url(){
		return REPORTS + "?unit_id=" + device + "&tests=downstream_mt,upstream_mt,latency,packetloss,voip_jitter"; 
	}
	
	public String dateToString(Calendar date){
		CharSequence format = "yyyy-MM-dd";
		CharSequence dateStr = DateFormat.format(format, date);
		return "" + dateStr;
	}
	
	private String datesToString(Calendar start, Calendar end){
		return "&start_date=" + dateToString(start) + "&end_date=" + dateToString(end);
	}
	
	public Calendar getStartDate(int difference){
		Calendar end = Calendar.getInstance();
		Calendar start = (Calendar) end.clone();
		start.add(Calendar.DATE, - difference);
		return start;
	}
	
	public String dates(int difference){		
		return datesToString(getStartDate(difference), Calendar.getInstance());
	}
	
	private String recent(){
		return url();
	}
	
	private String week(){
		return url() + dates(7);		
	}
	
	private String month(){
		return url() + dates(30);
	}
	
	private String three_months(){
		return url() + dates(3 * 30);
	}
	
	private String six_months(){
		return url() + dates(6 * 30);
	}
	
	private String year(){
		return url() + dates(365);
	}

	public String getDevice() {
		return device;
	}
	
	
}