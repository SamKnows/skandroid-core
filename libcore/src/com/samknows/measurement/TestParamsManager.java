package com.samknows.measurement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.schedule.OutParamDescription;
import com.samknows.tests.ClosestTarget;
import com.samknows.tests.Param;

public class TestParamsManager implements Serializable {
  static final String TAG = "TestParamsManager";

	private static final long serialVersionUID = 1L;
	private HashMap<String, TestParam> map = new HashMap<String, TestParam>();
	
	public void put(String name, String value) {
		Log.d(TAG, "saving param: " + name + " with value: " + value);
		map.put(name, new TestParam(name, value));
	}
	
	public List<Param> prepareParams(List<Param> params) {
		List<Param> result = new ArrayList<Param>();
		StringBuilder sb = new StringBuilder();
		for (Param p : params) {
			sb.append(p.getName()).append(" ").append(p.getValue()).append(". ");
			if (p.getValue().startsWith(SKConstants.PARAM_PREFIX)) {
				String name = p.getValue().substring(SKConstants.PARAM_PREFIX.length());
				TestParam newParam = map.get(name);
				if (newParam != null) {
					if (p.getValue().equals("$closest")) {
						ClosestTarget.sSetClosestTarget(newParam.value);
					}
					Log.d(TAG, "replacing param.name=" + p.getName() + " with value: " + p.getValue() + " with: " + newParam.value);
					result.add(new Param(p.getName(), newParam.value));
				} else {
					SKLogger.e(this, "can't replace param: " + p.getName() + " with value: " + p.getValue(), new RuntimeException());
				}
			} else {
				result.add(p);
			}
		}
		Log.d(TAG, "Test params are: "+sb.toString());
		return result;
	}
	
	public void processOutParams(String out, List<OutParamDescription> outParamsDescription) {
		String data[] = out.split(SKConstants.RESULT_LINE_SEPARATOR);
		for (OutParamDescription pd : outParamsDescription) {
			put(pd.name, data[pd.idx]);
		}
	}
	
	public boolean isExpired(String param, long expTime) {
		TestParam p = map.get(param);
		if (p == null) {
			Log.e(getClass().getName(), "can not find param for name: " + param);
			return true;
		}
		return (p.createdTime + expTime) < System.currentTimeMillis();
	}
	
	public boolean hasParam(String param) {
		return map.get(param) != null;
	}
	
	public String getParam(String name) {
		TestParam p = map.get(name); 
		return p == null ? null : p.value; 
	}

	private class TestParam implements Serializable{
		private static final long serialVersionUID = 1L;
		public String name, value;
		public long createdTime;
		public TestParam(String name, String value) {
			super();
			this.name = name;
			this.value = value;
			createdTime = System.currentTimeMillis();
		}
	}
}
