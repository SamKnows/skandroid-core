package com.samknows.measurement.environment;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.net.SimpleHttpToJsonQuery;

import org.json.JSONException;

/**
 * Created by pcole on 31/07/15.
 */
public abstract class QueryWlanCarrier extends SimpleHttpToJsonQuery {

  public QueryWlanCarrier() {
    super("http://dcs-mobile-fcc.samknows.com/mobile/lookup.php", null);
  }

  abstract public void doHandleGotWlanCarrier(String wlanCarrier);

  @Override
  public void doPerformQuery() {
    super.doPerformQuery();
  }

  @Override
  public Void call() throws Exception {

    try {
      String wlanCarrier = mJSONResponse.getString("organization");

      // TODO - we have the response here!

      doHandleGotWlanCarrier(wlanCarrier);

    } catch (JSONException e) {
      SKLogger.sAssert(getClass(), false);
    }

    return null;
  }
}
