package com.samknows.measurement.environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.samknows.measurement.storage.PassiveMetric;
import com.samknows.measurement.util.DCSConvertorUtil;
import com.samknows.measurement.util.DCSStringBuilder;
import com.samknows.measurement.util.SKDateFormat;

public class NetworkData implements DCSData{
	
	private static final String ID_PHONE = "NETWORKSTATE";
	private static final String ID_NETWORK_OP = "NETWORKOPERATOR";
	private static final String ID_SIM_OP = "SIMOPERATOR";
	//JSONOutput
	public static final String JSON_TYPE_VALUE = "network_data";
	public static final String JSON_PHONE_TYPE = "phone_type";
	public static final String JSON_PHONE_TYPE_CODE = "phone_type_code";
	public static final String JSON_NETWORK_TYPE = "network_type";
	public static final String JSON_NETWORK_TYPE_CODE = "network_type_code";
	public static final String JSON_ACTIVE_NETWORK_TYPE = "active_network_type";
	public static final String JSON_ACTIVE_NETWORK_TYPE_CODE = "active_network_type_code";
	public static final String JSON_CONNECTED = "connected";
	public static final String JSON_ROAMING = "roaming";
	public static final String JSON_NETWORK_OPERATOR_CODE = "network_operator_code";
	public static final String JSON_NETWORK_OPERATOR_NAME = "network_operator_name";
	public static final String JSON_SIM_OPERATOR_CODE = "sim_operator_code";
	public static final String JSON_SIM_OPERATOR_NAME = "sim_operator_name";
	public static final String JSON_WIFI_SSID = "wifi_ssid";
  public static final String JSON_WLAN_CARRIER = "wlan_carrier";

	
	/** time in milis */
	public long time;
	
	//phone
	public int phoneType;
	public int networkType;
	public NetworkInfo activeNetworkInfo;
	public boolean isConnected;
	public boolean isRoaming;

	//network operator
	public String networkOperatorCode = "";
	public String networkOperatorName = "";
	
	//sim operator
	public String simOperatorCode = "";
	public String simOperatorName = "";

	public String wifiSSID; // e.g. "SK1" ... might be null!
  public String wlanCarrier; // e.g. "SK1" ... might be null!

	public List<String> convert() {
		List<String> list = new ArrayList<>();
		
		DCSStringBuilder builder = new DCSStringBuilder();
		builder.append(ID_PHONE);
		builder.append(time/1000);
		builder.append(DCSConvertorUtil.convertPhoneType(phoneType));
		builder.append(DCSConvertorUtil.convertNetworkType(networkType));
		
		String s = "NONE";
		if (activeNetworkInfo != null) {
			switch (activeNetworkInfo.getType()) {
			case ConnectivityManager.TYPE_MOBILE: {
				s = "MOBILE";
				break;
			}
			case ConnectivityManager.TYPE_WIFI: {
				s = "WiFi";
				break;
			}
			}
		}
		builder.append(s);
		builder.append(isConnected ? 1 : 0);
		builder.append(isRoaming ? 1 : 0);
		list.add(builder.build());
		
		builder = new DCSStringBuilder();
		builder.append(ID_NETWORK_OP);
		builder.append(time/1000);
		builder.append(networkOperatorCode);
		builder.append(networkOperatorName);
    if (wifiSSID != null) {
      builder.append(wifiSSID);
    }
    if (wlanCarrier != null) {
      builder.append(wlanCarrier);
    }
		list.add(builder.build());
		
		builder = new DCSStringBuilder();
		builder.append(ID_SIM_OP);
		builder.append(time/1000);
		builder.append(simOperatorCode);
		builder.append(simOperatorName);
		list.add(builder.build());

		return list;
	}

	//ret.add(new PassiveMetric());
	@Override
	public List<JSONObject> getPassiveMetric() {
		List<JSONObject> ret = new ArrayList<>();
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.PHONETYPE, time, DCSConvertorUtil.convertPhoneType(phoneType)));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.NETWORKTYPE, time, DCSConvertorUtil.convertNetworkType(networkType)));
		if(activeNetworkInfo != null){
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.ACTIVENETWORKTYPE, time, DCSConvertorUtil.convertConnectivityType(activeNetworkInfo.getType())));
		}
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.ROAMINGSTATUS, time, (isRoaming ? "true" : "false")));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.NETWORKOPERATORCODE, time, networkOperatorCode));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.NETWORKOPERATORNAME, time, networkOperatorName));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.SIMOPERATORCODE, time, simOperatorCode));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.SIMOPERATORNAME, time, simOperatorName));
    if (wifiSSID != null) {
      ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.WIFISSID, time, wifiSSID));
    }
    if (wlanCarrier != null) {
      ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.WLANCARRIER, time, wlanCarrier));
    }

		return ret;
	}

	@Override
	public List<JSONObject> convertToJSON() {
		Map<String, Object> ret = new HashMap<>();
		ret.put(JSON_TYPE, NetworkData.JSON_TYPE_VALUE);
		ret.put(JSON_PHONE_TYPE_CODE, phoneType);
		ret.put(JSON_PHONE_TYPE, DCSConvertorUtil.convertPhoneType(phoneType));
		ret.put(JSON_TIMESTAMP, time/1000);
		ret.put(JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(time)));
		ret.put(JSON_NETWORK_TYPE_CODE, networkType);
		ret.put(JSON_NETWORK_TYPE, DCSConvertorUtil.convertNetworkType(networkType));
		
		if(activeNetworkInfo != null){
			ret.put(JSON_ACTIVE_NETWORK_TYPE, activeNetworkInfo.getTypeName());
			ret.put(JSON_ACTIVE_NETWORK_TYPE_CODE, activeNetworkInfo.getType());
		}
		ret.put(JSON_CONNECTED, isConnected);
		ret.put(JSON_ROAMING, isRoaming);
		ret.put(JSON_NETWORK_OPERATOR_CODE, networkOperatorCode);
		ret.put(JSON_NETWORK_OPERATOR_NAME, networkOperatorName);
		ret.put(JSON_SIM_OPERATOR_CODE, simOperatorCode);
		ret.put(JSON_SIM_OPERATOR_NAME, simOperatorName);
    if (wifiSSID != null) {
      ret.put(JSON_WIFI_SSID, wifiSSID);
    }
    if (wlanCarrier != null) {
      ret.put(JSON_WLAN_CARRIER, wlanCarrier);
    }

		List<JSONObject> l = new ArrayList<>();
		l.add(new JSONObject(ret));
		return l;
	}

	
}
