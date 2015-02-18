package com.samknows.ui2.activity;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.R;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.environment.NetworkDataCollector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * This class is responsible for the connectivity stuff, check the kind of connectivity, connection status, speed connection...
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */


public class Connectivity
{	  
	/**
	 * Get the details about the currently active default data network
	 * 
	 * @param pContext
	 * 
	 * @return details about the currently active default data network
	 */
	public static NetworkInfo sGetNetworkInfo(Context pContext)
	{
		return NetworkDataCollector.sGetNetworkInfo();
	}
	
	/**
	 * Check if there is any connectivity. True means that is connected, false that there is no connection
	 * 
	 * @param pContext
	 * 
	 * @return true or false
	 */
	public static boolean sGetIsConnected(Context pContext)
	{
		return NetworkDataCollector.sGetIsConnected();
	}
	
	/**
	 * Check if there is any connectivity to a Wifi network. True means that we have WiFi connection, false means that we don't have WiFi connectivity
	 * 
	 * @param pContext
	 * @param type
	 * 
	 * @return true or false
	 */
	public static boolean isConnectedWifi(Context pContext)
	{
	    NetworkInfo info = Connectivity.sGetNetworkInfo(pContext);
	    return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
	}
	
	/**
	 * Check if there is any connectivity to a mobile network. True means we are in mobile network, false means we are not in a mobile network
	 * 
	 * @param pContext
	 * @param type
	 * 
	 * @return true or false
	 */
	public static boolean isConnectedMobile(Context pContext)
	{
	    NetworkInfo info = Connectivity.sGetNetworkInfo(pContext);
	    return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
	}
	
	/**
	 * Check if there is fast connectivity. True means we are connected and we have fast connectivity, false means we are not connected or we don't have fast connectivity
	 * 
	 * @param pContext
	 * 
	 * @return true or false
	 */
	public static boolean isConnectedFast(Context pContext)
	{
	    NetworkInfo info = Connectivity.sGetNetworkInfo(pContext);
	    return (info != null && info.isConnected() && Connectivity.isConnectionFast(info.getType(),info.getSubtype()));
	}
	
	/**
	 * Check if the connection is fast. True means we are in a fast connection (fast connections are defined down below), false means we are in a slow connection (slow connections defined down below)
	 * 
	 * @param pType
	 * @param pSubType
	 * 
	 * @return true or false
	 */
	public static boolean isConnectionFast(int pType, int pSubType)
	{
		if(pType==ConnectivityManager.TYPE_WIFI)
		{
			return true;
		}
		else if(pType==ConnectivityManager.TYPE_MOBILE)
		{
			switch(pSubType)
			{
				case TelephonyManager.NETWORK_TYPE_1xRTT:
					return false; // Current network is 1xRTT, ~ 50-100 kbps
				case TelephonyManager.NETWORK_TYPE_CDMA:
					return false; // Current network is CDMA: Either IS95A or IS95B, ~ 14-64 kbps
				case TelephonyManager.NETWORK_TYPE_EDGE:
					return false; // Current network is EDGE, ~ 50-100 kbps
				case TelephonyManager.NETWORK_TYPE_EVDO_0:
					return true; // Current network is EVDO revision 0, ~ 400-1000 kbps
				case TelephonyManager.NETWORK_TYPE_EVDO_A:
					return true; // Current network is EVDO revision A, ~ 600-1400 kbps
				case TelephonyManager.NETWORK_TYPE_GPRS:
					return false; // Current network is GPRS, ~ 100 kbps
				case TelephonyManager.NETWORK_TYPE_HSDPA:
					return true; // Current network is HSDPA, ~ 2-14 Mbps
				case TelephonyManager.NETWORK_TYPE_HSPA:
					return true; // Current network is HSPA, ~ 700-1700 kbps
				case TelephonyManager.NETWORK_TYPE_HSUPA:
					return true; // Current network is HSUPA, ~ 1-23 Mbps
				case TelephonyManager.NETWORK_TYPE_UMTS:
					return true; // Current network is UMTS, ~ 400-7000 kbps
				/*
				 * Above API level 7, make sure to set android:targetSdkVersion 
				 * to appropriate level to use these
				 */
				case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11 
					return true; // Current network is eHRPD, ~ 1-2 Mbps
				case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
					return true; // Current network is EVDO revision B, ~ 5 Mbps
				case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
					return true; // Current network is HSPA+, ~ 10-20 Mbps
				case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
					return false; // Current network is iDen, ~25 kbps 
				case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
					return true; // Current network is LTE, ~ 10+ Mbps
				// Unknown
				case TelephonyManager.NETWORK_TYPE_UNKNOWN:
					return false;	// Network type is unknown
				default:
					return false;	// Network type is unknown
			}
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Check the kind of connectivity. Returns the specific name of the connectivity.
	 * 
	 * @param pContext 
	 * 
	 * @return String representing the name of the connectivity type
	 */
	public static String getConnectionType(Context pContext)
	{
		NetworkInfo info = Connectivity.sGetNetworkInfo(pContext);
		
		if (info == null)
		{
			// Impossible to find out!
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.unknown);
		}
		
		if (!info.isConnected())
		{
			return "No Network";	// No connection
		}
		
		
		int type = info.getType();
		int subType = info.getSubtype();
		
		if(type == ConnectivityManager.TYPE_WIFI)
		{
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_wifi);	// The WIFI data connection.
		}
		else if(type==ConnectivityManager.TYPE_MOBILE)	// The Mobile data connection.
		{
			switch(subType)
			{
			case TelephonyManager.NETWORK_TYPE_1xRTT:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_1xRTT);
			case TelephonyManager.NETWORK_TYPE_CDMA:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_CDMA); 	// Current network is CDMA: Either IS95A or IS95B, ~ 14-64 kbps
			case TelephonyManager.NETWORK_TYPE_EDGE:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_EDGE); 	// Current network is EDGE ~ 50-100 kbps
			case TelephonyManager.NETWORK_TYPE_EVDO_0:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_EVDO_0); 	// Current network is EVDO revision 0, ~ 400-1000 kbps
			case TelephonyManager.NETWORK_TYPE_EVDO_A:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_EVDO_A);
			case TelephonyManager.NETWORK_TYPE_GPRS:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_GPRS);
			case TelephonyManager.NETWORK_TYPE_HSDPA:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_HSDPA);
			case TelephonyManager.NETWORK_TYPE_HSPA:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_HSPA);
			case TelephonyManager.NETWORK_TYPE_HSUPA:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_HSUPA);
			case TelephonyManager.NETWORK_TYPE_UMTS:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_UMTS);
				/*
				 * Above API level 7, make sure to set android:targetSdkVersion 
				 * to appropriate level to use these
				 */
			case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11 
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_EHRPD);
			case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_EVDO_B);
			case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_HSPAP);
			case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_IDEN);
			case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_NETWORK_TYPE_LTE);
			case TelephonyManager.NETWORK_TYPE_UNKNOWN:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.unknown);
			default:
				return SKApplication.getAppInstance().getApplicationContext().getString(R.string.unknown);
			}
		}
		else if (type == ConnectivityManager.TYPE_WIMAX)
		{
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_TYPE_WIMAX);
		}
		else if (type == ConnectivityManager.TYPE_BLUETOOTH)
		{
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_TYPE_BLUETOOTH);
		}
		else if (type == ConnectivityManager.TYPE_ETHERNET)
		{
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_TYPE_ETHERNET);
		}
		else if (type == ConnectivityManager.TYPE_DUMMY)
		{
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_TYPE_DUMMY);
		}
		else if (type == ConnectivityManager.TYPE_MOBILE_DUN)
		{
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_TYPE_MOBILE_DUN);
		}
		else if (type == ConnectivityManager.TYPE_MOBILE_HIPRI)
		{
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_TYPE_MOBILE_HIPRI);
		}
		else if (type == ConnectivityManager.TYPE_MOBILE_MMS)
		{
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_TYPE_MOBILE_MMS);
		}
		else if (type == ConnectivityManager.TYPE_MOBILE_SUPL)
		{
			return SKApplication.getAppInstance().getApplicationContext().getString(R.string.network_type_TYPE_MOBILE_SUPL);
		}
		else
		{
         	return SKApplication.getAppInstance().getApplicationContext().getString(R.string.unknown);
		}
	}
}