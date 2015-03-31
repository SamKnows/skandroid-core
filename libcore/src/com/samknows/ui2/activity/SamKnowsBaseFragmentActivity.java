package com.samknows.ui2.activity;

import java.util.List;

import com.samknows.libcore.SKConstants;
import com.samknows.libcore.SKLogger;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;

public class SamKnowsBaseFragmentActivity extends FragmentActivity {

  static final String TAG = "SamKnowsBaseFragmentAct";

  public SamKnowsBaseFragmentActivity() {
    // TODO Auto-generated constructor stub
    super();
  }

  @Override
  public void onBackPressed() {
    // Ask the user if they want to quit the application when the back key is pressed!
    // This is done ONLY if we're the task root!
    // Note that this will keep the MainService running...
    if (wouldBackButtonReturnMeToTheHomeScreen()) {
      new AlertDialog.Builder(this)
          .setIcon(android.R.drawable.ic_dialog_alert)
          .setTitle(SKConstants.RStringQuit)
          .setMessage(SKConstants.RStringReallyQuit)
          .setPositiveButton(SKConstants.RStringYes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

              // Stop the activity - which will not stop the application, even though we're the
              // root task!
              SamKnowsBaseFragmentActivity.this.finish();

              Log.d(TAG, "+++++DEBUG+++++ closing the application!");

              // http://stackoverflow.com/questions/2033914/quitting-an-application-is-that-frowned-upon?lq=1
              // Exit the application cleanly... with all destructors called properly.
              // This is to try to resolve battery draining issues.
              System.runFinalizersOnExit(true);

              Log.d(TAG, "+++++DEBUG+++++ about to call exit(0)...");
              System.exit(0);

              Log.d(TAG, "+++++DEBUG+++++ exit(0) called!");
            }

          })
          .setNegativeButton(SKConstants.RStringNoDialog, null)
          .show();
      return;
    }

    super.onBackPressed();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // Respond to the action bar's Up/Home button
      case android.R.id.home:
//	    	try {
//	    		NavUtils.navigateUpFromSameTask(this);
//	    	} catch (Exception e) {
//	    		// This is reuqired some times, e.g. coming back from SystemInfo!
//	    		SKLogger.sAssert(getClass(), false);
//	    	}
        onBackPressed();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public boolean forceBackToAllowClose() {
    return false;
  }

  public boolean wouldBackButtonReturnMeToTheHomeScreen() {

    if (forceBackToAllowClose()) {
      return true;
    }

    try {

      ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
      List<RunningTaskInfo> tasks = am.getRunningTasks(2);

      SKLogger.sAssert(getClass(), tasks != null);

      if (tasks == null) {
        SKLogger.sAssert(getClass(), false);
        // Assume it must be OK to close!
        return true;
      }

      if (tasks.size() == 0) {
        SKLogger.sAssert(getClass(), false);
        // Assume it must be OK to close!
        return true;
      }

      RunningTaskInfo currentTask = tasks.get(0);
      if (currentTask == null) {
        SKLogger.sAssert(getClass(), false);
        // Assume it must be OK to close!
        return true;
      }

      if (tasks.size() < 2) {
        SKLogger.sAssert(getClass(), false);
        // Assume it must be OK to close!
        return true;
      }
      RunningTaskInfo nextTask = tasks.get(1);
      if (nextTask == null) {
        SKLogger.sAssert(getClass(), false);
        // Assume it must be OK to close!
        return true;
      }

      // if we're looking at this application's base/launcher Activity,
      // and the next task is the Android home screen, then we know we're
      // about to close the app...
      if (currentTask.topActivity.equals(currentTask.baseActivity)
          && nextTask.baseActivity.getPackageName().startsWith("com.android.launcher")) {
        Log.d(this.getClass().toString(), "This activity is the top activity, and will return us to the Home screen");
        return true;
      }
    } catch (java.lang.NullPointerException ex) {
      // Seen on some devices!
      SKLogger.sAssert(getClass(), false);
    }

    Log.d(this.getClass().toString(), "This activity is not the top activity, and will not return us to the Home screen");

    return false;
  }


}
