package com.samknows.measurement.environment;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKSimpleHttpToJsonQuery;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class QueryWlanCarrier extends SKSimpleHttpToJsonQuery implements SKSimpleHttpToJsonQuery.QueryCompletion {

  public QueryWlanCarrier() {
    super("http://dcs-mobile-fcc.samknows.com/mobile/lookup.php", null, null);
    super.mQueryCompletion = this;
  }

  abstract public void doHandleGotWlanCarrier(String wlanCarrier);

  @Override
  public void doPerformQuery() {
    super.doPerformQuery();
  }

  @Override
  public void OnQueryCompleted(boolean queryWasSuccessful, final JSONObject jsonResponse) {

    try {
      String wlanCarrier = jsonResponse.getString("organization");

      // TODO - we have the response here!

      doHandleGotWlanCarrier(wlanCarrier);

    } catch (JSONException e) {
      SKLogger.sAssert(getClass(), false);
    }
  }
}
