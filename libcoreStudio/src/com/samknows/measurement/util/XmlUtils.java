package com.samknows.measurement.util;

import org.w3c.dom.Element;

import com.samknows.measurement.schedule.TestDescription;

public class XmlUtils {
	public static String getNodeAttrValue(Element parent, String nodeName, String attrName) {
		try {
			return ((Element)parent.getElementsByTagName(nodeName).item(0)).getAttribute(attrName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 *  convert time from xml to millis. Time examples - 30s, 30m, 30h, 30d
	 */
	public static long convertTime(String original) {
		String time = original.substring(0, original.length() - 1);
		long number = Long.valueOf(time);
		
		if (original.endsWith("s")) {
			return number*1000;
		} else if (original.endsWith("m")) {
			return TimeUtils.minutesToMillis(number);
		} else if (original.endsWith("h")) {
			return TimeUtils.hoursToMillis(number);
		} else if (original.endsWith("d")) {
			return TimeUtils.daysToMillis(number);
		} else {
			throw new RuntimeException("failed to parse time: " + original);
		}
	}
	
	/**
	 * in example 13:20. hh:mm
	 * @param original
	 * @return
	 */
	public static long convertTestStartTime(String original) {
		if (original == null || original.equals("")) return TestDescription.NO_START_TIME;
		String parts[] = original.split(":");
		return TimeUtils.hoursToMillis(Long.valueOf(parts[0])) + TimeUtils.minutesToMillis(Long.valueOf(parts[1])); 
	}
	
}

