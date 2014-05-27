package com.samknows.measurement.activity.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class SuffixButton extends Button{
	private String suffix = "sfx";
	public SuffixButton(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	public void setText(String text){
		super.setText(text + " " + suffix);
	}
	public void setSuffix(String text){
		suffix = text;
	}
};
