package com.samknows.measurement.activity.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.Button;

public class ButtonWithRightArrow extends Button {

    private final int   arrowColor = 0xFF888888;
    private Paint arrowPaint;
    private Path  arrowPath;

    public ButtonWithRightArrow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        stuff();
    }

    public ButtonWithRightArrow(Context context, AttributeSet attrs) {
        super(context, attrs);
        stuff();
    }

    public ButtonWithRightArrow(Context context) {
        super(context);
        stuff();
    }

    private void stuff() {

        arrowPaint = new Paint();
        arrowPaint.setAntiAlias(true);
        arrowPaint.setColor(arrowColor);
        arrowPaint.setStrokeWidth(2);
        arrowPaint.setStrokeCap(Cap.ROUND);
        arrowPaint.setStyle(Style.STROKE);

        arrowPath = new Path();

        this.setWillNotDraw(false);
    }
  
    // http://stackoverflow.com/questions/4074937/android-how-to-get-a-custom-views-height-and-width
    int viewWidth = 100;
    int viewHeight = 100;
   
    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld){
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        viewWidth = xNew;
        viewHeight = yNew;
   }

    @Override
    public void onDraw(Canvas c) {
    	
    	super.onDraw(c);

        double offsetXY = viewHeight/4;
        
        int centerRightX = viewWidth - 15;
        int centerRightY = viewHeight/2;
        int topLeftX = (int) (centerRightX - offsetXY);
        int topLeftY = (int) (centerRightY - offsetXY);
        int bottomLeftX = (int) (centerRightX - offsetXY);
        int bottomLeftY = (int) (centerRightY + offsetXY);
        
        // 
        // Draw arrow on RHS!
        //
        arrowPaint.setColor(arrowColor);
        arrowPath.reset();
        arrowPath.moveTo(topLeftX, topLeftY);
        arrowPath.lineTo(centerRightX, centerRightY);
        arrowPath.lineTo(bottomLeftX, bottomLeftY);
        //arrowPath.close();
        c.drawPath(arrowPath, arrowPaint);
       
//        // Draw a green bounding box to test the sizing logic!
//        arrowPaint.setColor(0xff00ff00);
//        arrowPath.reset();
//        arrowPath.moveTo(4, 4);
//        arrowPath.lineTo(viewWidth-4, 4);
//        arrowPath.lineTo(viewWidth-4, viewHeight-4);
//        arrowPath.lineTo(4, viewHeight-4);
//        arrowPath.lineTo(4, 4);
//        //arrowPath.close();
//        c.drawPath(arrowPath, arrowPaint);
    }
}
