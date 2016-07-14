package com.samknows.libcore;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.Date;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.samknows.measurement.util.OtherUtils;
import com.samknows.measurement.util.SKDateFormat;

public class SKOperators {
  public enum SKOperators_Return {
    SKOperators_Return_NoThrottleQuery,
    SKOperators_Return_FiredThrottleQueryAwaitCallback
  }

  public class SKThrottledQueryResult {

    public SKOperators_Return returnCode;
    public String timestamp; // Unix time - seconds since 1970
    //public String datetimeUTCMilliZ;  // UTC string
    public String datetimeUTCSimple;  // UTC string
    public String carrier;

    public SKThrottledQueryResult() {
      Date now = new Date();

      returnCode = SKOperators_Return.SKOperators_Return_NoThrottleQuery;
      timestamp = String.valueOf(now.getTime());
      //datetimeUTCMilliZ = SKDateFormat.sGetDateAsIso8601StringMilliZ(now);
      datetimeUTCSimple = SKDateFormat.sGetDateAsIso8601String(now);
      carrier = "";
    }
  }

  public interface ISKQueryCompleted {
    void onQueryCompleted(Exception e, long responseCode, String responseDataAsString);
  }

  private Context mContext = null;

  JSONArray mpOperatorArray = null;

  private String loadOperatorData() {
    InputStream theInputStream = null;
    try {
      theInputStream = this.mContext.getAssets().open("operators.json");
    } catch (IOException e) {
      return null;
    }

    Writer writer = new StringWriter();
    char[] buffer = new char[1024];
    try {
      Reader reader = new BufferedReader(new InputStreamReader(theInputStream));
      int n;
      while ((n = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, n);
      }
    } catch (UnsupportedEncodingException e) {
      SKPorting.sAssert(getClass(), false);
      return null;
    } catch (IOException e) {
      SKPorting.sAssert(getClass(), false);
      return null;
    } finally {
      try {
        theInputStream.close();
      } catch (IOException e) {
        SKPorting.sAssert(getClass(), false);
      }
    }

    String jsonString = writer.toString();
    return jsonString;
  }

  private SKOperators(Context context) {
    this.mContext = context;

    String jsonString = loadOperatorData();
    if (jsonString == null) {
      return;
    }

    try {
      JSONObject mainObject = new JSONObject(jsonString);
      mpOperatorArray = mainObject.getJSONArray("operators");

      int items = mpOperatorArray.length();
      if (items <= 0) {
        SKPorting.sAssert(getClass(), false);
      }

      int index = 0;
      for (index = 0; index < items; index++) {
        JSONObject operator = mpOperatorArray.getJSONObject(index);
        try {
          String value = operator.getString("name");
          SKPorting.sAssert(getClass(), value.length() > 0);
        } catch (JSONException e) {
          SKPorting.sAssert(getClass(), false);
        }

        try {
          String value = operator.getString("class");
          SKPorting.sAssert(getClass(), value.length() > 0);

          SKPorting.sAssert(getClass(), (value.equals("isthrottledwebservice")) || (value.equals("isthrottledwebservice_test")));
        } catch (JSONException e) {
          SKPorting.sAssert(getClass(), false);
        }

        try {
          JSONArray value = operator.getJSONArray("mcc+mnc");
          SKPorting.sAssert(getClass(), value.length() > 0);

          int theMccMncArrayItems = value.length();
          int theMccMncIndex = 0;
          for (theMccMncIndex = 0; theMccMncIndex < theMccMncArrayItems; theMccMncIndex++) {
            String theText = value.getString(theMccMncIndex);
            SKPorting.sAssert(getClass(), theText.length() >= 5);
            SKPorting.sAssert(getClass(), theText.length() <= 6);
          }
        } catch (JSONException e) {
          SKPorting.sAssert(getClass(), false);
        }

        try {
          String value = operator.getString("url");
          SKPorting.sAssert(getClass(), value.length() > 0);
        } catch (JSONException e) {
          SKPorting.sAssert(getClass(), false);
        }

        try {
          String value = operator.getString("username");
          SKPorting.sAssert(getClass(), value.length() > 0);
        } catch (JSONException e) {
          SKPorting.sAssert(getClass(), false);
        }

        try {
          String value = operator.getString("password");
          SKPorting.sAssert(getClass(), value.length() > 0);
        } catch (JSONException e) {
          SKPorting.sAssert(getClass(), false);
        }
      }
    } catch (JSONException e) {
      SKPorting.sAssert(getClass(), false);
    }
  }

  //
  // Singleton method...
  //
  private static SKOperators sOperatorInstance = null;

  public static synchronized SKOperators getInstance(Context context) {
    if (sOperatorInstance == null) {
      sOperatorInstance = new SKOperators(context);
    }

    return sOperatorInstance;
  }

  /**
   * Returns the SHA-1 hashcode of the given string.
   *
   * @param s the string to be hashed.
   * @return the SHA-1 hashcode of {@code s}.
   */
  // http://stackoverflow.com/questions/5980658/how-to-sha1-hash-a-string-in-android
  private String getSha1(String s) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    md.update(s.getBytes());
    byte[] bytes = md.digest();
    StringBuilder buffer = new StringBuilder();
    for (byte aByte : bytes) {
      String tmp = Integer.toString((aByte & 0xff) + 0x100, 16).substring(1);
      buffer.append(tmp);
    }
    return buffer.toString();
  }

  //
  //
  //

  // Returns object containing immediate results.
  // The async part of the result, will occur later (if at all!)

  // Private method (can be used for mock testing purposes)
  public SKThrottledQueryResult fireThrottledWebServiceQueryForDeviceMccMnc(String deviceMccMnc, final ISKQueryCompleted callback) {

    final SKThrottledQueryResult throttledQueryResult = new SKThrottledQueryResult();

    if (mpOperatorArray == null) {
      // No operators at all!
      return throttledQueryResult;
    }

    String lookForService = "isthrottledwebservice";
    if (OtherUtils.isThisDeviceAnEmulator() == true) {
      if (OtherUtils.isDebuggable(mContext)) {
        // On emulator in debug mode...!
        SKPorting.sAssert(getClass(), deviceMccMnc.length() == 0);
        lookForService = "isthrottledwebservice_test";
        if (deviceMccMnc.length() == 0) {
          deviceMccMnc = "tester";
        }
      }
    }

    Log.d(getClass().getName(), "DEBUG: search for (" + lookForService + "), using deviceMccMnc=(" + deviceMccMnc + ")");

    try {
      // Every entry must contain valid data!
      int items = mpOperatorArray.length();
      if (items <= 0) {
        SKPorting.sAssert(getClass(), false);
      }

      int index = 0;
      for (index = 0; index < items; index++) {
        JSONObject operator = mpOperatorArray.getJSONObject(index);
        String theClassName = null;
        theClassName = operator.getString("class");


        if (theClassName.equals(lookForService)) {
          // This is a potential match by MMC/MNC!

          JSONArray value = operator.getJSONArray("mcc+mnc");
          SKPorting.sAssert(getClass(), value.length() > 0);

          int theMccMncArrayItems = value.length();
          int theMccMncIndex = 0;
          for (theMccMncIndex = 0; theMccMncIndex < theMccMncArrayItems; theMccMncIndex++) {
            String mccMnc = value.getString(theMccMncIndex);
            if (mccMnc.equals(deviceMccMnc)) {
              // Match!

              final String urlString = operator.getString("url");
              throttledQueryResult.carrier = operator.getString("name");

							/*
    						The following items shall be sent to the API via the REQUEST HEADERS:
							username - provide
							password - combination (concatenation of first 8 numbers/characters of UTC field
							and the CODEWORD, and then SHA-1 hashed ...!)
							UTC      - UTC time.
							For example, the password will be sha1sum(2014-02-codeword) ...
     						*/
              String username = operator.getString("username");
              String utcDateTimeSimple = throttledQueryResult.datetimeUTCSimple;
              String passwordBase = operator.getString("password");

              StringBuilder passwordSb = new StringBuilder();
              passwordSb.append(utcDateTimeSimple.substring(0, 8));
              passwordSb.append(passwordBase);

              // The following will be something like "2014-03-dummypassword"
              String passwordTemp = passwordSb.toString();

              // The following will be something like "f4401e54f4472a576281375e3f89f87a8e7547af"
              String password = getSha1(passwordTemp);

              throttledQueryResult.returnCode = SKOperators_Return.SKOperators_Return_FiredThrottleQueryAwaitCallback;

              AsyncHttpClient client = new AsyncHttpClient();
              client.getHttpClient().getParams().setParameter("username", username);
              client.getHttpClient().getParams().setParameter("UTC", utcDateTimeSimple);
              client.getHttpClient().getParams().setParameter("password", password);

              Log.d(getClass().getName(), "DEBUG: fire throttle query at (" + urlString + ")");

              final AsyncHttpClient theClient = client;

              // The internal can interfere with the external run loop.
              // So, we fire this query under ownership the MAIN UI thread, to prevent the hang.
              // TODO - investigate if we could fire this instead in a separate thread.
              new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                  // Code here will run in UI thread
                  theClient.get(urlString, new AsyncHttpResponseHandler() {
                    int responseCode = 200;

                    @Override
                    public void sendResponseMessage(HttpResponse response) {
                      responseCode = response.getStatusLine().getStatusCode();
                      try {
                        super.sendResponseMessage(response);
                      } catch (IOException e) {
                        SKPorting.sAssert(getClass(), false);
                      }
                    }

                    @Override
                    public void onSuccess(int statusCode,
                                          Header[] headers, byte[] responseBody) {
                      String response = String.valueOf(responseBody);
                      String trimmed = response.trim();
                      callback.onQueryCompleted(null, responseCode, trimmed);
                    }

                    @Override
                    public void onFailure(int statusCode,
                                          Header[] headers, byte[] responseBody,
                                          Throwable error) {
                      callback.onQueryCompleted(new Exception(error), responseCode, "");

                    }
                  });
                }
              });


              return throttledQueryResult;
            }
          }
        }
      }
    } catch (JSONException e) {
      // An error! Ignore this...
      SKPorting.sAssert(getClass(), false);
      throttledQueryResult.returnCode = SKOperators_Return.SKOperators_Return_NoThrottleQuery;
    } catch (Exception e) {
      SKPorting.sAssert(getClass(), false);
      throttledQueryResult.returnCode = SKOperators_Return.SKOperators_Return_NoThrottleQuery;
    }

    // No operator match!
    Log.d(getClass().getName(), "DEBUG: no isthrottledwebservice found for device...");

    return throttledQueryResult;
  }

  // TODO: Public method
  public SKThrottledQueryResult fireThrottledWebServiceQueryWithCallback(ISKQueryCompleted callback) {
    TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    String deviceMccMnc = manager.getSimOperator();
    if (OtherUtils.isThisDeviceAnEmulator() == true) {
      if (OtherUtils.isDebuggable(mContext)) {
        deviceMccMnc = "";
      }
    }
    return fireThrottledWebServiceQueryForDeviceMccMnc(deviceMccMnc, callback);
  }
}
