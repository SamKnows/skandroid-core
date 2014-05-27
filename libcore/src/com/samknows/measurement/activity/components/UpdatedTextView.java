package com.samknows.measurement.activity.components;

import com.samknows.libcore.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class UpdatedTextView extends TextView{
	public UpdatedTextView(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	public void setText(String time, String date){
		setText(getContext().getString(R.string.updated) + time + " | " + date);
	}
}
