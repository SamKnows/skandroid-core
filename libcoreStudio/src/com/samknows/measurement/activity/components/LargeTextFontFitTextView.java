package com.samknows.measurement.activity.components;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

// The standard Android TextView doesn't paint with correct alignment on Samsung devices
// (maybe others?) if text size started as too large for the view, and if text size revised
// down from that "too large" to a size that fits; this works on the Emulator, however.
// To work around this, in a few cases we can use this class, which uses a custom onDraw
// method to render the text such that it is not offset incorrectly!
// At the moment, this is used only for displaying Upload/Download speed values, vertically
// centered in their containers; and also in the Summary view screen!
// Note that neither SimpleFontFitTextView or FontFitTextView behave exactly as required;
// SimpleFontFitTextView shows the text mis-aligned (probably a native-layer bug on Samsung, as
// it is fine in the Emulator), and FontFitTextView shows the text much
// smaller that required (this could be because the that class calculates size having
// mis-aligned the text down, not allowing correctly for the cut-off part of the text...?)

public class LargeTextFontFitTextView extends SimpleFontFitTextView {

    public LargeTextFontFitTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
    
    public LargeTextFontFitTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

  	public LargeTextFontFitTextView(Context context) {
        super(context);
    }

	// Enable the following if you want some cyan debug boxes drawn around your items!
	@Override
	protected void onDraw(Canvas canvas) {
		
		// Call the base implementation (draws text with correct style & colour, at the correct place...)
//		super.onDraw(canvas);

		Paint paint = new Paint();
		paint.setStyle(Style.STROKE);
		
		// DEBUG only: draw a bounding box, to show where it really is on screen (only use this in debug builds!)
//		paint.setColor(Color.CYAN);
//		canvas.drawRect(new Rect(0,0,getWidth()-1, getHeight()-1), paint);
	
		paint.setColor(getCurrentTextColor());
		paint.setTextSize(getTextSize());
		// Assume vertical centered!
		// We give text baseline to draw.
		int height = getHeight();
		int paddingTop = getPaddingTop();
		int paddingBottom = getPaddingBottom();
		canvas.drawText((String)getText(), getPaddingLeft(), paddingTop + height - 5, paint);
	}
}

