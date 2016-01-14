package com.samknows.measurement.environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import com.samknows.measurement.util.OtherUtils;
import com.samknows.measurement.util.SKDateFormat;

import android.net.TrafficStats;
import android.util.Log;

public class TrafficData implements DCSData, Serializable {

	private static final long serialVersionUID = 1L;
	public static final String JSON_TYPE_NETUSAGE	= "net_usage";
	public static final String JSON_MOBILERXBYTES	= "mobile_rx_bytes";
	public static final String JSON_MOBILETXBYTES	= "mobile_tx_bytes";
	public static final String JSON_TOTALRXBYTES 	= "total_rx_bytes";
	public static final String JSON_TOTALTXBYTES 	= "total_tx_bytes";
	public static final String JSON_APPRXBYTES	 	= "app_rx_bytes";
	public static final String JSON_APPTXBYTES 		= "app_tx_bytes";
	public static final String JSON_DURATION		= "duration";
	
	
	public long mobileRxBytes = 0;
	public long mobileTxBytes = 0;
	public long totalRxBytes = 0;
	public long totalTxBytes = 0;
	public long appRxBytes = 0;
	public long appTxBytes = 0;
	public long time = 0;
	public long duration = 0;
	
	
	public TrafficData(){}
		
	public static TrafficData interval(TrafficData start, TrafficData end){
		TrafficData ret = new TrafficData();
		
		ret.mobileRxBytes = statDiff(end.mobileRxBytes, start.mobileRxBytes);
		ret.mobileTxBytes = statDiff(end.mobileTxBytes, start.mobileTxBytes);
		ret.totalRxBytes = statDiff(end.totalRxBytes, start.totalRxBytes);
		ret.totalTxBytes = statDiff(end.totalTxBytes, start.totalTxBytes);
		ret.appRxBytes = statDiff(end.appRxBytes, start.appRxBytes);
		ret.appTxBytes = statDiff(end.appTxBytes, start.appTxBytes);
		ret.duration = (end.time - start.time) * 1000;
		ret.time = end.time;
		return ret;
	}
	
	private static long statDiff(long a, long b){
		return (a == TrafficStats.UNSUPPORTED || b == TrafficStats.UNSUPPORTED ) ? TrafficStats.UNSUPPORTED : a - b;
	}
	
	private static long statSum(long a, long b){
		return (a == TrafficStats.UNSUPPORTED || b == TrafficStats.UNSUPPORTED ) ? TrafficStats.UNSUPPORTED : a + b;
	}
		
	public void add(TrafficData td){
		time = td.time;
		appRxBytes = statSum(appRxBytes, td.appRxBytes);
		appTxBytes = statSum(appTxBytes, td.appTxBytes);
		mobileRxBytes = statSum(mobileRxBytes,td.mobileRxBytes);
		mobileTxBytes = statSum(mobileTxBytes, td.mobileTxBytes);
		totalRxBytes = statSum(totalRxBytes, td.totalRxBytes);
		totalTxBytes = statSum(totalTxBytes, td.totalTxBytes);
	}
	
	public boolean checkCondition(long bytesIn, long bytesOut){

    boolean result =  (totalRxBytes <= bytesIn && totalTxBytes <= bytesOut);

    if (result == false) {
      if (OtherUtils.isThisDeviceAnEmulator() == true)
      {
        Log.w("TrafficData", "WARNING: TrafficData.checkCondition failed - but overriding to true as on emulator, to allow background test to!");
        return true;
      }

      Log.w("TrafficData", "WARNING: TrafficData.checkCondition failed - background test will not run!");
    }

    return result;
	}
	
	public static TrafficData extractList(List<TrafficData> list){
		TrafficData ret = new TrafficData();
		for(int i = 1; i < list.size(); i++){
			ret.add(delta(list.get(i-1),list.get(i)));	
		}
		return ret;
	}
	
	
	private static TrafficData delta(TrafficData a, TrafficData b){
		TrafficData zero = new TrafficData();
		if(a.time >= b.time)
			return zero;
		if(a.appRxBytes > b.appRxBytes)
			return zero;
		if(a.appTxBytes > b.appTxBytes)
			return zero;
		if(a.mobileRxBytes > b.mobileRxBytes)
			return zero;
		if(a.mobileTxBytes > b.mobileTxBytes)
			return zero;
		if(a.totalRxBytes > b.totalRxBytes )
			return zero;
		if(a.totalTxBytes > b.totalTxBytes)
			return zero;
		return interval(a,b);
					
	}
	
	@Override
	public List<String> convert() {
		
		return new ArrayList<>();
	}

	@Override
	public List<JSONObject> getPassiveMetric() {
		return new ArrayList<>();
		
		
	}

	@Override
	public List<JSONObject> convertToJSON() {
		List<JSONObject> ret = new ArrayList<>();
		HashMap<String, Object> jo = new HashMap<>();
		jo.put(JSON_TYPE, JSON_TYPE_NETUSAGE);
		jo.put(JSON_TIMESTAMP, time/1000);
		jo.put(JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(time)));
		jo.put(JSON_MOBILERXBYTES, mobileRxBytes);
		jo.put(JSON_MOBILETXBYTES, mobileTxBytes);
		jo.put(JSON_TOTALRXBYTES, totalRxBytes);
		jo.put(JSON_TOTALTXBYTES, totalTxBytes);
		jo.put(JSON_APPRXBYTES, appRxBytes);
		jo.put(JSON_APPTXBYTES, appTxBytes);
		jo.put(JSON_DURATION, duration);
		ret.add(new JSONObject(jo));
		return ret;
	}
	
}
