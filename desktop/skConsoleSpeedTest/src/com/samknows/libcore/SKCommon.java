package com.samknows.libcore;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class SKCommon {
	
	public static String getVersion() {
		return "1.0";
	}
	
	//public static String sConvertDigits(String value)
	//{
	//    String newValue =  value.replaceAll("١", "1").replaceAll("٢", "2").replaceAll("٣", "3").replaceAll("٤", "4").replaceAll("٥", "5").replaceAll("٦", "6").replaceAll("٧", "7").replaceAll("٨", "8").replaceAll("٩", "9").replaceAll("٠", "0");       
	//    return newValue;
	//}
	
	public static String sGetDecimalStringAnyLocaleAs1Pt5LocalisedString(String value) {
		
		Locale theLocale = Locale.getDefault();
		
		NumberFormat numberFormat = DecimalFormat.getInstance(theLocale);
		Number theNumber;
		try {
			theNumber = numberFormat.parse(value);
		} catch (ParseException e) {
			
        	//value = sConvertDigits(value);
	 		String valueWithDot = value.replaceAll(",",".");
	
			theNumber = Double.valueOf(valueWithDot);
		}
		
		// String.format uses the JVM's default locale.
		//String s = String.format(Locale.US, "%.2f", price);
		NumberFormat outputFormat = DecimalFormat.getInstance(theLocale);
		outputFormat.setMinimumIntegerDigits(1);
		outputFormat.setMinimumFractionDigits(5);
		outputFormat.setMaximumFractionDigits(5);
		String theStringResult = outputFormat.format(theNumber);
		//String theStringResult = String.format("%1.5f", theDoubleValue.doubleValue());
		
		return theStringResult;
	}

	
	public static String sGetDecimalStringUSLocaleAs1Pt5LocalisedString(String value) {
		
		Locale theLocale = Locale.ENGLISH;
		
		NumberFormat numberFormat = DecimalFormat.getInstance(theLocale);
		Number theNumber;
		try {
			theNumber = numberFormat.parse(value);
		} catch (ParseException e) {
			SKCommon.sDoAssert(SKCommon.class,  false);
			return value;
		}
		
		// String.format uses the JVM's default locale.
		//String s = String.format(Locale.US, "%.2f", price);
		NumberFormat outputFormat = DecimalFormat.getNumberInstance();
		outputFormat.setMinimumIntegerDigits(1);
		outputFormat.setMinimumFractionDigits(5);
		outputFormat.setMaximumFractionDigits(5);
		String theStringResult = outputFormat.format(theNumber);
		//String theStringResult = String.format("%1.5f", theDoubleValue.doubleValue());
		
		return theStringResult;
	}
	
	public static double sGetDecimalStringAnyLocaleAsDouble (String value) {
		
		if (value == null) {
			SKCommon.sDoAssert(SKCommon.class, false);
			return 0.0;
		}
		
		Locale theLocale = Locale.getDefault();
		NumberFormat numberFormat = DecimalFormat.getInstance(theLocale);
		Number theNumber;
		try {
			theNumber = numberFormat.parse(value);
			return theNumber.doubleValue();
		} catch (ParseException e) {
			// The string value might be either 99.99 or 99,99, depending on Locale.
			// We can deal with this safely, by forcing to be a point for the decimal separator, and then using Double.valueOf ...
	 		//http://stackoverflow.com/questions/4323599/best-way-to-parsedouble-with-comma-as-decimal-separator
	 		String valueWithDot = value.replaceAll(",",".");
	 		
	 		try {
			  return Double.valueOf(valueWithDot);
	 		} catch (NumberFormatException e2)  {
	 			// This happens if we're trying (say) to parse a string "Failed" as though it were a number!
	 			// If this happens, it should only be due to application logic problems.
	 			// In this case, the safest thing to do is return 0, having first fired-off a debug-time warning.
	 			//SKCommon.sDoAssert(SKCommon.class,  false);
	 			SKCommon.sDoLogD("SKCommon", "Warning: Value is not a number" + value);
	 			return 0.0;
	 		}
		}
	}

	//
  // BEGIN: code that you can make behave differently if you so wish on Android...
  //
  public static void sDoAssert(Boolean value) {
    //SKCommon.sDoAssert(value);
  }

	public static void sDoAssert(Class clazz, Boolean value) {
    SKCommon.sDoAssert(value);
  }

	public static Boolean sIsDebuggerConnected() {
    return false;
    //return Debug.isDebuggerConnected();
  }

	public static void sDoLogD(String tag, String value) {
    //SKCommon.sDoLogD("ClosestTarget", value);
  }

	public static void sDoLogE(String tag, String value) {
    //Log.e(tag, value);
  }

	public static void sDoLogE(String tag, String value, Exception e) {
    //Log.e(tag, value, e);
  }

	public static Boolean sIsDebug() {
    return false;
    //return BuildConfig.DEBUG;
  }

	public static String sGetDateInIso8601Format(java.util.Date inDate) {
    return inDate.toString(); // NOTE: <- this would be only used as a hack for non-Android platforms!
    //return SKDateFormat.sGetDateAsIso8601String(inDate);
  }
}
