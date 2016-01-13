package com.samknows.measurement.net;

import android.net.ParseException;
import android.util.Log;
import android.util.Pair;

import com.samknows.libcore.SKLogger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public abstract class SimpleHttpToJsonQuery implements Callable<Void> {
  String mFullUploadUrl = null;
  byte[] mOptionalContentData = null;
  protected JSONObject mJSONResponse = null;
  protected boolean mSuccess = false;
  ArrayList<Pair<String,String>> mOptionalHeaderFields = null;

  public SimpleHttpToJsonQuery(String fullUploadUrl, byte[] optionalContentData) {
    mFullUploadUrl = fullUploadUrl;
    mOptionalContentData = optionalContentData;
    this.mJSONResponse = null;
  }

  public void setOptionalHeaderFields(ArrayList<Pair<String,String>> values) {
    mOptionalHeaderFields = values;
    //request.addValue("Accept", forHTTPHeaderField:"application/json")
    //let credentials = SKGlobalMethods.getCredentials(mEmail, password:mPassword)
    //request.addValue(credentials, forHTTPHeaderField:"Authorization")
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
  // The default, which is a POST query...
  public void doPerformQuery() {
    doPerformPost();
  }
  public void doPerformPost() {
    doPerformBase(true);
  }
  public void doPerformGet() {
    doPerformBase(false);
  }
  private void doPerformBase(boolean isPost) {

    HttpContext httpContext = null;

    HttpRequestBase httpRequestBase;
    if (isPost) {
      HttpPost httpPost = new HttpPost(mFullUploadUrl);
      httpRequestBase = httpPost;
      if (mOptionalContentData != null) {
        httpPost.setEntity(new ByteArrayEntity(mOptionalContentData));

        httpContext = new BasicHttpContext();
        httpContext.setAttribute("Content-Length", mOptionalContentData.length);
      }
    } else {
      httpRequestBase = new HttpGet(mFullUploadUrl);
    }


    if (mOptionalHeaderFields != null) {
      for (Pair<String, String> value : mOptionalHeaderFields) {
//        if (httpContext == null) {
//          httpContext = new BasicHttpContext();
//        }
        httpRequestBase.setHeader(value.first, value.second);
        //httpContext.setAttribute(value.first, value.second);
      }
    }

    try {
      HttpClient httpClient = new DefaultHttpClient();
      HttpResponse httpResponse;
      if (httpContext != null) {
        httpResponse = httpClient.execute(httpRequestBase, httpContext);
      } else {
        httpResponse = httpClient.execute(httpRequestBase);
      }
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
          return;

        } catch (ParseException e) {
          SKLogger.sAssert(false);
          call();
        } catch (IOException e) {
          SKLogger.sAssert(false);
          call();
        }
      } else {
        SKLogger.sAssert(false);
        call();
      }
    } catch (Exception e) {
      SKLogger.sAssert(false);
      try {
        call();
      } catch (Exception e1) {
        SKLogger.sAssert(false);
      }
    }
  }

}
