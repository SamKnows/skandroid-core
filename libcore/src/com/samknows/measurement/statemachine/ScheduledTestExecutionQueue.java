package com.samknows.measurement.statemachine;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.PriorityQueue;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.schedule.TestGroup;
import com.samknows.measurement.schedule.condition.ConditionGroupResult;
import com.samknows.measurement.schedule.condition.DatacapCondition;
import com.samknows.measurement.schedule.failaction.RetryFailAction;
import com.samknows.measurement.TestRunner.TestContext;
import com.samknows.measurement.TestRunner.TestExecutor;
import com.samknows.measurement.util.SKDateFormat;
import com.samknows.measurement.util.TimeUtils;

public class ScheduledTestExecutionQueue implements Serializable{
	private static final long serialVersionUID = 1L;

	private long startTime;
	private long endTime; //end time for the queue
	private PriorityQueue<QueueEntry> entries = new PriorityQueue<>();
	
	private transient TestContext tc;
	
	private long accumulatedTestBytes = 0L;
	
	private ScheduledTestExecutionQueue() {}
	
	public ScheduledTestExecutionQueue(TestContext tc) {
		this.tc = tc;
    accumulatedTestBytes = 0L;
		startTime = endTime = System.currentTimeMillis();
		
		long daysDiff = TimeUtils.daysToMillis(SKConstants.TEST_QUEUE_MAX_SIZE_IN_DAYS);
		long newEndTime = startTime + daysDiff;
		populate(newEndTime);
	}
	
	private void extendSize() {
		long minSize = TimeUtils.daysToMillis(SKConstants.TEST_QUEUE_NORMAL_SIZE_IN_DAYS);
		long currentSize = endTime - System.currentTimeMillis();
		if (currentSize < minSize) {
			SKLogger.d(this, "extending queue");
			long maxSize = TimeUtils.daysToMillis(SKConstants.TEST_QUEUE_MAX_SIZE_IN_DAYS);
			long newEndSize = System.currentTimeMillis() + maxSize;
			populate(newEndSize);
		} else {
			SKLogger.d(this, "no need to extend queue, endTime: " + TimeUtils.logString(endTime));
		}
	}

	private synchronized void populate(long newEndTime) {
		long timeNow = System.currentTimeMillis();
		startTime = endTime >= timeNow ? endTime : timeNow;
		endTime = newEndTime;
		SKLogger.d(this, "populating test queue from: " + TimeUtils.logString(startTime) + " to " + TimeUtils.logString(endTime));
		for(TestGroup tg: tc.config.backgroundTestGroups){
			for(Long t:tg.getTimesInInterval(startTime, endTime)){
				SKLogger.d(this, "Add test group id "+ tg.id +" at time: "+TimeUtils.logString(t) );
				addEntry(t, tg);
			}
			
		}
		SKLogger.d(this, "queue populated with: " + entries.size());
	}
	
	public void addEntry(long time, TestGroup tg){
		entries.add(new QueueEntry(time, tg.id, tc.config.backgroundTestGroups.indexOf(tg)));
		SKLogger.d(this, "scheduling test group at: "+TimeUtils.logString(time));
	}
	/*
	public void addEntry(long time, TestDescription td) {
		entries.add(new QueueEntry(time, td.id, tc.config.tests.indexOf(td)));
		Logger.d(this, "scheduling test at: " + new SimpleDateFormat().format(new Date(time)));
	}
	*/
	public void setTestContext(TestContext tc) {
		this.tc  = tc;
	}
	
	public long getAccumulatedTestBytes() {
		return accumulatedTestBytes;
	}

	/**
	 * @return reschedule time duration in milliseconds
	 */
	public long executeReturnRescheduleDurationMilliseconds() {

    // TODO - is the following assertion something that should be re-activated?
	  //SKLogger.sAssert(getClass(), (accumulatedTestBytes == 0L));
	    
		TestExecutor scheduledTestExecutor = new TestExecutor(tc);
		long time = System.currentTimeMillis();
		
    // The events in the queue are run through, until all are processed.
    // So, we must start by clearing-out all tests that pre-date the current time.
		while (true){
			QueueEntry entry = entries.peek();
			if (entry != null && !canExecute(entry, time) && entry.getSystemTimeMilliseconds() < time) {
				entries.remove();
				SKLogger.d(this, "removing test scheduled at: " + entry.getSystemTimeAsDebugString());
			} else {
				break;
			}
		}
		
		boolean result = true;
		boolean fail = false;
		if (canExecute(time))
		{
			scheduledTestExecutor.start();
			
			while (canExecute(time))
			{
				QueueEntry entry = entries.remove();
				long maximumTestUsage = tc.config == null ? 0: tc.config.maximumTestUsage;
				//if data cap is going to be breached do not run test
				//the datacap condition is successful if the datacap is not reached

				boolean dataCapLikelyToBeReachedFlag = SK2AppSettings.getSK2AppSettingsInstance().isDataCapLikelyToBeReached(maximumTestUsage);
				// If we're not going to reach the limit, then success is true.
				// If we're going to reach the limit, then success is false.
				boolean dataCapSuccessFlag = !dataCapLikelyToBeReachedFlag;
				DatacapCondition dc = new DatacapCondition(dataCapSuccessFlag);

				if (dc.isSuccess()) {
					ConditionGroupResult tr = scheduledTestExecutor.executeBackgroundTestGroup(entry.groupId);
					accumulatedTestBytes += scheduledTestExecutor.getAccumulatedTestBytes();
					result = tr.isSuccess;
				} else {
					SKLogger.d(this, "Active metrics won't be collected due to potential datacap breach");
					scheduledTestExecutor.getResultsContainer().addFailedCondition(DatacapCondition.JSON_DATACAP);
				}
				scheduledTestExecutor.getResultsContainer().addCondition(dc.getCondition());
			}


			scheduledTestExecutor.stop();
			scheduledTestExecutor.save("scheduled_tests", -1);
		}
		
		extendSize();
		
		if (fail) {
			RetryFailAction failAction = tc.config.retryFailAction;
			if (failAction != null) {
				return tc.config.retryFailAction.delay; //reschedule
			} else {
				SKLogger.e(this, "can't find on test fail action, just skipping the test.");
				entries.remove();
			}
		} 
		
		return getSleepTimeDurationMilliseconds();
	}
	
	private boolean canExecute(long time) {
		QueueEntry entry = entries.peek();
		if (entry == null) {
			return false;
		} else {
			return canExecute(entry, time);
		}
	}

  // The queue is populated with all events for a given day.
  // The events in the queue are run through, until all are processed.
	public boolean canExecute(QueueEntry entry, long time) {
		long timeUntilEntry = Math.abs(entry.getSystemTimeMilliseconds() - time);
    long timeWindow = SK2AppSettings.getSK2AppSettingsInstance().getTestStartWindow();
    if (timeWindow == 60000) {
      if (timeUntilEntry < timeWindow) {
        return true;
      }
    }
    timeWindow /= 2;
    if (timeUntilEntry < timeWindow) {
      return true;
    }

		// This allows us to override the value in the debugger...
		return false;
	}
	
	private long getSleepTimeDurationMilliseconds() {
		if (entries.isEmpty()) {
			return TimeUtils.daysToMillis(SKConstants.TEST_QUEUE_NORMAL_SIZE_IN_DAYS);
		} else {
			QueueEntry entry = entries.peek();
			long value = entry.getSystemTimeMilliseconds() - System.currentTimeMillis();
      return value;
		}
	}

	public int size() {
		return entries.size();
	}
	
	class QueueEntry implements Serializable, Comparable<QueueEntry>{
		private static final long serialVersionUID = 1L;
		private long systemTimeMilliseconds;
    private String systemTimeAsDebugString;
		long groupId;
		int orderIdx;

    public long getSystemTimeMilliseconds() {
      return systemTimeMilliseconds;
    }

    public String getSystemTimeAsDebugString() {
      return systemTimeAsDebugString;
    }


		public QueueEntry(long time, long groupId, int orderIdx) {
			super();
			this.systemTimeMilliseconds = time;
      this.systemTimeAsDebugString = new SKDateFormat(SKApplication.getAppInstance()).UITime(systemTimeMilliseconds);
			this.groupId = groupId;
			this.orderIdx = orderIdx;
		}

		@Override
		public int compareTo(@NonNull QueueEntry another) {
			if (systemTimeMilliseconds == another.getSystemTimeMilliseconds()) {
			//if time is the same we want to the save original order from config
				return Integer.valueOf(orderIdx).compareTo(another.orderIdx);
			}
			return Long.valueOf(systemTimeMilliseconds).compareTo(another.getSystemTimeMilliseconds());
		}

		@Override
		public String toString() {
			return groupId + " : " + getSystemTimeAsDebugString();
		}
			
	}
	
	
}
