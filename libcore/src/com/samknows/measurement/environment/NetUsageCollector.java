package com.samknows.measurement.environment;

import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.Storage;

import android.content.Context;

public class NetUsageCollector extends BaseDataCollector {
	public static final String NETUSAGE_STORAGE = "netusage_storage";
	public NetUsageCollector(Context context) {
		super(context);
	}

	@Override
	public DCSData collect() {
		Storage storage = CachingStorage.getInstance();
		TrafficData start = storage.loadNetUsage();
		TrafficData now = TrafficStatsCollector.collectTraffic();
		storage.saveNetUsage(now);
		//if there is no data in cache return null
		if(start == null){
			return null;
		}
		return TrafficData.interval(start, now);		
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}

}
