package com.samknows.measurement;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.samknows.libcore.SKConstants;
import com.samknows.libcore.SKPorting;
import com.samknows.measurement.util.TimeUtils;

public class SKAppSettings {
	
	static final String TAG = SKAppSettings.class.getName();

	protected Context ctx;
	public String reportingServerPath;
	public long rescheduleTime;
	public long rescheduleServiceTime;
	public long testStartWindow;
	public long testStartWindowWakeup;
	public String brand;
	public boolean multipleTabsEnabled;
	public int app_version_code;
	public String app_version_name;
	public boolean force_download = false;
	protected static SKAppSettings instance;
	
	protected SKAppSettings(Context c)
	{
		ctx = c;

		try {
			int propertiesId = c.getResources().getIdentifier("properties", "raw", c.getPackageName());

			InputStream is = c.getResources().openRawResource(propertiesId);
			Properties p = new Properties();
			try {
				p.load(is);
				reportingServerPath = p.getProperty(SKConstants.PROP_REPORTING_PATH);
				rescheduleTime = Long.valueOf(p.getProperty(SKConstants.PROP_RESCHEDULE_TIME));
				testStartWindow = Long.valueOf(p.getProperty(SKConstants.PROP_TEST_START_WINDOW_RTC));
				rescheduleServiceTime = Long.valueOf(p.getProperty(SKConstants.PROP_KILLED_SERVICE_RESTART_TIME_IN_MILLIS));
				brand = p.getProperty(SKConstants.PROP_BRAND);
				multipleTabsEnabled = Boolean.valueOf(p.getProperty(SKConstants.ENABLE_MULTIPLE_TABS, "true"));
				PackageInfo pInfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
				app_version_code = pInfo.versionCode;
				app_version_name = pInfo.versionName;

			} catch (IOException e) {
				SKPorting.sAssertE(TAG, "failed to load properties!");
			} catch (NameNotFoundException nnfe) {
				SKPorting.sAssertE(TAG, "failed to read manifest file: " + nnfe.getMessage());
			} catch (NullPointerException npe) {
				// This should be seen only when running a mock test.
				Log.e(this.getClass().getName(), "NullPointerException - make sure this happens only when running a mock test!");
				SKPorting.sAssertE(TAG, npe.getMessage());
				app_version_code = 0;
			} finally {
				IOUtils.closeQuietly(is);
			}
		} catch (Resources.NotFoundException e) {
			// Deal with apps that *don't* have a raw properties file!
      SKPorting.sAssertE(TAG, "failed to find raw/properties in the project!");

		}
	}

	public static SKAppSettings getInstance() {
		return instance;
	}
	
	public boolean getIsBackgroundProcessingEnabledInTheSchedule() {
		
		long testsScheduled = getInstance().getLong("number_of_tests_schedueld", -1);
		return testsScheduled > 0;

	}

//	public static void create(Context c) {
//		instance = new SKAppSettings(c);
//	}

	public SKAppSettings() {
		super();
	}

	public void saveString(String key, String value) {
		Editor editor = ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).edit();
		editor.putString(key, value);
		editor.commit();
	}

	public String getString(String key) {
		return ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).getString(key, null);
	}

	public String getResourceString(int id) {
		return ctx.getString(id);
	}

	public void saveBoolean(String key, boolean value) {
		Editor editor = ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	public boolean getBoolean(String key, boolean def) {
		return ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).getBoolean(key, def);
	}

	public void saveLong(String key, long value) {
		Editor editor = ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).edit();
		editor.putLong(key, value);
		editor.commit();
	}

	public long getLong(String key, long defValue) {
		return ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).getLong(key, defValue);
	}

	public String getUnitId() {
		return getString(SKConstants.PREF_KEY_UNIT_ID);
	}

//	public String getServerBaseUrl() {
//    return SKApplication.getAppInstance().getBaseUrlForUpload();
//	}

//	public void saveServerBaseUrl(String url) {
//		saveString(SKConstants.PREF_KEY_SERVER_BASE_URL, url);
//	}

	private long getUsedBytesPrevTime() {
		return getLong(SKConstants.PREF_KEY_USED_BYTES_MONTH, TimeUtils.getStartMonthTime());
	}

	public void appendUsedBytes(long bytes) {
		long time = TimeUtils.getStartMonthTime();
		if (getUsedBytesPrevTime() != time) { //current month is different from previous, means we are over a month and have to skip all previous results
			bytes = getUsedBytes();
		} else {
			bytes += getUsedBytes();
		}
		Editor editor = ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).edit();
		editor.putLong(SKConstants.PREF_KEY_USED_BYTES, bytes);
		editor.putLong(SKConstants.PREF_KEY_USED_BYTES_MONTH, time);
		editor.commit();
		Log.d(TAG, "saved used bytes: " + bytes);
	}

	public long getUsedBytes() {
		return getLong(SKConstants.PREF_KEY_USED_BYTES, 0);
	}

	/**
	 * data cap in bytes
	 * if preference has been defined use it
	 * otherwise use the datacap from config file
	 * if none of them is defined use Long.MAX_VALUE
	 * 	  
	 */
	public long getDataCapBytes() {
		long mb = Long.valueOf(PreferenceManager.getDefaultSharedPreferences(ctx).getString(SKConstants.PREF_DATA_CAP, "-1")); //in megs
		long bytes = mb * 1024L * 1024L;
	
		if (bytes < 0) {
			Log.e(this.getClass().toString(), "getDataCapBytes < 0");
		}
		
		if (bytes < 0) {
			bytes = Long.MAX_VALUE;
		}
		return bytes;
	}

	public boolean isDataCapReached() {
		boolean reached = getDataCapBytes() <= getUsedBytes();
		
		if (reached) {
			if (SKApplication.getAppInstance().getIsDataCapEnabled() == true) {
				return true;
			} else {
				// Datacap DISABLED, by user-specific override in Preferences screen.
				return false;
			}
		}
		
		return false;
	}

	public boolean isWakeUpEnabled() {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(SKConstants.PREF_ENABLE_WAKEUP, true);
	}

	public void setWakeUpEnabledIfNull(boolean enabled) {
		if (!PreferenceManager.getDefaultSharedPreferences(ctx).contains(SKConstants.PREF_ENABLE_WAKEUP)) {
			PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(SKConstants.PREF_ENABLE_WAKEUP, enabled).commit();
		}
	}

	public void saveNextRunTime(long time) {
		Editor editor = ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).edit();
		editor.putLong(SKConstants.PREF_NEXT_RUN_TIME,  time);
		editor.commit();
	}

	public long getNextRunTime() {
		long value = getLong(SKConstants.PREF_NEXT_RUN_TIME, SKConstants.NO_NEXT_RUN_TIME);
    return value;
	}

	public String getConfigPath() {
		return getString(SKConstants.PREF_KEY_CONFIG_PATH);
	}

	public String getUsername() {
		return getString(SKConstants.PREF_KEY_USERNAME);
	}

	public String getPassword() {
		return getString(SKConstants.PREF_KEY_PASSWORD);
	}

	public List<DeviceDescription> getDevices() {
		String devices = getString(SKConstants.PREF_KEY_DEVICES); 
		return DeviceDescription.parce(devices);
	}
}
