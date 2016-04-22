package com.samknows.measurement.activity.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

// http://stackoverflow.com/questions/2646028/android-horizontalscrollview-within-scrollview-touch-handling
// http://stackoverflow.com/questions/17053270/android-viewpager-with-scrollviews-with-viewpagers-inside-the-scrollviews
public class CustomScrollView extends ScrollView {
	//private float xDistance, yDistance, lastX, lastY;

	private GestureDetector mGestureDetector;
	View.OnTouchListener mGestureListener;

	public CustomScrollView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    mGestureDetector = new GestureDetector(context, new YScrollDetector());
	    setFadingEdgeLength(0);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
	    return super.onInterceptTouchEvent(ev)
	            && mGestureDetector.onTouchEvent(ev);
	}

	// Return false if we're scrolling in the x direction
	class YScrollDetector extends SimpleOnGestureListener {
	    @Override
	    public boolean onScroll(MotionEvent e1, MotionEvent e2,
	            float distanceX, float distanceY) {
				return Math.abs(distanceY) > Math.abs(distanceX);
			}
	}
}