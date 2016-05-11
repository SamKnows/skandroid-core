package com.samknows.SKKit.SKMobileApp.Database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKApplication;

public class SKMDatabase extends SQLiteOpenHelper {
  // If you change the database schema, you must increment the database version.
  private static final int DATABASE_VERSION = 1;
  private static final String DATABASE_NAME = "SKMobile.db";

  private SKMDatabase(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  public void onCreate(SQLiteDatabase db) {
    try {
      db.execSQL(SKMStoredResult.SKMStoredResultContract.SQL_CREATE_ENTRIES);
    } catch (SQLException sqle) {
      SKLogger.sAssert(false);
    }
  }

  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // This database is only a cache for online data, so its upgrade policy is
    // to simply to discard the data and start over
    try {
      db.execSQL(SKMStoredResult.SKMStoredResultContract.SQL_DELETE_ENTRIES);
      onCreate(db);
    } catch (SQLException sqle) {
      SKLogger.sAssert(false);
    }
  }

  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    onUpgrade(db, oldVersion, newVersion);
  }


  private static SKMDatabase sSKMobileDatabaseHelper = null;
  public static void sResetSingletons() {
    // This is required for Robolectric testing!
    sSKMobileDatabaseHelper = null;
  }

  // Use this SINGLETON to access the database!
  // See http://stackoverflow.com/questions/2493331/what-are-the-best-practices-for-sqlite-on-android
  // "So, multiple threads? Use one helper. Period."
  //
  private static SKMDatabase sGetSKMobileHelper(Context context) {
    if (sSKMobileDatabaseHelper != null) {
      return sSKMobileDatabaseHelper;
    }

    sSKMobileDatabaseHelper = new SKMDatabase(context);
    return sSKMobileDatabaseHelper;
  }

  public static SQLiteDatabase sGetWriteableDatabase(Context context) {
    return SKMDatabase.sGetSKMobileHelper(context).getWritableDatabase();
  }

  public static SQLiteDatabase sGetReadableDatabase(Context context) {
    return SKMDatabase.sGetSKMobileHelper(context).getReadableDatabase();
  }
}
