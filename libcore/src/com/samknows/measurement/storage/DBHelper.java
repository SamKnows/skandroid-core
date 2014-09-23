package com.samknows.measurement.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
//import android.os.Trace;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.activity.components.SKGraphForResults;
import com.samknows.measurement.activity.components.SKGraphForResults.DATERANGE_1w1m3m1y;
import com.samknows.measurement.environment.TrafficData;
import com.samknows.measurement.util.DCSConvertorUtil;
import com.samknows.measurement.util.OtherUtils;

//Helper class for accessing the data stored in the SQLite DB
//It Exposes only the methods to populate the Interface
//and to insert new data in the db
public class DBHelper {
	// grapdata JSONObject keys
	public static final String GRAPHDATA_TYPE = "type";
	public static final String GRAPHDATA_YLABEL = "y_label";
	public static final String GRAPHDATA_STARTDATE = "start_date";
	public static final String GRAPHDATA_ENDDATE = "end_date";
	public static final String GRAPHDATA_RESULTS = "results";
	public static final String GRAPHDATA_RESULTS_DATETIME = "datetime";
	public static final String GRAPHDATA_RESULTS_VALUE = "value";
	public static final String[] GRAPHDATA_JSON_KEYS = { GRAPHDATA_TYPE,
			GRAPHDATA_YLABEL, GRAPHDATA_STARTDATE, GRAPHDATA_ENDDATE,
			GRAPHDATA_RESULTS, GRAPHDATA_RESULTS_DATETIME,
			GRAPHDATA_RESULTS_VALUE };

	// gridtata JSONObject keys
	public static final String GRIDDATA_TYPE = "type";
	public static final String GRIDDATA_RESULTS = "results";
	public static final String GRIDDATA_RESULTS_ARCHIVEINDEX = "archiveindex";
	public static final String GRIDDATA_RESULTS_DTIME = "dtime";
	public static final String GRIDDATA_RESULTS_DATETIME = "datetime";
	public static final String GRIDDATA_RESULTS_LOCATION = "location";
	public static final String GRIDDATA_RESULTS_RESULT = "result";
	public static final String GRIDDATA_RESULTS_SUCCESS = "success";
	public static final String GRIDDATA_RESULTS_HRRESULT = "hrresult";
	public static final String GRIDDATA_RESULTS_NETWORK_TYPE = "network_type";
	public static final String[] GRIDDATA_JSON_KEYS = { GRIDDATA_TYPE,
			GRIDDATA_RESULTS };

	// averagedata JSONObject keys
	public static final String AVERAGEDATA_TYPE = "type";
	public static final String AVERAGEDATA_VALUE = "value";
	public static final String[] AVERAGEDATA_JSON_KEYS = { AVERAGEDATA_TYPE,
			AVERAGEDATA_VALUE };

	// archivedata JSONObject keys
	public static final String ARCHIVEDATA_INDEX = "index";
	public static final String ARCHIVEDATA_DTIME = "dtime";
	public static final String ARCHIVEDATA_DATETIME = "datetime";
	public static final String ARCHIVEDATA_ACTIVEMETRICS = "activemetrics";
	public static final String ARCHIVEDATA_ACTIVEMETRICS_TEST = "test";
	public static final String ARCHIVEDATA_ACTIVEMETRICS_DTIME = "dtime";
	public static final String ARCHIVEDATA_ACTIVEMETRICS_DATETIME = "datetime";
	public static final String ARCHIVEDATA_ACTIVEMETRICS_LOCATION = "location";
	public static final String ARCHIVEDATA_ACTIVEMETRICS_RESULT = "result";
	public static final String ARCHIVEDATA_ACTIVEMETRICS_SUCCESS = "success";
	public static final String ARCHIVEDATA_ACTIVEMETRICS_HRRESULT = "hrresult";
	public static final String ARCHIVEDATA_PASSIVEMETRICS_METRIC = "metric";
	public static final String ARCHIVEDATA_PASSIVEMETRICS_TYPE = "type";
	public static final String ARCHIVEDATA_PASSIVEMETRICS_VALUE = "value";
	public static final String ARCHIVEDATA_PASSIVEMETRICS = "passivemetrics";
	public static final String[] ARCHIVEDATA_JSON_KEYS = { ARCHIVEDATA_INDEX,
			ARCHIVEDATA_DTIME, ARCHIVEDATA_DATETIME, ARCHIVEDATA_ACTIVEMETRICS,
			ARCHIVEDATA_PASSIVEMETRICS };

	// archivedatasummary JSONObject keys
	public static final String ARCHIVEDATASUMMARY_COUNTER = "counter";
	public static final String ARCHIVEDATASUMMARY_STARTDATE = "startdate";
	public static final String ARCHIVEDATASUMMARY_ENDDATE = "enddate";
	public static final String ARCHIVEDATASUMMARY_TESTCOUNTER = "test_counter";
	public static final String[] ARCHIVEDATASUMMARY_JSON_KEYS = {
			ARCHIVEDATASUMMARY_COUNTER, ARCHIVEDATASUMMARY_STARTDATE,
			ARCHIVEDATASUMMARY_ENDDATE };

	// members
	private SQLiteDatabase database;
	private SKSQLiteHelper dbhelper;
	private static Object sync = new Object();

	// Constructor used to set the context
	public DBHelper(Context context) {
		dbhelper = new SKSQLiteHelper(context);
	}

	private boolean open() {
		boolean ret = false;
		try {
			database = dbhelper.getWritableDatabase();
			ret = true;
		} catch (SQLException sqle) {
			SKLogger.e(this, "Error in opening the database.", sqle);
		}
		return ret;
	}

	private void close() {
		database.close();
	}

	public synchronized boolean isEmpty() {
		synchronized (sync) {
			boolean ret = false;
			if (open() == false) {
				SKLogger.sAssert(getClass(),  false);
				return ret;
			}
			Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM "
					+ SKSQLiteHelper.TABLE_TESTRESULT, null);
			cursor.moveToFirst();
			ret = cursor.getInt(0) == 0;
			cursor.close();
			close();
			return ret;
		}
	}

	// converter
	private static String testValueToGraph(int test_type_id, double value) {
		String ret = value + "";
		switch (test_type_id) {
		case StorageTestResult.UPLOAD_TEST_ID:
		case StorageTestResult.DOWNLOAD_TEST_ID:
			ret = ((double) value / 1000000) + "";
			break;
		case StorageTestResult.LATENCY_TEST_ID:
		case StorageTestResult.JITTER_TEST_ID:
			ret = ((long) value / 1000) + "";
			break;
		case StorageTestResult.PACKETLOSS_TEST_ID:
			ret = String.format("%.2f", value);
			break;
		}
		return ret;
	}

	// Translate an entry in the test result table results entry if a JSONObject
	// for archivedata
	private static JSONObject testResultToArchiveData(JSONObject tr) {
		JSONObject ret = new JSONObject();
		try {
			String test_type = tr.getString(SKSQLiteHelper.TR_COLUMN_TYPE);
			int test_type_id = StorageTestResult.testStringToId(test_type);
			long dtime = tr.getLong(SKSQLiteHelper.TR_COLUMN_DTIME);
			String location = tr.getString(SKSQLiteHelper.TR_COLUMN_LOCATION);
			int success = tr.getInt(SKSQLiteHelper.TR_COLUMN_SUCCESS);
			double result = tr.getDouble(SKSQLiteHelper.TR_COLUMN_RESULT);
			String hrresult = StorageTestResult.hrResult(test_type_id, result);
			ret.put(ARCHIVEDATA_ACTIVEMETRICS_TEST, test_type_id);
			ret.put(ARCHIVEDATA_ACTIVEMETRICS_DTIME, dtime);
			ret.put(ARCHIVEDATA_ACTIVEMETRICS_SUCCESS, success + "");
			ret.put(ARCHIVEDATA_ACTIVEMETRICS_LOCATION, location);
			ret.put(ARCHIVEDATA_ACTIVEMETRICS_RESULT, result);
			ret.put(ARCHIVEDATA_ACTIVEMETRICS_HRRESULT, hrresult);
		} catch (JSONException je) {

		}
		return ret;
	}

	// Translate an entry in the test result table results entry if a JSONObject
	// for griddata
	private static JSONObject testResultToGridData(JSONObject tr) {
		JSONObject ret = new JSONObject();
		try {
			String test_type = tr.getString(SKSQLiteHelper.TR_COLUMN_TYPE);
			int test_type_id = StorageTestResult.testStringToId(test_type);
			long dtime = tr.getLong(SKSQLiteHelper.TR_COLUMN_DTIME);
			String location = tr.getString(SKSQLiteHelper.TR_COLUMN_LOCATION);
			int success = tr.getInt(SKSQLiteHelper.TR_COLUMN_SUCCESS);
			double result = tr.getDouble(SKSQLiteHelper.TR_COLUMN_RESULT);
			String hrresult = StorageTestResult.hrResult(test_type_id, result);
			ret.put(GRIDDATA_RESULTS_DTIME, dtime);
			ret.put(GRIDDATA_RESULTS_SUCCESS, success + "");
			ret.put(GRIDDATA_RESULTS_LOCATION, location);
			ret.put(GRIDDATA_RESULTS_RESULT, result);
			ret.put(GRIDDATA_RESULTS_HRRESULT, hrresult);
		} catch (JSONException je) {

		}
		return ret;
	}

	// Translate an entry in the test result table results entry if a JSONObject
	// for graphdata
	private static JSONObject testResultToGraphData(int test_type_id,
			JSONObject tr) {
		JSONObject ret = new JSONObject();
		try {
			String value = testValueToGraph(test_type_id,
					tr.getDouble(SKSQLiteHelper.TR_COLUMN_RESULT));
			//value = "0.00499"; // TODO - this is for DEBUG/TESTING only!
			long dtime = tr.getLong(SKSQLiteHelper.TR_COLUMN_DTIME);
			ret.put(GRAPHDATA_RESULTS_DATETIME, "" + dtime);
			ret.put(GRAPHDATA_RESULTS_VALUE, value);
		} catch (JSONException je) {

		}
		return ret;
	}

	private static JSONObject passiveMetricToArchiveData(JSONObject pm) {
		JSONObject ret = new JSONObject();
		try {
			String metric = pm.getString(SKSQLiteHelper.PM_COLUMN_METRIC);
			String type = pm.getString(SKSQLiteHelper.PM_COLUMN_TYPE);
			String value = pm.getString(SKSQLiteHelper.PM_COLUMN_VALUE);
			ret.put(ARCHIVEDATA_PASSIVEMETRICS_METRIC, metric);
			ret.put(ARCHIVEDATA_PASSIVEMETRICS_TYPE, type);
			
        	//value = "0.00499"; // TODO - this is for DEBUG/TESTING only!
			ret.put(ARCHIVEDATA_PASSIVEMETRICS_VALUE, value);
		} catch (JSONException je) {
			SKLogger.e(DBHelper.class, "error creating json object", je);
		}
		return ret;
	}

	// Translatror
	private static String testIdToGraphLabel(int test_type_id) {
		String ret = "";
		switch (test_type_id) {
		case StorageTestResult.UPLOAD_TEST_ID:
		case StorageTestResult.DOWNLOAD_TEST_ID:
			ret = "Mbps";
			break;
		case StorageTestResult.LATENCY_TEST_ID:
		case StorageTestResult.JITTER_TEST_ID:
			ret = "ms";
			break;
		case StorageTestResult.PACKETLOSS_TEST_ID:
			ret = "%";
			break;
		}
		return ret;
	}

	// Returns the JSONObject containing the data to draw a graph for one test
	// of type test_type_id between startdtime and enddtime
	// Whereas on iOS, the equivalent search would average all data by day;
	// on Android, this returns all the point data in the specified period.
	public JSONObject fetchGraphData(int test_type_id, long startdtime,
			long enddtime, DATERANGE_1w1m3m1y dateRange) {

		JSONObject ret = new JSONObject();
		String test_type = StorageTestResult.testIdToString(test_type_id);
		try {
			ret.put(GRAPHDATA_TYPE, test_type_id);
			ret.put(GRAPHDATA_YLABEL, testIdToGraphLabel(test_type_id));
			ret.put(GRAPHDATA_STARTDATE, startdtime + "");
			ret.put(GRAPHDATA_ENDDATE, enddtime + "");
			List<JSONObject> entries = getTestResultByTypeAndInterval(
					test_type, startdtime, enddtime, " AND success <> 0");
			JSONArray results = new JSONArray();
			for (JSONObject jo : entries) {
				results.put(testResultToGraphData(test_type_id, jo));

			}
			ret.put(GRAPHDATA_RESULTS, results);

			// if (dateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_DAY) {
			// 	// No need to specifically extract "24hours" data, c.f. iOS;
			// 	// as the data returned is single point data on Android
			// 	// (whereas on iOS, it is returned averaged by day).
			// }
		} catch (JSONException je) {

		}


		return ret;
	}

	// Returns the JSONObject containing the data to populate a grid for a
	// specific test
	// with id test_type_id, returns offset entry starting from index
	public JSONObject getGridData(int test_type_id, int index, int offset, long startdtime,
			long enddtime) {
		JSONObject ret = new JSONObject();
		String test_type = StorageTestResult.testIdToString(test_type_id);
		List<JSONObject> entries = getFilteredTestResultsInDateRange(test_type, index, offset, startdtime, enddtime);
		try {
			ret.put(GRIDDATA_TYPE, test_type_id);
			
			JSONArray results = new JSONArray();
			for (JSONObject jo : entries) {
    			long testId = jo.getLong(SKSQLiteHelper.TR_COLUMN_BATCH_ID);
				String networkType = "";
	    		List<JSONObject> passive_metrics = getPassiveMetrics(testId);
				for (JSONObject pm : passive_metrics) {
    				String type = pm.getString(SKSQLiteHelper.PM_COLUMN_METRIC);
	    			if (type.equals("activenetworktype")) {
    	    			String value = pm.getString(SKSQLiteHelper.PM_COLUMN_VALUE);
    	    			networkType = value;
    	    			break;
	    			}
				}

				JSONObject theGridData = testResultToGridData(jo);
				if (networkType != null) {
				    theGridData.put(GRIDDATA_RESULTS_NETWORK_TYPE, networkType);
				}
				results.put(theGridData);
			}
			ret.put(GRIDDATA_RESULTS, results);
		} catch (JSONException je) {
			SKLogger.e(DBHelper.class, "Error in creating data for the grid");
		}
		return ret;
	}

	// Return the JSONObject to populate an archive view
	// index is the position of the archive data in the database
	
	// TODO - change this to query ONLY for batches for the current activenetworktype ...!
	public JSONObject getArchiveData(int index) {
		synchronized (sync) {
			JSONObject ret = new JSONObject();
			if (open() == false) {
				SKLogger.sAssert(getClass(),  false);
				return ret;
			}
			
	 		// A consequence of the system "collecting" metrics both when we start *and* stop a test, is that 
	 		// this leads to multiple rows in the passive_metric table, with the same batch_id and metric...!
			// So, our query needs to cater for this...
		
			String metricValue = "";
			if (SKApplication.getNetworkTypeResults() == SKApplication.eNetworkTypeResults.eNetworkTypeResults_Any) {
	  		  metricValue = "'mobile', 'WiFi'";
			} else if (SKApplication.getNetworkTypeResults() == SKApplication.eNetworkTypeResults.eNetworkTypeResults_Mobile) {
	  		  metricValue = "'mobile'";
			} else if (SKApplication.getNetworkTypeResults() == SKApplication.eNetworkTypeResults.eNetworkTypeResults_WiFi) {
	  		  metricValue = "'WiFi'";
			}	
			
			/*
			SELECT _id, dtime, manual 
			FROM test_batch AS tb 
			WHERE tb._id IN 
			(select tb2._id 
			 from test_batch as tb2,
			      passive_metric  as pm2 
			      where pm2.batch_id = tb2._id 
			      and pm2.metric = 'activenetworktype' 
			      and pm2.value = 'mobile')
			ORDER BY dtime DESC;
			 */
			
			StringBuilder MY_QUERY = new StringBuilder();
			MY_QUERY.append("SELECT _id, dtime, manual ");
			MY_QUERY.append("FROM test_batch AS tb ");
			MY_QUERY.append("WHERE tb._id IN ");
			MY_QUERY.append("(select tb2._id ");
			MY_QUERY.append(" from test_batch as tb2,");
			MY_QUERY.append("      passive_metric  as pm2 ");
			MY_QUERY.append("      where pm2.batch_id = tb2._id ");
			MY_QUERY.append("      and pm2.metric = 'activenetworktype' ");
			MY_QUERY.append("      and pm2.value in (" + metricValue + ")) ");
			MY_QUERY.append("ORDER BY dtime DESC ");
	  		
	  		//Log.d("!!", MY_QUERY.toString());
			
			Cursor cursor1 = database.rawQuery(MY_QUERY.toString(), new String[]{});
			
			if (cursor1 == null) {
				SKLogger.sAssert(getClass(), false);
				close();
				return null;
			}
			if (!cursor1.moveToFirst()) {
				// Nothing to return!
				cursor1.close();
				close();
				return ret;
			}
			
			for (;;) {
			
				if (index == 0) {
					break;
				}
				
    			// Find the indexed item!
				cursor1.moveToNext();
    			if (cursor1.isAfterLast()) {
    				SKLogger.sAssert(getClass(),  false);
    			}
    			
    			index--;
			}
			
			long test_batch_id = cursor1.getLong(0);
			long test_batch_time = cursor1.getLong(1);
			cursor1.close();
			
			String selection = SKSQLiteHelper.TR_COLUMN_BATCH_ID + " = " + test_batch_id;
			List<JSONObject> tests = getTestResults(selection);
			List<JSONObject> passive_metrics = getPassiveMetrics(test_batch_id);
			JSONArray j_tests = new JSONArray();
			JSONArray j_pm = new JSONArray();
			try {
				ret.put(ARCHIVEDATA_INDEX, index + "");
				ret.put(ARCHIVEDATA_DTIME, test_batch_time + "");
				for (JSONObject jo : tests) {
					j_tests.put(testResultToArchiveData(jo));
				}
				for (JSONObject jo : passive_metrics) {
					j_pm.put(passiveMetricToArchiveData(jo));
				}
				ret.put(ARCHIVEDATA_ACTIVEMETRICS, j_tests);
				ret.put(ARCHIVEDATA_PASSIVEMETRICS, j_pm);
			} catch (JSONException je) {
				SKLogger.e(DBHelper.class,
						"Error in converting tests and passive metrics for archive data"
								+ je.getMessage());
			}
			close();
			return ret;
		}
	}

	// Return a summary of the archive data
	public JSONObject getArchiveDataSummary() {
		synchronized (sync) {
     		//Trace.beginSection("getArchiveDataSummary");
			List<Integer> batches = getTestBatchesByPassiveMetric(getPassiveMetricsFilter());
			
			JSONObject ret = new JSONObject();
			if (open() == false) {
         		//Trace.endSection();
				SKLogger.sAssert(getClass(),  false);
				return ret;
			}
			// test batch counter
			
	 		// A consequence of the system "collecting" metrics both when we start *and* stop a test, is that 
	 		// this leads to multiple rows in the passive_metric table, with the same batch_id and metric...!
			// So, our query needs to cater for this...
		
			String metricValue = "";
			if (SKApplication.getNetworkTypeResults() == SKApplication.eNetworkTypeResults.eNetworkTypeResults_Any) {
				// Nothing to append!
	  		  metricValue = "'mobile', 'WiFi'";
			} else if (SKApplication.getNetworkTypeResults() == SKApplication.eNetworkTypeResults.eNetworkTypeResults_Mobile) {
	  		  metricValue = "'mobile'";
			} else if (SKApplication.getNetworkTypeResults() == SKApplication.eNetworkTypeResults.eNetworkTypeResults_WiFi) {
	  		  metricValue = "'WiFi'";
			}
		
			// Query the number of test batches (only of the required network type!), together with
			// min/max test dates.
			/*
			 SELECT COUNT(*), MIN(tb.dtime), MAX(tb.dtime)
             FROM test_batch AS tb
             WHERE tb._id in
             (select tb2._id
              from test_batch as tb2,
                   passive_metric  as pm2
                   where pm2.batch_id = tb2._id
                   and pm2.metric = 'activenetworktype'
                   and pm2.value = 'mobile');
			 */
			
			StringBuilder MY_QUERY = new StringBuilder();
			MY_QUERY.append("SELECT COUNT(*), MIN(tb.dtime), MAX(tb.dtime) ");
			MY_QUERY.append("FROM test_batch AS tb ");
			MY_QUERY.append("WHERE tb._id in ");
			MY_QUERY.append("(select tb2._id ");
			MY_QUERY.append(" from test_batch as tb2,");
			MY_QUERY.append("      passive_metric  as pm2 ");
			MY_QUERY.append("      where pm2.batch_id = tb2._id ");
			MY_QUERY.append("      and pm2.metric = 'activenetworktype' ");
			MY_QUERY.append("      and pm2.value in (" + metricValue + ")) ");
	  		
	  		//Log.d("!!", MY_QUERY.toString());
			
			Cursor cursor1 = database.rawQuery(MY_QUERY.toString(), new String[]{});
			
			String counter = "0";
			String min = "0";
			String max = "0";
			if (cursor1.moveToFirst() == true) {
				// Got something!
				counter = cursor1.getLong(0) + "";
				min = cursor1.getLong(1) + "";
				max = cursor1.getLong(2) + "";
			} else {
				// The first time this query is run, there might be zero rows returned;
				// do not treat this as a failure.
			}
	  		cursor1.close();

	  		/*
	  		 SELECT tr.type, COUNT(*)
             FROM  test_result AS tr
             WHERE tr.batch_id IN
             (select pm.batch_id
             FROM passive_metric AS pm
             WHERE pm.metric = 'activenetworktype' AND pm.value = 'mobile')
             GROUP BY tr.type;
	  		 */
			
			// test results counter
			MY_QUERY = new StringBuilder();
			MY_QUERY.append("SELECT tr.type, COUNT(*) ");
			MY_QUERY.append("FROM  test_result AS tr ");
			MY_QUERY.append("WHERE tr.batch_id IN ");
			MY_QUERY.append("(SELECT pm.batch_id ");
			MY_QUERY.append("FROM passive_metric AS pm ");
			MY_QUERY.append("WHERE pm.metric = 'activenetworktype' AND pm.value in (" + metricValue + ")) ");
			MY_QUERY.append("GROUP BY tr.type ");
	  		
	  		// Log.d("!!", MY_QUERY.toString());
	  		
			Cursor cursor2 = database.rawQuery(MY_QUERY.toString(), new String[]{});
			
			cursor2.moveToFirst();
			JSONObject test_counter = new JSONObject();
			while (!cursor2.isAfterLast()) {
				try {
					test_counter.put(StorageTestResult.testStringToId(cursor2.getString(0)) + "", cursor2.getInt(1) + "");
				} catch (JSONException je) {
					SKLogger.sAssert(getClass(),  false);
				}
				cursor2.moveToNext();
			}
			cursor2.close();
			
			try {
				ret.put(ARCHIVEDATASUMMARY_COUNTER, counter);
				ret.put(ARCHIVEDATASUMMARY_STARTDATE, min);
				ret.put(ARCHIVEDATASUMMARY_ENDDATE, max);
				ret.put(ARCHIVEDATASUMMARY_TESTCOUNTER, test_counter);
			} catch (JSONException je) {
				SKLogger.sAssert(getClass(),  false);
			}
			close();
			
         	//Trace.endSection();
         		
			return ret;
		}
	}

	public void insertTestBatch(JSONObject test_batch, JSONArray tests,
			JSONArray passive_metrics) {
		long test_batch_id;
		test_batch_id = insertTestBatch(test_batch);
		insertTestResult(tests, test_batch_id);
		insertPassiveMetric(passive_metrics, test_batch_id);
	}

	public void insertTestBatch(JSONObject test_batch, List<JSONObject> tests,
			List<JSONObject> passive_metrics) {
		long test_batch_id = insertTestBatch(test_batch);
		insertTestResult(tests, test_batch_id);
		insertPassiveMetric(passive_metrics, test_batch_id);
	}

	public long insertTestBatch(JSONObject test_batch) {
		long start_time;
		int run_manually;
		long ret = -1;
		try {
			start_time = test_batch.getLong(TestBatch.JSON_DTIME);
			run_manually = Integer.parseInt(test_batch
					.getString(TestBatch.JSON_RUNMANUALLY));
			ret = insertTestBatch(start_time, run_manually);
		} catch (JSONException je) {
			SKLogger.e(this, "Error in creating json object.", je);
		}
		return ret;
	}

	public long insertTestBatch(long start_time, int run_manually) {
		synchronized (sync) {
			if (open() == false) {
				SKLogger.sAssert(getClass(),  false);
				return -1;
			}
			ContentValues values = new ContentValues();
			values.put(SKSQLiteHelper.TB_COLUMN_DTIME, start_time);
			values.put(SKSQLiteHelper.TB_COLUMN_MANUAL, run_manually);
			long insertId = database.insert(SKSQLiteHelper.TABLE_TESTBATCH,
					null, values);
			close();
			return insertId;
		}
	}

	public void insertTestResult(List<JSONObject> tests, long test_batch_id) {
		for (JSONObject t : tests) {
			insertTestResult(t, test_batch_id);
		}
	}

	public void insertTestResult(JSONArray tests, long test_batch_id) {
		for (int i = 0; i < tests.length(); i++) {
			try {
				insertTestResult(tests.getJSONObject(i), test_batch_id);
			} catch (JSONException je) {
				SKLogger.e(DBHelper.class, "Error in converting JSONArray.", je);
			}
		}
	}

	public void insertTestResult(JSONObject test, long test_batch_id) {
		try {
			String type_name = test.getString(StorageTestResult.JSON_TYPE_NAME);
			long dtime = test.getLong(StorageTestResult.JSON_DTIME);
			long success = test.getLong(StorageTestResult.JSON_SUCCESS);
			double result = test.getDouble(StorageTestResult.JSON_RESULT);
			String location = test.getString(StorageTestResult.JSON_LOCATION);
			insertTestResult(type_name, dtime, success, result, location,
					test_batch_id);
		} catch (JSONException je) {
			SKLogger.e(
					DBHelper.class,
					"Error in converting TestResult JSONObject in database entry.",
					je);
		}

	}

	private void insertTestResult(String type_name, long dtime, long success,
			double result, String location, long test_batch_id) {
		synchronized (sync) {
			if (open() == false) {
				SKLogger.sAssert(getClass(),  false);
				return;
			}
			ContentValues values = new ContentValues();
			values.put(SKSQLiteHelper.TR_COLUMN_DTIME, dtime);
			values.put(SKSQLiteHelper.TR_COLUMN_TYPE, type_name);
			values.put(SKSQLiteHelper.TR_COLUMN_LOCATION, location);
			values.put(SKSQLiteHelper.TR_COLUMN_SUCCESS, success);
			values.put(SKSQLiteHelper.TR_COLUMN_RESULT, result);
			values.put(SKSQLiteHelper.TR_COLUMN_BATCH_ID, test_batch_id);
			long id = database.insert(SKSQLiteHelper.TABLE_TESTRESULT, null,
					values);
			close();
		}
	}

	private void insertPassiveMetric(JSONArray metrics, long test_batch_id) {
		for (int i = 0; i < metrics.length(); i++) {
			try {
				insertPassiveMetric(metrics.getJSONObject(i), test_batch_id);
			} catch (JSONException je) {
				SKLogger.e(DBHelper.class,
						"Error in converting JSONArray: " + je.getMessage());
			}
		}
	}

	public void insertPassiveMetric(List<JSONObject> metrics, long test_batch_id) {
		for (JSONObject pm : metrics) {
			insertPassiveMetric(pm, test_batch_id);
		}
	}

	private void insertPassiveMetric(JSONObject metric, long test_batch_id) {
		String metric_type;
		long dtime;
		String value;
		String type;
		try {
			metric_type = metric.getString(PassiveMetric.JSON_METRIC_NAME);
			dtime = metric.getLong(PassiveMetric.JSON_DTIME);
			value = metric.getString(PassiveMetric.JSON_VALUE);
			type = metric.getString(PassiveMetric.JSON_TYPE);
			insertPassiveMetric(metric_type, type, dtime, value, test_batch_id);
		} catch (JSONException je) {
			SKLogger.e(
					DBHelper.class,
					"Error in converting JSONObject ot passive metric: "
							+ je.getMessage());
		}
	}

	private void insertPassiveMetric(String metric_type, String type,
			long dtime, String value, long test_batch_id) {
		synchronized (sync) {
			ContentValues values = new ContentValues();
			if (open() == false) {
				SKLogger.sAssert(getClass(),  false);
				return;
			}
			
     		// A consequence of the system "collecting" metrics both when we start *and* stop a test, is that 
     		// this leads to multiple rows in the passive_metric table, with the same batch_id and metric...!
			if (metric_type.equals("activenetworktype")) {
				Log.d("activenetworktype", "value=" + value);
				//Log.d("activenetworktype", Thread.currentThread().getStackTrace().toString());
			}
			
//			if (metric_type.equals("activenetworktype")) {
//		        if (OtherUtils.isThisDeviceAnEmulator() == true) {
//		        	if (value.equals(DCSConvertorUtil.convertConnectivityType(ConnectivityManager.TYPE_MOBILE))) {
//		        		// Can force to save as particular network type, to assist in debugging!
//		        		value = DCSConvertorUtil.convertConnectivityType(ConnectivityManager.TYPE_WIFI);
//		        	}
//        		}
//			}
			
			values.put(SKSQLiteHelper.PM_COLUMN_METRIC, metric_type);
			values.put(SKSQLiteHelper.PM_COLUMN_TYPE, type);
			values.put(SKSQLiteHelper.PM_COLUMN_DTIME, dtime);
			values.put(SKSQLiteHelper.PM_COLUMN_VALUE, value);
			values.put(SKSQLiteHelper.PM_COLUMN_BATCH_ID, test_batch_id);
			
			long id = database.insert(SKSQLiteHelper.TABLE_PASSIVEMETRIC, null,
					values);
			close();
		}
	}

	// Returns all the TestResult stored in the db
	public List<JSONObject> getAllTestResults() {
		return getTestResults();
	}

	// Returns all the TestResult stored in the db for a given test type
	public List<JSONObject> getAllTestResultsByType(String type) {
		String selection = String.format(Locale.US, "%s = '%s'",
				SKSQLiteHelper.TR_COLUMN_TYPE, type);
		return getTestResults(selection);
	}

	public List<JSONObject> getTestResultByTypeAndInterval(String type,
			long starttime, long endtime, String extraFilter) {
		String selection = String.format(Locale.US, "%s = '%s' AND %s BETWEEN %d AND %d %s",
				SKSQLiteHelper.TR_COLUMN_TYPE, type,
				SKSQLiteHelper.TR_COLUMN_DTIME, starttime, endtime, extraFilter);
		List<Integer> batches = getTestBatchesByPassiveMetric(getPassiveMetricsFilter());
		if (batches == null || batches.size() == 0) {
			return new ArrayList<JSONObject>();
		}
		selection += " AND "
				+ getInClause(SKSQLiteHelper.TR_COLUMN_BATCH_ID, batches);
		return getTestResults(selection);
	}

	// Returns all the TestResult stored in the db run in an interval
	public List<JSONObject> getAllTestResultsInterval(long starttime,
			long endtime) {
		String selection = String.format(Locale.US, "%s BETWEEN %d AND %d",
				SKSQLiteHelper.TR_COLUMN_DTIME, starttime, endtime);
		return getTestResults(selection);
	}

	// Returns n TestResult from the database for a given type after a specific
	// time
	/*
	 * public List<JSONObject> getTestResults(String type, long starttime, int
	 * n) { String selection = String.format(Locale.US, "%s = '%s' AND %s >= %d",
	 * SKSQLiteHelper.TR_COLUMN_TYPE, type, SKSQLiteHelper.TR_COLUMN_DTIME,
	 * starttime); return getTestResults(selection, n + ""); }
	 */

	// Returns n TestResult the i-th result for a given type, irrespective of date range.
	public List<JSONObject> getFilteredTestResults(String type, int startindex,
			int n) {
		String selection = String.format(Locale.US, "%s = '%s'",
				SKSQLiteHelper.TR_COLUMN_TYPE, type);
		String limit = String.format(Locale.US, "%d,%d", startindex, n);
		List<Integer> batches = getTestBatchesByPassiveMetric(getPassiveMetricsFilter());
		if (batches == null || batches.size() == 0) {
			return new ArrayList<JSONObject>();
		}
		selection += " AND " + getInClause(SKSQLiteHelper.TR_COLUMN_BATCH_ID, batches);
		return getTestResults(selection, limit);
	}

	// Returns n TestResult the i-th result for a given type, given a date range.
	public List<JSONObject> getFilteredTestResultsInDateRange(String type, int startindex,
			int n, long starttime, long endtime) {
		String selection = String.format(Locale.US, "%s = '%s'",
				SKSQLiteHelper.TR_COLUMN_TYPE, type);
		String limit = String.format(Locale.US, "%d,%d", startindex, n);
		List<Integer> batches = getTestBatchesByPassiveMetric(getPassiveMetricsFilter());
		if (batches == null || batches.size() == 0) {
			return new ArrayList<JSONObject>();
		}
		selection += " AND " + getInClause(SKSQLiteHelper.TR_COLUMN_BATCH_ID, batches);
		String selection2 = String.format(Locale.US,
					"AND dtime BETWEEN %d AND %d AND success <> 0", starttime, endtime);
		selection += selection2;
		return getTestResults(selection, limit);
	}
	// Returns a JSONArray with the averages for the tests in the interval
	// between starttime and endtime
	// the average is computed only on successful tests
	public JSONArray getAverageResults(long starttime, long endtime,
			List<Integer> test_batches) {
		synchronized (sync) {
			JSONArray ret = new JSONArray();
			if (open() == false) {
				SKLogger.sAssert(getClass(),  false);
				return ret;
			}
			String selection = String.format(Locale.US,
					"dtime BETWEEN %d AND %d AND success <> 0", starttime,
					endtime);
			if (test_batches != null && test_batches.size() == 0) {
				return ret;
			}
			if (test_batches != null) {
				selection += " AND "
						+ getInClause(SKSQLiteHelper.TR_COLUMN_BATCH_ID,
								test_batches);
			}
			String averageColumn = String.format(Locale.US, "AVG(%s)",
					SKSQLiteHelper.TR_COLUMN_RESULT);

			String[] columns = { SKSQLiteHelper.TR_COLUMN_TYPE, averageColumn,
					"COUNT(*)" };
			String groupBy = SKSQLiteHelper.TR_COLUMN_TYPE;
			Cursor cursor = database.query(SKSQLiteHelper.TABLE_TESTRESULT,
					columns, selection, null, groupBy, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {

				JSONObject curr = new JSONObject();
				try {
					int test_type_id = StorageTestResult.testStringToId(cursor
							.getString(0));
					curr.put(AVERAGEDATA_TYPE, test_type_id + "");
					
					String value = StorageTestResult.hrResult(test_type_id, cursor.getDouble(1));
        			//value = "0.00499"; // TODO - this is for DEBUG/TESTING only!
					curr.put( AVERAGEDATA_VALUE, value);
				} catch (JSONException je) {

				}
				ret.put(curr);
				cursor.moveToNext();
			}
			cursor.close();
			close();
			return ret;
		}
	}

	//
	public JSONArray getAverageResults(long starttime, long endtime) {
		List<Integer> batches = getTestBatchesByPassiveMetric(starttime,
				endtime);
		return getAverageResults(starttime, endtime, batches);
	}

	private String getInClause(String field, List<Integer> values) {
		StringBuilder sb = new StringBuilder();
		sb.append(field).append(" IN (");
		for (Iterator<Integer> it = values.iterator(); it.hasNext();) {
			sb.append(it.next());
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(" )");
		return sb.toString();
	}

	// When retrieving averages, graph and grid data we have to filter by
	// Passive metric
	private String getPassiveMetricsFilter() {

		StringBuilder sb = new StringBuilder();
		sb.append(" metric = 'activenetworktype' AND value in(");

		if (SKApplication.getNetworkTypeResults() == SKApplication.eNetworkTypeResults.eNetworkTypeResults_Any) {
	  		sb.append ("'mobile', 'WiFi'");
		} else if (SKApplication.getNetworkTypeResults() == SKApplication.eNetworkTypeResults.eNetworkTypeResults_Mobile) {
			sb.append("'mobile'");
		} else if (SKApplication.getNetworkTypeResults() == SKApplication.eNetworkTypeResults.eNetworkTypeResults_WiFi) {
			sb.append("'WiFi'");
		}
		sb.append(")");
		
		return sb.toString();
	}

	// Return a list of test batch ids with a passive metric value equal to
	// value in the specified period
	public List<Integer> getTestBatchesByPassiveMetric(String selection) {
		synchronized (sync) {
			List<Integer> ret = new ArrayList<Integer>();
			if (open() == false) {
				SKLogger.sAssert(getClass(),  false);
				return ret;
			}
			String[] columns = { SKSQLiteHelper.PM_COLUMN_BATCH_ID };
			Cursor cursor = database.query(SKSQLiteHelper.TABLE_PASSIVEMETRIC,
					columns, selection, null, SKSQLiteHelper.PM_COLUMN_BATCH_ID, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				ret.add(cursor.getInt(0));
				cursor.moveToNext();
			}
			cursor.close();
			close();
			return ret;
		}
	}

	public List<Integer> getTestBatchesByPassiveMetric(long start_time,
			long end_time) {

		String selection = String.format(Locale.US, SKSQLiteHelper.PM_COLUMN_DTIME
				+ " BETWEEN %d AND %d", start_time, end_time);
		selection += " AND " + getPassiveMetricsFilter();
		return getTestBatchesByPassiveMetric(selection);
	}

	private List<JSONObject> getTestResults() {
		return getTestResults(null, null);
	}

	private List<JSONObject> getTestResults(String selection) {
		return getTestResults(selection, null);
	}

	private List<JSONObject> getTestResults(String selection, String limit) {
		synchronized (sync) {
			List<JSONObject> ret = new ArrayList<JSONObject>();
			if (open() == false) {
				SKLogger.sAssert(getClass(),  false);
				return ret;
			}
			Cursor cursor = database.query(SKSQLiteHelper.TABLE_TESTRESULT,
					SKSQLiteHelper.TABLE_TESTRESULT_ALLCOLUMNS, selection,
					null, null, null, SKSQLiteHelper.TEST_RESULT_ORDER, limit);

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				ret.add(cursorTestResultToJSONObject(cursor));
				cursor.moveToNext();
			}
			cursor.close();
			close();
			return ret;
		}
	}

	private List<JSONObject> getPassiveMetrics(long test_batch_id) {
		synchronized (sync) {
			List<JSONObject> ret = new ArrayList<JSONObject>();
			if (open() == false) {
				SKLogger.sAssert(getClass(),  false);
				return ret;
			}
			String selection = SKSQLiteHelper.PM_COLUMN_BATCH_ID + " = "
					+ test_batch_id;
			Cursor cursor = database.query(SKSQLiteHelper.TABLE_PASSIVEMETRIC,
					SKSQLiteHelper.TABLE_PASSIVEMETRIC_ALLCOLUMNS, selection,
					null, null, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				ret.add(cursorPassiveMetricToJSONObject(cursor));
				cursor.moveToNext();
			}
			cursor.close();
			close();
			return ret;
		}
	}

	// Translate a cursor to a TestResult JSONObject ready to be sent to the UI
	private JSONObject cursorTestResultToJSONObject(Cursor c) {
		JSONObject ret = new JSONObject();
		try {
			ret.put(SKSQLiteHelper.TR_COLUMN_ID, c.getLong(0));
			ret.put(SKSQLiteHelper.TR_COLUMN_TYPE, c.getString(1));
			ret.put(SKSQLiteHelper.TR_COLUMN_DTIME, c.getLong(2));
			ret.put(SKSQLiteHelper.TR_COLUMN_LOCATION, c.getString(3));
			ret.put(SKSQLiteHelper.TR_COLUMN_SUCCESS, c.getInt(4));
			ret.put(SKSQLiteHelper.TR_COLUMN_RESULT, c.getDouble(5));
			ret.put(SKSQLiteHelper.TR_COLUMN_BATCH_ID, c.getLong(6));
		} catch (JSONException je) {

		}
		return ret;
	}

	// Translate a cursor to a PassiveMetric JSONObject ready to be sent to the
	// UI
	private JSONObject cursorPassiveMetricToJSONObject(Cursor c) {
		JSONObject ret = new JSONObject();
		// PM_COLUMN_ID, PM_COLUMN_METRIC, PM_COLUMN_DTIME, PM_COLUMN_VALUE,
		// PM_COLUMN_TYPE, PM_COLUMN_BATCH_ID
		try {
			ret.put(SKSQLiteHelper.PM_COLUMN_ID, c.getLong(0));
			ret.put(SKSQLiteHelper.PM_COLUMN_METRIC, c.getString(1));
			ret.put(SKSQLiteHelper.PM_COLUMN_DTIME, c.getLong(2));
			ret.put(SKSQLiteHelper.PM_COLUMN_VALUE, c.getString(3));
			ret.put(SKSQLiteHelper.PM_COLUMN_TYPE, c.getString(4));
			ret.put(SKSQLiteHelper.PM_COLUMN_BATCH_ID, c.getLong(5));
		} catch (JSONException je) {
			SKLogger.e(DBHelper.class,
					"Error in converting passive metric entry into JSONObject"
							+ je.getMessage());
		}
		return ret;
	}
	
	// PABLO'S ADDITION
	// This method is used to retrieve information for the summary screen results about average and best results.
	public ArrayList<SummaryResult> getSummaryValues(int pNetworkType, long pTimePeriodStart)
	{
		ArrayList<SummaryResult> summaryResults = new ArrayList<SummaryResult>();		
		String whereClause;
		
		// Depending on the network type, we use different where clauses.
		switch (pNetworkType)
		{
			case 0:
				whereClause = " WHERE " + SKSQLiteHelper.TABLE_TESTBATCH + "." + SKSQLiteHelper.TB_COLUMN_DTIME + " > " + pTimePeriodStart;
				break;
				
			case 1:
				whereClause = " WHERE " + SKSQLiteHelper.TABLE_PASSIVEMETRIC + "." + SKSQLiteHelper.PM_COLUMN_METRIC + "= \"activenetworktype\" AND " + SKSQLiteHelper.TABLE_PASSIVEMETRIC + "." + SKSQLiteHelper.PM_COLUMN_VALUE + " = " + "\"WiFi\"" + 
				" AND " + SKSQLiteHelper.TABLE_TESTBATCH + "." + SKSQLiteHelper.TB_COLUMN_DTIME + " > " + pTimePeriodStart;
				break;
				
			case 2:
				whereClause = " WHERE " + SKSQLiteHelper.TABLE_PASSIVEMETRIC + "." + SKSQLiteHelper.PM_COLUMN_METRIC + "= \"activenetworktype\" AND " + SKSQLiteHelper.TABLE_PASSIVEMETRIC + "." + SKSQLiteHelper.PM_COLUMN_VALUE + " = " + "\"mobile\"" + 
				" AND " + SKSQLiteHelper.TABLE_TESTBATCH + "." + SKSQLiteHelper.TB_COLUMN_DTIME + " > " + pTimePeriodStart;
				break;
	
			default:
				whereClause = " WHERE " + SKSQLiteHelper.TABLE_TESTBATCH + "." + SKSQLiteHelper.TB_COLUMN_DTIME + " > " + pTimePeriodStart;
				break;
		}
		
		// Defining the query
		String query = "SELECT " + SKSQLiteHelper.TABLE_TESTRESULT + "." + SKSQLiteHelper.TR_COLUMN_TYPE+ " ,AVG(" + SKSQLiteHelper.TR_COLUMN_RESULT + "), MAX(" + SKSQLiteHelper.TR_COLUMN_RESULT + "), MIN(" + SKSQLiteHelper.TR_COLUMN_RESULT + ")" +
				" FROM " + SKSQLiteHelper.TABLE_TESTBATCH + " JOIN " + SKSQLiteHelper.TABLE_PASSIVEMETRIC + " ON " + SKSQLiteHelper.TABLE_TESTBATCH + "." + SKSQLiteHelper.TB_COLUMN_ID + " = " + SKSQLiteHelper.TABLE_PASSIVEMETRIC + "." + SKSQLiteHelper.PM_COLUMN_BATCH_ID + 
				" JOIN " + SKSQLiteHelper.TABLE_TESTRESULT + " ON " + SKSQLiteHelper.TABLE_TESTBATCH + "." + SKSQLiteHelper.TB_COLUMN_ID + " = " + SKSQLiteHelper.TABLE_TESTRESULT + "." + SKSQLiteHelper.TR_COLUMN_BATCH_ID
				+ whereClause +
				" GROUP BY " + SKSQLiteHelper.TABLE_TESTRESULT + "." + SKSQLiteHelper.TR_COLUMN_TYPE;
		
		if (open() == true)
		{			
			Cursor cursor = database.rawQuery(query, null);
			
			int testType = 0;
			float max = 0;
			float min = 0;
			float average = 0;
			
			if (cursor.moveToFirst())
			{
				do
				{
					if (cursor.getString(0).equals("download"))
					{
						testType = 0;						
						average = cursor.getFloat(1) / 1000000;
					    max = cursor.getFloat(2) / 1000000;
					    min = cursor.getFloat(3) / 1000000;
					}
					else if (cursor.getString(0).equals("upload"))
					{
						testType = 1;
						average = cursor.getFloat(1) / 1000000;
					    max = cursor.getFloat(2) / 1000000;
					    min = cursor.getFloat(3) / 1000000;
					}
					else if (cursor.getString(0).equals("latency"))
					{
						testType = 2;
						average = cursor.getFloat(1) / 1000;
					    max = cursor.getFloat(2) / 1000;
					    min = cursor.getFloat(3) / 1000;
					}
					else if (cursor.getString(0).equals("packetloss"))
					{
						testType = 3;
						average = cursor.getFloat(1);
					    max = cursor.getFloat(2);
					    min = cursor.getFloat(3);
					}
					else if (cursor.getString(0).endsWith("jitter"))
					{
						testType = 4;
						average = cursor.getFloat(1) / 1000;
					    max = cursor.getFloat(2) / 1000;
					    min = cursor.getFloat(3) / 1000;						
					}
					
					summaryResults.add(new SummaryResult(testType, average, max, min));
				}
				while (cursor.moveToNext());
			}
		}		
		return summaryResults;
	}

}
