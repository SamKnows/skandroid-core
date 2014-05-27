package com.samknows.measurement.util;

import java.lang.reflect.Method;

import android.telephony.SignalStrength;
import android.util.Log;

public class SKGsmSignalStrength {

	public SKGsmSignalStrength() {
	}

	public static int getGsmSignalStrength(SignalStrength signalStrength) {
	
		// Work-around problem shown by Android devices!
		// http://strangedevexperience.blogspot.co.uk/2013_03_01_archive.html
		// https://code.google.com/p/android/issues/detail?id=18336 
		// Use reflection to call a private method available on some Samsung devices.

		Integer value = -1;
		Method m;
		try {
			m = SignalStrength.class.getMethod("getGsmSignalBar");
			value = (Integer) m.invoke(signalStrength);
			Log.d("Value signal Bar", "" + value);
		} catch (NoSuchMethodException nsme) {
			Log.d("Value signal Bar", "No such method...");
		} catch (Exception e) {
			Log.d("Value signal Bar", "Unexpected exception!");
		}
		
		int asu = signalStrength.getGsmSignalStrength();
		if (asu == 99) {
			if (value == 4) {
				asu = 18;
			} else if (value == 3) {
				asu = 9;
			} else if (value == 2) {
				asu = 3;
			} else if (value != 1) {
				asu = 1;
			}
		}

		return asu;
	}

}
