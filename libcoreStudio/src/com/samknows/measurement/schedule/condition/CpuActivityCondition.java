package com.samknows.measurement.schedule.condition;

import org.w3c.dom.Element;

import android.util.Log;

import com.samknows.measurement.environment.linux.CpuUsageReader;
import com.samknows.measurement.test.TestContext;
import com.samknows.measurement.util.OtherUtils;
import com.samknows.measurement.util.XmlUtils;

public class CpuActivityCondition extends Condition {
	private static final long serialVersionUID = 1L;
	public static final String TYPE_VALUE = "CPUACTIVITY";
	public static final String JSON_MAX_AVG = "max_average";
	public static final String JSON_READ_AVG = "read_average";

	private int maxAvg;
	private long time;
	
	public static CpuActivityCondition parseXml(Element node) {
		CpuActivityCondition c = new CpuActivityCondition();
		c.maxAvg = Integer.valueOf(node.getAttribute("maxAvg"));
		String time = node.getAttribute("time");
		c.time = XmlUtils.convertTime(time);
		return c;
	}

	@Override
	public boolean needSeparateThread() {
		return true;
	}

	@Override
	public ConditionResult doTestBefore(TestContext tc) {

		int cpuLoad;
		boolean isSuccess;
		if (tc.getIsManualTest()) {
			isSuccess = true;
			cpuLoad = 0;
		} else {
			new CpuUsageReader();
			cpuLoad = (int) CpuUsageReader.read(time);
			isSuccess = cpuLoad < maxAvg;

			if (!isSuccess) {
				if (OtherUtils.isThisDeviceAnEmulator() == true) {
					Log.d(this.getClass().getName(), "DEBUG: would have failed CPU test, but running on Emulator, so let it pass!");
					isSuccess = true;
				}
			}
		}

		ConditionResult result = new ConditionResult(isSuccess,failQuiet);
		result.setJSONFields(JSON_MAX_AVG,JSON_READ_AVG);
		result.generateOut(TYPE_VALUE, maxAvg, cpuLoad);
		return result;
	}
	
	@Override
	public String getConditionStringForReportingFailedCondition() {
		return TYPE_VALUE;
	}	
}
