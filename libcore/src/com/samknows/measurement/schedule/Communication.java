package com.samknows.measurement.schedule;

import android.util.Log;

import java.io.Serializable;

import org.w3c.dom.Element;

import com.samknows.measurement.util.OtherUtils;

public class Communication implements Serializable {
  static final String TAG = "Communication";

	private static final long serialVersionUID = 1L;
	
	public String id;
	public String type;
	public String content;
	
	public static Communication parseXml(Element node){
		Communication ret = new Communication();
		ret.type = node.getAttribute("type");
		ret.id = node.getAttribute("id");
		ret.content = OtherUtils.stringEncoding(node.getAttribute("content"));
		Log.d(TAG, String.format("%s %s %s", ret.id, ret.type, ret.content));
		return ret;
	}
	
	public boolean isPopup(){
		return type.equals("popup");
	}
}
