package com.samknows.measurement.schedule;

import java.io.Serializable;

import org.w3c.dom.Element;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.util.OtherUtils;

public class Communication implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String id;
	public String type;
	public String content;
	
	public static Communication parseXml(Element node){
		Communication ret = new Communication();
		ret.type = node.getAttribute("type");
		ret.id = node.getAttribute("id");
		ret.content = OtherUtils.stringEncoding(node.getAttribute("content"));
		SKLogger.d(Communication.class, String.format("%s %s %s",ret.id, ret.type, ret.content));
		return ret;
	}
	
	public boolean isPopup(){
		return type.equals("popup");
	}
}
