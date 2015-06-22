package com.samknows.measurement.environment;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.net.SimpleHttpToJsonQuery;
import com.samknows.measurement.storage.PassiveMetric;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NetworkDataCollector extends EnvBaseDataCollector {

	public NetworkDataCollector(Context context) {
		super(context);
	}
	
	public static NetworkInfo sGetNetworkInfo()
	{
		try {
			ConnectivityManager cm = (ConnectivityManager) SKApplication.getAppInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm == null) {
				SKLogger.sAssert(NetworkInfo.class, false);
				return null;
			}
			return cm.getActiveNetworkInfo();
		} catch (NullPointerException e) {
			// This has been seen on customer devices.
			SKLogger.sAssert(NetworkInfo.class, false);
			return null;
		}
	}
	
	public static boolean sGetIsConnected() {
		
		NetworkInfo activeNetworkInfo = sGetNetworkInfo();
		if (activeNetworkInfo == null) {
			SKLogger.sAssert(NetworkDataCollector.class, false);
			return false;
		}
		
		return activeNetworkInfo.isConnected();
	}
	
	TelephonyManager mTelManager;
	ConnectivityManager mConnManager;
	NetworkDataListener mNetworkDataListener;

  public static String sCurrentWifiSSIDNullIfNotFound() {
    Context context = SKApplication.getAppInstance().getApplicationContext();

    //ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    WifiManager wifiManager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    //if (connManager != null && wifiManager != null) {
    if (wifiManager != null) {
      //NetworkInfo netInfo = connManager.getActiveNetworkInfo();
      WifiInfo wifiInfo = wifiManager.getConnectionInfo();
      //if (netInfo != null && wifiInfo != null) {
      if (wifiInfo != null) {
        String theSSID = wifiInfo.getSSID();
        if (theSSID == null) {
					// e.g. if we're on mobile!
          // SKLogger.sAssert(false);
        } else {
          String wifiInfoSSID = wifiInfo.getSSID().replace("\"", "");
          //wifiInfoSSID = "A test network";
          return wifiInfoSSID;
        }
      }
    }

    return null;
  }
	
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

    ret.wifiSSID = sCurrentWifiSSIDNullIfNotFound();

    if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
      // Cannot do this in the main thread - otherwise, we get an exception!
    } else {
      final NetworkData finalRet = ret;
      SimpleHttpToJsonQuery httpToJsonQuery = new SimpleHttpToJsonQuery("http://dcs-mobile-fcc.samknows.com/mobile/lookup.php", null) {
        @Override
        public Void call() throws Exception {

          try {
            String wlanCarrier = mJSONResponse.getString("organization");
            finalRet.wlanCarrier = wlanCarrier;
          } catch (JSONException e) {
            SKLogger.sAssert(getClass(), false);
          }

          return null;
        }
      };

      httpToJsonQuery.doPerformQuery();
    }

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
