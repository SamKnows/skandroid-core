package com.samknows.tests;

import java.util.HashMap;
import java.util.Map;
//import java.util.Vector;

import org.json.JSONObject;

import com.samknows.libcore.SKLogger;

//
// Base class for the tests
//

abstract public class Test implements Runnable {
	private String[] outputFields = null;
	private String errorString = "";
	private JSONObject json_output = null;
	
	public static final String TARGET = "target";
	public static final String PORT = "port";
	public static final String FILE = "file";
	
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
	abstract public int getProgress0To100(); 										/* from 0 to 100 */
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
		String exErr = e.getMessage() == null ? "No exception message" : e.getMessage();
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

	//region Test Cancel control
	// The PassiveServerUploadTest, DownloadTest, LatencyTest classes all detect this stage and allow quick Cancelling of the test
	// even while it is running.
  // Other implements of Test (e.g. ActivServerloadTest) do not yet support this approach.
	private boolean mbShouldCancel = false;

	public boolean getShouldCancel() {
		return mbShouldCancel;
	}

	public void setShouldCancel() {
		mbShouldCancel = true;
	}
	//endregion Test Cancel control
}
