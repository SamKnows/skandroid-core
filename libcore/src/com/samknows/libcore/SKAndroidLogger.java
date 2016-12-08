package com.samknows.libcore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.samknows.measurement.SKApplication;
import com.samknows.measurement.util.OtherUtils;
import com.samknows.measurement.util.TimeUtils;

import android.util.Log;


public class SKAndroidLogger {
  private static File folder;
  private static final String ERROR = "Error";
  private static final String WARNING = "Warning";
  private static final String DEBUG = "Debug";

  // This must be turned OFF for production builds!
  private static final boolean LOG_TO_FILE = false;

  private static void appendLog(String severity, String tag, String text) {
    Log.d("SKLogger - appendLog", tag + ":" + text);

    if (LOG_TO_FILE) {
      // This MUST be synchronized, as multiple threads might try to write at once!
      synchronized (SKAndroidLogger.class) {
        File logFile = new File(folder, "log.file");
        if (!logFile.exists()) {
          try {
            boolean bRes = logFile.createNewFile();
            SKPorting.sAssert(bRes);
          } catch (IOException e) {
            SKPorting.sAssert(false);
          }
        }
        try {
          // BufferedWriter for performance, true to set append to file flag
          BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
          buf.append(TimeUtils.logString(System.currentTimeMillis())).append(" : ");
          buf.append(severity + " : ");
          buf.append(tag).append(" : ");
          buf.append(text);
          buf.newLine();
          buf.close();
        } catch (IOException e) {
          SKPorting.sAssert(false);
        }
      }
    }
  }

  private static String getStackTrace(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

  public static void setStorageFolder(File f) {
    // IGNORE the supplied folder, as it is in the CACHE area ... unless we have an exception!
    try {
      File storage = android.os.Environment.getExternalStorageDirectory();
      String subFolderName = SKApplication.getAppInstance().getAppName();
      File storageSubFolder = new File(storage, subFolderName);

      if (LOG_TO_FILE == true) {
        // Only make the folder, if we have logging enabled!
        //noinspection ResultOfMethodCallIgnored
        storageSubFolder.mkdir();
      }

      folder = storageSubFolder;
    } catch (Exception e) {
      SKPorting.sAssert(false);
      folder = f;
    }
//    File writeHere = new File(storageSubFolder, fileName);
//    folder = f;
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
          sAssert(SKAndroidLogger.class, false);
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
    appendLog(DEBUG, parent.getClass().getName(), message);
  }

  public static void d(Class clazz, String message) {

    Log.d(clazz.getName(), message);

    appendLog(DEBUG, clazz.getName(), message);
  }

  public static void e(String tag, String message) {
    Log.e(tag, message);

    appendLog(ERROR, tag, message);
    sAssert(false);
  }

  public static void e(Class clazz, String message) {
    Log.e(clazz.getName(), message);

    appendLog(ERROR, clazz.getName(), message);
    sAssert(clazz, false);
  }

  public static void e(Object parent, String message, Throwable t) {
    Log.e(parent.getClass().getName(), message, t);

    appendLog(ERROR, parent.getClass().getName(), message + " " + t.getMessage() + " " + getStackTrace(t));
    sAssert(parent.getClass(), false);
  }

  public static void e(Object parent, String message) {
    Log.e(parent.getClass().getName(), message);

    appendLog(ERROR, parent.getClass().getName(), message);
    sAssert(parent.getClass(), false);
  }

  // If debugging, you should set a breakpoint here to trap all assertions!
  private static void sInternalCalledOnAssert() {

  }


  public static void sAssert(Class clazz, String message, final boolean check) {
    if (check == false) {
      if (message.length() > 0) {
        Log.e(clazz.getName(), "sAssertFailed (" + message + "): you can trap with a breakpoint in " + SKAndroidLogger.class.getName());
      } else {
        Log.e(clazz.getName(), "sAssertFailed: you can trap with a breakpoint in " + SKAndroidLogger.class.getName());
      }

      sInternalCalledOnAssert();
    }
  }

  public static void sAssert(Class clazz, final boolean check) {
    sAssert(clazz, "", check);
  }

  public static void sAssert(String message, final boolean check) {
    if (check == false) {

      if ( (SKApplication.getAppInstance() == null && BuildConfig.DEBUG) ||
           (OtherUtils.isDebuggable(SKApplication.getAppInstance().getApplicationContext()))
         )
      {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String where = "?";
        if (elements.length >= 4) {
          where = elements[3].toString();
        }
        if (message.length() > 0) {
          Log.e("SKLOGGER", "sAssertFailed (" + message + "): you can trap with a breakpoint in " + where);
        } else {
          Log.e("SKLOGGER", "sAssertFailed: you can trap with a breakpoint in " + where);
        }
      }

      sInternalCalledOnAssert();
    }
  }

  public static void sAssert(final boolean check) {
    if (check == false) {

      if ( (SKApplication.getAppInstance() == null && BuildConfig.DEBUG) ||
           (OtherUtils.isDebuggable(SKApplication.getAppInstance().getApplicationContext()))
         )
      {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String where = "?";
        if (elements.length >= 4) {
          where = elements[3].toString();
        }
        Log.e("SKLOGGER", "sAssertFailed: you can trap with a breakpoint in " + where);
      }

      sInternalCalledOnAssert();
    }
  }

  public static void sAssertResourcesNotFoundExceptionNotRobolectric(Exception e) {
    boolean bIgnore = false;
    for (StackTraceElement item : e.getStackTrace()) {
      String className = item.getClassName();
      if (className.startsWith("org.robolectric")) {
        return;
      }
    }
    SKPorting.sAssert(false);
  }
}