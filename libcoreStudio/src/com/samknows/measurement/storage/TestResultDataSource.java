package com.samknows.measurement.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.test.TestExecutor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

public class TestResultDataSource {
	private SQLiteDatabase database;
	private SKSQLiteHelper dbhelper;
	
	private static final Map<String, Integer> columnIdx;
	static {
		Map<String, Integer> tmpMap = new HashMap<String,Integer>();
		for(int i = 0; i < SKSQLiteHelper.TABLE_TESTRESULT_ALLCOLUMNS.length; i++){
			tmpMap.put(SKSQLiteHelper.TABLE_TESTRESULT_ALLCOLUMNS[i], i);
		}
		columnIdx = Collections.unmodifiableMap(tmpMap);
	}
	
	private static final String order = SKSQLiteHelper.TR_COLUMN_TYPE + " ASC";
	
	public TestResultDataSource(Context context){
		dbhelper = new SKSQLiteHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbhelper.getWritableDatabase();
	}
	
	public void close(){
		database.close();
	}
	
	
	//Given a test output string it parses it and than inserts it in the test_result table
	public void insertTest(String data, int test_batch_id, TestExecutor forTestExecutor){
		List<JSONObject> tests = StorageTestResult.testOutput(data.split(SKConstants.RESULT_LINE_SEPARATOR), forTestExecutor);
		for(JSONObject test: tests){
			insert(test,test_batch_id);
		}	
	}
	
	
	public void insert(List<JSONObject> tests, long test_batch_id){
		for(JSONObject test:tests){
			insert(test, test_batch_id);
		}
	}
	
	public void insert(JSONObject test, long test_batch_id){
		try{
			String type_name = test.getString(StorageTestResult.JSON_TYPE_NAME);
			long dtime = test.getLong(StorageTestResult.JSON_DTIME);
			long success = test.getLong(StorageTestResult.JSON_SUCCESS);
			double result = test.getDouble(StorageTestResult.JSON_RESULT);
			String location = test.getString(StorageTestResult.JSON_LOCATION);
			insert(type_name, dtime, success, result, location, test_batch_id);
		}catch(JSONException je){
			SKLogger.e(TestResultDataSource.class, "Error in converting TestResult JSONObject in database entry: " + je.getMessage());
		}
		
	}
	
	public void insert(String type_name, long dtime, long success, double result, String location, long test_batch_id){
		ContentValues values = new ContentValues();
		values.put(SKSQLiteHelper.TR_COLUMN_DTIME, dtime);
		values.put(SKSQLiteHelper.TR_COLUMN_TYPE, type_name);
		values.put(SKSQLiteHelper.TR_COLUMN_LOCATION,location);
		values.put(SKSQLiteHelper.TR_COLUMN_SUCCESS, success);
		values.put(SKSQLiteHelper.TR_COLUMN_RESULT, result);
		values.put(SKSQLiteHelper.TR_COLUMN_BATCH_ID, test_batch_id);
		database.insert(SKSQLiteHelper.TABLE_TESTRESULT, null, values);	
	}
	
	//Returns all the TestResult stored in the db
	public List<StorageTestResult> getAllTestResults(){
		return getTestResults(null);
	}
	
	//Returns all the TestResult stored in the db for a given test type
	public List<StorageTestResult> getAllTestResultsByType(String type){
		String selection = String.format(Locale.US, "%s = '%s'",SKSQLiteHelper.TR_COLUMN_TYPE, type );
		return getTestResults(selection);
	}
	
	//Returns all the TestResult stored in the db run in an interval
	public List<StorageTestResult> getAllTestResultsInterval(long starttime, long endtime){
		String selection = String.format(Locale.US, "%s BETWEEN %d AND %d", SKSQLiteHelper.TR_COLUMN_DTIME, starttime, endtime);
		return getTestResults(selection);
	}
	
	//Returns n TestResult from the database for a given type after a specific time
	public List<StorageTestResult> getTestResults(String type, long starttime, int n){
		String selection = String.format(Locale.US, "%s = '%s' AND %s >= %d", SKSQLiteHelper.TR_COLUMN_TYPE, type,
				SKSQLiteHelper.TR_COLUMN_DTIME, starttime);
		return getTestResults(selection, n+"");
	}
	
	//Returns n TestResult the ith result for a given tpe
	public List<StorageTestResult> getTestResults(String type, int startindex, int n){
		String selection = String.format(Locale.US, "%s = '%s'", SKSQLiteHelper.TR_COLUMN_TYPE, type);
		String limit = String.format("%d,%d",startindex, n );
		return getTestResults(selection, limit);
	}
	
	
	public List<AggregateTestResult> getAverageResults(long starttime, long endtime){
		List<AggregateTestResult> ret = new ArrayList<AggregateTestResult>();
		String selection= String.format(Locale.US, "dtime BETWEEN %d AND %d AND success <> 0", starttime, endtime);
		String averageColumn = String.format("AVG(%s)",SKSQLiteHelper.TR_COLUMN_RESULT);
		
		String[] columns = {SKSQLiteHelper.TR_COLUMN_TYPE, averageColumn, "COUNT(*)" }; 
		String groupBy = SKSQLiteHelper.TR_COLUMN_TYPE;
		Cursor cursor = database.query(SKSQLiteHelper.TABLE_TESTRESULT, columns,
				selection, null, groupBy, null, null);
		cursor.moveToFirst();
		while(!cursor.isAfterLast()){
			AggregateTestResult curr = new AggregateTestResult();
			curr.testType = cursor.getString(0);
			curr.aggregateFunction = "average";
			curr.value = cursor.getDouble(1);
			curr.numberOfResults = cursor.getInt(2);
			ret.add(curr);
		}
		cursor.close();
		return ret;
	}
	
	private List<StorageTestResult> getTestResults(String selection){
		return getTestResults(selection, null);
	}
	
	private List<StorageTestResult> getTestResults(String selection, String limit){
		List<StorageTestResult> ret = new ArrayList<StorageTestResult>();
		Cursor cursor = database.query(SKSQLiteHelper.TABLE_TESTRESULT, SKSQLiteHelper.TABLE_TESTRESULT_ALLCOLUMNS,
				selection, null, null, null, order, limit);
		cursor.moveToFirst();
		while(!cursor.isAfterLast()){
			ret.add(cursorToTestResult(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return ret;
	}

	private StorageTestResult cursorToTestResult(Cursor cursor){
		long id = cursor.getLong(columnIdx.get(SKSQLiteHelper.TR_COLUMN_ID));
		String type = cursor.getString(columnIdx.get(SKSQLiteHelper.TR_COLUMN_TYPE));
		long dtime = cursor.getLong(columnIdx.get(SKSQLiteHelper.TR_COLUMN_DTIME));
		String location = cursor.getString(columnIdx.get(SKSQLiteHelper.TR_COLUMN_LOCATION));
		long success = cursor.getLong(columnIdx.get(SKSQLiteHelper.TR_COLUMN_SUCCESS));
		double result = cursor.getDouble(columnIdx.get(SKSQLiteHelper.TR_COLUMN_RESULT));
		return new StorageTestResult(type, dtime, location,success, result);
	}
	
}
