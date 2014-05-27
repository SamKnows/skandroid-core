package com.samknows.measurement.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.samknows.measurement.test.ScheduledTestExecutionQueue;
import com.samknows.measurement.util.IdGenerator;
import com.samknows.measurement.util.TimeUtils;
import com.samknows.measurement.util.XmlUtils;

public class TestGroup implements Serializable{
	
	private static final long serialVersionUID = 1L;
	public long id = IdGenerator.generate();
	
	//condition group to be executed before and after
	public String conditionGroupId;
	public long netUsage = 0;
	//time the tests belonging to the group should start during the day
	public List<TestTime> times;
	
	//List of tests ids belonging to the test group
	public List<Integer> testIds;
	
	public static TestGroup parseXml(Element node){
		TestGroup ret = new TestGroup();
		ret.conditionGroupId = node.getAttribute(ScheduleConfig.CONDITION_GROUP_ID);
		ret.times = new ArrayList<TestTime>();
		
		//get the times the test group is supposed to run during the day
		NodeList list = node.getElementsByTagName(ScheduleConfig.TIME);
		for(int i = 0; i < list.getLength(); i++){
			Element ep = (Element) list.item(i);
			long time = XmlUtils.convertTestStartTime(ep.getFirstChild().getNodeValue());
			String attribute = ep.getAttribute(ScheduleConfig.RANDOM_INTERVAL);
			TestTime tt;
			if(attribute != null && ! attribute.equals("")){
				tt = new TestTime(time, XmlUtils.convertTime(attribute));
			}else{
				tt = new TestTime(time);
			}			
			ret.times.add(tt);
		}
		Collections.sort(ret.times);
		
		
		//get the list of test belonging to the test group
		ret.testIds = new ArrayList<Integer>();
		NodeList test_ids = node.getElementsByTagName(ScheduleConfig.TEST);
		for(int i=0; i < test_ids.getLength(); i++){
			Element ep = (Element) test_ids.item(i);
			ret.testIds.add(Integer.parseInt(ep.getAttribute(ScheduleConfig.ID)));
		}
		return ret;
	}
	
	public void setUsage(List<TestDescription> tds){
		for(TestDescription td: tds){
			if(testIds.contains(td.testId)){
				netUsage += td.maxUsageBytes;
			}
		}
	}
	
	
	public long getNextTime(long time){
		long ret = TestTime.NO_START_TIME;
		for(TestTime tt: times){
			if(tt.getNextStart(time) > time){
				ret = tt.getNextTime(time);
				break;
			}
		}
		
		if(ret <= time && !times.isEmpty()){
			ret = times.get(0).getNextTime(TimeUtils.getStartNextDayTime(time));
		}
		return ret;
	}
	
	
	
	public List<Long> getTimesInInterval(long startInterval, long endInterval){
		List<Long> ret = new ArrayList<Long>();
		long time = startInterval;
		while(time <= endInterval){
			for(TestTime tt: times){
				if (ScheduledTestExecutionQueue.sbForceCanExecuteNow == true) {
					if (ret.size() == 0) {
     					ret.add(startInterval); // ??  + 100); // Delay by 100ms!
     					continue;
					}
				}
				
				if(tt.getNextStart(time) > startInterval && tt.getNextEnd(time) < endInterval){
					ret.add(tt.getNextTime(time));
				}
			}
			time = TimeUtils.getStartNextDayTime(time);
		}
		return ret;
	}
}
