package com.samknows.measurement.storage;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;

public class Conversions {
	//test id to test string conversion
	public static final int UPLOAD_TEST_ID = 0;
	public static final int DOWNLOAD_TEST_ID = 1;
	public static final int LATENCY_TEST_ID = 2;
	public static final int PACKETLOSS_TEST_ID = 3;
	public static final int JITTER_TEST_ID = 4;
	public static final String UPLOAD_TEST_STRING = "upload";
	public static final String DOWNLOAD_TEST_STRING = "download";
	public static final String LATENCY_TEST_STRING = "latency";
	public static final String PACKETLOSS_TEST_STRING = "packet loss";
	public static final String JITTER_TEST_STRING = "jitter";
	
	public static final String DOWNSTREAMTHROUGHPUT = "JHTTPGET"; 
	public static final String UPSTREAMTHROUGHPUT = "JHTTPPOST";
	public static final String LATENCY = "JUDPLATENCY";
	public static final String JITTER = "JUDPJITTER";
	
	
	
	public static String testIdToString(int test_id){
		switch(test_id){
		case UPLOAD_TEST_ID: return UPLOAD_TEST_STRING;
		case DOWNLOAD_TEST_ID: return DOWNLOAD_TEST_STRING;
		case LATENCY_TEST_ID: return LATENCY_TEST_STRING;
		case PACKETLOSS_TEST_ID: return PACKETLOSS_TEST_STRING;
		case JITTER_TEST_ID: return JITTER_TEST_STRING;
		}
		return "";
	}
	
	public static int testStringToId(String testString){
		int ret = -1;
		if(UPLOAD_TEST_STRING.equals(testString)){
			ret = UPLOAD_TEST_ID;
		}else if(DOWNLOAD_TEST_STRING.equals(testString)){
			ret = DOWNLOAD_TEST_ID;
		}else if(LATENCY_TEST_STRING.equals(testString)){
			ret = LATENCY_TEST_ID;
		}else if(PACKETLOSS_TEST_STRING.equals(testString)){
			ret = PACKETLOSS_TEST_ID;
		}else if(JITTER_TEST_STRING.equals(testString)){
			ret = JITTER_TEST_ID;
		}
		return ret;
	}
	
	public static String testMetricToString(int test_id, double value){
		String ret = "";
		switch(test_id){
		case UPLOAD_TEST_ID:
		case DOWNLOAD_TEST_ID:
			ret = throughputToString(value);
			break;
		case LATENCY_TEST_ID:
		case JITTER_TEST_ID:
			ret = timeToString(value);
			break;
		case PACKETLOSS_TEST_ID:
			ret = String.format("%.2f %%", value);
			break;
		}
		return ret;
	}
	
	public static String throughputToString(double value){
		String ret = "";
		if(value < 1000){
			ret = String.format("%.0f bps", value);
		}else if(value < 1000000 ){
			ret = String.format("%.2f Kbps", (double)(value/1000.0));
		}else{
			ret = String.format("%.2f Mbps", (double)(value/1000000.0));
		}
		return ret;
	}
	
	private static String timeToString(double value){
		String ret = "";
		if(value < 1000){
			ret = String.format("%.0f microseconds", value); 
		}else if(value < 1000000 ){
			ret = String.format("%.0f ms", value);
		}else {
			ret = String.format("%.2f s", value);
		}
		return ret;
	}
	
	
	//Method for converting a testoutput string in a JSNObject suitable for the database
	public static List<JSONObject> testToJSON(String data){
		return testToJSON(data.split(SKConstants.RESULT_LINE_SEPARATOR));
	}
	
	public static List<JSONObject> testToJSON(String[] data){
		List<JSONObject> ret = new ArrayList<JSONObject>();
		String test_id = data[0];
		if(test_id.startsWith(DOWNSTREAMTHROUGHPUT)){
			ret.add(convertThroughputTest(DOWNLOAD_TEST_STRING, data));
		}else if(test_id.startsWith(UPSTREAMTHROUGHPUT)){
			ret.add(convertThroughputTest(DOWNLOAD_TEST_STRING, data));
		}else if(test_id.startsWith(LATENCY)){
			
		}
		
		return ret;
	}
	
	private static JSONObject convertThroughputTest(String test, String[] data){
		JSONObject ret = new JSONObject();
		
		return ret;
	}
	
	private static void put(JSONObject obj, String key, String value){
			try{
				obj.put(key, value);
			}catch(JSONException je){
				SKLogger.e(StorageTestResult.class, "JSONException "+ key +" "+ value);
			}
	}
	
	
}
