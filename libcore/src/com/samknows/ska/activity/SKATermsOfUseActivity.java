package com.samknows.ska.activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.samknows.libcore.R;
import com.samknows.libcore.SKPorting;
import com.samknows.libcore.SKTypeface;
import com.samknows.measurement.activity.BaseLogoutActivity;

public class SKATermsOfUseActivity extends BaseLogoutActivity {
	WebView mWebView;
	WebAppInterface mWebInterface;
	
	@Override
	public void onStart(){
		super.onStart();
		//getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.ska_terms_of_use_activity);
		setTitle(getString(R.string.terms_of_use_title));
		
		mWebView = (WebView) findViewById(R.id.webview);
		SKPorting.sAssert(getClass(), mWebView != null);
		
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebInterface = new WebAppInterface(this);
		mWebView.addJavascriptInterface(mWebInterface, "Android");

		mWebView.loadUrl("file:///android_asset/terms_of_use.htm");
	}
	
	
	public class WebAppInterface {
		final Context mContext;

		/** Instantiate the interface and set the context */
		WebAppInterface(Context c) {
			mContext = c;
		}

		@JavascriptInterface
		public void showToast(String toast) {

			final AlertDialog alertDialog = new AlertDialog.Builder(
					SKATermsOfUseActivity.this).create();
			alertDialog.setMessage(toast);
			alertDialog.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {

					alertDialog.dismiss();

				}
			});

			SKTypeface.sChangeChildrenToDefaultFontTypeface(findViewById(android.R.id.content));

			alertDialog.show();

		}
		
	}
}
