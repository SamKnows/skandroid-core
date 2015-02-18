package com.samknows.measurement.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.util.Base64;
import com.samknows.measurement.SK2AppSettings;

public class LoginHelper {
	private static final String TAG = LoginHelper.class.getSimpleName();
	
	
	public static void showErrorDialog(Context c, int messId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(c);
		builder.setMessage(messId)
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			});
		
		builder.create().show();
	}
	
	@SuppressLint("InlinedApi")
	public static void openMainScreen(Activity acc, Class theActivityClass) {
		boolean  bWithTransitionAnimationTrue = true;
    	openMainScreenWithTransitionAnimation(acc, bWithTransitionAnimationTrue, theActivityClass);
	}
	
	public static void openMainScreenWithNoTransitionAnimation(Activity acc, Class theActivityClass) {
		boolean  bWithTransitionAnimationFalse = false;
    	openMainScreenWithTransitionAnimation(acc, bWithTransitionAnimationFalse, theActivityClass);
	}
	
	public static void openMainScreenWithTransitionAnimation(Activity acc, boolean PWithTransitionAnimation, Class theActivityClass) {
		//Intent intent = new Intent(acc, SKAMainResultsActivity.class);
		Intent intent = new Intent(acc, theActivityClass);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		  intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TASK );
		}
		if (PWithTransitionAnimation) {
			// Default: use the standard transition animation!
		} else {
			// Not the default - do NOT use the transition animation!
		  intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		}
		acc.startActivity(intent);
		acc.finish();
	}
	
	public static String getCredentialsEncoded() {
		return Base64.encodeToString((getCredentials()).getBytes(), Base64.NO_WRAP);
	}
	
	public static String getCredentials() {
		SK2AppSettings appSettings = SK2AppSettings.getSK2AppSettingsInstance(); 
		return appSettings.getUsername() + ":" + appSettings.getPassword();
	}
}
