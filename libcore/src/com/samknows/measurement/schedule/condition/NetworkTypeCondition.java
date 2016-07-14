package com.samknows.measurement.schedule.condition;

import org.w3c.dom.Element;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.samknows.libcore.SKPorting;
import com.samknows.measurement.TestRunner.TestContext;
import com.samknows.measurement.util.DCSConvertorUtil;

public class NetworkTypeCondition extends Condition{
	private static final long serialVersionUID = 1L;

	private ConnectivityType expectedNetworkType;
	private int type = -1;
	private transient boolean hasNetworkTypeChanged;
	private transient BroadcastReceiver networkStateReceiver;
	
	private transient String networkChangedString = "";
	
	public enum ConnectivityType {
		TYPE_MOBILE, TYPE_WIFI
	}
	
	@Override
	public ConditionResult doTestBefore(final TestContext tc) {
		NetworkInfo info = ((ConnectivityManager) tc.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (info != null) {
			type = info.getType();
		}
		boolean isSuccess;
		switch (expectedNetworkType) {
		case TYPE_MOBILE: {
			isSuccess = type == ConnectivityManager.TYPE_MOBILE;
			break;
		}
		case TYPE_WIFI: {
			isSuccess = type == ConnectivityManager.TYPE_WIFI;
			break;
		}
		default:
			SKPorting.sAssertE(this, "null network info");
			isSuccess = false;
		}
		
		if (isSuccess) {
			hasNetworkTypeChanged = false;
			networkChangedString = DCSConvertorUtil.convertActiveConnectivityType(type);
			networkStateReceiver = new BroadcastReceiver() {
			    @Override
			    public void onReceive(Context context, Intent intent) {
			    	NetworkInfo info = ((ConnectivityManager) tc.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			    	if (info != null) { 
			    		if (type != info.getType()) { //skip the same events
					    	type = info.getType();
					    	networkChangedString += "," + DCSConvertorUtil.convertActiveConnectivityType(type);
					    	hasNetworkTypeChanged = true;
			    		}
			    	} else if (type != -1){ //skip the same events
			    		if (type != -1) {
				    		type = -1;
				    		networkChangedString += "," + "NONE";
				    		hasNetworkTypeChanged = true;
			    		}
			    	}
			    }
			};

			IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);        
			tc.getContext().registerReceiver(networkStateReceiver, filter);

		}
		
		ConditionResult result = new ConditionResult(isSuccess,failQuiet);
		result.setJSONFields("expected_network", "connectivity");
		result.generateOut("NETWORKTYPE", DCSConvertorUtil.covertConnectivityType(expectedNetworkType), DCSConvertorUtil.convertActiveConnectivityType(type));
		
		return result;
	}

	@Override
	public ConditionResult testAfter(TestContext tc) {
		ConditionResult result = new ConditionResult(!hasNetworkTypeChanged);
		result.setJSONFields("expected_network", "connectivity_changed");
		result.generateOut("NETWORKTYPELISTENER", DCSConvertorUtil.covertConnectivityType(expectedNetworkType), networkChangedString);
		return result;
	}

	@Override
	public void release(TestContext tc) {
		if (networkStateReceiver != null) {
			tc.getContext().unregisterReceiver(networkStateReceiver);
		}
	}

	@Override
	public boolean needSeparateThread() {
		return false;
	}

	public static NetworkTypeCondition parseXml(Element node) {
		NetworkTypeCondition c = new NetworkTypeCondition();
		String type = node.getAttribute("value");
		if (type.equalsIgnoreCase("mobile")) c.expectedNetworkType = ConnectivityType.TYPE_MOBILE;
		else if (type.equalsIgnoreCase("wifi")) c.expectedNetworkType = ConnectivityType.TYPE_WIFI;
		else {
			throw new RuntimeException("unknown connectivity type: " + type);
		}
		return c;
	}
	
	
	@Override
	public String getConditionStringForReportingFailedCondition() {
		return "NETWORKTYPE";
	}	
}
