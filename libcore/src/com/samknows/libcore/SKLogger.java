package com.samknows.libcore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.samknows.measurement.util.TimeUtils;

import android.util.Log;

public class SKLogger {
	private static File folder;
	private static final String ERROR = "Error";
	private static final String WARNING = "Warning";
	private static final String DEBUG = "Debug";
	
	public static void setStorageFolder(File f) {
		folder = f;
	}

	// This can dump-out a string longer than the build-in limit to Log.d!
	public static void d(String tag, String message) {
		final int chunkLength = 4000;
		if (message.length() > chunkLength) {
			final int chunkCount = message.length() / chunkLength;     // integer division
			for (int i = 0; i <= chunkCount; i++) {
				try {
					int offset = i * chunkLength;
					String theChunk = message.substring(offset);
					if (theChunk.length() > chunkLength) {
						theChunk = theChunk.substring(0, 4000);
					}
					Log.d("", theChunk);
				} catch (IndexOutOfBoundsException e) {
					sAssert(SKLogger.class, false);
				}
			}

	    	appendLog(DEBUG, tag, message);
		
			return;
		}
		
		Log.d(tag, message);
		appendLog(DEBUG, tag, message);
	}
	
	public static void d(Object parent, String message) {
		Log.d(parent.getClass().getName(), message);
		appendLog(DEBUG,parent.getClass().getName(), message);
	}

	public static void d(Class clazz, String message) {
		Log.d(clazz.getName(), message);
		appendLog(DEBUG,clazz.getName(), message);
	}
	
	public static void e(String tag, String message) {
		Log.d(tag, message);
		appendLog(ERROR, tag, message);
	}
	
	public static void e(Class clazz, String message) {
		Log.d(clazz.getName(), message);
		appendLog(ERROR, clazz.getName(), message);
		sAssert(clazz,  false);
	}

	public static void e(Object parent, String message, Throwable t) {
		Log.e(parent.getClass().getName(), message, t);
		appendLog(ERROR,parent.getClass().getName(), message+" "+t.getMessage()+" "+getStackTrace(t));
		sAssert(parent.getClass(),  false);
	}

	public static void e(Object parent, String message) {
		Log.e(parent.getClass().getName(), message);
		appendLog(ERROR,parent.getClass().getName(), message);
		sAssert(parent.getClass(),  false);
	}

	public static void w(Class clazz, String message) {
		Log.w(clazz.getName(), message);
		appendLog(WARNING, clazz.getName(), message);
	}

	private static void appendLog(String severety, String tag, String text) {
		if (SKConstants.LOG_TO_FILE) {
			File logFile = new File(folder, "log.file");
			if (!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				// BufferedWriter for performance, true to set append to file
				// flag
				BufferedWriter buf = new BufferedWriter(new FileWriter(logFile,
						true));
				buf.append(TimeUtils.logString(System.currentTimeMillis()) + " : ");
				buf.append(severety +" : ");
				buf.append(tag + " : ");
				buf.append(text);
				buf.newLine();
				buf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static String getStackTrace(Throwable t){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}
	
	public static void sAssert(Class clazz, String message, final boolean check) {
		if (check == false) {
			if (message.length() > 0) {
    			Log.e(clazz.getName(), "sAssertFailed (" + message + "): you can trap with a breakpoint in " + SKLogger.class.getName());
			} else {
    			Log.e(clazz.getName(), "sAssertFailed: you can trap with a breakpoint in " + SKLogger.class.getName());
			}
		}
	}
	
	public static void sAssert(Class clazz,  final boolean check) {
			sAssert(clazz, "", check);
	}

}
