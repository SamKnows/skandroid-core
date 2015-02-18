package com.samknows.measurement.schedule.failaction;

import java.io.Serializable;

import org.w3c.dom.Element;

import com.samknows.measurement.util.XmlUtils;

public class RetryFailAction implements Serializable{
	private static final long serialVersionUID = 1L;
	public long delay;
	
	public static RetryFailAction parseXml(Element node) {
		RetryFailAction action = new RetryFailAction();
		String type = node.getAttribute("type");
		
		if (!type.equals("retry")) {
			throw new RuntimeException();
		}
		
		String time = node.getAttribute("delay");
		action.delay = XmlUtils.convertTime(time);
		return action;
	}
}
