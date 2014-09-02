package com.samknows.ui2.activity;

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
	public static NetworkInfo getNetworkInfo(Context pContext)
	{
	    ConnectivityManager cm = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	    return cm.getActiveNetworkInfo();
	}
	
	/**
	 * Check if there is any connectivity. True means that is connected, false that there is no connection
	 * 
	 * @param pContext
	 * 
	 * @return true or false
	 */
	public static boolean isConnected(Context pContext)
	{
	    NetworkInfo info = Connectivity.getNetworkInfo(pContext);
	    return (info != null && info.isConnected());
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
	    NetworkInfo info = Connectivity.getNetworkInfo(pContext);
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
	    NetworkInfo info = Connectivity.getNetworkInfo(pContext);
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
	    NetworkInfo info = Connectivity.getNetworkInfo(pContext);
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
		NetworkInfo info = Connectivity.getNetworkInfo(pContext);
		
		if (info == null)
		{
			return "";	// Impossible to find out			
		}
		
		if (!info.isConnected())
		{
			return "No Network";	// No connection
		}
		
		
		int type = info.getType();
		int subType = info.getSubtype();
		
		if(type == ConnectivityManager.TYPE_WIFI)
		{
			return "WiFi";	// The WIFI data connection.
		}
		else if(type==ConnectivityManager.TYPE_MOBILE)	// The Mobile data connection.
		{
			switch(subType)
			{
				case TelephonyManager.NETWORK_TYPE_1xRTT:
					return "CDMA2000"; 	// Current network is 1xRTT, ~ 50-100 kbps
				case TelephonyManager.NETWORK_TYPE_CDMA:
					return "CDMA"; 	// Current network is CDMA: Either IS95A or IS95B, ~ 14-64 kbps
				case TelephonyManager.NETWORK_TYPE_EDGE:
					return "EDGE"; 	// Current network is EDGE ~ 50-100 kbps
				case TelephonyManager.NETWORK_TYPE_EVDO_0:
					return "EV-DO 0"; 	// Current network is EVDO revision 0, ~ 400-1000 kbps
				case TelephonyManager.NETWORK_TYPE_EVDO_A:
					return "EV-DO A"; 	// Current network is EVDO revision A, ~ 600-1400 kbps
				case TelephonyManager.NETWORK_TYPE_GPRS:
					return "GPRS"; 	// Current network is GPRS, ~ 100 kbps
				case TelephonyManager.NETWORK_TYPE_HSDPA:
					return "HSDPA"; 	// Current network is HSDPA, ~ 2-14 Mbps
				case TelephonyManager.NETWORK_TYPE_HSPA:
					return "HSPA"; 	// Current network is HSPA, ~ 700-1700 kbps
				case TelephonyManager.NETWORK_TYPE_HSUPA:
					return "HSUPA"; 	// Current network is HSUPA, ~ 1-23 Mbps
				case TelephonyManager.NETWORK_TYPE_UMTS:
					return "UMTS"; 	// Current network is UMTS, ~ 400-7000 kbps
				/*
				 * Above API level 7, make sure to set android:targetSdkVersion 
				 * to appropriate level to use these
				 */
				case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11 
					return "eHRPD"; 	// Current network is eHRPD ~ 1-2 Mbps
				case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
					return "EV-DO B"; 	// Current network is EVDO revision B, ~ 5 Mbps
				case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
					return "HSPA+"; 	// Current network is HSPA+, ~ 10-20 Mbps
				case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
					return "iDEN"; 	// Current network is iDen, ~25 kbps 
				case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
					return "LTE"; 	// Current network is LTE, ~ 10+ Mbps				
				case TelephonyManager.NETWORK_TYPE_UNKNOWN:
					return "Unknown";	// Network type is unknown
				default:
					return "Unknown";	// // Network type is unknown
			}
		}
		else if (type == ConnectivityManager.TYPE_WIMAX)
		{
			return "WiMax";	// The WiMAX data connection.			
		}
		else if (type == ConnectivityManager.TYPE_BLUETOOTH)
		{
			return "Bluetooth";	// The Bluetooth data connection.			
		}
		else if (type == ConnectivityManager.TYPE_ETHERNET)
		{
			return "Ethernet";	// The Ethernet data connection.			
		}
		else if (type == ConnectivityManager.TYPE_DUMMY)
		{
			return "Dummy";	// Dummy data connection. This should not be used on shipping devices.			
		}
		else if (type == ConnectivityManager.TYPE_MOBILE_DUN)
		{
			return "Mobile DUN";	// A DUN-specific Mobile data connection.		
		}
		else if (type == ConnectivityManager.TYPE_MOBILE_HIPRI)
		{
			return "Mobile HIPRI";	// A High Priority Mobile data connection.			
		}
		else if (type == ConnectivityManager.TYPE_MOBILE_MMS)
		{
			return "Mobile MMS";	// An MMS-specific Mobile data connection.			
		}
		else if (type == ConnectivityManager.TYPE_MOBILE_SUPL)
		{
			return "Mobile SUPL";	// A SUPL-specific Mobile data connection.		
		}
		else
		{
			return "Unknown";	// Unknown data connection.
		}
	}
}