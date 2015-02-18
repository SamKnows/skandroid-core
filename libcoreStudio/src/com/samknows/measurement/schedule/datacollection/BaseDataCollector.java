package com.samknows.measurement.schedule.datacollection;

import java.io.Serializable;
import java.util.List;

import org.json.JSONObject;
import org.w3c.dom.Element;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.test.TestContext;

public abstract class BaseDataCollector implements Serializable{
	private static final long serialVersionUID = 1L;
	public boolean isEnabled;
	protected TestContext tc;
	
	
	public void start(TestContext ctx){
		clearData();
		tc = ctx;
	}
	public void stop(TestContext ctx){}
	public abstract List<String> getOutput();
	public abstract List<JSONObject> getJSONOutput();
	public abstract List<JSONObject> getPassiveMetric();
	public abstract void clearData();
	public enum Type {
		Location, Environment
	}
	
	public static BaseDataCollector parseXml(Element node) {
		BaseDataCollector c = null;
		try {
			Type type = Type.valueOf(node.getAttribute("type"));
			switch (type) {
			case Location : {
				c = LocationDataCollector.parseXml(node);
				break;
			}
			case Environment : {
				c = new EnvironmentDataCollector();
				break;
			}
			default : SKLogger.e(BaseDataCollector.class, "not such data collector: " + type);
			}
		} catch (Exception e) {
			SKLogger.e(BaseDataCollector.class, "Error in parsing data collector type: "+ e.getMessage());
		}
		
		if (c != null) {
			c.isEnabled = true;
			if(!node.getAttribute("enabled").equals("")){
				c.isEnabled = Boolean.valueOf(node.getAttribute("enabled"));
			}
			
		}
		
		return c;
	}
}
