package com.samknows.measurement;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.environment.TrafficData;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.test.ScheduledTestExecutionQueue;

public class Storage {
	private Context c;
	
	protected Storage(Context c) {
		super();
		this.c = c;
	}

	public ScheduledTestExecutionQueue loadQueue() {
		return (ScheduledTestExecutionQueue) load(SKConstants.EXECUTION_QUEUE_FILE_NAME);
	}
	
	public void saveExecutionQueue(ScheduledTestExecutionQueue eq) {
		save(SKConstants.EXECUTION_QUEUE_FILE_NAME, eq);
	}
	
	public void dropExecutionQueue() {
		drop(SKConstants.EXECUTION_QUEUE_FILE_NAME);
	}
	
	public void dropParamsManager() {
		drop(SKConstants.TEST_PARAMS_MANAGER_FILE_NAME);
	}
	
	public void saveScheduleConfig(ScheduleConfig sg) {
		save(SKConstants.SCHEDULE_CONFIG_FILE_NAME, sg);
	}
	
	public TestParamsManager loadParamsManager() {
		return (TestParamsManager) load(SKConstants.TEST_PARAMS_MANAGER_FILE_NAME);
	}
	
	public void saveTestParamsManager(TestParamsManager m) {
		save(SKConstants.TEST_PARAMS_MANAGER_FILE_NAME, m);
	}
	
	public ScheduleConfig loadScheduleConfig() {
		return (ScheduleConfig) load(SKConstants.SCHEDULE_CONFIG_FILE_NAME);
	}
	
	public void dropScheduleConfig(){
		drop(SKConstants.SCHEDULE_CONFIG_FILE_NAME);
	}
	
	public void saveNetUsage(TrafficData netusage){
		save(SKConstants.NETUSAGE_STORAGE, netusage);
	}
	
	public void dropNetUsage(){
		drop(SKConstants.NETUSAGE_STORAGE);
	}

	public TrafficData loadNetUsage(){
		return (TrafficData) load(SKConstants.NETUSAGE_STORAGE);
	}
	
	
	
	protected synchronized void save(String id, Object data) {
		ObjectOutputStream dos = null;
		try {
			OutputStream os = c.openFileOutput(id, Context.MODE_PRIVATE);
			dos = new ObjectOutputStream(os);
			dos.writeObject(data);
		} catch (Exception e) {
			SKLogger.e(this, "failed to save object for id: " + id, e);
		} finally {
			IOUtils.closeQuietly(dos);
		}
	}
	
	protected synchronized Object load(String id) {
		ObjectInputStream dis = null;
		try {
			InputStream is = c.openFileInput(id);
			dis = new ObjectInputStream(is);
			return dis.readObject();
		} catch (Exception e) {
			Log.w(getClass().getName(), "failed to load data for id: " + id);
		} finally {
			IOUtils.closeQuietly(dis);
		}
		
		return null;
	}
	
	protected synchronized void drop(String id) {
		c.deleteFile(id);
	}
}
