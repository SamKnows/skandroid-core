package com.samknows.measurement.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

import com.samknows.libcore.SKLogger;

public class DialogUtils {
	public static void showErrorDialog(Activity ctx, String mess) {
		new AlertDialog.Builder(ctx)
			.setMessage(mess)
			.setPositiveButton("Ok", null)
			.create().show();
	}
	
	public static void showErrorDialog(Activity ctx, int messId) {
		new AlertDialog.Builder(ctx)
			.setMessage(messId)
			.setPositiveButton("Ok", null)
			.create().show();
	}
	
	public static void showErrorDialog(Activity ctx, int messId, int titleId) {
		new AlertDialog.Builder(ctx)
			.setMessage(messId)
			.setTitle(titleId)
			.setPositiveButton("Ok", null)
			.create().show();
	}
	
	public static void showErrorDialog(Activity ctx, int messId, DialogInterface.OnClickListener listener) {
		new AlertDialog.Builder(ctx)
			.setMessage(messId)
			.setPositiveButton("Ok", listener)
			.create().show();
	}
	

	
	
	/**
	 * Sometimes activity might be killed earlier than actual callback from http method will arrive and when it will try to dismiss dialog it won't exist anymore, 
	 * in that situation dismiss will throw an exception. We don't really care about this exception as there is no UI anymore, just ignore it
	 */
	public static void dismissQuietly(Dialog dialog) {
		try {
			if (dialog != null && dialog.isShowing()) {
				dialog.hide();
				dialog.dismiss();
			}
		} catch (IllegalArgumentException e) {
			SKLogger.e(DialogUtils.class, "failed to close dialog: " + e, e);
		}
	}

}
