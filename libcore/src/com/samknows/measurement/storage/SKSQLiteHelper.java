package com.samknows.measurement.storage;

import com.samknows.libcore.SKLogger;


import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SKSQLiteHelper extends SQLiteOpenHelper {
	
	// database and version definition
	private static final String DATABASE_NAME = "sk.db";
	private static final int DATABASE_VERSION = 2;
	
	//tables and columns definition
	
	//test result table
	public static final String TABLE_TESTRESULT = "test_result";
	public static final String TR_COLUMN_ID = "_id";
	public static final String TR_COLUMN_TYPE = "type";
	public static final String TR_COLUMN_DTIME = "dtime";
	public static final String TR_COLUMN_LOCATION = "location";
	public static final String TR_COLUMN_SUCCESS = "success";
	public static final String TR_COLUMN_RESULT = "result";
	public static final String TR_COLUMN_BATCH_ID = "batch_id";
	
	public static final String[] TABLE_TESTRESULT_ALLCOLUMNS = {
		TR_COLUMN_ID, TR_COLUMN_TYPE, TR_COLUMN_DTIME, 
		TR_COLUMN_LOCATION, TR_COLUMN_SUCCESS, TR_COLUMN_RESULT, TR_COLUMN_BATCH_ID
	};
	
	public static String CREATE_TABLE_TESTRESULT = "CREATE TABLE "
			+ TABLE_TESTRESULT + " ( "
			+ TR_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ TR_COLUMN_TYPE + " TEXT NOT NULL, "
			+ TR_COLUMN_DTIME + " INTEGER NOT NULL, "
			+ TR_COLUMN_LOCATION + " TEXT, "
			+ TR_COLUMN_SUCCESS + " INTEGER, "
			+ TR_COLUMN_RESULT + " REAL, "
			+ TR_COLUMN_BATCH_ID + " INTEGER "
			+ " ); ";
	
	public static final String TEST_RESULT_ORDER = TR_COLUMN_DTIME + " DESC";
	
	//Passive metric result table
	public static final String TABLE_PASSIVEMETRIC = "passive_metric";
	public static final String PM_COLUMN_ID = "_id";
	public static final String PM_COLUMN_METRIC = "metric";
	public static final String PM_COLUMN_DTIME = "dtime";
	public static final String PM_COLUMN_VALUE = "value";
	public static final String PM_COLUMN_TYPE = "type";
	public static final String PM_COLUMN_BATCH_ID = "batch_id";
	public static final String[] TABLE_PASSIVEMETRIC_ALLCOLUMNS = {
		PM_COLUMN_ID, PM_COLUMN_METRIC, PM_COLUMN_DTIME, PM_COLUMN_VALUE, PM_COLUMN_TYPE, PM_COLUMN_BATCH_ID	};
	public String CREATE_TABLE_PASSIVEMETRIC = "CREATE TABLE "
			+ TABLE_PASSIVEMETRIC + " ( "
			+ PM_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ PM_COLUMN_METRIC + " TEXT NOT NULL, "
			+ PM_COLUMN_DTIME + " INTEGER NOT NULL, "
			+ PM_COLUMN_VALUE + " TEXT, "
			+ PM_COLUMN_TYPE + " TEXT, "
			+ PM_COLUMN_BATCH_ID + " INTEGER "
			+ " ); ";
	
	//Test batch table
	public static final String TABLE_TESTBATCH = "test_batch";
	public static final String TB_COLUMN_ID = "_id";
	public static final String TB_COLUMN_DTIME = "dtime";
	public static final String TB_COLUMN_MANUAL = "manual";
	public static final String[] TABLE_TESTBATCH_ALLCOLUMNS = {
		TB_COLUMN_ID, TB_COLUMN_DTIME, TB_COLUMN_MANUAL};
	public String CREATE_TABLE_TESTBATCH = "CREATE TABLE "
			+ TABLE_TESTBATCH + " ( "
			+ TB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ TB_COLUMN_DTIME + " INTEGER NOT NULL, "
			+ TB_COLUMN_MANUAL + " INTEGER "
			+ " ); ";
	
	public static final String TEST_BATCH_ORDER = TB_COLUMN_DTIME + " DESC ";
	
	
	public static final String[] TABLES = {TABLE_TESTRESULT, 
		TABLE_PASSIVEMETRIC,TABLE_TESTBATCH };
	
	
			
	//database creation sql statement	
	public String[] DATABASE_CREATE = {
			CREATE_TABLE_TESTRESULT,  
			CREATE_TABLE_PASSIVEMETRIC,
			CREATE_TABLE_TESTBATCH
	};
	
	public SKSQLiteHelper(Context context){
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database){
		try{
			for(String s: DATABASE_CREATE){
				database.execSQL(s);
				SKLogger.d(this, "onCreate: "+ s);
			}
		}catch(SQLException sqle){
			SKLogger.e(SKSQLiteHelper.class, "Error in creating the database "+ sqle);
		}
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion){
		SKLogger.d(SKSQLiteHelper.class, "Upgrading database from version "+ oldVersion + " to version "+ newVersion +". All Data will be destroyed.");
		for(String table: TABLES){
			database.execSQL("DROP TABLE IF EXISTS "+ table);
		}
		onCreate(database);
	}
	
}
