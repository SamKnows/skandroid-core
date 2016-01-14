package com.samknows.measurement.activity.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

// http://stackoverflow.com/questions/2646028/android-horizontalscrollview-within-scrollview-touch-handling
// http://stackoverflow.com/questions/17053270/android-viewpager-with-scrollviews-with-viewpagers-inside-the-scrollviews

public class CustomViewPager extends android.support.v4.view.ViewPager {
	public CustomViewPager(final Context context, final AttributeSet attrs) {
	    super(context, attrs);
	}

	public CustomViewPager(final Context context) {
	    super(context);
	}

	@Override
	protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
	    if (v != this && v instanceof android.support.v4.view.ViewPager) {
	        return true;
	    }
	    return false; // super.canScroll(v, checkV, dx, x, y);
	}
}
