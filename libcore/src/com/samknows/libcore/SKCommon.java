package com.samknows.libcore;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

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
			SKLogger.sAssert(SKCommon.class,  false);
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
			SKLogger.sAssert(SKCommon.class, false);
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
	 			//SKLogger.sAssert(SKCommon.class,  false);
	 			Log.d("SKCommon", "Warning: Value is not a number" + value);
	 			return 0.0;
	 		}
		}
	}
	
	//
	// Helpful dialogs for prompting the user...
	//
	
	private static class DialogActionClickListener implements DialogInterface.OnClickListener
	{
		private Runnable mActionRunnable;

		private DialogActionClickListener( Runnable actionRunnable )
		{
			mActionRunnable = actionRunnable;
		}

		@Override
		public void onClick( DialogInterface dialog, int which )
		{
			dialog.dismiss();

			// If no action runnable is supplied, the dialog will simply be dismissed
			// and no further action will be taken.
			if ( mActionRunnable != null ) mActionRunnable.run();
		}
	}
	
	// Displays an alert dialog with two buttons.
	public static void displayPositiveNegativeAlertDialog( Context context, int titleResourceId, int iconResourceId, int messageResourceId, int negativeButtonTextResourceId, Runnable negativeRunnable, int positiveButtonTextResourceId, Runnable positiveRunnable )
	{
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( context );

		if ( titleResourceId != 0 )
		{
			dialogBuilder.setTitle( titleResourceId );

			// The icon won't work without a title
			if ( iconResourceId != 0 ) dialogBuilder.setIcon( iconResourceId );
		}

		if ( negativeButtonTextResourceId != 0 ) dialogBuilder.setNegativeButton( negativeButtonTextResourceId, new DialogActionClickListener( negativeRunnable ) );
		if ( positiveButtonTextResourceId != 0 ) dialogBuilder.setPositiveButton( positiveButtonTextResourceId, new DialogActionClickListener( positiveRunnable ) );

		dialogBuilder
		.setMessage( messageResourceId )
		.create()
		.show();
	}


	private static final int       NO_ICON                           = 0;
	private static final int       NO_BUTTON                         = 0;
	private static final Runnable  NO_ACTION                         = null;
	private static final Runnable  DISMISS_DIALOG                    = null;

	public void displayPositiveNegativeAlertDialog( Context context, int titleResourceId, int messageResourceId, int negativeButtonTextResourceId, Runnable negativeRunnable, int positiveButtonTextResourceId, Runnable positiveRunnable )
	{
		displayPositiveNegativeAlertDialog( context, titleResourceId, NO_ICON, messageResourceId, negativeButtonTextResourceId, negativeRunnable, positiveButtonTextResourceId, positiveRunnable );
	}


	public void displayActionDismissAlertDialog( Context context, int titleResourceId, int iconResourceId, int messageResourceId, int dismissButtonTextResourceId, int actionButtonTextResourceId, Runnable actionRunnable )
	{
		displayPositiveNegativeAlertDialog( context, titleResourceId, iconResourceId, messageResourceId, dismissButtonTextResourceId, null, actionButtonTextResourceId, actionRunnable );
	}


	public static void displayActionDismissAlertDialog( Context context, int titleResourceId, int messageResourceId, int dismissButtonTextResourceId, int actionButtonTextResourceId, Runnable actionRunnable )
	{
		displayPositiveNegativeAlertDialog( context, titleResourceId, NO_ICON, messageResourceId, dismissButtonTextResourceId, null, actionButtonTextResourceId, actionRunnable );
	}


	public void displayDismissAlertDialog( Context context, int titleResourceId, int iconResourceId, int messageResourceId, int dismissButtonTextResourceId )
	{
		displayPositiveNegativeAlertDialog( context, titleResourceId, iconResourceId, messageResourceId, dismissButtonTextResourceId, DISMISS_DIALOG, NO_BUTTON, NO_ACTION );
	}

	public void displaySingleButtonAlertDialog(Context context, int messageResourceId, int actionButtonTextResourceId) {
		displayActionDismissAlertDialog(context, 0, 0, messageResourceId, actionButtonTextResourceId, 0, null);
	}

	public static void displayDismissAlertDialog( Context context, int titleResourceId, int messageResourceId, int dismissButtonTextResourceId )
	{
		displayPositiveNegativeAlertDialog( context, titleResourceId, NO_ICON, messageResourceId, dismissButtonTextResourceId, DISMISS_DIALOG, NO_BUTTON, NO_ACTION );
	}

	public void displayActionAlertDialog( Context context, int titleResourceId, int iconResourceId, int messageResourceId, int actionButtonTextResourceId, Runnable actionRunnable )
	{
		displayPositiveNegativeAlertDialog( context, titleResourceId, iconResourceId, messageResourceId, NO_BUTTON, NO_ACTION, actionButtonTextResourceId, actionRunnable );
	}

	public void displayActionAlertDialog( Context context, int titleResourceId, int messageResourceId, int actionButtonTextResourceId, Runnable actionRunnable )
	{
		displayPositiveNegativeAlertDialog( context, titleResourceId, NO_ICON, messageResourceId, NO_BUTTON, NO_ACTION, actionButtonTextResourceId, actionRunnable );
	}
	
	public void displayPopupMessage( Context context, CharSequence messageText )
	{
		Toast.makeText( context, messageText, Toast.LENGTH_LONG ).show();
	}

	public void displayPopupMessage( Context context, int messageResourceId )
	{
		Toast.makeText( context, messageResourceId, Toast.LENGTH_LONG ).show();
	}
}
