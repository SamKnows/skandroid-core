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
import android.preference.PreferenceManager;
import android.util.Log;

import com.samknows.libcore.SKConstants;
import com.samknows.libcore.SKLogger;

import com.samknows.measurement.util.TimeUtils;

public class SKAppSettings {
	
	static final String TAG = SKAppSettings.class.getName();

	protected Context ctx;
	public String dCSInitUrl;
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

		int propertiesId = c.getResources().getIdentifier("properties", "raw", c.getPackageName());

		InputStream is = c.getResources().openRawResource(propertiesId);
		Properties p = new Properties();
		try {
			p.load(is);
			dCSInitUrl = p.getProperty(SKConstants.PROP_DCS_URL);
			reportingServerPath = p.getProperty(SKConstants.PROP_REPORTING_PATH);
			rescheduleTime = Long.valueOf(p.getProperty(SKConstants.PROP_RESCHEDULE_TIME));
			testStartWindow = Long.valueOf(p.getProperty(SKConstants.PROP_TEST_START_WINDOW_RTC));
			rescheduleServiceTime = Long.valueOf(p.getProperty(SKConstants.PROP_KILLED_SERVICE_RESTART_TIME_IN_MILLIS));
			brand = p.getProperty(SKConstants.PROP_BRAND);
			multipleTabsEnabled = Boolean.valueOf(p.getProperty(SKConstants.ENABLE_MULTIPLE_TABS, "true"));
			PackageInfo pInfo 		= c.getPackageManager().getPackageInfo(c.getPackageName(),0);
			app_version_code 		= pInfo.versionCode;
			app_version_name 		= pInfo.versionName;

		} catch (IOException e) {
			SKLogger.e(TAG, "failed to load properies!");
		} catch (NameNotFoundException nnfe) {
			SKLogger.e(TAG, "failed to read manifest file: "+ nnfe.getMessage());
		} catch(NullPointerException npe){
			// This should be seen only when running a mock test.
			Log.e(this.getClass().getName(), "NullPointerException - make sure this happens only when running a mock test!");
			SKLogger.e(TAG, npe.getMessage());
			app_version_code = 0;
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	public static SKAppSettings getInstance() {
		return instance;
	}

	public boolean getIsBackgroundProcessingEnabledInTheSchedule() {
		
		long testsScheduled = getInstance().getLong("number_of_tests_schedueld", -1);
		if (testsScheduled > 0) {
			return true;
		}
		
		return false;
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

	public String getString(String key, String default_value) {
		return ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).getString(key, default_value);
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

	public void saveUnitId(String unitId) {
		saveString(SKConstants.PREF_KEY_UNIT_ID, unitId);
	}

	public String getServerBaseUrl() {
		return getString(SKConstants.PREF_KEY_SERVER_BASE_URL);
	}

	public void saveServerBaseUrl(String url) {
		saveString(SKConstants.PREF_KEY_SERVER_BASE_URL, url);
	}


	public boolean isServiceActivated() {
		return getBoolean(SKConstants.PREF_KEY_SERVICE_ACTIVATED, false);
	}

	public void setServiceActivated(boolean activated) {
		saveBoolean(SKConstants.PREF_KEY_SERVICE_ACTIVATED, activated);
	}

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
		SKLogger.d(TAG, "saved used bytes: " + bytes);
	}

	public long getUsedBytes() {
		return getLong(SKConstants.PREF_KEY_USED_BYTES, 0);
	}

	public void saveDataCap(long cap) {
		PreferenceManager.getDefaultSharedPreferences(ctx).edit().putString(SKConstants.PREF_DATA_CAP, cap+"").commit();
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

	public void setDataCapMbIfNull(long cap) {
		if (getDataCapBytes() == Long.MAX_VALUE || getDataCapBytes() <= 0) {
			saveDataCap(cap);
		}
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
		return getLong(SKConstants.PREF_NEXT_RUN_TIME, SKConstants.NO_NEXT_RUN_TIME);
	}

	public String getConfigVersion() {
		return getString(SKConstants.PREF_KEY_CONFIG_VERSION);
	}

	public void saveConfigVersion(String v) {
		saveString(SKConstants.PREF_KEY_CONFIG_VERSION, v);
	}

	public String getConfigPath() {
		return getString(SKConstants.PREF_KEY_CONFIG_PATH);
	}

	public void setForceDownload() {
		force_download = true;
	}

	public boolean forceDownload() {
		boolean ret = force_download;
		force_download = false;
		return ret;
	}


	public void saveConfigPath(String path) {
		saveString(SKConstants.PREF_KEY_CONFIG_PATH, path);
	}

	public boolean wasIntroShown() {
		return getBoolean(SKConstants.PREF_WAS_INTRO_SHOWN, false);
	}

	public void saveIntroShown(boolean wasShown) {
		saveBoolean(SKConstants.PREF_FILE_NAME, wasShown);
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

	public void saveUsername(String username) {
		saveString(SKConstants.PREF_KEY_USERNAME, username);
	}

	public void savePassword(String password) {
		saveString(SKConstants.PREF_KEY_PASSWORD, password);
	}

	public void saveDevices(String device) {
		saveString(SKConstants.PREF_KEY_DEVICES, device);
	}

	public void clearAll() {
		Editor editor = ctx.getSharedPreferences(SKConstants.PREF_FILE_NAME, Context.MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();
	}

}