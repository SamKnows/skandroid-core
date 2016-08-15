package com.samknows.measurement.environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.samknows.libcore.SKPorting;
import com.samknows.measurement.schedule.ScheduleConfig.LocationType;
import com.samknows.measurement.util.DCSStringBuilder;
import com.samknows.measurement.util.SKDateFormat;

import android.location.Location;
import android.location.LocationProvider;

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


	private final Location mLocation;
	private boolean mIsLastKnown = false;
	private final LocationType mLocType;
	private int mProviderStatus = LocationProvider.AVAILABLE;

  private long mForcedTimeMilli = -1L;

  public void setLocationTimeMilli(long timeMilli) {
    // This is used ONLY for special cases.
    //mLocation.setTime(timeMilli);
    mForcedTimeMilli = timeMilli;
  }

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

//  private static Location sLastKnownLocation = null;
//
//	public static Location sGetLastKnownLocation() {
//    synchronized (LocationData.class) {
//      return sLastKnownLocation;
//    }
//  }

	private void OnLocationChanged(Location location) {
//    synchronized (LocationData.class) {
//      sLastKnownLocation = location;
//    }
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

		OnLocationChanged(loc);
	}
	
	public LocationData(Location location, LocationType locType, int providerStatus){
    mLocation = location;
		mProviderStatus = providerStatus;
		mLocType = locType;

		OnLocationChanged(location);
	}

	public LocationData(boolean isLastKnown, Location loc, LocationType locType) {
		mLocation = loc;
		mIsLastKnown = isLastKnown;
		mLocType = locType;

		OnLocationChanged(loc);
	}

	@Override
	public List<String> convert() {
		List<String> ret = new ArrayList<>();
		DCSStringBuilder dcsBuilder = new DCSStringBuilder();
		dcsBuilder.append(mIsLastKnown ? LASTKNOWNLOCATION : LOCATION)
				.append(mLocation.getTime() / 1000).append(mLocType + "") // Location time is in milliseconds; we write a value in SECONDS!
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
		SKPorting.sAssert(getClass(), false);
		return new ArrayList<>();
	}

	@Override
	public List<JSONObject> convertToJSON() {
		List<JSONObject> ret = new ArrayList<>();
		Map<String, Object> loc = new HashMap<>();
		
		loc.put(DCSData.JSON_TYPE, mIsLastKnown ? JSON_LASTKNOWNLOCATION : JSON_LOCATION);
    long useTimeMilli = mLocation.getTime();
    if (mForcedTimeMilli != -1) {
      useTimeMilli = mForcedTimeMilli;
    }
		loc.put(DCSData.JSON_TIMESTAMP, useTimeMilli / 1000); // Location time is in milliseconds; we write a value in SECONDS!
		loc.put(DCSData.JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(useTimeMilli)));
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
