package com.samknows.measurement.environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;
import org.w3c.dom.Element;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.schedule.ScheduleConfig.LocationType;
import com.samknows.measurement.storage.PassiveMetric;
import com.samknows.measurement.TestRunner.TestContext;
import com.samknows.measurement.storage.StorageTestResult;
import com.samknows.measurement.util.OtherUtils;
import com.samknows.measurement.util.XmlUtils;

public class LocationDataCollector extends BaseDataCollector implements LocationListener {
  static final String TAG = "LocationDataCollector";

  private static final long serialVersionUID = 1L;

  private long time;
  private long listenerDelay;
  //	private float listenerMinDst;
  private boolean getLastKnown;


  private transient List<Location> mLocations;

  Location mLastLocation = null;

  // There is more than one way to obtain Location on Android.
  // - The Android Location API (LOCATION_SERVICE - android.location.*)
  // - Google Play Services Location API (com.google.android.gms.location.*)
  // We use the first of these two options, as then there is no dependency on
  // Google Play Services...
  // See http://www.rahuljiresal.com/2014/02/user-location-on-android/ for some
  // useful background on this.

  private transient LocationManager manager;
  private boolean gotLastLocation = false;
  private LocationType locationType;
  static private Location sLastKnown = null;

  private int mProviderStatus = LocationProvider.AVAILABLE;




  public static void sForceFastLocationCheck() {
    Context context = SKApplication.getAppInstance().getApplicationContext();

    LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    if (manager == null) {
      SKLogger.sAssert(LocationDataCollector.class, false);
      return;
    }

    LocationListener locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(Location location) {
        // Got location change!
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
      }

      @Override
      public void onProviderEnabled(String provider) {
      }

      @Override
      public void onProviderDisabled(String provider) {
      }
    };

    // http://stackoverflow.com/questions/10405277/requestsingleupdate-doesnt-automatically-fetch-gps-location
    // Network location is fast and cheap (in terms of battery use) and a good strategy is to use network location until you get GPS.

//    if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//      manager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, Looper.getMainLooper());
//    } else
    if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      manager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, Looper.getMainLooper());
    } else {
  		 // Don't do this, as it annoys the unit tests!		SKLogger.sAssert(OtherUtils.isThisDeviceAnEmulator());
    }
  }

		
	public static Pair<Location,LocationType> sGetLastKnownLocation() {
    Context context = SKApplication.getAppInstance().getApplicationContext();

    LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (manager == null) {
			SKLogger.sAssert(LocationDataCollector.class,  false);
		  return null;
		}

		if (SK2AppSettings.sHasPermission("android.permission.ACCESS_FINE_LOCATION")) {
			if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

				try {
					Location lastKnownLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					// This MIGHT be null!

					if (lastKnownLocation != null) {
						sLastKnown = lastKnownLocation;
						return new Pair<>(lastKnownLocation, LocationType.gps);
					}

				} catch (Exception e) {
					SKLogger.sAssert(false);
				}
			}
		}

    if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      try {
        Location lastKnownLocation = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        // This MIGHT be null!

        if (lastKnownLocation != null) {
          sLastKnown = lastKnownLocation;
          return new Pair<>(lastKnownLocation, LocationType.network);
        }

      } catch (Exception e) {
        SKLogger.sAssert(false);
      }
    }

    return null;
	}

	static public List<JSONObject> sGetPassiveLocationMetric(boolean forceReportLastKnownAsLocation) {
		Pair<Location, LocationType> lastKnownPair = sGetLastKnownLocation();
		if (lastKnownPair == null) {
			// Nothing known - don't store a passive metric, simply return empty instead...
			SKLogger.sAssert(OtherUtils.isThisDeviceAnEmulator());
			return new ArrayList<>();
		}

		Location lastKnownLocation = lastKnownPair.first;

		if (lastKnownLocation == null) {
			// Nothing known - don't store a passive metric, simply return empty...
			return new ArrayList<>();
		}

		LocationType lastKnownLocationType = lastKnownPair.second;

    boolean bReportAsLastKnownLocation = true;
    if (forceReportLastKnownAsLocation == true) {
      bReportAsLastKnownLocation = false;
    }
		LocationData locationData = new LocationData(bReportAsLastKnownLocation, lastKnownLocation, lastKnownLocationType);

		// The following should only ever return a List<JSONObject> containing one item!
		List<JSONObject> passiveMetrics = locationData.convertToJSON();
		int items = passiveMetrics.size();
		SKLogger.sAssert(StorageTestResult.class, items == 1);
		return passiveMetrics;
	}


	@Override
	public void start(TestContext tc) {
		super.start(tc);
		mLocations = Collections.synchronizedList(new ArrayList<Location>());
		manager = (LocationManager) tc.getSystemService(Context.LOCATION_SERVICE);
		
		if (manager == null) {
			SKLogger.sAssert(getClass(),  false);
			return;
		}
		
		
		locationType = SK2AppSettings.getSK2AppSettingsInstance().getLocationServiceType();
		if (locationType == null) {
      SKLogger.sAssert(false);
      return;
    }

		//if the provider in the settings is gps but the service is not enable fail over to network provider
		if (locationType == LocationType.gps && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      // The following call has been seen to return null on some devices!
			List<String> providers = manager.getAllProviders();
			if (providers != null) {
				if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
					locationType = LocationType.network;
				}
			} else {
				SKLogger.sAssert(false);
			}
		}
		
		if (locationType != LocationType.gps && locationType != LocationType.network) {
			// Rather than simply crashing the app with an exception - stick to Network type, which will
			// be handled benignly...
			locationType = LocationType.network;
		}
		
		String provider = locationType == LocationType.gps ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;

		if (getLastKnown) {
      Location tryLocation = manager.getLastKnownLocation(provider);
      if (tryLocation != null) {
        sLastKnown = tryLocation;
      }
		}
		gotLastLocation = false;
		
		// On some devices, this can throw an exception, of the form:
		//   java.lang.IllegalArgumentException: provider doesn't exist: network
		// or (sic!):
		//   java.lang.IllegalArgumentException: provider doesn't exist: null
		// We must not allow that behavior to cause the app to crash.
		try {
			
			manager.requestLocationUpdates(provider, 0, 0, LocationDataCollector.this, Looper.getMainLooper());
	
			Log.d(TAG, "start collecting location data from: " + provider);
		
		} catch (java.lang.IllegalArgumentException ex) {
			
			SKLogger.sAssert(getClass(),  false);
			
		}
		
		try {
			Log.d(TAG, "sleeping: " + time);
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//stop listening for location updates if we are on network. That is done because network location uses network and breaks NetworkCondition
		if (locationType == LocationType.network) {
			manager.removeUpdates(this);
		}
		
	}

	@Override
	public void clearData(){
		if(mLocations != null ){
			mLocations.clear();
		}
	}
	
	/**
	 * returns true if got location
	 * @param time
	 * @return
	 */
	public synchronized boolean waitForLocation(long time) {
		if (!gotLastLocation) {
			try {
				wait(time);
			} catch (InterruptedException e) {
				SKLogger.e(this, "Interruption while waiting for location", e);
			}
		}
		return gotLastLocation;
	}
	
	@Override
	public void stop(TestContext ctx) {
		if(isEnabled){
			super.stop(ctx);
			manager.removeUpdates(this);
			
			lastReceivedTime = -1;
			Log.d(TAG, "location datas: " + mLocations.size());
			Log.d(TAG, "stop collecting location data");
		}else{
			Log.d(TAG, "LocationDataCollector is not enabled");
		}
	}
	
	private long lastReceivedTime=-1;
	@Override
	public synchronized void onLocationChanged(Location location) {
		//Log.d(TAG, "received new location");
		if (location != null) {
			long timeDiff = System.currentTimeMillis() - lastReceivedTime;
			if (lastReceivedTime == -1 || timeDiff > listenerDelay) {
				lastReceivedTime = System.currentTimeMillis();
				synchronized(this) {
					mLocations.add(location);
				}
				mLastLocation = location;
				gotLastLocation = true;
				notifyAll();
			}
		}
	}
	
//	@Override
//	public List<String> getOutput() {
//		List<String> list = new ArrayList<String>();
//		synchronized(this) {
//			for(Location l: mLocations){
//				list.addAll(new LocationData(l,locationType).convert());
//			}
//		}
//		return list;
//	}
	
	@Override
	public List<JSONObject> getPassiveMetric() {
		List<JSONObject> ret = new ArrayList<>();
		if(sLastKnown != null){
			ret.addAll(locationToPassiveMetric(sLastKnown));
		}
		synchronized(this) {
			for(Location l: mLocations){
				ret.addAll(locationToPassiveMetric(l));
			}
		}
		return ret;
	}

	//Receive a Location object and returns a JSONObject ready to be inserted in the database
	//and displayed to the interface
	private List<JSONObject> locationToPassiveMetric(Location loc){
		List<JSONObject> ret = new ArrayList<>();
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.LOCATIONPROVIDER, loc.getTime(), locationType+""));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.LATITUDE, loc.getTime(), String.format("%1.5f", loc.getLatitude())));	
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.LONGITUDE, loc.getTime(), String.format("%1.5f", loc.getLongitude())));
		ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.ACCURACY, loc.getTime(), loc.getAccuracy()+" m"));

//    LocationData locationData = new LocationData(loc, LocationType.gps);
//    ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.MUNICIPALITY, loc.getTime(), locationData.mMuncipality));
//    ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.COUNTRYNAME, loc.getTime(), locationData.mCountryName));
		return ret;
	}

	//Used to che
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		mProviderStatus = status;
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "onProviderEnabled: " + provider);
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "onProviderDisabled: "+provider);
	}
	
	//---------------------------------------------------------
	public static BaseDataCollector parseXml(Element node) {
		LocationDataCollector c = new LocationDataCollector();
		String time = node.getAttribute("time");
		c.time = XmlUtils.convertTime(time);
		
		String listenerDelay = node.getAttribute("listenerDelay");
		c.listenerDelay = XmlUtils.convertTime(listenerDelay);
//		c.listenerMinDst = Float.valueOf(node.getAttribute("listenerMinDistance"));
		c.getLastKnown = Boolean.parseBoolean(node.getAttribute("lastKnown"));
		return c;
	}
	@Override
	public List<JSONObject> getJSONOutput(){
		List<JSONObject> ret = new ArrayList<>();
		if(getLastKnown && sLastKnown != null){
			ret.addAll(new LocationData(true, sLastKnown, locationType).convertToJSON());
		}
		synchronized(this) {
			for(Location l: mLocations){
				ret.addAll((new LocationData(l, locationType)).convertToJSON());
			}
		}
		return ret;
	}
	
	public List<DCSData> getPartialData(){
		List<DCSData> ret = new ArrayList<>();
		synchronized(this){
			if(getLastKnown && sLastKnown != null){
				ret.add(new LocationData(true, sLastKnown, locationType));
				sLastKnown = null;
			}

			if( mLocations.isEmpty() && mLastLocation != null){
				ret.add(new LocationData(mLastLocation, locationType, mProviderStatus));
			}
			for(Location l: mLocations){
				ret.add(new LocationData(l, locationType));
			}
			mLocations.clear();
		}
		return ret;
	}

}
