package com.samknows.measurement.activity.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.samknows.libcore.*;

// http://stackoverflow.com/questions/8034494/tint-dim-drawable-on-touch?lq=1
public class TintableImageView extends ImageView {

	private boolean mIsSelected;

	public TintableImageView(Context context) {
		super(context);
		init();
	}   

	public TintableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}   

	public TintableImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		mIsSelected = false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN && !mIsSelected) {
			setColorFilter(0x99991133);
			invalidate();
			mIsSelected = true;
		} else if (event.getAction() == MotionEvent.ACTION_UP && mIsSelected) {
			setColorFilter(Color.TRANSPARENT);
			mIsSelected = false;
		}

		return super.onTouchEvent(event);
	}
}