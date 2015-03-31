package com.samknows.measurement.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.storage.PassiveMetric;
import com.samknows.measurement.storage.PassiveMetric.METRIC_TYPE;
import com.samknows.measurement.test.TestResultsManager;

import android.content.Context;
import android.content.Intent;
import android.net.ParseException;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class SubmitTestResultsAnonymousAction {
  static final String TAG = "SubmitTestResultsAnonymousAction";

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

      long batchId = -1;
      String dataAsString = results[i];
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
        }
      } catch (JSONException e) {
        SKLogger.sAssert(getClass(), false);
        jObject = null;
      }

      // If this is OLD data, it will NOT have a batch id...
      //SKLogger.sAssert(getClass(), batchId != -1);

      HttpContext httpContext = new BasicHttpContext();
      HttpClient httpClient = new DefaultHttpClient();
      HttpPost httpPost = new HttpPost(buildUrl());
      httpPost.setEntity(new ByteArrayEntity(data));
      httpContext.setAttribute("Content-Length", data.length);
      try {
        HttpResponse httpResponse = httpClient.execute(httpPost);
        StatusLine sl = httpResponse.getStatusLine();
        isSuccess = sl.getStatusCode() == HttpStatus.SC_OK
            && sl.getReasonPhrase().equals("OK");
        int code = sl.getStatusCode();
        Log.d(TAG, "submitting test results to server: " + isSuccess);

        // http://stackoverflow.com/questions/15704715/getting-json-response-android
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
          try {
            String jsonString = EntityUtils.toString(entity);
            // e.g. {"public_ip":"89.105.103.193","submission_id":"58e80db491ee3f7a893aee307dc7f5e1"}
            Log.d(TAG, "Process response from server as string, to extract data from the JSON!: " + jsonString);

            JSONObject jsonResponse = new JSONObject(jsonString);

            if (batchId != -1) {

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

//							metric_type = metric.getString(PassiveMetric.JSON_METRIC_NAME);
//							dtime = metric.getLong(PassiveMetric.JSON_DTIME);
//							value = metric.getString(PassiveMetric.JSON_VALUE);
//							type = metric.getString(PassiveMetric.JSON_TYPE);
//							insertPassiveMetric(metric_type, type, dtime, value

              JSONArray jsonArray = new JSONArray();

              jsonArray.put(item1);
              jsonArray.put(item2);
              dbHelper.insertPassiveMetric(jsonArray, batchId);

              // Force the History screen to re-query, so it can show the submission id/public ip
              LocalBroadcastManager.getInstance(SKApplication.getAppInstance().getApplicationContext()).sendBroadcast(new Intent("refreshUIMessage"));
            }

          } catch (ParseException e) {
            SKLogger.sAssert(getClass(), false);
          } catch (IOException e) {
            SKLogger.sAssert(getClass(), false);
          }
        }
      } catch (Exception e) {
        SKLogger.e(this, "failed to submit results to server", e);
        isSuccess = false;
      }

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

  public String buildUrl() {

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
