package com.samknows.measurement.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.ByteArrayInputStream;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;


import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.DeviceDescription;
import com.samknows.measurement.MainService;
import com.samknows.libcore.R;
import com.samknows.measurement.environment.PhoneIdentityDataCollector;
import com.samknows.measurement.statemachine.State;
import com.samknows.measurement.storage.Conversions;
import com.samknows.measurement.test.ScheduledTestExecutionQueue;

public class OtherUtils {
	public static String formatToBytes(long bytes) {
		double data = bytes;
		if (data > 1024*1024) { 
			data /= 1024d;
			data /= 1024d;
			return String.format("%.2fMB", data);
		} else if (data > 1024) {
			data /= 1024d;
			return String.format("%.2fKB", data);
		} else {
			return bytes + "B";
		}
	}
	
	public static double sConvertBytesPerSecondToMbps1024Based(double bytesPerSecond) {
		  return bytesPerSecond * 8.0 / (1024.0 * 1024.0);
	}

	public static double sConvertMbps1024BasedToMBps1000Based(double value1024Based) {
		return value1024Based * (1024.0 * 1024.0) / (1000.0 * 1000.0);
	}
	
	public static String sBitrateMbps1024BasedToString (double bitrateMbps1024Based) {
		double bitrateMbps1000Based = sConvertMbps1024BasedToMBps1000Based(bitrateMbps1024Based);
		double bitrateBitsPerSecond = 1000000.0 * bitrateMbps1000Based;
		  
		return Conversions.throughputToString(bitrateBitsPerSecond);
	}

	public static String formatToBits(long bytes) {
		double data = bytes;
		data *= 8;
		if (data > 1000*1000) { 
			data /= 1000d;
			data /= 1000d;
			return String.format("%.2fMb", data);
		} else if (data > 1000) {
			data /= 1000d;
			return String.format("%.2fKb", data);
		} else {
			return bytes + "b";
		}
	}

	public static void reschedule(Context ctx, long timeDurationMilliseconds){
		long actualSystemTimeMilliseconds;
		if(SK2AppSettings.getInstance().isWakeUpEnabled()){
			actualSystemTimeMilliseconds = rescheduleWakeup(ctx, timeDurationMilliseconds);
		}else{
			actualSystemTimeMilliseconds = rescheduleRTC(ctx, timeDurationMilliseconds);
		}
		SKLogger.d(ctx,  "Rescheduled to " + TimeUtils.logString(actualSystemTimeMilliseconds) + ", "+ (actualSystemTimeMilliseconds - System.currentTimeMillis()) +" ms from now...");
	}

	public static long rescheduleRTC(Context ctx, long time) {

		SKLogger.d(OtherUtils.class, "+++++DEBUG+++++ rescheduleRTC time=" + time);

		if (ScheduledTestExecutionQueue.sGetDebugOnlyForceBackgroundTestingToBeFrequent()) {
			time = 5L * 60L * 1000L; Log.d(OtherUtils.class.getName(), "+++++DEBUG+++++ rescheduleRTC sGetDebugOnlyForceBackgroundTestingToBeFrequent forced time to time=" + time);
			SKLogger.sAssert(OtherUtils.class, false);
		}

		time = checkRescheduleTime(time);
		SKLogger.d(OtherUtils.class, "+++++DEBUG+++++ schedule RTC for " + time/1000 + "s from now");
		PendingIntent intent = PendingIntent.getService(ctx, 0, new Intent(ctx, MainService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager manager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		long systemTimeMilliseconds = System.currentTimeMillis() + time;

		// AlarmManager.RTC - This does NOT wake-up the device if it is asleep.
		manager.set(AlarmManager.RTC, systemTimeMilliseconds, intent);
		SK2AppSettings.getInstance().saveNextRunTime(systemTimeMilliseconds);
		return systemTimeMilliseconds;
	}

	public static void cancelAlarm(Context ctx){
		SKLogger.d(OtherUtils.class, "+++++DEBUG+++++ cancelAlarm");

		AlarmManager manager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		if(PendingIntent.getService(ctx,0, new Intent(ctx, MainService.class), PendingIntent.FLAG_NO_CREATE) == null){
			SKLogger.d(ctx, "There is no pending intent for the service");
		}
		PendingIntent intent = PendingIntent.getService(ctx, 0, new Intent(ctx, MainService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		manager.cancel(intent);
		SK2AppSettings.getInstance().saveNextRunTime(SKConstants.NO_NEXT_RUN_TIME);
	}

	public static long rescheduleWakeup(Context ctx, long time) {
		SKLogger.d(OtherUtils.class, "+++++DEBUG+++++ rescheduleWakeup time=" + time);
		time = checkRescheduleTime(time);
		SKLogger.d(OtherUtils.class, "+++++DEBUG+++++ time immediately overridden (by) = checkRescheduleTime to " + time);

		if (ScheduledTestExecutionQueue.sGetDebugOnlyForceBackgroundTestingToBeFrequent()) {
			time = 5L * 60L * 1000L; Log.d(OtherUtils.class.getName(), "+++++DEBUG+++++ rescheduleWakeup sGetDebugOnlyForceBackgroundTestingToBeFrequent forced time to time=" + time);
			SKLogger.sAssert(OtherUtils.class, false);
		}

		SKLogger.d(OtherUtils.class, "+++++DEBUG+++++ schedule RTC_WAKEUP for " + time/1000 + "s from now");
		PendingIntent intent = PendingIntent.getService(ctx, 0, new Intent(ctx, MainService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager manager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		long systemTimeMilliseconds = System.currentTimeMillis() + time;

		// AlarmManager.RTC_WAKEUP - This DOES wake-up the device if it is asleep.
		manager.set(AlarmManager.RTC_WAKEUP, systemTimeMilliseconds, intent);
		SK2AppSettings.getInstance().saveNextRunTime(systemTimeMilliseconds);
		return systemTimeMilliseconds;
	}

	//if the reschedule time is less than the testStart window play it safe an reschedule the main service for the 
	//the rescheduleTime
	private static long checkRescheduleTime(long time){
		SK2AppSettings a = SK2AppSettings.getSK2AppSettingsInstance();
		
		long ret = time;

		if (ScheduledTestExecutionQueue.sGetDebugOnlyForceBackgroundTestingToBeFrequent()) {
			SKLogger.sAssert(OtherUtils.class, false);
			return ret;
		}
		
		if(time <= a.getTestStartWindow()){
			SKLogger.w(OtherUtils.class, "reschedule time less than testStartWindow ("+a.getTestStartWindow()+"), changing it to: "+ a.rescheduleTime/1000+"s.");
			ret = a.rescheduleTime;
		}
		return ret;
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			SKLogger.e(OtherUtils.class, "failed to get ip address", ex);
		}
		return null;
	}

	public static boolean isRoaming(Context ctx) {
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.isNetworkRoaming();
	}

	public static boolean isWifi(Context ctx) {
		NetworkInfo info = ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (info != null) {
			return info.getType() == ConnectivityManager.TYPE_WIFI;
		}
		return false;
	}

	public static int getStateDescriptionRId(State state) {
		switch(state) {
		case NONE : return R.string.st_none_descr;
		case INITIALISE : return R.string.st_initialise_descr;
		case ACTIVATE : return R.string.st_activate_descr;
		case ASSOCIATE : return R.string.st_assosiate_descr;
		case CHECK_CONFIG_VERSION : return R.string.st_check_config_version_descr;
		case DOWNLOAD_CONFIG : return R.string.st_download_config_descr;
		case EXECUTE_QUEUE : return R.string.st_execute_queue_descr;
		case RUN_INIT_TESTS : return R.string.st_run_init_test_descr;
		case SUBMIT_RESULTS : return R.string.st_submit_results_descr;
		case SHUTDOWN : return R.string.st_shutdown_descr;
		default : throw new RuntimeException("no such state: " + state);
		}
	}

	public static boolean isPhoneAssosiated(Context ctx) {
		String imei = PhoneIdentityDataCollector.getImei(ctx);
		for (DeviceDescription dd : SK2AppSettings.getInstance().getDevices()) {
			if (dd.isCurrentDevice(imei)) return true;
		}
		return false;
	}

	public static void removeDeviceForImei(String imei, List<DeviceDescription> list) {
		for (int i = 0; i < list.size(); i++) {
			DeviceDescription dd = list.get(i);
			if (dd.getMac().equals(imei)) {
				list.remove(i);
				return;
			}
		}
	}

	public static  String stringEncoding(String value){
		Pattern p = Pattern.compile("%u([a-zA-Z0-9]{4})");
		Matcher m = p.matcher(value);
		StringBuffer sb = new StringBuffer();
		while(m.find()){
			m.appendReplacement(sb, String.valueOf((char)Integer.parseInt(m.group(1), 16)));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	public static boolean isThisDeviceAnEmulator() {
		if (Build.FINGERPRINT.startsWith("generic")) {
			// This is an Emulator!
			return true;
		}

		// This is a real device!
		return false;
	}

	// http://stackoverflow.com/questions/7085644/how-to-check-if-apk-is-signed-or-debug-build
	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");
	public static boolean isDebuggable(Context ctx)
	{
		boolean debuggable = false;

		try
		{
			PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),PackageManager.GET_SIGNATURES);
			Signature signatures[] = pinfo.signatures;

			CertificateFactory cf = CertificateFactory.getInstance("X.509");

			for ( int i = 0; i < signatures.length;i++)
			{   
				ByteArrayInputStream stream = new ByteArrayInputStream(signatures[i].toByteArray());
				X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);       
				debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
				if (debuggable)
					break;
			}
		}
		catch (NameNotFoundException e)
		{
			//debuggable variable will remain false
		}
		catch (CertificateException e)
		{
			//debuggable variable will remain false
		}
		return debuggable;
	}

}
