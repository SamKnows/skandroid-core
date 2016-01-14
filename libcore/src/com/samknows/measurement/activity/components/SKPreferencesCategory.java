package com.samknows.measurement.activity.components;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.samknows.libcore.SKTypeface;

public class SKPreferencesCategory extends PreferenceCategory {
  public SKPreferencesCategory(Context context) {
    super(context);
  }

  public SKPreferencesCategory(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SKPreferencesCategory(Context context, AttributeSet attrs,
                              int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onBindView(View view) {
    super.onBindView(view);
    TextView titleView = (TextView) view.findViewById(android.R.id.title);
    //titleView.setTextColor(Color.RED);
    titleView.setTypeface(SKTypeface.sGetDefaultTypeface());
  }
}
