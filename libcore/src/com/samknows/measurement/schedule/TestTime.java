package com.samknows.measurement.schedule;

import java.io.Serializable;

import com.samknows.measurement.util.TimeUtils;

public class TestTime implements Serializable, Comparable<TestTime> {
	public static final long NO_START_TIME = -1;
	private static final long serialVersionUID = 1L;
	public Long mTime;
	public Long mRandomInterval;
	
	public TestTime(Long time){
		mTime = time;
		mRandomInterval = 0l;
	}
	
	public TestTime(Long time, Long random_interval){
		mTime = time;
		mRandomInterval = random_interval;
	}
	
	@Override
	public int compareTo(TestTime another) {
		return this.mTime.compareTo(another.mTime);
	}
	
	public long getTime(){
		return mTime;
	}
	
	public long getEndInterval(){
		return mTime + mRandomInterval;
	}
	
	public long getRandomizedTime(){
		return mTime + getRandom();
	}
	
	public long getRandom(){
		return (long)(Math.random()*mRandomInterval);
	}
	
	public long getNextStart(long time){
		return TimeUtils.getStartDayTime(time) + mTime;
	}
	
	public long getNextStart(){
		return getNextStart(System.currentTimeMillis());
	}
	
	public long getNextEnd(long time){
		
		return TimeUtils.getStartDayTime(time) + mTime + mRandomInterval;
	}
	
	public long getNextEnd(){
		return getNextEnd(System.currentTimeMillis());
	}
	
	public long getNextTime(long time){
		return TimeUtils.getStartDayTime(time) + getRandomizedTime(); 
	}
	
	public long getNextTime(){
		return getNextTime(System.currentTimeMillis());
	}
	
}