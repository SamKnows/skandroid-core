package com.samknows.measurement.util;

import android.os.SystemClock;

public class IdGenerator {
	public static synchronized long generate() {
		SystemClock.sleep(1); //sleep to ensure ids won't overlap
		return System.currentTimeMillis();
	}
}
