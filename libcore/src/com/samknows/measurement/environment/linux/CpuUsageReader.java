package com.samknows.measurement.environment.linux;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.os.SystemClock;

import com.samknows.libcore.SKLogger;

public class CpuUsageReader {
	private long idle1, cpu1;
	
	public void start() {
		RandomAccessFile reader = null;
		try {
			reader = new RandomAccessFile("/proc/stat", "r");
			String load = reader.readLine();

			String[] toks = load.split(" ");

			idle1 = Long.parseLong(toks[5]);
			cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
		} catch (IOException ex) {
			ex.printStackTrace();
		}finally{
			try {
				if(reader != null){
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public float getUsageFromStart() {
		RandomAccessFile reader = null;
		try {
			reader = new RandomAccessFile("/proc/stat", "r");
			String load = reader.readLine();

			String[] toks = load.split(" ");
			long idle2 = Long.parseLong(toks[5]);
			long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
					+ Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			return ((float)cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1)) * 100;

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if(reader != null){
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return 0;
	}
	
	public static float read(long time) {
		long startTime = System.currentTimeMillis();
		CpuUsageReader reader = new CpuUsageReader();
		SKLogger.d(CpuUsageReader.class, "start cpu test for " + time/1000 + "s");
		reader.start();
		
		SystemClock.sleep(time);

	    SKLogger.d(CpuUsageReader.class, "finished cpu test in: " + (System.currentTimeMillis() - startTime)/1000 + "s");
	    return reader.getUsageFromStart();
	}
}
