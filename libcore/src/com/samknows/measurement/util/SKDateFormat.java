package com.samknows.measurement.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.samknows.libcore.SKLogger;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;


public class SKDateFormat {
  private Context mCtx;

  static final char DateFormat_MONTH = 'M'; // DateFormat.MONTH;
  static final char DateFormat_DATE = 'd'; // DateFormat.DATE;
  static final char DateFormat_YEAR = 'y'; // DateFormat.YEAR;

  public SKDateFormat(Context ctx) {
    mCtx = ctx;
  }

  public static String sGetGraphDateFormat(Context context) {
    char[] order = null;
    try {
      order = DateFormat.getDateFormatOrder(context);
    } catch (java.lang.IllegalArgumentException e) {
      // Deal with OEM bug seen on some devices...:
      // "java.lang.IllegalArgumentException: Bad pattern character 'E' in E, MMM d, yyyy at libcore.icu.ICU.getDateFormatOrder"
      SKLogger.sAssert(SKDateFormat.class, false);

      order = new char[3];
      order[0] = DateFormat_MONTH;
      order[1] = DateFormat_DATE;
      order[2] = DateFormat_YEAR;
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < order.length; i++) {
      switch (order[i]) {
        case DateFormat_DATE:
          if (i != 0) {
            sb.append("/");
          }
          sb.append("dd");
          break;
        case DateFormat_MONTH:
          if (i != 0) {
            sb.append("/");
          }
          sb.append("MM");
          break;
        case DateFormat_YEAR:
          //sb.append("yyyy");
          break;
      }
    }
    return sb.toString();
  }

  public static String sGetGraphTimeFormat() {
    return "HH:mm";
  }

  private String dateFormat() {
    StringBuilder sb = new StringBuilder();
    try {
      char[] order = DateFormat.getDateFormatOrder(mCtx);
      for (int i = 0; i < order.length; i++) {
        if (i != 0) {
          sb.append("/");
        }
        switch (order[i]) {
          case DateFormat_DATE:
            sb.append("dd");
            break;
          case DateFormat_MONTH:
            sb.append("MM");
            break;
          case DateFormat_YEAR:
            sb.append("yyyy");
            break;
        }
      }
    } catch (Exception e) {
      // Caused by: java.lang.IllegalArgumentException: Bad pattern character 'E' in yyyy年 MMM d日, E
      // Caused by: java.lang.IllegalArgumentException: Bad pattern character 'E' in E, d MMM yyyy
      SKLogger.sAssert(false);
      sb.append("yyyy");
      sb.append("MM");
      sb.append("dd");
    }
    return sb.toString();

  }

  private String shortDateTimeFormatForGraphColumn() {
    char[] order = DateFormat.getDateFormatOrder(mCtx);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < order.length; i++) {
      if (i != 0) {
        sb.append("/");
      }
      switch (order[i]) {
        case DateFormat_DATE:
          sb.append("dd");
          break;
        case DateFormat_MONTH:
          sb.append("MM");
          break;
        case DateFormat_YEAR:
          sb.append("yy");
          break;
      }
    }
    sb.append(" HH:mm");
    return sb.toString();

  }


  public String UIDate(long millis) {
    return new SimpleDateFormat(dateFormat()).format(millis);
  }

  public String getGraphMilliAsDateTimeString(long millis) {
    return new SimpleDateFormat(shortDateTimeFormatForGraphColumn()).format(millis);
  }


  public String UITime(long millis) {
    return UIDate(millis) + " " + DateUtils.formatDateTime(mCtx, millis, DateUtils.FORMAT_SHOW_TIME);
  }

  public String getJSDateFormat() {
    char[] order = DateFormat.getDateFormatOrder(mCtx);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < order.length; i++) {
      if (i != 0) {
        sb.append("/");
      }
      switch (order[i]) {
        case DateFormat_DATE:
          sb.append("%d");
          break;
        case DateFormat_MONTH:
          sb.append("%m");
          break;
        case DateFormat_YEAR:
          sb.append("%y");
          break;
      }

    }
    return sb.toString();
  }

  // https://developer.android.com/reference/java/text/SimpleDateFormat.html
  private static final String ISO_8601_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssZ";
  private static SimpleDateFormat sIso8601DateFormat = null;

  public static synchronized String sGetDateAsIso8601String(Date date) {

    if (sIso8601DateFormat == null) {
      sIso8601DateFormat = new SimpleDateFormat(ISO_8601_FORMAT_STRING, Locale.US);
    }

    String result = sIso8601DateFormat.format(date);
    return result;
  }

  private static final String ISO_8601_FORMAT_STRING_Z = "Z";
  private static SimpleDateFormat sIso8601DateFormatZ = null;

  public static synchronized int sUTCTimezoneAsInteger(Date date) {
    if (sIso8601DateFormatZ == null) {
      sIso8601DateFormatZ = new SimpleDateFormat(ISO_8601_FORMAT_STRING_Z, Locale.US);
    }

    // get e.g. -0400
    String timezoneAsString = sIso8601DateFormatZ.format(date);
    // Convert to e.g. -4

    // Leading '+' is supported in Double.valueOf(String) - but not Integer.valueOf(String) ...!
    int timezone = Double.valueOf(timezoneAsString).intValue() / 100;
    return timezone;
  }

  private static final String ISO_8601_FORMAT_STRING_MILLI_Z = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static SimpleDateFormat sIso8601DateFormatMilliZ = null;

  public static synchronized String sGetDateAsIso8601StringMilliZ(Date date) {
    if (sIso8601DateFormatMilliZ == null) {
      sIso8601DateFormatMilliZ = new SimpleDateFormat(ISO_8601_FORMAT_STRING_MILLI_Z, Locale.US);
    }

    String result = sIso8601DateFormatMilliZ.format(date);
    return result;
  }
}


