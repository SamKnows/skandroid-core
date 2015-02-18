package com.samknows.measurement.statemachine.state;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.MainService;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.Storage;
import com.samknows.measurement.net.RequestScheduleAnonymousAction;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.statemachine.StateResponseCode;

public class DownloadConfigAnonymousState extends BaseState {

	public DownloadConfigAnonymousState(MainService c) {
		super(c);
	}

	@Override
	public StateResponseCode executeState() throws Exception {
	  
		SKLogger.d(this, "Update config is not necessary");
		return StateResponseCode.OK;
		
		/*
		ScheduleConfig config = null;
		Storage storage = CachingStorage.getInstance();
		SK2AppSettings appSettings  = SK2AppSettings.getSK2AppSettingsInstance();
		RequestScheduleAnonymousAction action = new RequestScheduleAnonymousAction(ctx);
		action.execute();
		if (action.isSuccess() == false) {
			SKLogger.sAssert(this.getClass(), "schedule parsing", false);
		} else {
//				SKLogger.w(this.getClass(), "Using local config file");
//				try {
//    				config = ScheduleConfig.parseXml(ctx.getResources()
//						.openRawResource(R.raw.schedule_example));
//				} catch (Exception e) {
//					// Catch, and rethrow, the exception - as we'd seen this fail in some circumstances and needed to track it down.
//					SKLogger.d(this, "+++++DEBUG+++++ error (1) parsing XML!" + e.toString());
//					throw(e);
//					// TODO: should we NOT rethrow the exception?
//					// config = null;
//				}
//			} else {
			{
				// Write this to the cache storage, for easier debugging!
				try {
					File cacheDir = SKApplication.getAppInstance().getApplicationContext().getCacheDir();
					if (cacheDir != null) {
						
						// http://stackoverflow.com/questions/5923817/how-to-clone-an-inputstream
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						
						try {
							for (;;) {	
								byte data[] = new byte[1000];

								int bytesRead = action.content.read(data);
								if (bytesRead <= 0) {
									break;
								}

								baos.write(data, 0, bytesRead);
							}
							
							// We can now safely write the SCHEDULE.xml file to storage...
							File cacheToThisFile = new File(cacheDir, "SCHEDULE.xml");
							// Delete if already exists!
							cacheToThisFile.delete();
							FileOutputStream fos = new FileOutputStream(cacheToThisFile);
								
							if (fos != null) {
								fos.write(baos.toByteArray());
								
    							// And replace the existing stream, with our new, memory-buffer based stream!
	    						action.content = new ByteArrayInputStream(baos.toByteArray()); 
						
								try {
									fos.flush();
									fos.close();
								} catch (IOException e) {
									SKLogger.sAssert(ScheduleConfig.class, false);
								}
								fos = null;
							}
						} catch (IOException e) {
							SKLogger.sAssert(ScheduleConfig.class, false);
						} finally {
						}

					}
					
					config = ScheduleConfig.parseXml(action.content);
				} catch (Exception e) {
					// Catch, and rethrow, the exception - as we'd seen this fail in some circumstances and needed to track it down.
   			        SKLogger.sAssert(this.getClass(), "error parsing XML!", false);
					throw(e);
					// TODO: should we NOT rethrow the exception?
					//config = null;
				}
			}
			if(config == null){
				return StateResponseCode.FAIL;
			}
			SKLogger.d(this, "obtained config from server");
		
			if (appSettings.updateConfig(config)) {
				storage.saveScheduleConfig(config);
				SKLogger.d(this, "Update config");
			} else {
				SKLogger.d(this, "Update config is not necessary");
				return StateResponseCode.OK;
			}
			storage.dropExecutionQueue();
			storage.dropParamsManager();
			appSettings.setConfig(config);
			return StateResponseCode.NOT_OK;
		}

		return StateResponseCode.FAIL;
		*/
	}
}
