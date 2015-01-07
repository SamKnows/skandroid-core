package com.samknows.tests;

import java.util.HashMap;
import java.util.Map;
//import java.util.Vector;

import org.json.JSONObject;

import com.samknows.libcore.SKLogger;

//Base class for the tests 

/*	 
	public static final String TESTTYPE = "testType";
public static final String TYPE = "type";
public static final String DTIME = "datetime";
public static final String TARGET = "target";
public static final String SUCCESS = "success";
public static final String METRIC = "metric";

public enum POSITION {	INTERNAL, TRIGGER, EXTERNAL }
protected enum TEST_STRING { DOWNLOAD_SINGLE_SUCCESS, DOWNLOAD_MULTI_SUCCESS, DOWNLOAD_FAILED, UPLOAD_SINGLE_SUCCESS, UPLOAD_MULTI_SUCCESS, UPLOAD_FAILED, LATENCY_SUCCESS, LATENCY_FAILED, NONE }

HashMap<String, String> testDigest = new HashMap<String, String>();

protected void setDigestType(String type){					testDigest.put(TYPE, type);			}
protected void setDigestDatetime(long dtime){				testDigest.put(DTIME, dtime+"");	}
protected void setDigestTarget(String target){				testDigest.put(TARGET, target);		}
protected void setDigestMetric(double metric){				testDigest.put(METRIC, metric+"");	}
protected void setDigestSuccess(boolean succ){				testDigest.put(SUCCESS, succ+"");	}

public HashMap<String, String> getTestDigest() { return testDigest;	}

public void setRunMessage(String m) { 		runMessage = m; 	}
public String getRunMessage() {				return runMessage;	}
public void setDoneMessage(String m) {		doneMessage = m;	}
public String getDoneMessage() {			return doneMessage;	}

public synchronized STATUS getStatus() { 	return status;	}
public synchronized boolean isFinished() {	return status == STATUS.DONE; }

	String runMessage = "";
	String doneMessage = "";
	String result;
	String targetServer;
	
		public String getError() {												 If the test fails the return string should contain the reason; If it succeeds return empty String 
		String ret = "";
		synchronized (errorString) {
			ret = errorString;
		}
		return ret;
	}
	
	//abstract public String getResultsAsString();							// New Human readable implementation 
	//abstract public String getResults(String locale);


	//Used in ExportFile Class	
//	public static final String JSON_TYPE = "type";
//	public static final String JSON_TIMESTAMP = "timestamp";
//	public static final String JSON_DATETIME = "datetime";
//	public static final String JSON_TARGET = "target";
//	public static final String JSON_TARGET_IPADDRESS = "target_ipaddress";
//	public static final String JSON_SUCCESS = "success";
*/	

abstract public class Test implements Runnable {
	private String[] outputFields = null;
	private String errorString = "";
	private JSONObject json_output = null;
	
	protected final String TARGET = "target";
	protected final String PORT = "port";
	protected final String FILE = "file";
	
	protected STATUS status;	
	protected boolean finished;
	
	protected boolean initialised;
	
	public abstract void execute();
	
	abstract public String getStringID();
	abstract public boolean isSuccessful();
	abstract public void run();
	abstract public String getHumanReadableResult();
	
	abstract public HashMap<String, String> getResults();
	
	abstract public boolean isProgressAvailable();
	abstract public int getProgress(); 										/* from 0 to 100 */
	abstract public boolean isReady();										/* Checks if the test is ready to run */
	abstract public int getNetUsage();										/* The test has to provide the amount of data used */
					
	protected enum STATUS { WAITING, RUNNING, DONE }

	protected void setOutput(String[] o) { 						outputFields = o; 	}
	
	public JSONObject getJSONResult(){ 							return json_output; 	}
	protected void setJSONResult(Map<String, Object> output){ 	json_output = new JSONObject(output);	}
			
	protected long unixTimeStamp() { 
		return System.currentTimeMillis() / 1000;
	}

	public Test() {
		status = STATUS.WAITING;
	}

	protected synchronized void start() { 		status = STATUS.RUNNING;	}
	protected synchronized void finish() {		status = STATUS.DONE;		}

	protected String getOutputField(int i) {
		
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
	
	public String[] getOutputFields() { 		return outputFields;		}
	public String getOutputString() {
		if (null == outputFields) {
			return "";
		}
		return getOutputString(";");
	}
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
	
	protected boolean setErrorIfEmpty(String error, Exception e) {
		String exErr = e.getMessage() == null ? "No expetion message" : e.getMessage();
		return setErrorIfEmpty(error + " " + exErr);
	}
	protected boolean setErrorIfEmpty(String error) {
		boolean ret = false;
		synchronized (errorString) {
			if (errorString.equals("")) {
				errorString = error;
				ret = true;
			}
		}
		return ret;
	}
	protected void setError(String error) {
		synchronized (errorString) {
			errorString = error;
		}
	}
}
