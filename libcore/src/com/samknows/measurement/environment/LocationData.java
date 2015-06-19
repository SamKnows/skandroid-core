package com.samknows.measurement.environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.schedule.ScheduleConfig.LocationType;
import com.samknows.measurement.util.DCSStringBuilder;
import com.samknows.measurement.util.OtherUtils;
import com.samknows.measurement.util.SKDateFormat;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationProvider;
import android.util.Log;

public class LocationData implements DCSData {
	public static final String LASTKNOWNLOCATION 				= "LASTKNOWNLOCATION";
	public static final String LOCATION 						= "LOCATION";
	public static final String JSON_LOCATION					= "location";
	public static final String JSON_LASTKNOWNLOCATION 			= "last_known_location";
	public static final String JSON_LOCATION_TYPE 				= "location_type";
	public static final String JSON_LATITUDE 					= "latitude";
	public static final String JSON_LONGITUDE 					= "longitude";
//	public static final String JSON_MUNICIPALITY 					= "muncipality";
//	public static final String JSON_COUNTRY_NAME 					= "country_name";
	public static final String JSON_ACCURACY 					= "accuracy";

	public static final String JSON_PROVIDERSTATUS 				= "provider_status";
	public static final String JSON_PROVIDERSTATUS_UNKNOWN		= "unknown";
	public static final String JSON_PROVIDERSTATUS_AVAILABLE 	= "available";
	public static final String JSON_PROVIDERSTATUS_UNAVAILABLE	= "unavailable";


	private Location mLocation;
	private boolean mIsLastKnown = false;
	private LocationType mLocType;
	private int mProviderStatus = LocationProvider.AVAILABLE;

//	public String mMuncipality = "";
//	public String mCountryName = "";

	private static String providerStatusToString(int providerStatus){
		String ret = JSON_PROVIDERSTATUS_UNKNOWN;
		switch(providerStatus){
		case LocationProvider.OUT_OF_SERVICE:
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			ret = JSON_PROVIDERSTATUS_UNAVAILABLE;
			break;
		case LocationProvider.AVAILABLE:
			ret = JSON_PROVIDERSTATUS_AVAILABLE;
			break;
		}
		return ret;
	}

	private void OnLocationChanged() {
//		if (mLocation != null) {
//			// TODO - get municipality and country name!
//
//      Geocoder geocoder = new Geocoder(SKApplication.getAppInstance().getApplicationContext(), Locale.getDefault());
//      if (geocoder == null) {
//        SKLogger.sAssert(false);
//      } else {
//        if (geocoder.isPresent() == false) {
//          // Maybe we're on the Emulator!
//          SKLogger.sAssert(OtherUtils.isThisDeviceAnEmulator() == true);
//        } else {
//          try {
//            List<Address> addressList = geocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 1);
//            if (addressList != null && addressList.size() > 0) {
//              Address address = addressList.get(0);
//              String city = address.getLocality();
//              String country = address.getCountryName();
//
//              mMuncipality = city;
//              mCountryName = country;
//            } else {
//              SKLogger.sAssert(false);
//            }
//          } catch (IOException e) {
//            Log.e("LocationData", "Unable connect to Geocoder", e);
//            SKLogger.sAssert(false);
//          }
//        }
//      }
//    }
	}
	
	public LocationData(Location loc, LocationType locType) {
		mLocation = loc;
		mLocType = locType;

		OnLocationChanged();
	}
	
	public LocationData(Location location, LocationType locType, int providerStatus){
		mLocation = location;
		mProviderStatus = providerStatus;
		mLocType = locType;

		OnLocationChanged();
	}

	public LocationData(boolean isLastKnown, Location loc, LocationType locType) {
		mLocation = loc;
		mIsLastKnown = isLastKnown;
		mLocType = locType;

		OnLocationChanged();
	}

	@Override
	public List<String> convert() {
		List<String> ret = new ArrayList<String>();
		DCSStringBuilder dcsBuilder = new DCSStringBuilder();
		dcsBuilder.append(mIsLastKnown ? LASTKNOWNLOCATION : LOCATION)
				.append(mLocation.getTime() / 1000).append(mLocType + "")
				.append(mLocation.getLatitude())
				.append(mLocation.getLongitude())
				.append(mLocation.getAccuracy());
//				.append(mMuncipality)
//				.append(mCountryName);
		ret.add(dcsBuilder.build());
		return ret;
	}

	@Override
	public List<JSONObject> getPassiveMetric() {
		SKLogger.sAssert(getClass(), false);
		return new ArrayList<JSONObject>();
	}

	@Override
	public List<JSONObject> convertToJSON() {
		List<JSONObject> ret = new ArrayList<JSONObject>();
		Map<String, Object> loc = new HashMap<String, Object>();
		
		loc.put(DCSData.JSON_TYPE, mIsLastKnown ? JSON_LASTKNOWNLOCATION : JSON_LOCATION);
		loc.put(DCSData.JSON_TIMESTAMP, mLocation.getTime() / 1000);
		loc.put(DCSData.JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(mLocation.getTime())));
		loc.put(JSON_LOCATION_TYPE, mLocType + "");
		loc.put(JSON_LATITUDE, mLocation.getLatitude());
		loc.put(JSON_LONGITUDE, mLocation.getLongitude());
		loc.put(JSON_ACCURACY, mLocation.getAccuracy());
		loc.put(JSON_PROVIDERSTATUS, providerStatusToString(mProviderStatus));
//		loc.put(JSON_MUNICIPALITY, mMuncipality);
//		loc.put(JSON_COUNTRY_NAME, mCountryName);
		ret.add(new JSONObject(loc));
		
		return ret;
	}

}
