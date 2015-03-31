package com.samknows.measurement.schedule.condition;

import android.util.Log;

import org.w3c.dom.Element;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.schedule.datacollection.LocationDataCollector;
import com.samknows.measurement.test.TestContext;
import com.samknows.measurement.util.XmlUtils;

public class LocationAvailableCondition extends Condition{
  static final String TAG = "LocationAvailableCond";

	private static final long serialVersionUID = 1L;
	private long waitTime;
	
	@Override
	public ConditionResult doTestBefore(TestContext tc) {
		LocationDataCollector collector = tc.findLocationDataCollector();
		boolean result = false;
		String explanation = null;
		if (collector != null) {
			Log.d(TAG, "start waiting for location: " + waitTime / 1000 + "s");
			result = collector.waitForLocation(waitTime);
			if (!result) {
				explanation = "TIMEOUT";
			}
		} else {
			SKLogger.e(this, "can't get LocationDataCollector!");
			explanation = "NO_DATA_COLLECTOR";
		}
		ConditionResult res = new ConditionResult(result, failQuiet);
		res.generateOut("LOCATIONAVAILABLE", explanation);
		Log.d(TAG, "stop waiting for location");
		return res;
	}

	@Override
	protected boolean needSeparateThread() {
		return true;
	}

	public static LocationAvailableCondition parseXml(Element node) {
		LocationAvailableCondition c = new LocationAvailableCondition();
		String time = node.getAttribute("waitTime");
		c.waitTime = XmlUtils.convertTime(time);
		return c;
	}
	
	
	@Override
	public String getConditionStringForReportingFailedCondition() {
		return "LOCATIONAVAILABLE";
	}	
}
