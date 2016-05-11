package com.samknows.SKKit.SKMobileApp.Database;

import android.app.Activity;
import android.content.Context;

import com.samknows.XCT;
import com.samknows.libcore.BuildConfig;

import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;

// NOTE: Run the test EITHER from Android Studio, OR from command line with "./gradlew test"

@RunWith(RobolectricGradleTestRunner.class)
//@Config(constants = BuildConfig.class,application = SKMobileApplication.class, sdk = 19)
@Config(constants = BuildConfig.class, sdk = 19)
public class SKMDatabaseTest {

  @org.junit.Before
  public void setUp() throws Exception {
  }

  @org.junit.After
  public void tearDown() throws Exception {
    // https://github.com/robolectric/robolectric/issues/1890
    // "Robolectric testing has been designed on the assumption that no state is preserved
    // between tests & you're always starting from scratch.
    // Singletons & other static state maintained by your
    // application and/or test can break that assumption"
    SKMDatabase.sResetSingletons();
  }

  @org.junit.Test
  public void testBasicQuery() throws Exception {
    // Create a dummy Context!
    // http://robolectric.org/activity-lifecycle.html
    Activity activity = Robolectric.buildActivity(Activity.class).create().get();
    Context context = activity;

    // IN the context of Roboelectic, we started empty!

    ArrayList<SKMStoredResult> results = SKMStoredResult.sReadSKMStoredResults(context);
    org.junit.Assert.assertTrue(results != null);

    // Robolectric always enforces a reset of the database for each test!
    org.junit.Assert.assertTrue(results.size() == 0);
  }

  @org.junit.Test
  public void testClearWriteRetrieve() throws Exception {
    // Create a dummy Context!
    // http://robolectric.org/activity-lifecycle.html
    Activity activity = Robolectric.buildActivity(Activity.class).create().get();
    Context context = activity;

    // Performs a query (expected to return)
    // In response to that query, deletes any items found, then inserts an object.
    // Asserts object values inserted as expected.
    // Performs another query (expected to return)
    // In response to that query, asserts 1 object found with expected properties.
    // In response to that query, deletes that object
    // Performs another query (expected to return)
    // Asserts no objects returned by that query (i.e. the object was deleted)
    ArrayList<SKMStoredResult> results = SKMStoredResult.sReadSKMStoredResults(context);
    org.junit.Assert.assertTrue(results != null);

    // Robolectric always enforces a reset of the database for each test!
    org.junit.Assert.assertTrue(results.size() == 0);

    Date now = new Date();
    String metricId = SKMStoredResult.cDevice_LatencyLossJitter;
    Double expectedLatency = 97.0;
    Double expectedLoss = 98.0;
    Double expectedJitter = 99.0;
    SKMStoredResult item = SKMStoredResult.sCreateInManagedObjectContext_FromDevice_LatencyLossJitter(context, now, expectedLatency, expectedLoss, expectedJitter, true);
    XCT.AssertTrue(item != null);
    XCT.AssertTrue(item.unit_id.equals(SKMStoredResult.cUnitIdMobileDevice));
    XCT.AssertTrue(item.dtime.getTime() == now.getTime());
    XCT.AssertTrue(item.metric_id.equals(metricId));
    XCT.AssertDoubleApproxEqual(item.property_1_value, expectedLatency);
    XCT.AssertTrue(item.property_1_id.equals(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_latencyMs));
    XCT.AssertDoubleApproxEqual(item.property_2_value, expectedLoss);
    XCT.AssertTrue(item.property_2_id.equals(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_lossPercent));
    XCT.AssertDoubleApproxEqual(item.property_3_value, expectedJitter);
    XCT.AssertTrue(item.property_3_id.equals(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_jitterMs));

    results = SKMStoredResult.sReadSKMStoredResults(context);
    org.junit.Assert.assertTrue(results.size() == 1);
    for (SKMStoredResult result : results) {
      XCT.AssertTrue(result != null);
      XCT.AssertTrue(result.unit_id.equals(SKMStoredResult.cUnitIdMobileDevice));
      XCT.AssertTrue(result.dtime.getTime() == now.getTime());
      XCT.AssertTrue(result.metric_id.equals(metricId));
      XCT.AssertDoubleApproxEqual(result.property_1_value, expectedLatency);
      XCT.AssertTrue(result.property_1_id.equals(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_latencyMs));
      XCT.AssertDoubleApproxEqual(result.property_2_value, expectedLoss);
      XCT.AssertTrue(result.property_2_id.equals(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_lossPercent));
      XCT.AssertDoubleApproxEqual(result.property_3_value, expectedJitter);
      XCT.AssertTrue(result.property_3_id.equals(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_jitterMs));
    }

    // Now, delete the object...
    SKMStoredResult.sDeleteObject(context, item);
    //SKMDatabase.sDeleteObject(results.get(0));

    // ... and verify that the deleted object is no longer found.
    results = SKMStoredResult.sReadSKMStoredResults(context);
    org.junit.Assert.assertTrue(results != null);
    org.junit.Assert.assertTrue(results.size() == 0);
  }
}
