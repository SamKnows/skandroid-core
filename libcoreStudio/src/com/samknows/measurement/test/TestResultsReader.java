package com.samknows.measurement.test;

import java.io.File;

import android.content.Context;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;

public class TestResultsReader {
	private Context c;
	private String testType;
	private InnerReader r1, r2;
	
	public TestResultsReader(Context c, String testType) {
		super();
		this.testType = testType;
		this.c = c;
		File f1 = c.getFileStreamPath(SKConstants.TEST_RESULTS_TO_SUBMIT_FILE_NAME);
		File f2 = TestResultsManager.getSubmitedLogsFile(c);
		r1 = new InnerReader(f1);
		r2 = new InnerReader(f2);
	}
	
	public String read() {
		String result = r1.innerRead();
		if (result == null) {
			result = r2.innerRead();
		}
		return result;
	}

	
	private class InnerReader {
		private ReverseFileReader reader;
		private File f;
		
		private InnerReader(File f) {
			super();
			this.f = f;
		}

		private String innerRead() {
			String result = null;
			try {
				if (reader == null) {
					reader = new ReverseFileReader(f);
				}
				
				while ((result = reader.readLine()) != null) {
					if (result.split(SKConstants.RESULT_LINE_SEPARATOR)[0].startsWith(testType)) {
						break;
					}
				}
				
			} catch (Exception e) {
				SKLogger.e(this, "failed to parce test results "+e.toString());
			}
			return result;
		}
	}
}
