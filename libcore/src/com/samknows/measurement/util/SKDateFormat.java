package com.samknows.measurement.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;


public class SKDateFormat  {
	private Context mCtx;
	
	public SKDateFormat(Context ctx){
		mCtx = ctx;
	}
	
	public static String sGetGraphDateFormat(Context context){
		char[] order = DateFormat.getDateFormatOrder(context);
		StringBuilder sb = new StringBuilder();
		for(int i =0; i< order.length; i++){
			switch(order[i]){
			case DateFormat.DATE:
				if(i!=0){
					sb.append("/");
				}
				sb.append("dd");
				break;
			case DateFormat.MONTH:
				if(i!=0){
					sb.append("/");
				}
				sb.append("MM");
				break;
			case DateFormat.YEAR:
				//sb.append("yyyy");
				break;
			}
		}
		return sb.toString();
	}
	
	public static String sGetGraphTimeFormat() {
		return "HH:mm";
	}
	
	private String dateFormat(){
		char[] order = DateFormat.getDateFormatOrder(mCtx);
		StringBuilder sb = new StringBuilder();
		for(int i =0; i< order.length; i++){
			if(i!=0){
				sb.append("/");
			}
			switch(order[i]){
			case DateFormat.DATE:
				sb.append("dd");
				break;
			case DateFormat.MONTH:
				sb.append("MM");
				break;
			case DateFormat.YEAR:
				sb.append("yyyy");
				break;
			}
		}
		return sb.toString();
		
	}
	
	private String shortDateTimeFormatForGraphColumn(){
		char[] order = DateFormat.getDateFormatOrder(mCtx);
		StringBuilder sb = new StringBuilder();
		for(int i =0; i< order.length; i++){
			if(i!=0){
				sb.append("/");
			}
			switch(order[i]){
			case DateFormat.DATE:
				sb.append("dd");
				break;
			case DateFormat.MONTH:
				sb.append("MM");
				break;
			case DateFormat.YEAR:
				sb.append("yy");
				break;
			}
		}
		sb.append(" HH:mm");
		return sb.toString();
		
	}
	

	public String UIDate(long millis){
		return new SimpleDateFormat(dateFormat()).format(millis);
	}
	
	public String getGraphMilliAsDateTimeString(long millis){
		return new SimpleDateFormat(shortDateTimeFormatForGraphColumn()).format(millis);
	}
	
	
	public String UITime(long millis){
		return UIDate(millis)+" "+DateUtils.formatDateTime(mCtx, millis, DateUtils.FORMAT_SHOW_TIME);
	}
	
	public String getJSDateFormat(){
		char[] order = DateFormat.getDateFormatOrder(mCtx);
		StringBuilder sb = new StringBuilder();
		for(int i =0; i< order.length; i++){
			if(i!=0){
				sb.append("/");
			}
			switch(order[i]){
			case DateFormat.DATE:
				sb.append("%d");
				break;
			case DateFormat.MONTH:
				sb.append("%m");
				break;
			case DateFormat.YEAR:
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


