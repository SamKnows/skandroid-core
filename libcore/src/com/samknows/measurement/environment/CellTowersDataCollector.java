package com.samknows.measurement.environment;

import java.util.List;

import com.samknows.libcore.SKLogger;

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

public class CellTowersDataCollector extends BaseDataCollector{

	public CellTowersDataCollector(Context context) {
		super(context);
	}
	
	/*
	 * performs a synchronous read of the signal strength  
	 */
	static TelephonyManager mTelManager = null;
	static CellTowersData mData = new CellTowersData();
	static AndroidPhoneStateListener phoneStateListener = null;
	static CellTowersData neighbours = new CellTowersData();

	@Override
	synchronized public CellTowersData collect() {
		//final TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		final CellTowersData data = new CellTowersData();
		data.setTime(System.currentTimeMillis());
		
		try {
			data.setCellLocation(mTelManager.getCellLocation());
		} catch (SecurityException e) {
			// Seen - rarely - on some Android devices.
			// Neither user 99999 nor current process has android.permission.ACCESS_COARSE_LOCATION.
    		SKLogger.sAssert(CellTowersDataCollector.class, false);
		}
		
		// Note: the following call might return NULL
		
		//data.setNeighbors(mTelManager.getAllCellInfo());
		data.setNeighbors(mTelManager.getNeighboringCellInfo());
	
		SKLogger.sAssert(CellTowersDataCollector.class, mData.getSignal() != null);
		// This following line is actually essential!
		data.setSignal(mData.getSignal());
		addData(mData);
		addData(neighbours);
		
		return data;
	}
	
	synchronized static void sOnSignalStrengthsChanged(SignalStrength signalStrength) {
		mData.setTime(System.currentTimeMillis());
		mData.setSignal(signalStrength);
		SKLogger.sAssert(CellTowersDataCollector.class, mData.getSignal() != null);
		try {
			mData.setCellLocation(mTelManager.getCellLocation());
		} catch (SecurityException e) {
			// Seen - rarely - on some Android devices.
			// Neither user 99999 nor current process has android.permission.ACCESS_COARSE_LOCATION.
    		SKLogger.sAssert(CellTowersDataCollector.class, false);
		}
	}
	
	synchronized static	void sOnCellLocationChanged(CellLocation location){
		mData.setTime(System.currentTimeMillis());
		mData.setCellLocation(location);
	}
	
	synchronized static void sOnCellInfoChanged(List<CellInfo> cellInfo){
		List<NeighboringCellInfo> n = mTelManager.getNeighboringCellInfo();
		if( n != null && n.size() > 0){
			neighbours.setTime(System.currentTimeMillis());
			neighbours.setNeighbors(n);
		}
	}
	
	static class AndroidPhoneStateListener extends PhoneStateListener {
		
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength){
			super.onSignalStrengthsChanged(signalStrength);

			CellTowersDataCollector.sOnSignalStrengthsChanged(signalStrength);
		}
		
		@Override
		public void onCellLocationChanged(CellLocation location){
			super.onCellLocationChanged(location);
			CellTowersDataCollector.sOnCellLocationChanged(location);
		}
		
		@Override
		public void onCellInfoChanged(List<CellInfo> cellInfo){
			//super.onCellInfoChanged(cellInfo);
			CellTowersDataCollector.sOnCellInfoChanged(cellInfo);
		}
	}
	
	// This is called just once, to start monitoring for cell tower signal strength...
	// We need to do this, as Android does not allow us to query this information synchronously!
	static public void sStartToCaptureCellTowersData(Context context) {
		phoneStateListener = new AndroidPhoneStateListener ();  

		mTelManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		int mask = PhoneStateListener.LISTEN_CELL_INFO | PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		//int mask = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		
		try {
    		mTelManager.listen(phoneStateListener, mask);
		} catch (java.lang.SecurityException e) {
			// This has been seen on a small number of Android 2.3.3-2.3.7 devices, with the following:
			// "com.samknows.fcc.FCCApplication: java.lang.SecurityException: Neither user ... nor current process has android.permission.READ_PHONE_STATE."
			SKLogger.sAssert(CellTowersData.class, false);
		}
	}
	
	@Override
	public void start() {
		addData(collect());
	}

	@Override
	public void stop() {
	}

}
