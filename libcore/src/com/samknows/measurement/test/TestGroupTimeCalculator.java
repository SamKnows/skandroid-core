package com.samknows.measurement.test;


public class TestGroupTimeCalculator {
/*	private TestGroup tg;
	private long time;
	
	public TestGroupTimeCalculator(TestGroup tg){
		this(tg, System.currentTimeMillis());
	}
	
	public TestGroupTimeCalculator(TestGroup tg, long startTime){
		super();
		this.tg = tg;
		this.time = startTime;
	}
	
	public static long nextTime(long time, TestGroup tg){
		long ret = TestGroup.NO_START_TIME;
		long startOfTheDay = TimeUtils.getStartDayTime(time);
		for(TestTime dayTime: tg.times){
			String st = TimeUtils.logString(startOfTheDay + dayTime.getTime());
			String et = TimeUtils.logString(startOfTheDay + dayTime.getEndInterval());
			ret = startOfTheDay + dayTime.getRandomizedTime();
			String rt = TimeUtils.logString(ret);
			Logger.d(TestGroup.class,"Start interval "+st+ " end interval" +et+" time " +rt);
			if(ret > time){
				break;
			}
		}
		
		if(ret == TestGroup.NO_START_TIME || ret <= time){
			if(!tg.times.isEmpty()){
				ret = TimeUtils.getStartNextDayTime(time) + tg.times.get(0).getRandomizedTime();
				
			}
		}
		time = ret;
		return ret;
	}
	
	public long nextTime(){
		long ret = TestGroup.NO_START_TIME;
		long startOfTheDay = TimeUtils.getStartDayTime(time);
		for(TestTime dayTime: tg.times){
			String st = TimeUtils.logString(startOfTheDay + dayTime.getTime());
			String et = TimeUtils.logString(startOfTheDay + dayTime.getEndInterval());
			ret = startOfTheDay + dayTime.getRandomizedTime();
			String rt = TimeUtils.logString(ret);
			Logger.d(this,"Start interval "+st+ " end interval" +et+" time " +rt);
			if(ret > time){
				break;
			}
		}
		
		if(ret == TestGroup.NO_START_TIME || ret <= time){
			if(!tg.times.isEmpty()){
				ret = TimeUtils.getStartNextDayTime(time) + tg.times.get(0).getRandomizedTime();
				
			}
		}
		time = ret;
		return ret;
	}*/
}
