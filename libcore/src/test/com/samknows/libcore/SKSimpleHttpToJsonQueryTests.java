package com.samknows.libcore;

import android.app.Activity;

import com.samknows.XCT;
import com.samknows.libcore.SKOperators.ISKQueryCompleted;
import com.samknows.libcore.SKOperators.SKOperators_Return;
import com.samknows.libcore.SKOperators.SKThrottledQueryResult;

import org.json.JSONObject;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.httpclient.FakeHttp;
import org.robolectric.shadows.httpclient.FakeHttpLayer;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class SKSimpleHttpToJsonQueryTests {

  private FakeHttpLayer.RequestMatcherBuilder requestMatcherBuilder;

  @org.junit.Before
  public void setUp() throws Exception {
    FakeHttp.getFakeHttpLayer().interceptHttpRequests(true);

    requestMatcherBuilder = new FakeHttpLayer.RequestMatcherBuilder();
  }

  @Test
  public void testResponseCodeNot200() throws Exception{

    final boolean[] responseCalled = {false};

    FakeHttp.getFakeHttpLayer().addPendingHttpResponse(404, "some content that isn't JSON");

    String theURL = "dummyURL";
    byte[] optionalContentData = null;
    SKSimpleHttpToJsonQuery simpleHttpToJsonQuery = new SKSimpleHttpToJsonQuery(theURL, optionalContentData, new SKSimpleHttpToJsonQuery.QueryCompletion() {
      @Override
      public void OnQueryCompleted(boolean queryWasSuccessful, JSONObject jsonResponse) {
        responseCalled[0] = true;
        XCT.Assert(queryWasSuccessful == false);
        XCT.Assert(jsonResponse == null);
      }
    });
    simpleHttpToJsonQuery.doPerformQuery();

    XCT.Assert(responseCalled[0]);
  }

  final String validJSONString = "{\n" +
      "    \"id\": 1,\n" +
      "    \"tags\": [\"home\", \"green\"]\n" +
      "}";

  @Test
  public void testBadJSONData() throws Exception{

    final boolean[] responseCalled = {false};

    FakeHttp.getFakeHttpLayer().addPendingHttpResponse(200, "some content that isn't JSON");

    String theURL = "dummyURL";
    byte[] optionalContentData = null;
    SKSimpleHttpToJsonQuery simpleHttpToJsonQuery = new SKSimpleHttpToJsonQuery(theURL, optionalContentData, new SKSimpleHttpToJsonQuery.QueryCompletion() {
      @Override
      public void OnQueryCompleted(boolean queryWasSuccessful, JSONObject jsonResponse) {
        responseCalled[0] = true;
        XCT.Assert(queryWasSuccessful == false);
        XCT.Assert(jsonResponse == null);
      }
    });
    simpleHttpToJsonQuery.doPerformQuery();

    XCT.Assert(responseCalled[0]);
  }

  @Test
  public void testResponseCodeNot200GoodJSONData() throws Exception{

    final boolean[] responseCalled = {false};

    FakeHttp.getFakeHttpLayer().addPendingHttpResponse(400, validJSONString);

    String theURL = "dummyURL";
    byte[] optionalContentData = null;
    SKSimpleHttpToJsonQuery simpleHttpToJsonQuery = new SKSimpleHttpToJsonQuery(theURL, optionalContentData, new SKSimpleHttpToJsonQuery.QueryCompletion() {
      @Override
      public void OnQueryCompleted(boolean queryWasSuccessful, JSONObject jsonResponse) {
        responseCalled[0] = true;
        // Note that the CURRENT Implementation has this query treated with success, EVEN THOUGH the code is not 200!
        XCT.Assert(queryWasSuccessful == true);
        XCT.Assert(jsonResponse != null);
      }
    });
    simpleHttpToJsonQuery.doPerformQuery();

    XCT.Assert(responseCalled[0]);
  }

	@Test
	public void testResponseCode200GoodJSONData() throws Exception{

    final boolean[] responseCalled = {false};

    FakeHttp.getFakeHttpLayer().addPendingHttpResponse(200, validJSONString);

    String theURL = "dummyURL";
    byte[] optionalContentData = null;
    SKSimpleHttpToJsonQuery simpleHttpToJsonQuery = new SKSimpleHttpToJsonQuery(theURL, optionalContentData, new SKSimpleHttpToJsonQuery.QueryCompletion() {
      @Override
      public void OnQueryCompleted(boolean queryWasSuccessful, JSONObject jsonResponse) {
        responseCalled[0] = true;
        XCT.Assert(queryWasSuccessful == true);
        XCT.Assert(jsonResponse != null);
      }
    });
    simpleHttpToJsonQuery.doPerformQuery();

    XCT.Assert(responseCalled[0]);
	}


  @Test
  public void testPostResponseCode200GoodJSONData() throws Exception{

    final boolean[] responseCalled = {false};

    FakeHttp.getFakeHttpLayer().addPendingHttpResponse(200, validJSONString);

    String theURL = "dummyURL";
    byte[] optionalContentData = null;
    SKSimpleHttpToJsonQuery simpleHttpToJsonQuery = new SKSimpleHttpToJsonQuery(theURL, optionalContentData, new SKSimpleHttpToJsonQuery.QueryCompletion() {
      @Override
      public void OnQueryCompleted(boolean queryWasSuccessful, JSONObject jsonResponse) {
        responseCalled[0] = true;
        XCT.Assert(queryWasSuccessful == true);
        XCT.Assert(jsonResponse != null);
      }
    });
    simpleHttpToJsonQuery.doPerformPost();

    XCT.Assert(responseCalled[0]);
  }

  // TODO - is there a way to verify that POST command work as expected through the HTTP layer?
}
