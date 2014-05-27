package com.samknows.measurement.activity.components;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.MainService;
import com.samknows.libcore.R;
import com.samknows.measurement.util.OtherUtils;

public class DataUsedPreference extends DialogPreference{

	private TextView mSplashText,mValueText;
	private Context mContext;
	private String mDialogMessage, mSuffix;
	  
	public DataUsedPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		setTitle(mContext.getString(R.string.data_used_preference_title)+" "+OtherUtils.formatToBytes(SK2AppSettings.getInstance().getUsedBytes()));
	}

	public DataUsedPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mDialogMessage=mContext.getString(R.string.reset_data_cap_question);
		setTitle(mContext.getString(R.string.data_used_preference_title)+" "+OtherUtils.formatToBytes(SK2AppSettings.getInstance().getUsedBytes()));
		 
	}

	
	
	//If data cap was reached once has been reset restart the main service
	@Override
	protected void onDialogClosed(boolean positiveResult){
		super.onDialogClosed(positiveResult);
		SK2AppSettings app = SK2AppSettings.getSK2AppSettingsInstance();
		
		if (positiveResult){
			boolean restart = app.isDataCapReached();
			app.resetDataUsage();
			setTitle(mContext.getString(R.string.data_used_preference_title)+" "+OtherUtils.formatToBytes(app.getUsedBytes()));
			if(restart){
				MainService.poke(mContext);
			}
		}
		
		 		
	}
	
	  @Override 
	  protected View onCreateDialogView() {
	    LinearLayout.LayoutParams params;
	    LinearLayout layout = new LinearLayout(mContext);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    layout.setPadding(6,6,6,6);

	    mSplashText = new TextView(mContext);
	    if (mDialogMessage != null){
	    	mSplashText.setText(mDialogMessage);
	    }
	    layout.addView(mSplashText);

	    mValueText = new TextView(mContext);
	    mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
	    mValueText.setTextSize(32);
	    params = new LinearLayout.LayoutParams(
	        LinearLayout.LayoutParams.FILL_PARENT, 
	        LinearLayout.LayoutParams.WRAP_CONTENT);
	    layout.addView(mValueText, params);

	    return layout;
	  }


	
}
