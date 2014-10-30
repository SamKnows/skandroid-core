package com.samknows.ui2.activity;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.samknows.libui2.R;

/**
 * This class is responsible for painting the gauge in the home screen.
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */


class GaugeView extends View
{
	// *** CONSTANTS *** //
	private static final int TEST_NO_TEST = -1;
	private static final int TEST_DOWNLOAD = 0;
	private static final int TEST_UPLOAD = 1;
	private static final int TEST_LATENCY_LOSS = 2;
	
	// *** VARIABLES *** //
	private int kindOfTest = TEST_NO_TEST;
	private float textHeight, textOffset;
	private double result;	
	
	// UI elements
	private Typeface robotoCondensedTypeface = null;
	
	// Classes
	private Paint drawPaint, textPaint;
	
	// Other stuff	
	private Context mContext;
	
 
	// *** CONSTRUCTOR *** //
    public GaugeView(Context context,AttributeSet attributeSet)
    {
        super(context,attributeSet);
        mContext = context;
        
        setUpResources();        
    }
    
    // *** CUSTOM METHODS *** //
    /**
     * Create, bind and set up the resources (fonts, paints...)
     */
    private void setUpResources()
    {
    	// Set up the fonts
    	
    	// The createFromAsset call will fail in Edit mode in Eclipse!
    	if (isInEditMode() == false) { 
          robotoCondensedTypeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/roboto_condensed_regular.ttf");
    	}
        
    	// Draw Paint
        drawPaint = new Paint();
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(convertDpToPixel(10, mContext));        
        drawPaint.setColor(mContext.getResources().getColor(R.color.white));
        drawPaint.setAntiAlias(true);
        
        // Text Paint
        textPaint = new Paint();
        textPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_size_large));
        textPaint.setTextAlign(Paint.Align.CENTER);
        if (robotoCondensedTypeface != null) {
        	textPaint.setTypeface(robotoCondensedTypeface);
        }
        textPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialInnerLabelText));
        
        // This let us centre vertically the text
        textHeight = textPaint.descent() - textPaint.ascent();
        textOffset = (textHeight / 2) - textPaint.descent();        
    }
    
    /**
     * Set the main gauge measurement value field
     * 
     * @param pResult is the value to be shown in the gauge
     */
    public void setResult(Double pResult)
    {
    	this.result = pResult;
    	invalidate();    	
    }
    
    /**
     * Set the kind of test. 0 is download. 1 is upload. 2 is latency / packet loss / jitter
     * 
     * @param pKindOfTest
     */
    public void setKindOfTest(int pKindOfTest)
    {
    	this.kindOfTest = pKindOfTest;
    	invalidate();
    } 
 
    // *** ONDRAW METHOD *** //
    @Override
    protected void onDraw(Canvas canvas)
    {
    	double angleForDots;
    	float radius, smallCircleCenterX, smallCircleCenterY;     	
    	
        super.onDraw(canvas);
 
        // Calculate the centre of the current canvas
        RectF bounds = new RectF(canvas.getClipBounds());
        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        
        // The radius will be the shorter rectangle edge to make the draw fit in the rectangle
        //Log.d("Pixels to DP", String.valueOf(convertPixelsToDp(50, mContext)));
    	//Log.d("DP to Pixels", String.valueOf(convertDpToPixel(17, mContext)));
        
    	//radius = Math.min(bounds.width() / 2, bounds.height() / 2) - 50;
    	
    	radius = Math.min(bounds.width() / 2, bounds.height() / 2) - convertDpToPixel(17, mContext);    	 	
    	
        // Loop drawing the elements
        for (int i = 0; i <= 60; i++)
        {
        	switch (this.kindOfTest)
        	{
				case TEST_DOWNLOAD:
					if (this.result <= 2)
					{
						if (i <= this.result * 20/2)
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));
						}						
					}
					else if (this.result <=5)
					{
						if (i <= 20 + (this.result - 2) * 10/3)
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));							
						}
					}
					else if (this.result <= 10)
					{
						if (i <= 30 + (this.result - 5) * 10/5)
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));							
						}						
					}
					else if (this.result <= 30)
					{
						if (i <= 40 + (this.result - 10) * 10/20)
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));							
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));							
						}						
					}
					else if (this.result <= 100)
					{
						if (i <= 50 + (this.result - 30) * 10/70)
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));							
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));							
						}						
					}
					else if (this.result > 100)
					{
						drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));						
					}
					
				break;
				
				case TEST_UPLOAD:
					if (this.result <= 2)
					{
						if (i <= this.result * 20)
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));							
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));
						}						
					}					
					else if (this.result <= 10)
					{
						if (i <= 40 + (this.result - 2) * 10/8)
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));							
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));							
						}						
					}
					else if (this.result <= 50)
					{
						if (i <= 50 + (this.result - 10) * 10/40)
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));							
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));							
						}						
					}
					else if (this.result > 50)
					{
						drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));						
					}
					break;
					
				case TEST_LATENCY_LOSS:
					if (this.result <= 500)
					{
						if (i * 10 <= this.result)
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));
						}						
					}
					else if (this.result <= 2000)
					{
						if (i <= 50 + (this.result - 500)/150 )
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));							
						}
						else
						{
							drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));							
						}
					}							
					else
					{
						drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));						
					}
					break;

			default:
				drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));
				break;
			}        	         	         	         	        	         	 
        	 
        	//Draw outer arcs        	 
        	 canvas.drawArc(new RectF(centerX - radius - convertDpToPixel(10, mContext), centerY - radius - convertDpToPixel(10, mContext), centerX + radius + convertDpToPixel(10, mContext), centerY + radius + convertDpToPixel(10, mContext)),
        			 135 + (float)(i * (360.0 / 80) - 360.0 / 160 + 360.0 / 800),
        			 	(float)((360.0 / 80) - 360.0 / 400),
        			 		false, drawPaint);
        	 
        	angleForDots = 1.75 * Math.PI - i * (Math.PI / 40);
         	 
        	
        	
    	   	// Calculate circles position        	
        	smallCircleCenterX = (float) (centerX + (radius - convertDpToPixel(20, mContext)) * Math.sin(angleForDots));
    	   	smallCircleCenterY = (float) (centerY + (radius - convertDpToPixel(20, mContext)) * Math.cos(angleForDots));
        	
    	   	// Draw the inner arcs
         	if (i % 10 == 0)
            {         		
         		drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialInnerTicks));
             		
             	//Draw small arcs
             	canvas.drawArc(new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
               		 135 + (float)(i * (360.0 / 80) - 360.0 / 160 + 360.0 / 800),
               		 	(float)((360.0 / 80) - 360.0 / 400),
               		 		false, drawPaint);
             	
             	switch (kindOfTest)
             	{
					case TEST_DOWNLOAD:
						switch (i)
		             	{
		             		case 0:
		             			canvas.drawText("0" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);             			
		             			break;
		             			
		             		case 10:
		             			canvas.drawText("1" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 20:					
		             			canvas.drawText("2" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 30:					
		             			canvas.drawText("5" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 40:					
		             			canvas.drawText("10" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 50:					
		             			canvas.drawText("30" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 60:					
		             			canvas.drawText("100" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;

		             		default:
		             			break;
						}       
						break;
						
					case TEST_UPLOAD:
						switch (i)
		             	{
		             		case 0:
		             			canvas.drawText("0" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);             			
		             			break;
		             			
		             		case 10:
		             			canvas.drawText("0.5" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 20:					
		             			canvas.drawText("1" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 30:					
		             			canvas.drawText("1.5" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 40:					
		             			canvas.drawText("2" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 50:					
		             			canvas.drawText("10" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 60:					
		             			canvas.drawText("50" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;

		             		default:
		             			break;
						}
						break;
						
					case TEST_LATENCY_LOSS:
						switch (i)
		             	{
		             		case 0:
		             			canvas.drawText("0" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);             			
		             			break;
		             			
		             		case 10:
		             			canvas.drawText("100" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 20:					
		             			canvas.drawText("200" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 30:					
		             			canvas.drawText("300" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 40:					
		             			canvas.drawText("400" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 50:					
		             			canvas.drawText("500" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 60:					
		             			canvas.drawText("2000" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;

		             		default:
		             			break;
						}
						break;
						
					case TEST_NO_TEST:
						switch (i)
		             	{
		             		case 0:
		             			canvas.drawText("" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);             			
		             			break;
		             			
		             		case 10:
		             			canvas.drawText("" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 20:					
		             			canvas.drawText("" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 30:					
		             			canvas.drawText("" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 40:					
		             			canvas.drawText("" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 50:					
		             			canvas.drawText("" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;
		             			
		             		case 60:					
		             			canvas.drawText("" ,smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
		             			break;

		             		default:
		             			break;
						}
						break;
	
					default:
						break;
				}             	      	             	             	
     		}        	
        }
    }
    
    /**
     * This method converts dp unit to equivalent pixels, depending on device density. 
     * 
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context)
    {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     * 
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context)
    {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }    
}