package com.samknows.ska.activity;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

import com.samknows.libcore.R;
import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.activity.BaseLogoutActivity;
import com.samknows.measurement.environment.NetworkDataCollector;
import com.samknows.measurement.util.LoginHelper;

public class SKATermsOfUseWithButtonActivity extends BaseLogoutActivity {
	WebView mWebView;
	//WebAppInterface mWebInterface;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// ALWAYS show the action bar in the settings screen!
	    // Make sure we're running on Honeycomb or higher to use ActionBar APIs
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    	    final ActionBar actionBar = getActionBar();
			//getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
			actionBar.show();
		}
		
		//getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.ska_terms_of_use_withbutton_activity);
		//setTitle(getString(R.string.terms_of_use_title));
		
		mWebView = (WebView) findViewById(R.id.webview);
		SKLogger.sAssert(getClass(), mWebView != null);
		
		mWebView.getSettings().setJavaScriptEnabled(false);
		//mWebInterface = new WebAppInterface(this);
		//mWebView.addJavascriptInterface(mWebInterface, "Android");
		mWebView.loadUrl("file:///android_asset/terms_of_use.htm");
	}
	
	// *** MENUS *** //
    MenuItem menu_Item_IAgree = null;
   
    public static boolean sGetAreTermsAccepted(Activity activity) {
    	if (SKApplication.sGetTermsAcceptedAtThisVersionOrGreater(activity, SKApplication.getAppInstance().getTAndCVersionToCheckFor()) == true)
    	{
    		return true;
    	}

    	return false;
    }
    
	// Initialise the contents of the Activity's standard options menu.
    // You should place your menu items in to menu. For this method to be called, you must have first called setHasOptionsMenu(boolean).
    //public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = new MenuInflater(this);
    
    	inflater.inflate(R.menu.ska_menu_terms_withbutton, menu);
    	super.onCreateOptionsMenu(menu);
    	
    	menu_Item_IAgree = menu.findItem(R.id.menu_action_iagree);

    	// If terms have been accepted, we should hide the button!
    	if (SKApplication.sGetTermsAcceptedAtThisVersionOrGreater(this, SKApplication.getAppInstance().getTAndCVersionToCheckFor()) == true)
    	{
    		menu_Item_IAgree.setVisible(false);
		}
    	
    	return true;
    }
    
	
	private boolean checkIfIsConnectedAndIfNotShowAnAlert() {
		
		if (NetworkDataCollector.sGetIsConnected() == true) {
			return true;
		}
	
		// We're not connected - show an alert if possible, and return false!
		if (!isFinishing()) {
			new AlertDialog.Builder(this)
			.setMessage(R.string.Offline_message)
			.setPositiveButton(R.string.ok_dialog, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			}).show();
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.menu_action_iagree) {
			// Agreed - so we can dismiss?
			// Do this only if online!
			if (checkIfIsConnectedAndIfNotShowAnAlert() == true) {
				SKApplication.sSetTermsAcceptedAtThisVersion(this, SKApplication.getAppInstance().getTAndCVersionToCheckFor());
				LoginHelper.openMainScreenWithNoTransitionAnimation(this, SKApplication.getAppInstance().getTheMainActivityClass());
			}
		}
		return super.onMenuItemSelected(featureId, item);
	}
    
	
//	public class WebAppInterface {
//		Context mContext;
//
//		/** Instantiate the interface and set the context */
//		WebAppInterface(Context c) {
//			mContext = c;
//		}
//		
//	}
}
