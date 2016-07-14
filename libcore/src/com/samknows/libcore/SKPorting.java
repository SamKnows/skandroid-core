package com.samknows.libcore;

//
// Static methods that you can make behave differently if you so wish on Android... or desktop Java.
//

//
// Android version
//
import com.samknows.measurement.util.SKDateFormat;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class SKPorting {
  public static void sAssert(Boolean value) {
    SKAndroidLogger.sAssert(value);
  }

  public static void sAssertE(Class clazz, String value) {
    SKAndroidLogger.e(clazz, value);
  }

  public static void sAssertE(Object parent, String value) {
    SKAndroidLogger.e(parent, value);
  }

  public static void sAssertE(Class clazz, String value, Throwable t) {
    SKAndroidLogger.e(clazz, value, t);
  }

  public static void sAssertE(Object parent, String value, Throwable t) {
    SKAndroidLogger.e(parent, value, t);
  }

  public static void sAssert(Class clazz, Boolean value) {
    SKAndroidLogger.sAssert(value);
  }

  public static Boolean sIsDebuggerConnected() {
    return Debug.isDebuggerConnected();
  }

  public static void sLogD(String tag, String value) {
    Log.d(tag, value);
  }

  public static void sLogD(Object parent, String message) {
    sLogD(parent.getClass().getName(), message);
  }

  public static void sLogE(String tag, String value) {
    Log.e(tag, value);
  }

  public static void sLogE(String tag, String value, Exception e) {
    Log.e(tag, value, e);
  }

  public static Boolean sIsDebug() {
    return BuildConfig.DEBUG;
  }

  public static String sGetDateAsIso8601String(java.util.Date inDate) {
    return SKDateFormat.sGetDateAsIso8601String(inDate);
  }

  public static boolean sGetIsMainThread() {
    // http://stackoverflow.com/questions/11411022/how-to-check-if-current-thread-is-not-main-thread
		return Looper.getMainLooper().getThread() == Thread.currentThread();

	}

  public static Boolean sGetIsRunningJUnit() {
    StackTraceElement[] elements = Thread.currentThread().getStackTrace();

    for (StackTraceElement item : elements) {
      String className = item.getClassName();
      if (className.startsWith("org.robolectric")) {
        return true;
      }
    }

    return false;
  }

  // Calls handler. If the platform supports calling in main thread, then make the call in the main thread!
  public static class MainThreadResultHandler {

    Handler mHandler = new Handler();

    public void callUsingMainThreadWhereSupported(Runnable runnable) {

      mHandler.post(runnable);
    }
  }
}

/*
//
// Desktop Java version
//

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SKPorting {
  public static void sAssert(Boolean value) {
//SKCommon.sAssert(value);
  }

  public static void sAssertE(Class clazz, String value) {
    sLogE(value);
    sAssert(value);
  }

  public static void sAssertE(Object parent, String value) {
    sLogE.e(value);
    sAssert(value);
  }

  public static void sAssertE(Class clazz, String value, Exception e) {
    sLogE(value);
    sAssert(value);
  }

  public static void sAssertE(Object parent, String value, Throwable t) {
    sLogE(value);
    sAssert(value);
  }

  public static void sAssert(Class clazz, Boolean value) {
    sAssert(value);
  }

  public static Boolean sIsDebuggerConnected() {
    return false;
  }

  public static void sLogD(String tag, String value) {
  }

  public static void sLogD(Object parent, String message) {
    sLogD(parent.getClass().getName(), message);
  }

  public static void sLogE(String tag, String value) {
    System.out.println("sLogE: " + tag + " - " + value);
  }

  public static void sLogE(String tag, String value, Exception e) {
    System.out.println("sLogE: EXCEPTION: " + tag + " - " + value);
  }

  public static Boolean sIsDebug() {
    return false;
  }

  public static String sGetDateAsIso8601String(java.util.Date inDate) {
    return inDate.toString(); // NOTE: <- this would be only used as a hack for non-Android platforms!
  }

  public static boolean sGetIsMainThread() {
		return false;
	}

  public static Boolean sGetIsRunningJUnit() {
    return false;
  }

  // Calls handler. If the platform supports calling in main thread, then make the call in the main thread!
  public static class MainThreadResultHandler {

    public void callUsingMainThreadWhereSupported(Runnable runnable) {

      runnable.run();
    }
  }
}
*/
