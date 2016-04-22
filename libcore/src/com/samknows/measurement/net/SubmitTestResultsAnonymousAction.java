package com.samknows.measurement.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKSimpleHttpToJsonQuery;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.environment.LocationDataCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.PassiveMetric;
import com.samknows.measurement.storage.PassiveMetric.METRIC_TYPE;
import com.samknows.measurement.storage.ResultsContainer;
import com.samknows.measurement.storage.TestResultsManager;
import com.samknows.measurement.util.OtherUtils;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;


public class SubmitTestResultsAnonymousAction {
  static final String TAG = "SubmitTestResAnymAct";

  protected Context context;
  protected boolean isSuccess = false;

  public SubmitTestResultsAnonymousAction(Context _context) {
    context = _context;
  }

  // http://stackoverflow.com/questions/6198986/how-can-i-replace-non-printable-unicode-characters-in-java
  private String getStringWithControlsStripped(String myString) {
    StringBuilder newString = new StringBuilder(myString.length());
    for (int offset = 0; offset < myString.length();)
    {
      int codePoint = myString.codePointAt(offset);
      offset += Character.charCount(codePoint);

      // Replace invisible control characters and unused code points
      switch (Character.getType(codePoint))
      {
        case Character.CONTROL:     // \p{Cc}
        case Character.FORMAT:      // \p{Cf}
        case Character.PRIVATE_USE: // \p{Co}
        case Character.SURROGATE:   // \p{Cs}
        case Character.UNASSIGNED:  // \p{Cn}
          newString.append('?');
          break;
        default:
          newString.append(Character.toChars(codePoint));
          break;
      }
    }

    String result = newString.toString();

    return result;
  }

  public void execute() {

    DBHelper dbHelper = new DBHelper(context);
    List<JSONObject> batches = dbHelper.getTestBatches();

    String[] results = TestResultsManager.getJSONDataAsStringArray(context);
    List<Integer> fail = new ArrayList<>();
    for (int i = 0; i < results.length; i++) {
      byte[] data = new byte[0];
      try {
        // Deal with corrupted UTF-8 data...!
        results[i] = getStringWithControlsStripped(results[i]);
        data = results[i].getBytes("UTF-8");
      } catch (UnsupportedEncodingException e) {
        SKLogger.sAssert(false);
      }
      if (data == null) {
        Log.d(TAG, "no results to be submitted");
        break;
      }

      String dataAsString = results[i];
      uploadJsonData(dbHelper, batches, data, dataAsString);

      if (!isSuccess) {
        fail.add(i);
        TestResultsManager.clearResults(context);
        TestResultsManager.saveSubmittedLogs(context, data);
      }
    }
    TestResultsManager.clearResults(context);
    for (int i : fail) {
      TestResultsManager.saveResult(context, results[i]);
    }
  }

  // https://code.google.com/p/android/issues/detail?id=38009
  public static List<Address> getFromLocation(double lat, double lng, int maxResult){

    String address = String.format(Locale.ENGLISH,"http://maps.googleapis.com/maps/api/geocode/json?latlng=%1$f,%2$f&sensor=true&language="+Locale.getDefault().getCountry(), lat, lng);
    HttpGet httpGet = new HttpGet(address);
    HttpClient client = new DefaultHttpClient();
    HttpResponse response;
    StringBuilder stringBuilder = new StringBuilder();

    List<Address> retList = null;

    try {
      response = client.execute(httpGet);
      HttpEntity entity = response.getEntity();
      InputStream stream = entity.getContent();
      int b;
      while ((b = stream.read()) != -1) {
        stringBuilder.append((char) b);
      }

      JSONObject jsonObject = new JSONObject();
      jsonObject = new JSONObject(stringBuilder.toString());

      retList = new ArrayList<>();

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

          //JSONArray array = result.getJSONArray("results");
          JSONArray components = place.getJSONArray("address_components");
          for( int i = 0 ; i < components.length() ; i++ ) {
            JSONObject component = components.getJSONObject(i);
            JSONArray types = component.getJSONArray("types");
            for (int j = 0; j < types.length(); j++) {
              if (types.getString(j).equals("locality")) {
                cityName = component.getString("long_name");
              } else if (types.getString(j).equals("country")) {
                countryName = component.getString("long_name");
              }
            }
          }

          Address addr = new Address(currentLocale);

          //addr.setAddressLine(0, indiStr);
          addr.setLocality(cityName);
          addr.setCountryName(countryName);

          retList.add(addr);
        }
      }


    } catch (ClientProtocolException e) {
      //Log.e(MyGeocoder.class.getName(), "Error calling Google geocode webservice.", e);
      SKLogger.sAssert(false);
    } catch (IOException e) {
      //Log.e(MyGeocoder.class.getName(), "Error calling Google geocode webservice.", e);
      SKLogger.sAssert(false);
    } catch (JSONException e) {
      //Log.e(MyGeocoder.class.getName(), "Error parsing Google geocode webservice response.", e);
      SKLogger.sAssert(false);
    } catch (Exception e) {
      SKLogger.sAssert(false);
    }

    return retList;
  }


  private boolean uploadJsonData(final DBHelper dbHelper, List<JSONObject> batches, final byte[] data, String dataAsString) {

    double longitude = -999.0;
    double latitude = -999.0;

    long batchId = -1;
    JSONObject jObject = null;
    try {
      if (dataAsString.length() > 0) {
        jObject = new JSONObject(dataAsString);
      }
      if (jObject != null) {
        if (jObject.has("batch_id")) {
          try {
            String batchIdAsString = jObject.getString("batch_id");
            long thisBatchId = Long.valueOf(batchIdAsString);

            //    					long timeStamp = jObject.getLong("timestamp");
            //    					// Finally - do we have have an entry in the database for this?
            //    					// select id from test_batch where dtime = 1385458870802
            //
            if (batches != null) {
              int theCount = batches.size();
              int theIndex;
              for (theIndex = 0; theIndex < theCount; theIndex++) {
                JSONObject theBatch = batches.get(theIndex);
                Long theBatchId = theBatch.getLong("_id");
                if (theBatchId == thisBatchId) {
                  // Got it!
                  batchId = thisBatchId;

                  break;
                }
              }

            }
          } catch (JSONException e) {
            SKLogger.sAssert(getClass(), false);
          }
        }


        if (jObject.has("metrics") == false) {
          SKLogger.sAssert(false);
        } else {
          JSONArray metricsArray = jObject.getJSONArray("metrics");
          int items = metricsArray.length();
          int metricIndex = 0;
          for (metricIndex = 0; metricIndex < items; metricIndex++) {
            JSONObject metric = metricsArray.getJSONObject(metricIndex);
            if (metric.has("type")) {
              if (metric.getString("type").equals("location")) {
                if (metric.has("latitude")) {
                  Double latitudeAsDouble = metric.getDouble("latitude");
                  latitude = latitudeAsDouble;

                  if (metric.has("longitude")) {
                    Double longitudeAsDouble = metric.getDouble("longitude");
                    longitude = longitudeAsDouble;
                  } else {
                    SKLogger.sAssert(false);
                  }
                } else {
                  SKLogger.sAssert(false);
                }

                break;
              }
            } else {
              SKLogger.sAssert(false);
            }
          }
        }
      }
    } catch (JSONException e) {
      SKLogger.sAssert(getClass(), false);
      jObject = null;
    }

    // If this is OLD data, it will NOT have a batch id...
    //SKLogger.sAssert(getClass(), batchId != -1);

    SK2AppSettings settings = SK2AppSettings.getSK2AppSettingsInstance();
    Uri builtUri = Uri.parse(SKApplication.getAppInstance().getBaseUrlForUpload());
    String fullUploadUrl = builtUri.buildUpon().path(settings.submit_path).toString();

    if (SKApplication.getAppInstance().getShouldTestResultsBeUploadedToTestSpecificServer() == true) {
      // TODO: For SOME systems, we need to determine the server to use FROM THE DATA!
      if (jObject == null) {
        SKLogger.sAssert(false);
      } else {
        try {

          String targetServerUrl = null;
          JSONArray testArray = jObject.getJSONArray(ResultsContainer.JSON_TESTS);
          int count = testArray.length();
          int index;
          for (index = 0; index < count; index++) {
            JSONObject theTestDict = testArray.getJSONObject(index);
            //NSLog(@"DEBUG: description = %@", jsonObject.description);
            if (theTestDict.has("target")) {
                targetServerUrl = theTestDict.getString("target");
                break;
            }
          }

          if (targetServerUrl == null) {
            SKLogger.sAssert(false);
          } else {
            Log.d(TAG, "targetServerUrl=" + targetServerUrl);

            if (targetServerUrl.startsWith("http:"))  {
              // Already starts http
            } else {
              // Need to add http:// prefix!
              targetServerUrl = String.format("http://%s", targetServerUrl);
              SKLogger.sAssert(targetServerUrl.startsWith("http://"));

              // Use this overriding server URL!
              targetServerUrl =String.format("%s/log/receive_mobile.php", targetServerUrl);
              fullUploadUrl = targetServerUrl;
              Log.d(TAG, "overriding fullUploadUrl=" + fullUploadUrl);
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    }

    final long finalBatchId = batchId;

    // Get Geocoder data!
    if ((longitude == -999.0) && (latitude == -999.0)) {
      Pair<Location,ScheduleConfig.LocationType> lastLocationPair = LocationDataCollector.sGetLastKnownLocation();
      if (lastLocationPair != null) {
        Location lastLocation = lastLocationPair.first;
        if (lastLocation != null) {
          latitude = lastLocation.getLatitude();
          longitude = lastLocation.getLongitude();
        }
      }

      /*
      LocationManager manager = (LocationManager) SKApplication.getAppInstance().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
      if (manager == null) {
        SKLogger.sAssert(false);
      } else {
        Location location = manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (location == null) {
          location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (location == null) {
          location = manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }
        if (location != null) {
          latitude = location.getLatitude();
          longitude = location.getLongitude();
        }
      }
      */
    }

    if ((longitude != -999.0) && (latitude != -999.0))
    {
      //
      // There are TWO ways to get geolocation data.
      // 1) The first one is to use the Geocoder API - but that is very unreliable!
      // 2) The second one uses an http query direct to Google.
      //

      //
      // 1) First approach - use the Geocoder API - it works sometimes!
      //

      List<Address> addressList = null;
      Geocoder geocoder = new Geocoder(SKApplication.getAppInstance().getApplicationContext(), Locale.getDefault());
      if (geocoder == null) {
        SKLogger.sAssert(false);
      } else {
        if (Geocoder.isPresent() == false) {
          // Maybe we're on the Emulator!
          SKLogger.sAssert(OtherUtils.isThisDeviceAnEmulator() == true);
        } else {
          try {
            addressList = geocoder.getFromLocation(latitude, longitude, 1);

          } catch (IOException e) {
            // https://code.google.com/p/android/issues/detail?id=38009
            // This is quite a common problem!
            //Log.e("LocationData", "Unable connect to Geocoder", e);
            //SKLogger.sAssert(false);
          }
        }
      }

      //
      // 2) Second approach - use an http query direct to Google.
      //

      if (addressList == null || addressList.size() == 0) {
        // https://code.google.com/p/android/issues/detail?id=38009
        addressList = getFromLocation(latitude, longitude, 1);
      }

      if (addressList != null) {
        processGeocoderAddressList(dbHelper, finalBatchId, addressList);
      }
    }

    isSuccess = false;

    SKSimpleHttpToJsonQuery httpToJsonQuery = new SKSimpleHttpToJsonQuery(fullUploadUrl, data, new SKSimpleHttpToJsonQuery.QueryCompletion() {
      @Override
      public void OnQueryCompleted(boolean queryWasSuccessful, final JSONObject jsonResponse) {
//        try {
//          String bufferAsUtf8String = new String(data, "UTF-8");
//          Log.d("SubmitTestResults", "********* uploaded data: queryWasSuccessful=" + queryWasSuccessful + ", data was=" + bufferAsUtf8String);
//        } catch (UnsupportedEncodingException e) {
//          SKLogger.sAssert(false);
//        }
        Log.d("SubmitTestResults", "********* uploaded data: queryWasSuccessful=" + queryWasSuccessful);

        if (finalBatchId != -1) {
          isSuccess = true;

          JSONObject item1 = new JSONObject();
          try {
            item1.put(PassiveMetric.JSON_METRIC_NAME, "public_ip");
            item1.put(PassiveMetric.JSON_DTIME, System.currentTimeMillis());
            item1.put(PassiveMetric.JSON_VALUE, jsonResponse.get("public_ip"));
            item1.put(PassiveMetric.JSON_TYPE, METRIC_TYPE.PUBLICIP);
          } catch (JSONException e) {
            SKLogger.sAssert(getClass(), false);
          }


          JSONObject item2 = new JSONObject();
          try {
            item2.put(PassiveMetric.JSON_METRIC_NAME, "submission_id");
            item2.put(PassiveMetric.JSON_DTIME, System.currentTimeMillis());
            item2.put(PassiveMetric.JSON_VALUE, jsonResponse.get("submission_id"));
            item2.put(PassiveMetric.JSON_TYPE, METRIC_TYPE.SUBMISSIONID);

            SKApplication.getAppInstance().mLastPublicIp = jsonResponse.get("public_ip").toString();
            SKApplication.getAppInstance().mLastSubmissionId = jsonResponse.get("submission_id").toString();
          } catch (JSONException e) {
            SKLogger.sAssert(getClass(), false);
          }

          JSONArray jsonArray = new JSONArray();

          jsonArray.put(item1);
          jsonArray.put(item2);
          dbHelper.insertPassiveMetric(jsonArray, finalBatchId);

          // Force the History screen to re-query, so it can show the submission id/public ip
          LocalBroadcastManager.getInstance(SKApplication.getAppInstance().getApplicationContext()).sendBroadcast(new Intent("refreshUIMessage"));
        }
      }

    });

    httpToJsonQuery.doPerformQuery();

    return isSuccess;
  }

  private void processGeocoderAddressList(DBHelper dbHelper, long finalBatchId, List<Address> addressList) {
    if (addressList != null && addressList.size() > 0) {
      Address address = addressList.get(0);
      String city = address.getLocality();
      String country = address.getCountryName();

      String muncipality = city;
      JSONObject item1 = null;
      if (muncipality != null) {
        item1 = new JSONObject();
        try {
          item1.put(PassiveMetric.JSON_METRIC_NAME, "municipality");
          item1.put(PassiveMetric.JSON_DTIME, System.currentTimeMillis());
          item1.put(PassiveMetric.JSON_VALUE, muncipality);
          item1.put(PassiveMetric.JSON_TYPE, METRIC_TYPE.MUNICIPALITY);
        } catch (JSONException e) {
          SKLogger.sAssert(getClass(), false);
        }
      }

      String countryName = country;
      JSONObject item2 = null;
      if (countryName != null) {
        item2 = new JSONObject();
        try {
          item2.put(PassiveMetric.JSON_METRIC_NAME, "country_name");
          item2.put(PassiveMetric.JSON_DTIME, System.currentTimeMillis());
          item2.put(PassiveMetric.JSON_VALUE, countryName);
          item2.put(PassiveMetric.JSON_TYPE, METRIC_TYPE.COUNTRYNAME);
        } catch (JSONException e) {
          SKLogger.sAssert(getClass(), false);
        }
      }

      if (item1 != null || item2 != null) {
        JSONArray jsonArray = new JSONArray();

        if (item1 != null) {
          jsonArray.put(item1);
        }

        if (item2 != null) {
          jsonArray.put(item2);
        }
        dbHelper.insertPassiveMetric(jsonArray, finalBatchId);
      }
    } else {
      SKLogger.sAssert(false);
    }
  }

  private String privateBuildUrl() {

    SK2AppSettings settings = SK2AppSettings.getSK2AppSettingsInstance();
    //SKLogger.sAssert(getClass(), settings.protocol_scheme.length() > 0);
    //Uri.Builder scheme = builder.scheme(settings.protocol_scheme);

    Uri builtUri = Uri.parse(SKApplication.getAppInstance().getBaseUrlForUpload());

    String urlString = builtUri.buildUpon().path(settings.submit_path).toString();

    return urlString;

//		return new Uri.Builder().scheme(settings.protocol_scheme)
//				.authority(settings.getServerBaseUrl())
//				.path(settings.submit_path).build().toString();
  }
}
