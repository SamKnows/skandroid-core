package com.samknows.measurement.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.util.DCSConvertorUtil;

public class PassiveMetric extends JSONObject{
	public static final String JSON_METRIC_NAME = "metric_name";
	public static final String JSON_METRIC = "metric";
	public static final String JSON_DTIME= "dtime";
	public static final String JSON_TYPE = "type";
	public static final String JSON_VALUE = "value";
	
	public static final String TYPE_STRING = "string";
	public static final String TYPE_BOOLEAN = "boolean";
	
	public enum METRIC_TYPE{
		 GSMLAC("gsmlocationareacode"),
		 GSMCID("gsmcelltowerid"),
		 GSMSIGNALSTRENGTH("gsmsignalstrength"),
		 CDMADBM("cdmasignalstrength"),
		 CDMABSID("cdmabasestationid"),
		 CDMABSLAT("cdmabasestationlatitude"),
		 CDMABSLNG("cdmabasestationlongitude"),
		 CDMANETWORKID("cdmanetworkid"),
		 CDMASYSTEMID("cdmasystemid"),
		 PHONETYPE("phonetype"),
		 NETWORKTYPE("networktype"),
		 ACTIVENETWORKTYPE("activenetworktype"),
		 CONNECTIONSTATUS("connectionstatus"),
		 ROAMINGSTATUS("roamingstatus"),
		 NETWORKOPERATORCODE("networkoperatorcode"),
		 NETWORKOPERATORNAME("networkoperatorname"),
		 SIMOPERATORCODE("simoperatorcode"),
		 SIMOPERATORNAME("simoperatorname"),
		 IMEI("imei"),
		 IMSI("imsi"),
		 MANUFACTOR("manufactor"),
		 MODEL("model"),
		 OSTYPE("ostype"),
		 OSVERSION("osversion"),
		 GSMBER("gsmbiterrorrate"),
		 CDMAECIO("cdmaecio"),
		 CONNECTED("connected", TYPE_BOOLEAN),
		 CONNECTIVITYTYPE("connectivitytype"),
		 LATITUDE("latitude"),
		 LONGITUDE("longitude"),
		 ACCURACY("accuracy"),
		 LOCATIONPROVIDER("locationprovider");
		 public String metric_name;
		 public String type;
		 private METRIC_TYPE(String _name){
			 metric_name = _name;
			 type = TYPE_STRING;
		 }
		 private METRIC_TYPE(String _name, String _type){
			 metric_name = _name;
			 type = _type;
		 }
	}
	
	private static final Map<String, Integer> MetricStringToId;
	static{
		Map<String, Integer> tmpMap = new HashMap<String, Integer>();
		for(METRIC_TYPE mt: METRIC_TYPE.values()){
			tmpMap.put(mt.metric_name, mt.ordinal());
		}
		MetricStringToId = Collections.unmodifiableMap(tmpMap);
	}
	
	private void init(METRIC_TYPE metric, long dtime, String value){
		set(JSON_METRIC_NAME, metric.metric_name);
		set(JSON_DTIME, dtime);
		set(JSON_TYPE, metric.type);
		set(JSON_VALUE, value);
	}
	
	private PassiveMetric(METRIC_TYPE metric, long dtime, double value){
		init(metric, dtime, value+"");
	}
	
	private PassiveMetric(METRIC_TYPE metric, long dtime, String value){
		init(metric, dtime, value);
	}

	public static PassiveMetric create(String metric, long dtime, String value){
		if(!MetricStringToId.containsKey(metric)){
			return null;
		}
		METRIC_TYPE metric_type = METRIC_TYPE.values()[MetricStringToId.get(metric)];
		return new PassiveMetric(metric_type, dtime, value);
	}
	
	public static PassiveMetric create(METRIC_TYPE metric, long dtime, String value){
		return new PassiveMetric(metric, dtime, value);
	}
	
	public static PassiveMetric create(METRIC_TYPE metric, long dtime, int value){
		return new PassiveMetric(metric, dtime, convertValue(metric,value));
	}
	
	public static PassiveMetric create(METRIC_TYPE metric, long dtime, double value){
		return new PassiveMetric(metric, dtime, value);
	}
/*	
	public static PassiveMetric create(METRIC_TYPE metric, long dtime, int value){
		return new PassiveMetric(metric, dtime, value+"");
	}
	*/
	//Return the metric id if the metric exists, -1 otherwise
	public static int metricStringToId(String metric){
		return MetricStringToId.containsKey(metric) ? MetricStringToId.get(metric) : -1;
	}
	
	public String metricIdToString(int metric_id){
		String ret = null;
		if(metric_id >= 0 && metric_id < METRIC_TYPE.values().length){
			ret = METRIC_TYPE.values()[metric_id].metric_name;
		}
		return ret;
	}
	
	private void set(String key, Object value){
		try{
			put(key, value);
		}catch(JSONException je){
			SKLogger.e(PassiveMetric.class, "Error in creating the passive metric JSONObject");
		}
	}
	
	private static String convertValue(METRIC_TYPE metric, int value){
		String ret = value+"";
		switch(metric){
		case GSMCID:
		case GSMLAC:
			ret = String.format("%x",value);
			break;
		case GSMSIGNALSTRENGTH:
			ret = (value * 2 - 113) + " dBm";
			break;
		case NETWORKTYPE:
			ret = DCSConvertorUtil.convertNetworkType(value);
			break;
		case ACTIVENETWORKTYPE:
		case CONNECTIONSTATUS:
		case ROAMINGSTATUS:
			default:
		}
				
		return ret;
	}
	
	public static JSONObject passiveMetricToCurrentTest(JSONObject pm){
		JSONObject ret = new JSONObject();
		try{
			ret.put("type", "passivemetric");
			ret.put("metric", metricStringToId( pm.getString(PassiveMetric.JSON_METRIC_NAME)));
			ret.put("metricString", pm.getString(PassiveMetric.JSON_METRIC_NAME));
			ret.put("value", pm.getString("value"));
		}catch(JSONException je){
			SKLogger.d(PassiveMetric.class, "Error in creating json obj: " + je);
		}
		return ret;
	}
	
}
