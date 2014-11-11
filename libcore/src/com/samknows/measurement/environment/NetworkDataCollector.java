package com.samknows.measurement.environment;

import com.samknows.libcore.SKLogger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class NetworkDataCollector extends BaseDataCollector{

	public NetworkDataCollector(Context context) {
		super(context);
	}
	
	TelephonyManager mTelManager;
	ConnectivityManager mConnManager;
	NetworkDataListener mNetworkDataListener; 
	
	private static NetworkData extractData(TelephonyManager telManager, ConnectivityManager connManager){
		NetworkData ret = new NetworkData();
		
		ret.time = System.currentTimeMillis();
		
		// Hold on to networkInfo for the purposes of the null check and the
		// subsequent call to isConnected, as the value returned can change
		// between calls.
		ret.activeNetworkInfo = connManager.getActiveNetworkInfo();
		if (ret.activeNetworkInfo == null) {
			ret.isConnected = false;
		} else {
			ret.isConnected = ret.activeNetworkInfo.isConnected();
		}		
		
		ret.networkOperatorCode = telManager.getNetworkOperator();
		ret.networkOperatorName = telManager.getNetworkOperatorName();
		
		ret.simOperatorCode = telManager.getSimOperator();
		ret.simOperatorName = telManager.getSimOperatorName();
		
		ret.phoneType = telManager.getPhoneType();
		ret.isRoaming = telManager.isNetworkRoaming();
		ret.networkType = telManager.getNetworkType();

		return ret;
	}
	
	void collectData(){
		addData(extractData(mTelManager, mConnManager));
	}
	
	
	@Override
	public NetworkData collect() {
	
		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	
		return extractData(manager, connManager);
	}
	
	@Override
	public void start() {
		mTelManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		mConnManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		mNetworkDataListener = new NetworkDataListener(this);
		collectData();
		
		try {
    		mTelManager.listen(mNetworkDataListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
		} catch (SecurityException e) {
			// According to the documentation, this does NOT require READ_PHONE_STATE.
			// https://developer.android.com/reference/android/telephony/PhoneStateListener.html#LISTEN_DATA_CONNECTION_STATE
			// But, sometimes for some reason, a SecurityException is thrown with that given as the reason!
			// Some other states *do* require READ_PHONE_STATE (e.g. LISTEN_MESSAGE_WAITING_INDICATOR), but we're not listening for that!
			SKLogger.sAssert(getClass(),  false);
		}
	}

	@Override
	public void stop() {
		mTelManager.listen(mNetworkDataListener, PhoneStateListener.LISTEN_NONE);
	}
	
	private class NetworkDataListener extends PhoneStateListener{
		NetworkDataCollector mNetworkDataCollector;
		public NetworkDataListener(NetworkDataCollector ndc){
			mNetworkDataCollector = ndc;
		}
		@Override
		public void onDataConnectionStateChanged(int state){
			mNetworkDataCollector.collectData();
		}
		
	}
}
