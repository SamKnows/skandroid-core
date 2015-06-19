package com.samknows.measurement.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.environment.LocationData;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.PassiveMetric;
import com.samknows.measurement.storage.PassiveMetric.METRIC_TYPE;
import com.samknows.measurement.test.TestResultsManager;
import com.samknows.measurement.util.OtherUtils;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ParseException;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;



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
    List<Integer> fail = new ArrayList<Integer>();
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
        TestResultsManager.saveSumbitedLogs(context, data);
      }
    }
    TestResultsManager.clearResults(context);
    for (int i : fail) {
      TestResultsManager.saveResult(context, results[i]);
    }
  }

  private boolean uploadJsonData(final DBHelper dbHelper, List<JSONObject> batches,  byte[] data, String dataAsString) {

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
            long thisBatchId = Long.valueOf(batchIdAsString).longValue();

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
                  latitude = Double.valueOf(latitudeAsDouble);

                  if (metric.has("longitude")) {
                    Double longitudeAsDouble = metric.getDouble("longitude");
                    longitude = Double.valueOf(longitudeAsDouble);
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
          JSONArray testArray = jObject.getJSONArray("tests");
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
    if ((longitude != -999.0) && (latitude != -999.0))
    {
      // if (true) {
      // Get municipality and country name!

      Geocoder geocoder = new Geocoder(SKApplication.getAppInstance().getApplicationContext(), Locale.getDefault());
      if (geocoder == null) {
        SKLogger.sAssert(false);
      } else {
        if (geocoder.isPresent() == false) {
          // Maybe we're on the Emulator!
          SKLogger.sAssert(OtherUtils.isThisDeviceAnEmulator() == true);
        } else {
          try {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            if (addressList != null && addressList.size() > 0) {
              Address address = addressList.get(0);
              String city = address.getLocality();
              String country = address.getCountryName();

              String muncipality = city;
              String countryName = country;

              JSONObject item1 = new JSONObject();
              try {
                item1.put(PassiveMetric.JSON_METRIC_NAME, "municipality");
                item1.put(PassiveMetric.JSON_DTIME, System.currentTimeMillis());
                item1.put(PassiveMetric.JSON_VALUE, muncipality);
                item1.put(PassiveMetric.JSON_TYPE, METRIC_TYPE.MUNICIPALITY);
              } catch (JSONException e) {
                SKLogger.sAssert(getClass(), false);
              }

              JSONObject item2 = new JSONObject();
              try {
                item2.put(PassiveMetric.JSON_METRIC_NAME, "country_name");
                item2.put(PassiveMetric.JSON_DTIME, System.currentTimeMillis());
                item2.put(PassiveMetric.JSON_VALUE, countryName);
                item2.put(PassiveMetric.JSON_TYPE, METRIC_TYPE.COUNTRYNAME);
              } catch (JSONException e) {
                SKLogger.sAssert(getClass(), false);
              }

              JSONArray jsonArray = new JSONArray();

              jsonArray.put(item1);
              jsonArray.put(item2);
              dbHelper.insertPassiveMetric(jsonArray, finalBatchId);
            } else {
              SKLogger.sAssert(false);
            }

          } catch (IOException e) {
            Log.e("LocationData", "Unable connect to Geocoder", e);
            SKLogger.sAssert(false);
          }
        }
      }
    }

    isSuccess = false;

    SimpleHttpToJsonQuery httpToJsonQuery = new SimpleHttpToJsonQuery(fullUploadUrl, data) {
      @Override
      public Void call() throws Exception {
        if (finalBatchId != -1) {
          isSuccess = true;

          JSONObject item1 = new JSONObject();
          try {
            item1.put(PassiveMetric.JSON_METRIC_NAME, "public_ip");
            item1.put(PassiveMetric.JSON_DTIME, System.currentTimeMillis());
            item1.put(PassiveMetric.JSON_VALUE, mJSONResponse.get("public_ip"));
            item1.put(PassiveMetric.JSON_TYPE, METRIC_TYPE.PUBLICIP);
          } catch (JSONException e) {
            SKLogger.sAssert(getClass(), false);
          }


          JSONObject item2 = new JSONObject();
          try {
            item2.put(PassiveMetric.JSON_METRIC_NAME, "submission_id");
            item2.put(PassiveMetric.JSON_DTIME, System.currentTimeMillis());
            item2.put(PassiveMetric.JSON_VALUE, mJSONResponse.get("submission_id"));
            item2.put(PassiveMetric.JSON_TYPE, METRIC_TYPE.SUBMISSIONID);

            SKApplication.getAppInstance().mLastPublicIp = mJSONResponse.get("public_ip").toString();
            SKApplication.getAppInstance().mLastSubmissionId = mJSONResponse.get("submission_id").toString();
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
        return null;
      }
    };

    httpToJsonQuery.doPerformQuery();

    return isSuccess;
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
