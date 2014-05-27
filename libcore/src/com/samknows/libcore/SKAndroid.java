package com.samknows.libcore;

import android.content.Context;

public class SKAndroid {

	// Usage:
	//   int id = getResId("icon", context, Drawable.class);
    // This is a more general lookup, that can be used as an alternative:
	//   int id = context.getResources().getIdentifier("properties", "raw", c.getPackageName());
	
	public static int getResId(String variableName, Context context, Class<?> c) {

	    try {
	        java.lang.reflect.Field idField = c.getDeclaredField(variableName);
	        return idField.getInt(idField);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return -1;
	    } 
	}

}
