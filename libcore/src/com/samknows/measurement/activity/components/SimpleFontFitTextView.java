package com.samknows.measurement.activity.components;

import android.content.Context;

import android.util.AttributeSet;

import com.lb.auto_fit_textview.AutoResizeTextView;

// A version of TextView that automatically scales-back the font size to fit the available space.

public class SimpleFontFitTextView extends AutoResizeTextView {

  public SimpleFontFitTextView(Context context) {
    super(context);
  }

  public SimpleFontFitTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SimpleFontFitTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

//  // Enable the following if you want some yellow debug boxes drawn around your items!
//	@Override
//	protected void onDraw(Canvas canvas) {
//		// TODO Auto-generated method stub
//		super.onDraw(canvas);
//
//		Paint paint = new Paint();
//		paint.setColor(Color.CYAN);
//		paint.setStyle(Paint.Style.STROKE);
//		canvas.drawRect(new Rect(0,0,getWidth()-1, getHeight()-1), paint);
//	}
}

