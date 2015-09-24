package com.samknows.measurement.net;

import android.net.ParseException;
import android.util.Log;

import com.samknows.libcore.SKLogger;

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
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class SimpleHttpToJsonQuery implements Callable<Void> {
  String mFullUploadUrl = null;
  byte[] mOptionalContentData = null;
  protected JSONObject mJSONResponse = null;
  protected boolean mSuccess = false;

  public SimpleHttpToJsonQuery(String fullUploadUrl, byte[] optionalContentData) {
    mFullUploadUrl = fullUploadUrl;
    mOptionalContentData = optionalContentData;
    this.mJSONResponse = null;
  }

  public boolean getSuccess() {
    return mSuccess;
  }

  // This may be overriden *entirely* by subclasses...
  // for example, if the data is *not* JSON!
  public boolean processResponse(int responseCode, String responseDataAsString) {
    try {
      // The default version assumes the data is JSON!
      // e.g. {"public_ip":"89.105.103.193","submission_id":"58e80db491ee3f7a893aee307dc7f5e1"}
      //Log.d(TAG, "Process response from server as string, to extract data from the JSON!: " + jsonString);

      JSONObject jsonResponse = new JSONObject(responseDataAsString);
      SKLogger.sAssert(jsonResponse != null);

      mJSONResponse = jsonResponse;
      return true;

    } catch (ParseException e) {
      SKLogger.sAssert(false);
    } catch (Exception e) {
      SKLogger.sAssert(false);
    }

    return false;
  }

  // The response comes from the call() method!
  public void doPerformQuery() {
    HttpPost httpPost = new HttpPost(mFullUploadUrl);

    if (mOptionalContentData != null) {
      httpPost.setEntity(new ByteArrayEntity(mOptionalContentData));

      HttpContext httpContext = new BasicHttpContext();
      httpContext.setAttribute("Content-Length", mOptionalContentData.length);
    }

    try {
      HttpClient httpClient = new DefaultHttpClient();
      HttpResponse httpResponse = httpClient.execute(httpPost);
      StatusLine sl = httpResponse.getStatusLine();
      mSuccess = sl.getStatusCode() == HttpStatus.SC_OK && sl.getReasonPhrase().equals("OK");
      int code = sl.getStatusCode();
      //Log.d(TAG, "submitting test results to server: " + isSuccess);

      // http://stackoverflow.com/questions/15704715/getting-json-response-android
      HttpEntity entity = httpResponse.getEntity();

      if (entity != null) {
        try {
          mSuccess = processResponse(code, EntityUtils.toString(entity));
          call();

        } catch (ParseException e) {
          SKLogger.sAssert(false);
        } catch (IOException e) {
          SKLogger.sAssert(false);
        }
      }
    } catch (Exception e) {
      SKLogger.sAssert(false);
    }
  }
}
