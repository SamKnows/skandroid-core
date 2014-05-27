package com.samknows.measurement.test;

import com.samknows.measurement.schedule.TestDescription;
import com.samknows.measurement.util.TimeUtils;

/**
 * Iterator like class that provides calculates the time when test should be executed next time
 * @author ymyronovych
 *
 */
public class TestTimeCalculator {
	private TestDescription td;
	private long time;

	public TestTimeCalculator(TestDescription td) {
		this(td, System.currentTimeMillis());
	}
	
	public TestTimeCalculator(TestDescription td, long startTime) {
		super();
		this.td = td;
		this.time = startTime;
	}
	
	public long nextTime() {
		long result = TestDescription.NO_START_TIME;
		for (long dayTime : td.times) {
			result = TimeUtils.getStartDayTime(time) + dayTime;
			if (result > time) break;
		}
		
		if (result == TestDescription.NO_START_TIME || result <= time) {
			if (!td.times.isEmpty()) {
				result = TimeUtils.getStartNextDayTime(time) + td.times.get(0);
			}
		}
		
		time = result;
		return result;
	}
}
