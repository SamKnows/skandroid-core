package com.samknows.measurement.environment;

import android.content.Context;

import com.samknows.measurement.TestRunner.TestContext;
import com.samknows.measurement.environment.BaseDataCollector;
import com.samknows.measurement.environment.CellTowersDataCollector;
import com.samknows.measurement.environment.DCSData;
import com.samknows.measurement.environment.EnvBaseDataCollector;
import com.samknows.measurement.environment.LocationDataCollector;
import com.samknows.measurement.environment.NetworkDataCollector;
import com.samknows.measurement.environment.PhoneIdentityDataCollector;
import com.samknows.measurement.storage.ResultsContainer;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pcole on 18/04/16.
 */
public class PassiveMetricCollector {
  public PassiveMetricCollector(Context context, TestContext tc) {
    mContext = context;
    mTestContext = tc;
    mListDCSData = new ArrayList<>();
  }

  private Context mContext;
  private TestContext mTestContext;
  private List<EnvBaseDataCollector> mCollectors;
  private LocationDataCollector mLocationDataCollector;
  private List<DCSData> mListDCSData;

  public void startCollectors(List<BaseDataCollector> dataCollectors) {
    mCollectors = new ArrayList<>();
    mCollectors.add(new NetworkDataCollector(mContext));
    mCollectors.add(new CellTowersDataCollector(mContext));

    for (BaseDataCollector c : dataCollectors) {
      if (c instanceof LocationDataCollector) {
        mLocationDataCollector = (LocationDataCollector) c;
      }
    }

    for (EnvBaseDataCollector c : mCollectors) {
      c.start();
    }
    mLocationDataCollector.start(mTestContext);
  }

  public void stopCollectors() {
    for (EnvBaseDataCollector c : mCollectors) {
      c.stop();
    }
    if (mLocationDataCollector != null) {
      mLocationDataCollector.stop(mTestContext);
    }
  }

  private void collectData() {

    mListDCSData.add(new PhoneIdentityDataCollector(mContext).collect());
    for (EnvBaseDataCollector c : mCollectors) {
      mListDCSData.addAll(c.collectPartialData());
    }
    mListDCSData.addAll(mLocationDataCollector.getPartialData());
  }

  public List<JSONObject> collectMetricsIntoResultsContainer(ResultsContainer resultsContainer) {

    List<JSONObject> passiveMetrics = new ArrayList<>();
    collectData();
    for (DCSData d : mListDCSData) {
      passiveMetrics.addAll(d.getPassiveMetric());
      resultsContainer.addMetric(d.convertToJSON());
    }
    mListDCSData.clear();

    return passiveMetrics;
  }

}
