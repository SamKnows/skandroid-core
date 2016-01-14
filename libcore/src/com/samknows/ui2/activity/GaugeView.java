package com.samknows.ui2.activity;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.R;
import com.samknows.libcore.SKTypeface;
import com.samknows.measurement.SKApplication;

/**
 * This class is responsible for painting the gauge in the home screen.
 * <p/>
 * All rights reserved SamKnows
 *
 * @author pablo@samknows.com
 */


class GaugeView extends View {
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
  public GaugeView(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    mContext = context;

    setUpResources();
  }

  // *** CUSTOM METHODS *** //

  /**
   * Create, bind and set up the resources (fonts, paints...)
   */
  private void setUpResources() {
    // Set up the fonts
    View view = this; // findViewById(android.R.id.content);
    SKTypeface.sChangeChildrenToDefaultFontTypeface(view);

    // The createFromAsset call will fail in Edit mode in Eclipse!
    if (isInEditMode() == false) {
      robotoCondensedTypeface = SKTypeface.sGetTypefaceWithPathInAssets("fonts/roboto_condensed_regular.ttf");
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
  public void setAngleByValue(Double value) {
    this.result = value;
    invalidate();
  }

  /**
   * Set the kind of test. 0 is download. 1 is upload. 2 is latency / packet loss / jitter
   *
   * @param pKindOfTest
   */
  public void setKindOfTest(int pKindOfTest) {
    this.kindOfTest = pKindOfTest;
    invalidate();
  }

  private String getLabelForValue(double doubleValue) {
    double fractionalPart = doubleValue - (double) ((int) doubleValue);
    if (fractionalPart > 0.4) {
      // Something like 0.5, 1.5 etc.
      return String.format("%.1f", doubleValue);
    } else {
      return String.valueOf((int) doubleValue);
    }
  }

  // *** ONDRAW METHOD *** //
  @Override
  protected void onDraw(Canvas canvas) {
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

    //double arrSegmentMaxValues_Download[] = {1.0, 2.0, 5.0, 10.0, 30.0, 100.0};
    //double arrSegmentMaxValues_Upload[] = {0.5, 1.0, 1.5, 2.0, 10.0, 50.0};
    double arrSegmentMaxValues_Download[] = SKApplication.sGetDownloadSixSegmentMaxValues();
    double arrSegmentMaxValues_Upload[] = SKApplication.sGetUploadSixSegmentMaxValues();
    double arrSegmentMaxValues[] = arrSegmentMaxValues_Download;
    switch (this.kindOfTest) {
      case TEST_DOWNLOAD:
        break;
      case TEST_UPLOAD:
        arrSegmentMaxValues = arrSegmentMaxValues_Upload;
        break;
      case TEST_LATENCY_LOSS: {
        double arrSegmentMaxValues_LatencyLoss[] = {100.0, 200.0, 300.0, 400.0, 500.0, 600.0};
        arrSegmentMaxValues = arrSegmentMaxValues_LatencyLoss;
      }
      break;
      default:
        break;
    }

    // Loop drawing the elements
    // We start in the red zone.
    // Once the ticks represent a value > than the measured value, we go to the grey zone.
    boolean bGoneToGreyZone = false;

    for (int i = 0; i <= 60; i++) {
      switch (this.kindOfTest) {
        case TEST_DOWNLOAD:
        case TEST_UPLOAD:
        case TEST_LATENCY_LOSS: {
          // Which segment are we rendering?
          int segmentIndex = i / 10;
          if (segmentIndex >= 6) {
            segmentIndex = 5;
          }

          // What colour is this tick?
          if (bGoneToGreyZone == true) {
            // Already in the grey zone.
          } else {
            // Red - but potentially, moving to grey...

            double segmentStartValue = 0.0;
            if (segmentIndex > 0) {
              segmentStartValue = arrSegmentMaxValues[segmentIndex - 1];
            }

            if (segmentStartValue >= this.result) {
              // This segment starts with a value greater than our measured value. Draw as grey!
              bGoneToGreyZone = true;
            } else {
              double segmentMaxValue = arrSegmentMaxValues[segmentIndex];

              if (segmentMaxValue <= this.result) {
                // Stay red...
              } else {

                double fractionalPositionInIndex0To1 = ((this.result - segmentStartValue) / (segmentMaxValue - segmentStartValue));
                if (fractionalPositionInIndex0To1 < 0) {
                  SKLogger.sAssert(getClass(), false);
                  fractionalPositionInIndex0To1 = 0;
                } else if (fractionalPositionInIndex0To1 > 1.0F) {
                  SKLogger.sAssert(getClass(), false);
                  fractionalPositionInIndex0To1 = 1.0F;
                }
                int fractionalPositionInIndex0To10 = (int) (fractionalPositionInIndex0To1 * 10.0);
                if ((i % 10) > fractionalPositionInIndex0To10) {
                  // Grey!
                  bGoneToGreyZone = true;
                }
              }
            }
          }

          if (bGoneToGreyZone == true) {
            drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));
          } else {
            drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcRedZone));
          }
        }

        break;

        default:
          // For other tests, we always draw simple grey - we don't have a red zone.
          drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialArcGreyZone));
          break;
      }

      //Draw outer arcs
      canvas.drawArc(new RectF(centerX - radius - convertDpToPixel(10, mContext), centerY - radius - convertDpToPixel(10, mContext), centerX + radius + convertDpToPixel(10, mContext), centerY + radius + convertDpToPixel(10, mContext)),
          135 + (float) (i * (360.0 / 80) - 360.0 / 160 + 360.0 / 800),
          (float) ((360.0 / 80) - 360.0 / 400),
          false, drawPaint);

      // Draw the inner arcs which mark the six segments... and any associated labels...
      if (i % 10 == 0) {
        SKLogger.sAssert(getClass(), i >= 0 && i <= 60);
        SKLogger.sAssert(getClass(), ((i % 10) == 0));

        //
        // Inner arcs...
        //
        angleForDots = 1.75 * Math.PI - i * (Math.PI / 40);

        // Calculate circles position
        smallCircleCenterX = (float) (centerX + (radius - convertDpToPixel(20, mContext)) * Math.sin(angleForDots));
        smallCircleCenterY = (float) (centerY + (radius - convertDpToPixel(20, mContext)) * Math.cos(angleForDots));

        drawPaint.setColor(mContext.getResources().getColor(R.color.MainColourDialInnerTicks));

        //Draw small arcs
        canvas.drawArc(new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
            135 + (float) (i * (360.0 / 80) - 360.0 / 160 + 360.0 / 800),
            (float) ((360.0 / 80) - 360.0 / 400),
            false, drawPaint);

        //
        // Draw labels associated with the inner arcs - if required!
        //
        switch (kindOfTest) {
          case TEST_DOWNLOAD:
          case TEST_UPLOAD:
          case TEST_LATENCY_LOSS:
            switch (i) {
              case 0:
                canvas.drawText("0", smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
                break;

              default:
                canvas.drawText(getLabelForValue(arrSegmentMaxValues[(i - 10) / 10]), smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
                break;
            }
            break;

          case TEST_NO_TEST:
            canvas.drawText("", smallCircleCenterX, smallCircleCenterY + textOffset, textPaint);
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
   * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
   * @param context Context to get resources and device specific display metrics
   * @return A float value to represent px equivalent to dp depending on device density
   */
  public static float convertDpToPixel(float dp, Context context) {
    Resources resources = context.getResources();
    DisplayMetrics metrics = resources.getDisplayMetrics();
    float px = dp * (metrics.densityDpi / 160f);
    return px;
  }

  /**
   * This method converts device specific pixels to density independent pixels.
   *
   * @param px      A value in px (pixels) unit. Which we need to convert into db
   * @param context Context to get resources and device specific display metrics
   * @return A float value to represent dp equivalent to px value
   */
  public static float convertPixelsToDp(float px, Context context) {
    Resources resources = context.getResources();
    DisplayMetrics metrics = resources.getDisplayMetrics();
    float dp = px / (metrics.densityDpi / 160f);
    return dp;
  }
}