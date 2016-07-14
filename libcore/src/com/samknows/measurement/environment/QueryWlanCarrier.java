package com.samknows.measurement.environment;

import com.samknows.libcore.SKPorting;
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
      if (jsonResponse == null) {
        SKPorting.sAssert(false);
      } else {
        String wlanCarrier = jsonResponse.getString("organization");
        doHandleGotWlanCarrier(wlanCarrier);
      }

    } catch (JSONException e) {
      SKPorting.sAssert(getClass(), false);
    }
  }
}
