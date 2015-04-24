package com.samknows.ska.activity;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.libcore.SKTypeface;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.MainService;
import com.samknows.measurement.SKApplication;
import com.samknows.libcore.R;
import com.samknows.measurement.util.OtherUtils;
import com.samknows.measurement.util.TimeUtils;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import android.support.v4.app.NavUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.TypefaceSpan;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class SKAPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{

  // http://stackoverflow.com/questions/8607707/how-to-set-a-custom-font-in-the-actionbar-title
  /**
   * Style a {@link Spannable} with a custom {@link Typeface}.
   *
   * @author Tristan Waddington
   */
  private class TypefaceSpan extends MetricAffectingSpan {
//    /** An <code>LruCache</code> for previously loaded typefaces. */
//    //private static LruCache<String, Typeface> sTypefaceCache =
//    private LruCache<String, Typeface> sTypefaceCache =
//        new LruCache<String, Typeface>(12);

    private Typeface mTypeface;

    /**
     * Load the {@link Typeface} and apply to a {@link Spannable}.
     */
    //public TypefaceSpan(Context context, String typefaceName) {
    public TypefaceSpan(Context context) {
      //mTypeface = sTypefaceCache.get(typefaceName);
      mTypeface = SKTypeface.sGetDefaultTypeface();

//      if (mTypeface == null) {
//        mTypeface = Typeface.createFromAsset(context.getApplicationContext()
//            .getAssets(), String.format("fonts/%s", typefaceName));
//
//        // Cache the loaded Typeface
//        sTypefaceCache.put(typefaceName, mTypeface);
//      }
    }

    @Override
    public void updateMeasureState(TextPaint p) {
      p.setTypeface(mTypeface);

      // Note: This flag is required for proper typeface rendering
      p.setFlags(p.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
      tp.setTypeface(mTypeface);

      // Note: This flag is required for proper typeface rendering
      tp.setFlags(tp.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }
  }

  // http://stackoverflow.com/questions/13193405/set-custom-font-for-text-in-preferencescreen/13193623#13193623
  static private class CustomTypefaceSpan extends android.text.style.TypefaceSpan {

    private final Typeface newType;

    public CustomTypefaceSpan(String family, Typeface type) {
      super(family);
      newType = type;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
      applyCustomTypeFace(ds, newType);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
      applyCustomTypeFace(paint, newType);
    }

    private static void applyCustomTypeFace(Paint paint, Typeface tf) {
      int oldStyle;
      Typeface old = paint.getTypeface();
      if (old == null) {
        oldStyle = 0;
      } else {
        oldStyle = old.getStyle();
      }

      int fake = oldStyle & ~tf.getStyle();
      if ((fake & Typeface.BOLD) != 0) {
        paint.setFakeBoldText(true);
      }

      if ((fake & Typeface.ITALIC) != 0) {
        paint.setTextSkewX(-0.25f);
      }

      paint.setTypeface(tf);
    }
  }

  // http://stackoverflow.com/questions/13193405/set-custom-font-for-text-in-preferencescreen/13193623#13193623
  public static void sConvertPreferenceToUseCustomFont(Preference somePreference) {
    if (somePreference == null) {
      return;
    }

    CustomTypefaceSpan customTypefaceSpan = new CustomTypefaceSpan("", SKTypeface.sGetDefaultTypeface());

    SpannableStringBuilder ss;
    if (somePreference.getTitle() != null) {
      ss = new SpannableStringBuilder(somePreference.getTitle().toString());
      ss.setSpan(customTypefaceSpan, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
      somePreference.setTitle(ss);
    }

    if (somePreference.getSummary() != null) {
      ss = new SpannableStringBuilder(somePreference.getSummary().toString());
      ss.setSpan(customTypefaceSpan, 0, ss.length(),Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
      somePreference.setSummary(ss);
    }
  }


  @SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    // Make sure we're running on Honeycomb or higher to use ActionBar APIs
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		addPreferencesFromResource(R.xml.preferences);
		
		String versionName="";
		try {
			versionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			SKLogger.e(this, e.getMessage());
		}
		PreferenceCategory mCategory = (PreferenceCategory) findPreference("first_category");
		mCategory.setTitle(SKApplication.getAppInstance().getAppName() + " " + getString(R.string.Storyboard_Settings_Configuration));

		CheckBoxPreference mCheckBoxPref = (CheckBoxPreference) findPreference(SKConstants.PREF_SERVICE_ENABLED);
		if (SKApplication.getAppInstance().getIsBackgroundProcessingEnabledInTheSchedule() == false) {
			// Background processing is NOT enabled in the schedule!
			
			// MPC 26/08/2014 - remove the background processing preference entirely, rather than just
			// greying it out, if background processing is not in the schedule.
			//mCheckBoxPref.setChecked(false);
			//mCheckBoxPref.setEnabled(false);
			mCategory.removePreference(mCheckBoxPref);
			
			// MPC 26/08/2014 - remove the wakeup preference entirely, if background processing is not in the schedule.
    		CheckBoxPreference wakeupPref = (CheckBoxPreference) findPreference(SKConstants.PREF_ENABLE_WAKEUP);
			mCategory.removePreference(wakeupPref);
		} else {
			// Background processing is enabled in the schedule!
			
			mCheckBoxPref.setChecked(SKApplication.getAppInstance().getIsBackgroundTestingEnabledInUserPreferences());
      sConvertPreferenceToUseCustomFont(mCheckBoxPref);
		}
		
		// Hide the "Use Data Cap" checkbox only for some versions of the app...
		if(SKApplication.getAppInstance().canDisableDataCap() == false) {
			Preference user_tag = findPreference("data_cap_enabled");
			PreferenceCategory pc = (PreferenceCategory) findPreference("first_category");
			pc.removePreference(user_tag);
		}

    SpannableString s = new SpannableString(getTitle());
    s.setSpan(new TypefaceSpan(this), 0, s.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

// Update the action bar title with the TypefaceSpan instance
    ActionBar actionBar = getActionBar();
    actionBar.setTitle(s);
		
		updateLabels();
	
	}
	
	@SuppressWarnings("deprecation")
	protected void updateLabels(){
		SK2AppSettings app = SK2AppSettings.getSK2AppSettingsInstance();
		long configDataCap = app.getLong(SKConstants.PREF_DATA_CAP, -1l );
		String s_configDataCap = configDataCap == -1l ? "": configDataCap +"";
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SKAPreferenceActivity.this);

		String data_cap = preferences.getString(SKConstants.PREF_DATA_CAP, s_configDataCap);
		Preference dataCapEditTextPreference;
		dataCapEditTextPreference = (Preference) findPreference(SKConstants.PREF_DATA_CAP);
		dataCapEditTextPreference.setTitle(getString(R.string.data_cap_title)+ " "+data_cap+getString(R.string.mb));
    sConvertPreferenceToUseCustomFont(dataCapEditTextPreference);


    Preference p;
		int data_cap_day = preferences.getInt(SKConstants.PREF_DATA_CAP_RESET_DAY, 1);
		p = (Preference) findPreference(SKConstants.PREF_DATA_CAP_RESET_DAY);
		p.setTitle(getString(R.string.data_cap_day_title)+ TimeUtils.getDayOfMonthFrom1AsStringWithNoSuffix(data_cap_day));
    sConvertPreferenceToUseCustomFont(p);

		CheckBoxPreference mCheckBoxDataCapEnabled = (CheckBoxPreference) findPreference(SKConstants.PREF_DATA_CAP_ENABLED);
    sConvertPreferenceToUseCustomFont(mCheckBoxDataCapEnabled);
		if (SKApplication.getAppInstance().canDisableDataCap() == true) {
			if (SKApplication.getAppInstance().getIsDataCapEnabled() == false) {
				mCheckBoxDataCapEnabled.setChecked(false);
				dataCapEditTextPreference.setEnabled(false);
			} else {
				mCheckBoxDataCapEnabled.setChecked(true);
				dataCapEditTextPreference.setEnabled(true);
        sConvertPreferenceToUseCustomFont(mCheckBoxDataCapEnabled);
			}
		}
	}
	
	
	@Override
	protected void onResume(){
		super.onResume();

    View view = findViewById(android.R.id.content);
    SKTypeface.sChangeChildrenToDefaultFontTypeface(view);

		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
	}
	
	
	@SuppressWarnings("deprecation")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(SKConstants.PREF_SERVICE_ENABLED)) {
			if (SKApplication.getAppInstance().getIsBackgroundTestingEnabledInUserPreferences()) {
				MainService.poke(SKAPreferenceActivity.this);
			}else{
				OtherUtils.cancelAlarm(this);
			}
		}
		if (key.equals(SKConstants.PREF_DATA_CAP)) {

		    String dataCapValueString = sharedPreferences.getString(key, "");
		    Long dataCapValue;
		    try{
		    	dataCapValue = Long.parseLong(dataCapValueString);
		    }catch(NumberFormatException nfe){
		    	dataCapValue = 0L;
		    }
		    
		    if(dataCapValue > SKConstants.DATA_CAP_MAX_VALUE){
		  
		    	EditTextPreference p = (EditTextPreference) findPreference(key);
		    	p.setText(""+SKConstants.DATA_CAP_MAX_VALUE);
          sConvertPreferenceToUseCustomFont(p);
		    	Toast t = Toast.makeText(SKAPreferenceActivity.this,getString(R.string.max_data_cap_message)+" "+SKConstants.DATA_CAP_MAX_VALUE, Toast.LENGTH_SHORT);
		    	t.show();
		    }else if(dataCapValue < SKConstants.DATA_CAP_MIN_VALUE){
		    	EditTextPreference p = (EditTextPreference) findPreference(key);
          sConvertPreferenceToUseCustomFont(p);
		    	p.setText(""+SKConstants.DATA_CAP_MIN_VALUE);
		    	Toast t = Toast.makeText(SKAPreferenceActivity.this,getString(R.string.min_data_cap_message)+" "+SKConstants.DATA_CAP_MIN_VALUE, Toast.LENGTH_SHORT);
		    	t.show();
		    }
		}
		updateLabels();
	}
	
	//
	// Required for options menu ActionBar on Honeycomb and later...
	//
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu; this adds items to the action bar if it is present.
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.ska_menu_main, menu);
	    return true;
	}
	
	//
	// Required for options menu ActionBar on Honeycomb and later...
	//
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
	    case android.R.id.home:
	    	try {
	    		NavUtils.navigateUpFromSameTask(this);
	    	} catch (IllegalArgumentException e) {
	    		if (e.toString().contains("SKAPreferenceActivity")) {
    	    		// This is required some times, e.g. coming back from Settings/SystemInfo - we can ignore it.
	    			//SKLogger.sAssert(getClass(), false);
	    		} else {
	    			// Unexpected!
	    			SKLogger.sAssert(getClass(), false);
	    		}
	    	} catch (Exception e) {
	    			// Unexpected!
	    		SKLogger.sAssert(getClass(), false);
	    	}
			onBackPressed();
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
}
