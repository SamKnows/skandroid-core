package com.samknows.tests;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.samknows.libcore.SKLogger;

//Base class for the tests 
abstract public class Test implements Runnable {
	public static final int CONNECTIONTIMEOUT = 10000; // 10 seconds connection
														// timeout
	public static final int READTIMEOUT = 10000; // 10 seconds read timeout
	public enum POSITION {
		INTERNAL, TRIGGER, EXTERNAL
	}

	public enum TEST_STRING {
		DOWNLOAD_SINGLE_SUCCESS, DOWNLOAD_MULTI_SUCCESS, DOWNLOAD_FAILED, UPLOAD_SINGLE_SUCCESS, UPLOAD_MULTI_SUCCESS, UPLOAD_FAILED, LATENCY_SUCCESS, LATENCY_FAILED, NONE
	}

	public static final String JSON_TYPE = "type";
	public static final String JSON_TIMESTAMP = "timestamp";
	public static final String JSON_DATETIME = "datetime";
	public static final String JSON_TARGET = "target";
	public static final String JSON_TARGET_IPADDRESS = "target_ipaddress";
	public static final String JSON_SUCCESS = "success";
	
	
	public static final String TYPE = "type";
	public static final String DTIME = "datetime";
	public static final String TARGET = "target";
	public static final String SUCCESS = "success";
	public static final String METRIC = "metric";

	protected HashMap<String, String> testDigest = new HashMap<String, String>();

	protected void setDigestType(String type){
		testDigest.put(TYPE, type);
	}
	protected void setDigestDatetime(long dtime){
		testDigest.put(DTIME, dtime+"");
	}
	protected void setDigestTarget(String target){
		testDigest.put(TARGET, target);
	}
	protected void setDigestMetric(double metric){
		testDigest.put(METRIC, metric+"");
	}
	protected void setDigestSuccess(boolean succ){
		testDigest.put(SUCCESS, succ+"");
	}
	public HashMap<String, String> getTestDigest() {
		return testDigest;
	}

	public class HumanReadable {
		public TEST_STRING testString;
		public String[] values;

		public String getString(String locale) {
			switch (testString) {
			case DOWNLOAD_SINGLE_SUCCESS:
			case DOWNLOAD_MULTI_SUCCESS:
			case UPLOAD_SINGLE_SUCCESS:
			case UPLOAD_MULTI_SUCCESS:
				return String.format(locale, values[0]);
			case LATENCY_SUCCESS:
				return String.format(locale, values[0], values[1], values[2]);
			case DOWNLOAD_FAILED:
			case UPLOAD_FAILED:
			case LATENCY_FAILED:
				return locale;
			case NONE:
				return "";
			}
			return "";
		}

		public HashMap<String, String> getValues() {
			HashMap<String, String> ret = new HashMap<String, String>();
			switch (testString) {
			case DOWNLOAD_SINGLE_SUCCESS:
			case DOWNLOAD_MULTI_SUCCESS:
				ret.put("downspeed", values[0]);
				break;
			case UPLOAD_SINGLE_SUCCESS:
			case UPLOAD_MULTI_SUCCESS:
				ret.put("upspeed", values[0]);
				break;
			case LATENCY_SUCCESS:
				ret.put("latency", values[0]);
				ret.put("packetloss", values[1]);
				ret.put("jitter", values[2]);
				break;
			default:
			}
			return ret;
		}
	}

	public static long unixTimeStamp() {
		return System.currentTimeMillis() / 1000;
	}

	public static boolean paramMatch(String param, String value) {
		return param.equalsIgnoreCase(value);
	}

	protected static boolean between(int x, int a, int b) {
		if (x >= a && x <= b) {
			return true;
		}
		return false;
	}

	public enum STATUS {
		WAITING, RUNNING, DONE
	}

	String runMessage = "";
	String doneMessage = "";
	String[] outputFields = null;

	STATUS status;

	String result;

	String targetServer;

	boolean finished;
	
	private JSONObject json_output = null;
	

	public Test() {
		status = STATUS.WAITING;
	}

	public void setRunMessage(String m) {
		runMessage = m;
	}

	public String getRunMessage() {
		return runMessage;
	}

	public void setDoneMessage(String m) {
		doneMessage = m;
	}

	public String getDoneMessage() {
		return doneMessage;
	}

	public abstract void execute();

	public synchronized void start() {
		status = STATUS.RUNNING;
	}

	protected synchronized void finish() {
		status = STATUS.DONE;
	}

	public String getOutputField(int i) {
		
		if (i >= outputFields.length) {
			SKLogger.sAssert(getClass(), false);
			return "";
		}
		
		String result = outputFields[i];
		
		if (result == null) {
			SKLogger.sAssert(getClass(), false);
			return "";
		}
		
		return result;
	}

	public String[] getOutputFields() {
		return outputFields;
	}

	public String getOutputString() {
		if (null == outputFields) {
			return "";
		}
		return getOutputString(";");
	}
	
	abstract public String getStringID();

	public String getOutputString(String d) {
		String ret = "";
		if (outputFields == null) {
			return ret;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < outputFields.length - 1; i++) {
			sb.append(outputFields[i] + d);
		}
		sb.append(outputFields[outputFields.length - 1]);
		return sb.toString();
	}

//	public synchronized String getResult() {
//		String ret;
//		if (result == null || result.equals("")) {
//			if (finished) {
//				ret = "Done";
//			} else {
//				ret = "Waiting";
//			}
//		} else
//			ret = result;
//		return ret;
//	}

	public synchronized STATUS getStatus() {
		return status;
	}

	public synchronized boolean isFinished() {
		return status == STATUS.DONE;
	}

	abstract public boolean isSuccessful();

	abstract public void run();

	
	
	protected void setOutput(String[] o) {
		outputFields = o;
	}

	protected void setJSONOutput(Map<String, Object> output){
		json_output = new JSONObject(output);
	}

	abstract public String getHumanReadableResult();

	abstract public HumanReadable getHumanReadable();
	
	public JSONObject getJSONResult(){
		return json_output;
	}
	
	abstract public boolean isProgressAvailable();

	abstract public int getProgress(); // from 0 to 100

	// Checks if the test is ready to run
	abstract public boolean isReady();

	// The test has to provide the amount of data used
	abstract public int getNetUsage();

	// If the test fails the return string should contain the reason
	// If it succeeds return empty String
	public String getError() {
		String ret = "";
		synchronized (errorString) {
			ret = errorString;
		}
		return ret;
	}

	protected boolean setErrorIfEmpty(String error, Exception e) {
		String exErr = e.getMessage() == null ? "No expetion message" : e
				.getMessage();
		return setErrorIfEmpty(error + " " + exErr);
	}

	protected boolean setErrorIfEmpty(String error) {
		boolean ret = false;
		synchronized (this) {
			if (errorString.equals("")) {
				errorString = error;
				ret = true;
			}
		}
		return ret;
	}

	protected void setError(String error) {
		synchronized (this) {
			errorString = error;
		}
	}

	private String errorString = "";

}
