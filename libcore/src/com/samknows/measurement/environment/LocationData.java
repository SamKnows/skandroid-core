package com.samknows.measurement.environment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samknows.libcore.SKPorting;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.schedule.ScheduleConfig.LocationType;
import com.samknows.measurement.util.DCSStringBuilder;
import com.samknows.measurement.util.SKDateFormat;

import android.location.Address;
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

	// https://code.google.com/p/android/issues/detail?id=38009
	public static List<Address> sGetAddressFromLocation(double lat, double lng, int maxResult) {
		return sGetAddressFromLocation(lat, lng, maxResult, null);
	}

  public static List<Address> sGetAddressFromLocation(double lat, double lng, int maxResult, String withKeyOptional){

		String command = "https://maps.googleapis.com/maps/api/geocode/json?";

		if (withKeyOptional != null) {
			command += "key=";
			command += withKeyOptional;
      command += "&";
		}

	  //String arguments = String.format("latlng=%1$f,%2$f&sensor=true&result_type=locality|street_address|postal_code", lat, lng);
    String arguments = String.format("latlng=%1$f,%2$f&sensor=true", lat, lng);
    String finalCommand = command + arguments;

    List<Address> retList = new ArrayList<>();

    try {
      HttpGet httpGet = new HttpGet(finalCommand);

      HttpClient client = new DefaultHttpClient();
      HttpResponse response;
      StringBuilder stringBuilder = new StringBuilder();

      response = client.execute(httpGet);
      HttpEntity entity = response.getEntity();
      InputStream stream = entity.getContent();
      int b;
      while ((b = stream.read()) != -1) {
        stringBuilder.append((char) b);
      }

      JSONObject jsonObject = new JSONObject();
      jsonObject = new JSONObject(stringBuilder.toString());

      if ("OK".equalsIgnoreCase(jsonObject.getString("status"))
          && (jsonObject.has("results"))
          )
      {
        JSONArray results = jsonObject.getJSONArray("results");

        if (results.length() > 0) {
          JSONObject place = results.getJSONObject(0);

          Locale currentLocale = SKApplication.getAppInstance().getApplicationContext().getResources().getConfiguration().locale;

          String cityName = "";
          String countryName = "";
          String locality = "";
          String route = ""; // e.g. MyStreet
          String postalTown = ""; // e.g. MyStreet

          JSONArray addressComponents = place.getJSONArray("address_components");
          for (int i = 0; i < addressComponents.length(); i++) {
            JSONObject component = addressComponents.getJSONObject(i);
            JSONArray types = component.getJSONArray("types");
            for (int j = 0; j < types.length(); j++) {
              Object x = types.get(j);
              if (x instanceof String) {
                String typeCode = (String) x;
                if (typeCode.equals("locality")) {
                  cityName = component.getString("long_name");
                } else if (typeCode.equals("country")) {
                  countryName = component.getString("long_name");
                } else if (typeCode.equals("route")) {
                  route = component.getString("long_name");
                } else if (typeCode.equals("postal_town")) {
                  postalTown = component.getString("long_name");
                }
              }
            }
          }

          Address addr = new Address(currentLocale);
          addr.setLatitude(lat);
          addr.setLongitude(lng);

          addr.setCountryName(countryName);
          if (locality.length() == 0) {
            locality = postalTown;
          }
          addr.setLocality(locality);
          addr.setAddressLine(0, route);
          addr.setAddressLine(1, postalTown);


          retList.add(addr);
        }
      } else {
        // Most likely reason - we're offline!
        SKPorting.sAssert(false);
      }


    } catch (ClientProtocolException e) {
      //Log.e(MyGeocoder.class.getName(), "Error calling Google geocode webservice.", e);
      SKPorting.sAssert(false);
    } catch (IOException e) {
      //Log.e(MyGeocoder.class.getName(), "Error calling Google geocode webservice.", e);
      SKPorting.sAssert(false);
    } catch (JSONException e) {
      //Log.e(MyGeocoder.class.getName(), "Error parsing Google geocode webservice response.", e);
      SKPorting.sAssert(false);
    } catch (Exception e) {
      SKPorting.sAssert(false);
    }

    return retList;
  }

  public static List<Address> sGetAddressFromPostcode(String postcode, int maxResult, String withKey) {

    // Spaces will cause trouble!
    postcode = postcode.replace(" ", "");

    String command = "https://maps.googleapis.com/maps/api/geocode/json?";

    command += "key=";
    command += withKey;
    command += "&";

    //String arguments = String.format("latlng=%1$f,%2$f&sensor=true&result_type=locality|street_address|postal_code", lat, lng);
    String arguments = String.format("address=" + postcode + "&sensor=true");
    String finalCommand = command + arguments;

    List<Address> retList = new ArrayList<>();

    try {
      HttpGet httpGet = new HttpGet(finalCommand);

      HttpClient client = new DefaultHttpClient();
      HttpResponse response;
      StringBuilder stringBuilder = new StringBuilder();

      response = client.execute(httpGet);
      HttpEntity entity = response.getEntity();
      InputStream stream = entity.getContent();
      int b;
      while ((b = stream.read()) != -1) {
        stringBuilder.append((char) b);
      }

      JSONObject jsonObject = new JSONObject();
      jsonObject = new JSONObject(stringBuilder.toString());

      if ("OK".equalsIgnoreCase(jsonObject.getString("status"))
          && (jsonObject.has("results"))
          ) {
        JSONArray results = jsonObject.getJSONArray("results");

        if (results.length() > 0) {
          JSONObject place = results.getJSONObject(0);

          Locale currentLocale = SKApplication.getAppInstance().getApplicationContext().getResources().getConfiguration().locale;

          String cityName = "";
          String countryName = "";
          String locality = "";
          String route = ""; // e.g. MyStreet
          String postalTown = ""; // e.g. MyStreet

          Double latitude = null;
          Double longitude = null;

          //JSONArray array = result.getJSONArray("results");
          if (place.has("geometry")) {
            JSONObject geometry = place.getJSONObject("geometry");
            if (geometry.has("location")) {
              JSONObject location = geometry.getJSONObject("location");

              if (location.has("lat")) {
                latitude = location.getDouble("lat");
              }
              if (location.has("lng")) {
                longitude = location.getDouble("lng");
              }
            }
          }

          //JSONArray array = result.getJSONArray("results");
          JSONArray addressComponents = place.getJSONArray("address_components");
          for (int i = 0; i < addressComponents.length(); i++) {
            JSONObject component = addressComponents.getJSONObject(i);
            JSONArray types = component.getJSONArray("types");
            for (int j = 0; j < types.length(); j++) {
              if (types.getString(j).equals("locality")) {
                cityName = component.getString("long_name");
              } else if (types.getString(j).equals("country")) {
                countryName = component.getString("long_name");
              } else if (types.getString(j).equals("route")) {
                route = component.getString("long_name");
              } else if (types.getString(j).equals("postal_town")) {
                postalTown = component.getString("long_name");
              }
            }
          }

          Address addr = new Address(currentLocale);
          addr.setPostalCode(postcode);

          addr.setCountryName(countryName);
          if (locality.length() == 0) {
            locality = postalTown;
          }
          addr.setLocality(locality);
          addr.setAddressLine(0, postalTown);
          addr.setAddressLine(1, route);

          if (latitude != null) {
            addr.setLatitude(latitude);
          }
          if (longitude != null) {
            addr.setLongitude(longitude);
          }

          retList.add(addr);
        }
      } else {
        // Most likely reason - we're offline!
        SKPorting.sAssert(false);
      }


    } catch (ClientProtocolException e) {
      //Log.e(MyGeocoder.class.getName(), "Error calling Google geocode webservice.", e);
      SKPorting.sAssert(false);
    } catch (IOException e) {
      //Log.e(MyGeocoder.class.getName(), "Error calling Google geocode webservice.", e);
      SKPorting.sAssert(false);
    } catch (JSONException e) {
      //Log.e(MyGeocoder.class.getName(), "Error parsing Google geocode webservice response.", e);
      SKPorting.sAssert(false);
    } catch (Exception e) {
      SKPorting.sAssert(false);
    }

    return retList;
  }

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
