package com.samknows.measurement.activity.components;

import android.content.Context;

import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

// Based on https://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview

// A version of TextView that automatically scales-back the font size to fit the available space.
// This does not always work as expected on real devices (usually fine on the Emulator), hence
// we also have both LargeTextFontFitTextView and FontFitTextView as alternatives.

public class CustomFontFitTextView extends TextView {

    private float initialTextSizePx = 200;
        
    public CustomFontFitTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
        initialTextSizePx = getTextSize();
        initialise();
	}
    
    public CustomFontFitTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        initialTextSizePx = getTextSize();
        initialise();
    }

  	public CustomFontFitTextView(Context context) {
        super(context);
        initialise();
    }

    private void initialise() {
        mTestPaint = new Paint();
        mTestPaint.set(this.getPaint());
        //max size defaults to the initially specified text size unless it is too small
    }
   
    // https://stackoverflow.com/questions/6263250/convert-pixels-to-sp
    public static float spToPixels(Context context, Float sp) {
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return scaledDensity * sp;
    }

    /* Re size the font so the specified text fits in the text box
     * assuming the text box is the specified width.
     */
    private void refitText(String text, int textWidth, int textHeight) 
    { 
    	if (text.length() == 0) {
    		return;
    	}
        if (textWidth <= 0)
            return;
        if (textHeight <= 0)
            return;
        int targetWidth = textWidth - this.getPaddingLeft() - this.getPaddingRight();
        int targetHeight = textHeight - this.getPaddingTop() - this.getPaddingBottom();
        
        //final float hiSp = 16;
        //float hi = spToPixels(getContext(), hiSp);
        float hi = initialTextSizePx;
        
        float lo = 2;
        final float threshold = 0.5f; // How close we have to be

        mTestPaint.set(this.getPaint());

        // Home-in on the correct size to use - hi is upper bounds, lo is lower bounds.
        
        // NB the text bounds calculation doesn't account for multi-line text.
        // So if we need to account for this: if we have > 1 line, we need to sum-up the total line heights,
        // and use the maximum width.
        
        String[] lines = text.split("\n");
        
        // Notes:
        // - we finish when lo and hi are within a small difference threshold (0.5F)
        // - we use lo so that we are likely to be slightly too narrow rather than slightly too wide!
        while((hi - lo) > threshold) {
            float size = (hi+lo)/2;
            mTestPaint.setTextSize(size);
        	Rect bounds = new Rect();
        
        	float maxWidth = 0.0F;
        	float totalHeight = 0.0F;
        	
        	int lineCount = lines.length;

            for (String theLine : lines) {
                // Using getTextBounds accounts for multi-line text; measure text does *not*!
                mTestPaint.getTextBounds(theLine, 0, theLine.length(), bounds);
                maxWidth = Math.max(bounds.width(), maxWidth);
                totalHeight += bounds.height();
            }
        	
            if (maxWidth >= targetWidth)  {
                hi = size; // too big
            }
            else if (totalHeight >= targetHeight)  {
                hi = size; // too big
            }
            else {
                lo = size; // too small
            }
        }
        
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, lo);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        int height = getMeasuredHeight();
        refitText(this.getText().toString(), parentWidth, parentHeight);
        this.setMeasuredDimension(parentWidth, height);
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        refitText(text.toString(), this.getWidth(), this.getHeight());
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        if (w != oldw) {
            refitText(this.getText().toString(), w, h);
        }
    }

    //Attributes
    private Paint mTestPaint;
    
//	// Enable the following if you want some yellow debug boxes drawn around your items!
//	@Override
//	protected void onDraw(Canvas canvas) {
//		// TODO Auto-generated method stub
//		super.onDraw(canvas);
//	
//		Paint paint = new Paint();
//		paint.setColor(Color.CYAN);
//		paint.setStyle(Style.STROKE);
//		canvas.drawRect(new Rect(0,0,getWidth()-1, getHeight()-1), paint);
//	}
}


