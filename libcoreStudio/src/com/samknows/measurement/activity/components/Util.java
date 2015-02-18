package com.samknows.measurement.activity.components;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Util {
	private static Typeface FONT_REGULAR;

	public static void initializeFonts(final Context context) {
		FONT_REGULAR=Typeface.createFromAsset(context.getAssets(), "typewriter.ttf");   
	}
	
	public static void overrideFonts(final Context context, final View v) {
	    try {
	        if (v instanceof ViewGroup) {
	            ViewGroup vg = (ViewGroup) v;
	            for (int i = 0; i < vg.getChildCount(); i++) {
	                View child = vg.getChildAt(i);
	                overrideFonts(context, child);
	            }
	        } else if (v instanceof TextView) {
	            ((TextView)v).setTypeface(FONT_REGULAR);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        // ignore
	    }
	}
}
