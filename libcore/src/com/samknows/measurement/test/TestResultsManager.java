package com.samknows.measurement.test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import android.content.Context;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.schedule.condition.ConditionGroupResult;
import com.samknows.measurement.storage.ExportFile;
import com.samknows.measurement.storage.ResultsContainer;
import com.samknows.measurement.util.OtherUtils;

public class TestResultsManager {
	
	private static File storage;
	
	public static void setStorage(File storage) {
		TestResultsManager.storage = storage;
	}

	//
	// This is the main entry point for saving results json data.
	// 1. Save-off via ExportFile (this creates an archive for future export)
	// 2. Append to "test_results_to_submit" ... this might be malformed JSON data from what I
	//    can see.
	//    On iOS, the results are saved to separate .json files for subsequent upload, there is
	//    never any attempt to 'merge' them.
	//

	public static void saveResult(Context c, ResultsContainer rc){
		ExportFile.saveResults(rc.getJSON());
		saveResult(c, rc.getJSON().toString());
	}
	
	public static void saveResult(Context c, List<String> results) {
		//if there is nothing to save returns immediately
		if(results.size() == 0){
			return;
		}
		DataOutputStream dos = openOutputFile(c);
		if( dos == null){
			SKLogger.e(TestResultsManager.class, "Impossible to save results");
			return;
		}
		try {
			for (String outRes : results) {
				dos.writeBytes(outRes);
				dos.writeBytes("\r\n");
			}
		} catch (IOException ioe) {
			SKLogger.e(TestResultsManager.class, "Error while saving results: " + ioe.getMessage());
		} finally {
			IOUtils.closeQuietly(dos);
		}
	}
	
	//Tries to open output file, in case of failures returns null
	private static DataOutputStream openOutputFile(Context c){
		DataOutputStream ret = null;
		try{
			FileOutputStream os = c.openFileOutput(SKConstants.TEST_RESULTS_TO_SUBMIT_FILE_NAME, Context.MODE_APPEND);
			ret = new DataOutputStream(os);
		}catch(FileNotFoundException fnfe){
			SKLogger.e(TestResultsManager.class, SKConstants.TEST_RESULTS_TO_SUBMIT_FILE_NAME +" not found!");
			ret = null;
		}
		return ret;

	}
	
	public static void saveResult(Context c, String result) {

		if (OtherUtils.isDebuggable(c)) {
			// Debuggable build - so dump-out the JSON string!
			// You can use web services such as http://jsonformatter.curiousconcept.com/#jsonformatter to prettify
			// the output from this Log.d statement; assuming it isn't truncated.
			Log.d("TestResultsManager", "******** saveJSON result... (" + result + ")");

			// Enable the following if you want prettified JSON output for a very long JSON string;
			// useful sometimes, as the standard Log.d has a limited buffer size that it will output.
//			try {
//				// http://stackoverflow.com/questions/6185337/how-do-i-pretty-print-existing-json-data-with-java
//				String prettyJsonString = new JSONObject(result).toString(2);
//				SKLogger.d(TestResultsManager.class.getName(),  prettyJsonString);
//			} catch (JSONException e) {
//				SKLogger.sAssert(TestResultsManager.class, false);
//			}
		}

		DataOutputStream dos = openOutputFile(c);
		if( dos == null){
			SKLogger.e(TestResultsManager.class, "Impossible to save results");
			return;
		}
		try {
			dos.writeBytes(result);
			dos.writeBytes("\r\n");
		} catch (IOException ioe) {
			SKLogger.e(TestResultsManager.class, "Error while saving results: " + ioe.getMessage());
		} finally {
			IOUtils.closeQuietly(dos);
		}
	}

	public static void saveResult(Context c, ConditionGroupResult[] result) {
		for (ConditionGroupResult r : result) saveResult(c, r.results);
	}
	
	public static byte[] getJSONDataAsByteArray(Context c) {
		InputStream is = null;
		try {
			is = c.openFileInput(SKConstants.TEST_RESULTS_TO_SUBMIT_FILE_NAME);
			return IOUtils.toByteArray(is);
		} catch (Exception e) {
			Log.w(TestResultsManager.class.getName(), "no tests result file available");
			return null;
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	public static void clearResults(Context context) {
		context.deleteFile(SKConstants.TEST_RESULTS_TO_SUBMIT_FILE_NAME);
	}
	
	public static void saveSumbitedLogs(Context c, byte[] logs) {
		File logFile = new File(storage, SKConstants.TEST_RESULTS_SUBMITTED_FILE_NAME);
		FileOutputStream is = null;
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				SKLogger.e("TestResultsManager", "failed to save submitted logs to file", e);
				return;
			}
		}
		try {
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			is = new FileOutputStream(logFile, true);
			is.write(logs);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
		
		verifyReduceSize(logFile);
	}
	
	public static File getSubmitedLogsFile(Context c) {
		return new File(storage, SKConstants.TEST_RESULTS_SUBMITTED_FILE_NAME);
	}

	private static void verifyReduceSize(File logFile) {
		if (logFile.length() > SKConstants.SUBMITED_LOGS_MAX_SIZE) {
			File temp = new File(logFile.getAbsolutePath() + "_tmp");
			BufferedReader reader = null;
			FileWriter writer = null;
			try {
				reader = new BufferedReader(new FileReader(logFile));
				reader.skip(logFile.length() - SKConstants.SUBMITED_LOGS_MAX_SIZE / 2);
				reader.readLine();
				
				writer = new FileWriter(temp);
				IOUtils.copy(reader, writer);
				writer.close();
				reader.close();
				
				logFile.delete();
				temp.renameTo(logFile);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(reader);
				IOUtils.closeQuietly(writer);
			}
		}
	}
	
	public static String[] getJSONDataAsStringArray(Context context) {
		// This might return MORE THAN ONE test batch, each one as JSON String on a separate line per batch!
		byte[] data = getJSONDataAsByteArray(context);
		if(data == null){
			return new String[] {};
		}
		String results = new String(data);
		return results.split("\r\n");
	}
}	
