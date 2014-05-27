package com.samknows.measurement.activity.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class SuffixTextView extends TextView{
	private String suffix = "sfx";
	public SuffixTextView(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	public void setText(String text) {
		super.setText(text + " " + suffix);
	}
	public void setSuffix(String text){
		suffix = text;
	}
}
