package com.samknows.measurement.environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;

public abstract class BaseDataCollector {
	protected Context context;
	List<DCSData> mListDCSData;
	
	public BaseDataCollector(Context context) {
		super();
		this.context = context;
		mListDCSData = Collections.synchronizedList(new ArrayList<DCSData>());
	}
	
	/*
	 * Start the collector
	 */
	public abstract void start();
	
	/* 
	 * Stop the collector   
	 */
	public abstract void stop();
	
	/*
	 * Collect the pending data
	 * if the collected data is empty for the last period return the latest value collected
	 * 
	 */
	public List<DCSData> collectPartialData(){
		List<DCSData> ret = new ArrayList<DCSData>();
		synchronized(mListDCSData){
			if(!mListDCSData.isEmpty()){
				for(DCSData data: mListDCSData){
					ret.add(data);
				}
				mListDCSData.clear();
			}
			else{
				ret.add(collect());
			}
		}
		return ret;
	}
	
	public void addData(DCSData data){
		mListDCSData.add(data);
	}
	
	/*
	 * Collect a snapshot of the data instead listening for the changes
	 * it behaves as calling:
	 * start();
	 * stop();
	 * collectPartialData();
	 */
	public abstract DCSData collect();
	
}
