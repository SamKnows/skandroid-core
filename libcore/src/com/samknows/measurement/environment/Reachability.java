package com.samknows.measurement.environment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import com.samknows.libcore.R;
import com.samknows.libcore.SKPorting;
import com.samknows.measurement.SKApplication;

//
// This class offers similar functionality to the Reachability class on iOS.
//
public class Reachability {
  public static boolean sGetIsConnected() {

    NetworkInfo activeNetworkInfo = NetworkDataCollector.sGetNetworkInfo();
    if (activeNetworkInfo == null) {
      SKPorting.sAssert(NetworkDataCollector.class, false);
      return false;
    }

    return activeNetworkInfo.isConnected();
  }

  // We're not connected - show an alert - if possible - and optionally finish - and return false!
  public static boolean sCheckIfIsConnectedAndIfNotShowAnAlertThenFinish(final Activity activity, final boolean andThenFinish) {

    if (sGetIsConnected() == true) {
      return true;
    }

    // We're not connected - show an alert - if possible - and return false!
    if (!activity.isFinishing()) {
      new AlertDialog.Builder(activity)
          .setMessage(R.string.Offline_message)
          .setPositiveButton(R.string.ok_dialog, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              if (andThenFinish) {
                activity.finish();
                activity.overridePendingTransition(0, 0);
              }
            }
          }).show();
    }

    return false;
  }

  public static boolean sGetIsNetworkWiFi() {
    Context context = SKApplication.getAppInstance();
    if (context == null) {
      // We're UNDER TEST!
      return true;
    }

    NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
    if (info != null) {
      return info.getType() == ConnectivityManager.TYPE_WIFI;
    }
    return false;
  }

  public static boolean sGetIsNetworkConnectedToWiFi() {
    Context context = SKApplication.getAppInstance();
    if (context == null) {
      // We're UNDER TEST!
      return true;
    }
    ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    if (connManager != null && wifiManager != null) {
      NetworkInfo netInfo = connManager.getActiveNetworkInfo();
      WifiInfo wifiInfo = wifiManager.getConnectionInfo();
      if (netInfo != null && wifiInfo != null) {
        if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
          return sGetIsConnected();
        }
      }
    }

    return false;
  }

  // We're not connected - show an alert - if possible - and return false!
  public static boolean sCheckIfIsConnectedAndIfNotShowAnAlert(Activity activity) {
    return sCheckIfIsConnectedAndIfNotShowAnAlertThenFinish(activity, false);
  }

  @NonNull
  public static Boolean sGetGetDoesDeviceSupportCellularData() {
    // http://stackoverflow.com/questions/27536238/detect-if-an-android-device-has-mobile-data-capability
    Context context = SKApplication.getAppInstance().getApplicationContext();
    Boolean bDeviceHasCellularData = false; // Assume default case - it doesn't
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm != null) {
      NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
      if (ni != null) {
        // Not null - means that the device *DOES* have mobile data capability
        bDeviceHasCellularData = true;
        // Note that Genymotion devices take this route through the code!
      }
    }
    return bDeviceHasCellularData;
  }
}
