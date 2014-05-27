package com.samknows.measurement.storage;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TestBatchDataSource {
	private SQLiteDatabase database;
	private SKSQLiteHelper dbhelper;
	
	
	
	private static final String order =  SKSQLiteHelper.TB_COLUMN_DTIME + " ASC";
	
	public TestBatchDataSource(Context context){
		dbhelper = new SKSQLiteHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbhelper.getWritableDatabase();
	}
	
	public void close(){
		database.close();
	}
	
	public TestBatch insertTestBatch(long dtime, boolean manual, List<StorageTestResult> tr, List<PassiveMetric> pm){
		TestBatch ret = new TestBatch(dtime, manual);
		ContentValues values = new ContentValues();
		values.put(SKSQLiteHelper.TB_COLUMN_DTIME, dtime);
		values.put(SKSQLiteHelper.TB_COLUMN_MANUAL, manual ? 1 : 0 );
		long insertId = database.insert(SKSQLiteHelper.TABLE_TESTBATCH, null, values);
		
		return ret;
	}
	
	
	/*
	public TestBatch createTestGroup(long dtime, String result){
		ContentValues values = new ContentValues();
		values.put(SKSQLiteHelper.TG_COLUMN_DTIME, dtime);
		values.put(SKSQLiteHelper.TG_COLUMN_RESULT, result);
		long insertId = database.insert(SKSQLiteHelper.TABLE_TESTGROUP, null, values);
		String where = String.format(Locale.US, "%s = %d", SKSQLiteHelper.TG_COLUMN_ID, insertId);
		Cursor cursor = database.query(SKSQLiteHelper.TABLE_TESTGROUP, allColumns,
				where, null, null, null, null);
		TestBatch ret = cursorToTestGroup(cursor);
		cursor.close();
		return ret;
	}
	*/
	
	//Return all the TestGroup in the database ordered by the starttime field
	//in ascendic order
	/*
	public List<TestBatch> getAllTestGroups(){
		List<TestBatch> ret = new ArrayList<TestBatch>();
		Cursor cursor = database.query(SKSQLiteHelper.TABLE_TESTGROUP, allColumns,
				null, null, null, null, order);
		cursor.moveToFirst();
		while(!cursor.isAfterLast()){
			ret.add(cursorToTestGroup(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return ret;
	}
	
	//Return all the TestGroup in the database between starttime and endtime
	//in ascendic order
	public List<TestBatch> getTestGroupsInterval(long starttime, long endtime){
		List<TestBatch> ret = new ArrayList<TestBatch>();
		String where = String.format(Locale.US, "%s BETWEEN %d AND %d",SKSQLiteHelper.TG_COLUMN_DTIME, starttime, endtime);
		Cursor cursor = database.query(SKSQLiteHelper.TABLE_TESTGROUP, allColumns,
				where, null, null, null, order);
		cursor.moveToFirst();
		while(!cursor.isAfterLast()){
			ret.add(cursorToTestGroup(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return ret;
	}
	
	//Return the first TestGroup after a specific time
	//if there are no TestGroup after that time returns null 
	public TestBatch getFirstAfter(long dtime){
		TestBatch ret = null;
		String where = String.format(Locale.US, "%s >= %d", SKSQLiteHelper.TG_COLUMN_DTIME, dtime);
		String limit ="1";
		Cursor cursor = database.query(SKSQLiteHelper.TABLE_TESTGROUP, allColumns, 
				where, null, null, order, limit); 
		cursor.moveToFirst();
		if(!cursor.isAfterLast()){
			ret = cursorToTestGroup(cursor);
		}
		cursor.close();
		return ret;
	}
	
	private TestBatch cursorToTestGroup(Cursor cursor){
		TestBatch ret = new TestBatch();
		ret.id = cursor.getLong(0);
		ret.dtime = cursor.getLong(1);
		ret.result = cursor.getString(3);
		return ret;
	}
*/
}
