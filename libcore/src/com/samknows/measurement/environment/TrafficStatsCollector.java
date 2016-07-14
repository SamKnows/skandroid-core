package com.samknows.measurement.environment;

import java.io.Serializable;

import android.content.Context;
import android.net.TrafficStats;
import android.os.Process;
import android.util.Log;

import com.samknows.libcore.SKPorting;
import com.samknows.libcore.SKConstants;

public class TrafficStatsCollector extends EnvBaseDataCollector implements Serializable {

  static final String TAG = "TrafficStatsCollection";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public TrafficStatsCollector(Context context) {
		super(context);
	}

	private TrafficData start = new TrafficData();
	private TrafficData end = new TrafficData();
	private int uid;
	
	
	public void start() {
		start = collectTraffic();
		uid = Process.myUid();
	}
	
	public long finish() {
		end = collectTraffic();
		// Note that in the event of a failure by Android's TrafficStat method, 
		// the finish() method will return zero!
		return end.appRxBytes - start.appRxBytes + end.appTxBytes - start.appTxBytes;
	}
	
	
	public static TrafficData collectAll(long interval) {
		
		try {
			Thread.sleep(SKConstants.NET_ACTIVITY_CONDITION_WAIT_TIME);
		} catch (InterruptedException e1) {
      SKPorting.sAssert(false);
			e1.printStackTrace();
		}
		TrafficData a = collectTraffic();
		
		Log.d(TAG, "start collecting netData for " + interval / 1000 + "s");
		
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
      SKPorting.sAssert(false);
			e.printStackTrace();
		}
		
		TrafficData b = collectTraffic();

		Log.d(TAG, "finished collecting netData in: " + (b.time - a.time)/1000 + "s");
		
		return TrafficData.interval(a, b);
	}
	
	public DCSData collect(){
		return collectTraffic();
	}
	
	
	public static TrafficData collectTraffic(){
		// https://developer.android.com/reference/android/net/TrafficStats.html#getMobileRxBytes%28%29
		// According to the Android documentation, any one of the methods on TrafficStats might fail to work!
		// In such cases where a method is not supported, the method returns UNSUPPORTED (-1)...
		TrafficData ret = new TrafficData();
		
		ret.time = System.currentTimeMillis();
		
		ret.mobileRxBytes = TrafficStats.getMobileRxBytes();
		if (ret.mobileRxBytes == TrafficStats.UNSUPPORTED) {
    		ret.mobileRxBytes = 0;
		}
		
		ret.mobileTxBytes = TrafficStats.getMobileTxBytes();
		if (ret.mobileTxBytes == TrafficStats.UNSUPPORTED) {
    		ret.mobileTxBytes = 0;
		}
		
		ret.totalRxBytes = TrafficStats.getTotalRxBytes();
		if (ret.totalRxBytes == TrafficStats.UNSUPPORTED) {
    		ret.totalRxBytes = 0;
		}
		
		ret.totalTxBytes = TrafficStats.getTotalTxBytes();
		if (ret.totalTxBytes == TrafficStats.UNSUPPORTED) {
    		ret.totalTxBytes = 0;
		}
		
		int uid = Process.myUid();
		ret.appRxBytes = TrafficStats.getUidRxBytes(uid);
		if (ret.appRxBytes == TrafficStats.UNSUPPORTED) {
    		ret.appRxBytes = 0;
		}
		
		ret.appTxBytes = TrafficStats.getUidTxBytes(uid);
		if (ret.appTxBytes == TrafficStats.UNSUPPORTED) {
    		ret.appTxBytes = 0;
		}
		
		return ret;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

		
}
