package com.samknows.SKKit.SKMobileApp.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.environment.Reachability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class SKMStoredResult {

  // Supported metric_id constants.
  static public final String cServer_httpgetmt   = "httpgetmt";
  static public final String cServer_httppostmt  = "httppostmt";
  static public final String cServer_udpjitter   = "udpjitter";
  static public final String cServer_udplatency  = "udplatency";
  static public final String cServer_nflx        = "nflx";
  static public final String cServer_youtube     = "youtube";
  // Distinguish between MOBILE and WIFI results!
  static public final String cDevice_Download          = "device_download_mbps";
  static public final String cDevice_Upload            = "device_upload_mbps";
  static public final String cDevice_LatencyLossJitter = "device_LatencyLossJitter";

  // Supported property ids...
  static public final String cServer_Property_httpgetmt_bytes_sec   = "bytes_sec";
  static public final String cServer_Property_httppostmt_bytes_sec   = "bytes_sec";
  static public final String cServer_Property_udpjitter_jitter_up   = "jitter_up";
  static public final String cServer_Property_udpjitter_jitter_down   = "jitter_down";
  static public final String cServer_Property_udplatency_rtt_avg   = "rtt_avg";
  // This is a DERIVED property...
  static public final String cServer_Property_udplatency_packetLossPercent   = "packetLossPercent";
  static public final String cServer_Property_nflx_max_bitrate   = "max_bitrate";
  static public final String cServer_Property_youtube_prebuffering_duration   = "prebuffering_duration";
  static public final String cServer_Property_youtube_stall_events   = "stall_events";
  static public final String cServer_Property_Device_Download_bytes_sec   = "device_bytes_sec";
  static public final String cServer_Property_Device_Upload_bytes_sec   = "device_bytes_sec";
  static public final String cServer_Property_Device_LatencyLossJitter_latencyMs   = "latencyMs";
  static public final String cServer_Property_Device_LatencyLossJitter_lossPercent   = "lossPercent";
  static public final String cServer_Property_Device_LatencyLossJitter_jitterMs   = "jitterMs";

  static public final String cUnitIdMobileDevice = "device"; // Unit id that can be used, uniquely, for device results

  // Public static array, that we can use to view the supported metric types.
  // This is mainly used for debugging, to ensure that the code caters for all values properly.
  static public final String sSupportedMetrics[] = {
      // These are all MBPS etc. values from the server aggregate queries...
      cServer_httpgetmt,
      cServer_httppostmt,
      cServer_udpjitter ,
      cServer_udplatency,
      cServer_nflx,
      cServer_youtube,
      // These are from locally run queries...
      cDevice_Download,
      cDevice_Upload,
      cDevice_LatencyLossJitter
  };

  // Possible values for network type.
  static public final String cNetworkType_None = null;
  static public final String cNetworkType_WiFi = "wifi";
  static public final String cNetworkType_Mobile = "mobile";

  public long id; // The internal database id...
  public final Date dtime;
  public final String metric_id;
  public String property_1_id;
  public Double property_1_value;
  public String property_2_id;
  public Double property_2_value;
  public String property_3_id;
  public Double property_3_value;
  public final String unit_id;
  public boolean test_passed;
  public String network_type;

  public static class SKMPropertyPair {
    public String mPropertyId;
    public Double mPropertyValue;

    public SKMPropertyPair (String propertyId, Double propertyValue) {
      mPropertyId = propertyId;
      mPropertyValue = propertyValue;
    }
  }

  public SKMStoredResult(long millisecondsSinceJan1970, String metric_id, ArrayList<SKMPropertyPair> properties, String unit_id, String network_type, boolean test_passed) {
    this.dtime = new Date(millisecondsSinceJan1970);
    this.metric_id = metric_id;
    this.unit_id = unit_id;
    this.network_type = network_type;
    this.test_passed = test_passed;
    setPropertiesFrom(properties);
  }

  public SKMStoredResult(Date dtime, String metric_id, ArrayList<SKMPropertyPair> properties, String unit_id, String network_type, boolean test_passed) {
    this.dtime = dtime;
    this.metric_id = metric_id;
    this.unit_id = unit_id;
    this.network_type = network_type;
    this.test_passed = test_passed;
    setPropertiesFrom(properties);
  }

  private void setPropertiesFrom (ArrayList<SKMPropertyPair> properties) {
    property_1_id = null;
    property_1_value = null;
    property_2_id = null;
    property_2_value = null;
    property_3_id = null;
    property_3_value = null;

    if (properties.size() >= 1) {
      property_1_id = properties.get(0).mPropertyId;
      property_1_value = properties.get(0).mPropertyValue;
      if (properties.size() >= 2) {
        property_2_id = properties.get(1).mPropertyId;
        property_2_value = properties.get(1).mPropertyValue;
        if (properties.size() >= 3) {
          SKLogger.sAssert(properties.size() == 3);
          property_3_id = properties.get(2).mPropertyId;
          property_3_value = properties.get(2).mPropertyValue;
        }
      }
    } else {
      SKLogger.sAssert(false);
    }
  }

  public static SKMStoredResult sCreateInManagedObjectContext(Context context, String unit_id, Date dtime, String metric_id, ArrayList<SKMPropertyPair> properties, boolean test_passed) {

    // This must not occur in the main thread, as it involves the use of blocking queries?

    // If an item already exists with this unit_id and dtime and metric_id, then return that
    // object RATHER than creating a new one - having first changed existing metric_value if
    // that value has changed significantly!
    SKMStoredResult existingItem = null;

    // Need to wait for the block to return the query result.
    { // moc.performBlockAndWait
      ArrayList<SKMStoredResult> results = sReadSKMStoredResultsFor(context, dtime, metric_id, unit_id);
      //noinspection StatementWithEmptyBody
      if (results.size() == 0) {
        // Not already existing!
      } else {
        SKLogger.sAssert(results.size() == 1);
        SKMStoredResult result = results.get(0);
        existingItem = result;

        // metric_value = 0.99; // can use this for testing, to force the update...

        Double propertyValue1 = properties.get(0).mPropertyValue;
        if (Math.abs(result.property_1_value - propertyValue1) > 0.001) {
            // Detect change in value (might happen if value back from
            // server has changed significantly); and if detected, update the local database
            // item with the new value.
            // This happens if, for example, we queried an average download speed; and
            // subsequently re-query this value, and find that the reported average download
            // speed has changed because the whitebox had run more tests since the last query,
            // resulting in a value different to the value last returned.

            result.property_1_value = propertyValue1;

            // Do UPDATE, as we've made a change!
            // Otherwise, subsequent re-query might not work!
            sUpdateSKMStoredResult(context, result);
          }
        }
    }

    if (existingItem != null) {
      return existingItem;
    }

    SKMStoredResult newItem = new SKMStoredResult(dtime, metric_id, properties, unit_id, null, test_passed);
    //  SKMStoredResult.sCreateInManagedObjectContext(unit_id, dtime, metric_id, metric_value);

    if (unit_id.equals(cUnitIdMobileDevice)) {
      // For mobile device, record if this test was done (most probably!) on WiFi or Mobile networks.
      if (Reachability.sGetIsNetworkWiFi()) {
        newItem.network_type = cNetworkType_WiFi;
      } else {
        newItem.network_type = cNetworkType_Mobile;
      }
    }

    // Validate that the metric is a supported value!
    //noinspection StatementWithEmptyBody
    if (Arrays.asList(sSupportedMetrics).contains(metric_id)) {
      // Supported value!
    } else {
      SKLogger.sAssert(false);
    }

    // And save it in the database!
    sInsertSKMStoredResult(context, dtime, metric_id, properties, unit_id, newItem.network_type, test_passed);

    // TODO: Having created the value, always save it immediately (in the appropriate thread!)
    // otherwise, subsequent queries might fail!
    //sDoSave(moc)

    return newItem;
  }

  // Create an SKMStoredResult item, that is sourced from the current device (i.e. does not
  // come from a remote source)
  public static SKMStoredResult sCreateInManagedObjectContext_FromDevice(Context context, Date dtime, String metric_id, ArrayList<SKMPropertyPair> properties, boolean test_passed) {
    return sCreateInManagedObjectContext(context, cUnitIdMobileDevice, dtime, metric_id, properties, test_passed);
  }

  public static SKMStoredResult sCreateInManagedObjectContext_FromDevice_Download(Context context, Date dtime, Double bytesPerSecond, Boolean test_passed) {
    ArrayList<SKMPropertyPair> properties = new ArrayList<>();
    properties.add(new SKMPropertyPair(SKMStoredResult.cServer_Property_Device_Download_bytes_sec, bytesPerSecond));
    return sCreateInManagedObjectContext_FromDevice(context, dtime, SKMStoredResult.cDevice_Download, properties, test_passed);
  }

  public static SKMStoredResult sCreateInManagedObjectContext_FromDevice_Upload(Context context, Date dtime, Double bytesPerSecond, Boolean test_passed) {
    ArrayList<SKMPropertyPair> properties = new ArrayList<>();
    properties.add(new SKMPropertyPair(SKMStoredResult.cServer_Property_Device_Upload_bytes_sec, bytesPerSecond));
    return sCreateInManagedObjectContext_FromDevice(context, dtime, SKMStoredResult.cDevice_Upload, properties, test_passed);
  }

  public static SKMStoredResult sCreateInManagedObjectContext_FromDevice_LatencyLossJitter(Context context, Date dtime, Double latencyMs, Double lossPercent, Double jitterMs, Boolean test_passed) {
    ArrayList<SKMPropertyPair> properties = new ArrayList<>();
    properties.add(new SKMPropertyPair(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_latencyMs, latencyMs));
    properties.add(new SKMPropertyPair(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_lossPercent, lossPercent));
    properties.add(new SKMPropertyPair(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_jitterMs, jitterMs));
    return sCreateInManagedObjectContext_FromDevice(context, dtime, SKMStoredResult.cDevice_LatencyLossJitter, properties, test_passed);
  }

  public static void sDeleteObject(Context context, SKMStoredResult item) {
    // Gets the data repository in write mode
    SQLiteDatabase db = SKMDatabase.sGetWriteableDatabase(context);

    String whereClause =
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_DTIME + "=?" +
            " AND " + SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_METRIC_ID + "=?" +
            " AND " + SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_UNIT_ID + "=?";
    String[] whereArgs = {
        Long.toString(item.dtime.getTime()),
        item.metric_id,
        item.unit_id
    };

    // The expectation is that this will delete one row.
    int rows = db.delete(SKMStoredResultContract.SKMStoredResultTable.TABLE_NAME, whereClause, whereArgs);
    SKLogger.sAssert(rows == 1);
  }

  // Query individual SKMStoredResult item
  public static ArrayList<SKMStoredResult> sReadSKMStoredResultsFor(Context context, Date dtime, String metric_id, String unit_id) {

    ArrayList<SKMStoredResult> resultArray = new ArrayList<>();

    SQLiteDatabase db = SKMDatabase.sGetReadableDatabase(context);

// Define a projection that specifies which columns from the database
// you will actually use after this query.
    String[] projection = {
        SKMStoredResultContract.SKMStoredResultTable._ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_DTIME,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_METRIC_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_VALUE,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_VALUE,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_VALUE,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_UNIT_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_NETWORK_TYPE,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_TEST_PASSED
    };

    String whereClause =
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_DTIME + "=?" +
        " AND " + SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_METRIC_ID + "=?" +
        " AND " + SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_UNIT_ID + "=?";
    String[] whereArgs = {
        Long.toString(dtime.getTime()),
        metric_id,
        unit_id
    };

    Cursor cursor = db.query(
        SKMStoredResultContract.SKMStoredResultTable.TABLE_NAME,  // The table to query
        projection,    // The columns to return
        whereClause,   // Columns for where clause
        whereArgs,     // The values for the WHERE clause
        null,          // don't group the rows
        null,          // don't filter by row groups
        null           // The sort order
    );
    if (cursor == null) {
      SKLogger.sAssert(false);
      return resultArray;
    }

    if (!cursor.moveToFirst()) {
      // No data!
      cursor.close();
      return resultArray;
    }

    // To reach here, we should have at least some data.
    int colIndex_id = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable._ID);
    int colIndex_dTime = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_DTIME);
    int colIndex_metricId = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_METRIC_ID);
    int colIndex_property1Id = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_ID);
    int colIndex_property1Value = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_VALUE);
    int colIndex_property2Id = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_ID);
    int colIndex_property2Value = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_VALUE);
    int colIndex_property3Id = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_ID);
    int colIndex_property3Value = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_VALUE);
    int colIndex_unitId = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_UNIT_ID);
    int colIndex_testPassed = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_TEST_PASSED);
    int colIndex_networkType = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_NETWORK_TYPE);

    for (;;) {
      long itemId = cursor.getLong(colIndex_id);
      long itemDtime_millisecondsSinceJan1970 = cursor.getLong(colIndex_dTime);
      String itemMetricId = cursor.getString(colIndex_metricId);
      String itemProperty1Id = cursor.getString(colIndex_property1Id);
      Double itemProperty1Value = cursor.getDouble(colIndex_property1Value);
      String itemProperty2Id = cursor.getString(colIndex_property2Id);
      Double itemProperty2Value = cursor.getDouble(colIndex_property2Value);
      String itemProperty3Id = cursor.getString(colIndex_property3Id);
      Double itemProperty3Value = cursor.getDouble(colIndex_property3Value);
      String itemUnitId = cursor.getString(colIndex_unitId);
      boolean itemTestPassed = cursor.getInt(colIndex_testPassed) > 0;

      String itemNetworkType = null;
      if (colIndex_networkType != -1) {
        itemNetworkType = cursor.getString(colIndex_networkType);
      }

      ArrayList<SKMPropertyPair> properties = new ArrayList<>();
      properties.add(new SKMPropertyPair(itemProperty1Id, itemProperty1Value));
      if (itemProperty2Id != null) {
        properties.add(new SKMPropertyPair(itemProperty2Id, itemProperty2Value));
        if (itemProperty3Id != null) {
          properties.add(new SKMPropertyPair(itemProperty3Id, itemProperty3Value));
        }
      }

      SKMStoredResult result = new SKMStoredResult(itemDtime_millisecondsSinceJan1970, itemMetricId, properties, itemUnitId, itemNetworkType, itemTestPassed);
      result.id = itemId;

      resultArray.add(result);

      // Should be no more than one match!
      SKLogger.sAssert(resultArray.size() == 1);

      if (!cursor.moveToNext()) {
        break;
      }
    }

    cursor.close();

    return resultArray;
  }

  // Query all SKMStoredResult items
  public static ArrayList<SKMStoredResult> sReadSKMStoredResults(Context context) {

    ArrayList<SKMStoredResult> resultArray = new ArrayList<>();

    SQLiteDatabase db = SKMDatabase.sGetReadableDatabase(context);

// Define a projection that specifies which columns from the database
// you will actually use after this query.
    String[] projection = {
        SKMStoredResultContract.SKMStoredResultTable._ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_DTIME,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_METRIC_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_VALUE,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_VALUE,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_VALUE,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_UNIT_ID,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_NETWORK_TYPE,
        SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_TEST_PASSED
    };

// How you want the results sorted in the resulting Cursor
    String sortOrder = SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_DTIME + " DESC";

    Cursor cursor = db.query(
        SKMStoredResultContract.SKMStoredResultTable.TABLE_NAME,  // The table to query
        projection,    // The columns to return
        null,          // Columns for where clause - null returns all rows
        null,          // The values for the WHERE clause - null returns all rows
        null,          // don't group the rows
        null,          // don't filter by row groups
        sortOrder      // The sort order
    );

    if (cursor == null) {
      SKLogger.sAssert(false);
      return resultArray;
    }

    if (!cursor.moveToFirst()) {
      // No data!
      cursor.close();
      return resultArray;
    }

    // To reach here, we should have at least some data.
    int colIndex_id = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable._ID);
    int colIndex_dTime = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_DTIME);
    int colIndex_metricId = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_METRIC_ID);
    int colIndex_property1Id = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_ID);
    int colIndex_property1Value = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_VALUE);
    int colIndex_property2Id = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_ID);
    int colIndex_property2Value = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_VALUE);
    int colIndex_property3Id = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_ID);
    int colIndex_property3Value = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_VALUE);
    int colIndex_unitId = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_UNIT_ID);
    int colIndex_testPassed = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_TEST_PASSED);
    int colIndex_networkType = cursor.getColumnIndex(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_NETWORK_TYPE);

    for (;;) {
      long itemId = cursor.getLong(colIndex_id);
      long itemDtime_millisecondsSinceJan1970 = cursor.getLong(colIndex_dTime);
      String itemMetricId = cursor.getString(colIndex_metricId);
      String itemProperty1Id = cursor.getString(colIndex_property1Id);
      Double itemProperty1Value = cursor.getDouble(colIndex_property1Value);
      String itemProperty2Id = cursor.getString(colIndex_property2Id);
      Double itemProperty2Value = cursor.getDouble(colIndex_property2Value);
      String itemProperty3Id = cursor.getString(colIndex_property3Id);
      Double itemProperty3Value = cursor.getDouble(colIndex_property3Value);
      String itemUnitId = cursor.getString(colIndex_unitId);
      boolean itemTestPassed = cursor.getInt(colIndex_testPassed) > 0;

      String itemNetworkType = null;
      if (colIndex_networkType != -1) {
        itemNetworkType = cursor.getString(colIndex_networkType);
      }

      ArrayList<SKMPropertyPair> properties = new ArrayList<>();
      properties.add(new SKMPropertyPair(itemProperty1Id, itemProperty1Value));
      if (itemProperty2Id != null) {
        properties.add(new SKMPropertyPair(itemProperty2Id, itemProperty2Value));
        if (itemProperty3Id != null) {
          properties.add(new SKMPropertyPair(itemProperty3Id, itemProperty3Value));
        }
      }
      SKMStoredResult result = new SKMStoredResult(itemDtime_millisecondsSinceJan1970, itemMetricId, properties, itemUnitId, itemNetworkType, itemTestPassed);
      result.id = itemId;

      resultArray.add(result);

      if (!cursor.moveToNext()) {
        break;
      }
    }

    cursor.close();

    // To reach here, we should have at least some data.
    SKLogger.sAssert(resultArray.size() > 0);
    return resultArray;
  }

  // Returns -1 in the event of an error!
  private static void sUpdateSKMStoredResult(Context context, SKMStoredResult result) {

    // Gets the data repository in write mode
    SQLiteDatabase db = SKMDatabase.sGetWriteableDatabase(context);

// Create a new map of values, where column names are the keys
    ContentValues values = new ContentValues();
    values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_VALUE, result.property_1_value);
//    values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_VALUE, result.property_2_value);
//    values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_VALUE, result.property_3_value);
    values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_TEST_PASSED, result.test_passed);

    String whereClause =
        SKMStoredResultContract.SKMStoredResultTable._ID + "=?";
    String[] whereArgs = {
        String.valueOf(result.id)
    };

    // Insert the new row, returning the primary key value of the new row
    // Returns -1 in the event of an error!
    int rows = db.update(
        SKMStoredResultContract.SKMStoredResultTable.TABLE_NAME,
        values,
        whereClause,
        whereArgs);
    SKLogger.sAssert(rows == 1);
  }

  // Returns -1 in the event of an error!
  public static long sInsertSKMStoredResult(Context context, Date dtime, String metric_id, ArrayList<SKMPropertyPair> properties, String unit_id, String network_type, boolean test_passed) {

    // Gets the data repository in write mode
    SQLiteDatabase db = SKMDatabase.sGetWriteableDatabase(context);

// Create a new map of values, where column names are the keys
    ContentValues values = new ContentValues();
    long itemDtime_millisecondsSinceJan1970 = dtime.getTime();
    values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_DTIME, itemDtime_millisecondsSinceJan1970);
    values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_METRIC_ID, metric_id);
    if (properties.size() >= 1) {
      values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_ID, properties.get(0).mPropertyId);
      values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_VALUE, properties.get(0).mPropertyValue);
      if (properties.size() >= 2) {
        values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_ID, properties.get(1).mPropertyId);
        values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_VALUE, properties.get(1).mPropertyValue);
        if (properties.size() >= 3) {
          SKLogger.sAssert(properties.size() == 3);
          values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_ID, properties.get(2).mPropertyId);
          values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_VALUE, properties.get(2).mPropertyValue);
        }
      }
    } else {
      SKLogger.sAssert(false);
    }
    values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_TEST_PASSED, test_passed);
    values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_UNIT_ID, unit_id);

    if (network_type != null) {
      values.put(SKMStoredResultContract.SKMStoredResultTable.COLUMN_NAME_NETWORK_TYPE, network_type);
    }

    // Insert the new row, returning the primary key value of the new row
    // Returns -1 in the event of an error!
    long newRowId = db.insert(
        SKMStoredResultContract.SKMStoredResultTable.TABLE_NAME,
        null,
        values);
    SKLogger.sAssert(newRowId != -1);
    return newRowId;
  }

//  public static SKMStoredResult sCreateInManagedObjectContext(String unit_id, Date dtime, String metric_id, ArrayList<SKMPropertyPair> properties) {
//    SKMStoredResult result = SKMDatabase.sCreateInManagedObjectContext(unit_id, dtime, metric_id, metric_value);
//    return result;
//  }


  // The database contract class (static)
  public static class SKMStoredResultContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public SKMStoredResultContract() {
    }

    /* Inner class that defines the table contents */
    public abstract class SKMStoredResultTable implements BaseColumns {
      public static final String TABLE_NAME = "SKMStoredResult";

      public static final String COLUMN_NAME_DTIME = "dtime";
      public static final String COLUMN_NAME_METRIC_ID = "metric_id";
      public static final String COLUMN_NAME_PROPERTY_1_ID = "property_1_id";
      public static final String COLUMN_NAME_PROPERTY_1_VALUE = "property_1_value";
      public static final String COLUMN_NAME_PROPERTY_2_ID = "property_2_id";
      public static final String COLUMN_NAME_PROPERTY_2_VALUE = "property_2_value";
      public static final String COLUMN_NAME_PROPERTY_3_ID = "property_3_id";
      public static final String COLUMN_NAME_PROPERTY_3_VALUE = "property_3_value";
      public static final String COLUMN_NAME_UNIT_ID = "unit_id";
      public static final String COLUMN_NAME_TEST_PASSED = "test_passed";
      public static final String COLUMN_NAME_NETWORK_TYPE = "network_type";
    }

    // Helper strings to create/drop the database etc.
    //private static final String DATE_TYPE = " DATE";
    //private static final String TEXT_TYPE = " TEXT";
    //private static final String COMMA_SEP = ",";

    public static final String SQL_CREATE_ENTRIES =
        "CREATE TABLE " + SKMStoredResultTable.TABLE_NAME + " (" +
            SKMStoredResultTable._ID + " INTEGER PRIMARY KEY," +
            // http://stackoverflow.com/questions/7363112/best-way-to-work-with-dates-in-android-sqlite
            // We are recommended to store dates as milliseconds!
            SKMStoredResultTable.COLUMN_NAME_DTIME + " INTEGER NOT NULL," + // INTEGER as Unix Time, the number of seconds since 1970-01-01 00:00:00 UTC
            SKMStoredResultTable.COLUMN_NAME_METRIC_ID + " TEXT NOT NULL," +
            SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_ID + " TEXT NOT NULL," +
            SKMStoredResultTable.COLUMN_NAME_PROPERTY_1_VALUE + " REAL NOT NULL," +
            SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_ID + " TEXT NULL," +
            SKMStoredResultTable.COLUMN_NAME_PROPERTY_2_VALUE + " REAL NULL," +
            SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_ID + " TEXT NULL," +
            SKMStoredResultTable.COLUMN_NAME_PROPERTY_3_VALUE + " REAL NULL," +
            SKMStoredResultTable.COLUMN_NAME_UNIT_ID + " TEXT NOT NULL," +
            SKMStoredResultTable.COLUMN_NAME_TEST_PASSED + " INTEGER NOT NULL," +
            SKMStoredResultTable.COLUMN_NAME_NETWORK_TYPE + " TEXT NULL" +
            ")";

    public static final String SQL_DELETE_ENTRIES =
        "DROP TABLE IF EXISTS " + SKMStoredResultTable.TABLE_NAME;
  }


  public Double getDownloadSpeedBytesPerSecond() {
    if (property_1_id.equals(SKMStoredResult.cServer_Property_httpgetmt_bytes_sec) ||
        property_1_id.equals(SKMStoredResult.cServer_Property_Device_Download_bytes_sec))
    {
      Double result = property_1_value;
      return result;
    }

    SKLogger.sAssert(false);
    return 0.0;
  }


  public Double getUploadSpeedBytesPerSecond() {
    if (property_1_id.equals(SKMStoredResult.cServer_Property_httppostmt_bytes_sec) ||
        property_1_id.equals(SKMStoredResult.cServer_Property_Device_Upload_bytes_sec))
    {
      Double result = property_1_value;
      return result;
    }

    SKLogger.sAssert(false);
    return 0.0;
  }


  public Double getUdpDownloadJitterMs() {
    if (property_1_id.equals(SKMStoredResult.cServer_Property_udpjitter_jitter_down)) {
      // The stored value is in microseconds!
      Double result = property_1_value * 0.001;
      return result;
    } else if (property_3_id.equals(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_jitterMs)) {
      Double result = property_3_value;
      return result;
    } else {
      SKLogger.sAssert(false);
      return 0.0;
    }
  }

  public Double getUdpUploadJitterMs() {
    if (property_1_id.equals(SKMStoredResult.cServer_Property_udpjitter_jitter_up)) {
      // The stored value is in microseconds!
      Double result = property_1_value * 0.001;
      return result;
    }

    SKLogger.sAssert(false);
    return 0.0;
  }

  public Double getUdpLatencyMs() {
    if (property_1_id.equals(SKMStoredResult.cServer_Property_udplatency_rtt_avg)) {
      // The stored value is in microseconds!
      Double result = property_1_value * 0.001;
      return result;
    } else if (property_2_id.equals(SKMStoredResult.cServer_Property_Device_LatencyLossJitter_lossPercent)) {
      Double result = property_2_value;
      return result;
    } else {
      SKLogger.sAssert(false);
      return 0.0;
    }
  }

  public Double getNflxMaxBitrateBytesPerSecond() {
    if (property_1_id.equals(SKMStoredResult.cServer_Property_nflx_max_bitrate)) {
      Double result = property_1_value;
      return result;
    }

    SKLogger.sAssert(false);
    return 0.0;
  }

  public Double getYoutubePrebufferingSeconds() {
    if (property_1_id.equals(SKMStoredResult.cServer_Property_youtube_prebuffering_duration)) {
      // Value stored in microseconds?
      Double result = property_1_value * 0.0000001;
      return result;
    }

    SKLogger.sAssert(false);
    return 0.0;
  }
}
