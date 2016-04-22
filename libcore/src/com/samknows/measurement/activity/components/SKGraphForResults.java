package com.samknows.measurement.activity.components;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer.*;
import org.achartengine.util.MathHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samknows.libcore.SKCommon;
import com.samknows.libcore.SKLogger;
import com.samknows.libcore.R;
import com.samknows.measurement.util.SKDateFormat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

public class SKGraphForResults {

  public enum DATERANGE_1w1m3m1y {
    DATERANGE_1w1m3m1y_ONE_WEEK,
    DATERANGE_1w1m3m1y_ONE_MONTH,
    DATERANGE_1w1m3m1y_THREE_MONTHS,
    DATERANGE_1w1m3m1y_ONE_YEAR,
    DATERANGE_1w1m3m1y_ONE_DAY,
    DATERANGE_1w1m3m1y_SIX_MONTHS // Only for the SK app...
  }

  private String TAG = SKGraphForResults.class.getSimpleName();
  private ViewGroup containerViewCroup;
  private String json;
  private String date;
  private String tag = "tag";

  //
  // achartengine (begin)
  //

  // http://stackoverflow.com/questions/8869854/how-to-implement-timechart-in-achartengine-with-android

  private TimeSeries mTimeSeries;


  private GraphicalView mGraphicalView = null;
  private TextView mCaptionView = null;

  XYMultipleSeriesRenderer multipleSeriesRenderer = null;

  //private int mFillColorEnd = Color.argb(0xff,  0x6d,  0xad,  0xce);

  private void createChartRendererSeriesAndView(Context context) {

    // The color values are from the iOS version...
    //int areaTopColor = Color.argb(0xff,  0xb8,  0xd3,  0xe1);
    //SKApplication appInstance = SKApplication.getAppInstance();
    //Context context = appInstance.getApplicationContext();
    Resources resources = context.getResources();

    int areaEndColor = resources.getColor(R.color.GraphColourTopAreaFill);
    int lineColor = resources.getColor(R.color.GraphColourTopLine);
    int gridLineColor = resources.getColor(R.color.GraphColourVerticalGridLine);

    // Create the multiple-series renderer... you might have more than one series
    // plotted at once, if you wanted, through this API...
    // XYMultipleSeriesRenderer multipleSeriesRenderer = new XYMultipleSeriesRenderer();
    multipleSeriesRenderer = new XYMultipleSeriesRenderer();
    multipleSeriesRenderer.setApplyBackgroundColor(true);
    multipleSeriesRenderer.setBackgroundColor(Color.WHITE); // This is the graph BACKGROUND color
    multipleSeriesRenderer.setMarginsColor(Color.WHITE); // This is the color that SURROUNDS the graph
    multipleSeriesRenderer.setAntialiasing(true);
    multipleSeriesRenderer.setChartTitle("");
    multipleSeriesRenderer.setClickEnabled(false);

    // Always show both axes
    multipleSeriesRenderer.setAxesColor(Color.LTGRAY);
    //multipleSeriesRenderer.setAxesColor(Color.TRANSPARENT);

    multipleSeriesRenderer.setLabelsColor(Color.BLACK);
    multipleSeriesRenderer.setXLabelsColor(Color.BLACK);
    multipleSeriesRenderer.setYLabelsColor(0, Color.BLACK); // 0 is the scale - a mystery value!
    multipleSeriesRenderer.setYLabelsPadding(5);
    multipleSeriesRenderer.setYLabelsAlign(Paint.Align.RIGHT);

    // Needs to be fairly, to cater for e.g. 0.008 type values!
    multipleSeriesRenderer.setLabelsTextSize(12);
    NumberFormat format = NumberFormat.getNumberInstance();
    format.setMaximumFractionDigits(3);
    multipleSeriesRenderer.setLabelFormat(format);
    //multipleSeriesRenderer.setChartValuesFormat(format);

    multipleSeriesRenderer.setLegendTextSize(15);
    multipleSeriesRenderer.setPanEnabled(false);
    //multipleSeriesRenderer.setPointSize(3f);
    multipleSeriesRenderer.setShowAxes(true);
    multipleSeriesRenderer.setShowGridX(false);
    multipleSeriesRenderer.setShowGridY(true);
    multipleSeriesRenderer.setGridColor(gridLineColor);
    multipleSeriesRenderer.setShowLegend(false);
    multipleSeriesRenderer.setShowLabels(true);
    multipleSeriesRenderer.setZoomButtonsVisible(false);
    multipleSeriesRenderer.setZoomEnabled(false);
    multipleSeriesRenderer.setZoomEnabled(false, false);
    multipleSeriesRenderer.setAxisTitleTextSize(16);
    multipleSeriesRenderer.setMargins(new int[]{20, 40, 15, 5}); // Pixels: top/left/bottom/right
    multipleSeriesRenderer.setChartTitleTextSize(20);

    // The next two lines prevent the Y axis zero from being suppressed!
    multipleSeriesRenderer.setYAxisMin(0.0);
    multipleSeriesRenderer.setYAxisMax(this.corePlotMaxValue); //  * 1.05); // Allow border at top, for the points to draw in the case of 24-hour mod!

    // http://stackoverflow.com/questions/13216619/android-chart-with-dates-on-x-axis?rq=1
    // multipleSeriesRenderer.setXRoundedLabels(true);

    //multipleSeriesRenderer.setYTitle(mYAxisTitle);
    //multipleSeriesRenderer.setSelectableBuffer(20);

    // Create the series renderer - we have just one of these.
    XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();

    seriesRenderer.setColor(lineColor);

//        if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_DAY) {
//        	// 24-hour mode - just plot hourly averages!
//        	seriesRenderer.setPointStyle(PointStyle.CIRCLE);
//        	seriesRenderer.setFillPoints(true);
//        	seriesRenderer.setPointStrokeWidth(10);
//        	
//        } else 
    {
      // Tell the line to fill below!
      FillOutsideLine fillOutsideLine = new FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BELOW);
      fillOutsideLine.setColor(areaEndColor);
      seriesRenderer.addFillOutsideLine(fillOutsideLine);
      // TODO - might be possible to use a gradient at some point?
      //seriesRenderer.setGradientEnabled(true);
      //seriesRenderer.setGradientStart(0, areaTopColor);
      //seriesRenderer.setGradientStop(0,  areaEndColor);

      // Show points, filled!
      seriesRenderer.setPointStyle(PointStyle.CIRCLE);
      //seriesRenderer.setFillPoints(true);
      seriesRenderer.setPointStrokeWidth(8);
    }

    // Add the series renderer to the multiple series renderer...
    multipleSeriesRenderer.addSeriesRenderer(seriesRenderer);

    // Create a series, and it it to the dataset...
    mTimeSeries = new TimeSeries("");

    // Create a dataset, and add the series to it...
    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    dataset.addSeries(mTimeSeries);


    // Finally: create the chart graphical view!
    if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_DAY) {
      // 24-hour format - just show time!
      mGraphicalView = ChartFactory.getTimeChartView(context, dataset, multipleSeriesRenderer, SKDateFormat.sGetGraphTimeFormat());

//        	int lLabels = multipleSeriesRenderer.getXLabels();
//        	multipleSeriesRenderer.setXLabels(0);
//        	int i;
//        	for (i = 0; i < 5; i++) {
//          	  multipleSeriesRenderer.addXTextLabel(i,  "Wow!");
//        	}
//        	

      multipleSeriesRenderer.setXLabels(0);
    } else {
      // 1 week or more - show date!
      mGraphicalView = ChartFactory.getTimeChartView(context, dataset, multipleSeriesRenderer, SKDateFormat.sGetGraphDateFormat(context));
    }

    // Now that we have the chart, we'll be able to add-in some data...
    // and add to the ViewGroup... straight after this call!
  }

  //
  // Values extracted from the JSON data!
  //
  JSONObject jsonData = null;
  ArrayList<Double> mpCorePlotDataPoints;
  ArrayList<Date> mpCorePlotDates;
  double corePlotMinValue;
  double corePlotMaxValue;
  String mYAxisTitle = "Mbps";

  // For Mock Testing...
  public ArrayList<Double> getCorePlotDataPoints() {
    return mpCorePlotDataPoints;
  }

  // For Mock Testing...
  public ArrayList<Date> getCorePlotDates() {
    return mpCorePlotDates;
  }

  private void extractCorePlotData() {

    if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_DAY) {
      // We're dealing with 24-hour data!
      extractPlotDataAveragedByHourFor24Hours();
      return;
    }

    //
    // To get here, we're dealing with a time period more than 24-hours...
    //

    extractPlotDataAveragedByDay();
  }

  private void extractPlotDataAveragedByDay() {

    //
    // Dealing with a time period more than 24-hours...
    //

    // On Android, the data in jsonResults contains all the point
    // data in the specified period.

    // The data points received are from the start day, to the end day.
    // However, values might not be present for any given day.
    // The actually calculates the *average* values
    // for any given day.
    // The way that the old graph system seemed to work, is to start from a zero value.
    // If a data point is missing, the value used is interpolated between the last received value,
    // and the next value; if no value has yet been seen, then the value remains at zero.

    ArrayList<Double> theNewArray = new ArrayList<>();
    ArrayList<Date> theDateArray = new ArrayList<>();

    //Log.d(getClass().getName(), "jsonData=" + jsonData.toString());

    try {
      String theStartDateString = jsonData.getString("start_date");
      String theEndDateString = jsonData.getString("end_date");
      mYAxisTitle = jsonData.getString("y_label");

      Date theStartDate = new Date((Long.valueOf(theStartDateString)));
      //Date theEndDate = new Date((Long.valueOf(theEndDateString)));

      long daysBetween = (Long.valueOf(theEndDateString) - Long.valueOf(theStartDateString)) / (1000L * 60L * 60L * 24L);
      daysBetween++;

      if (daysBetween <= 0) {
        SKLogger.sAssert(getClass(), false);
        daysBetween = 1;
      }

      //Log.d(getClass().getName(), "daysBetween=" + daysBetween);

      theNewArray.ensureCapacity((int) daysBetween);
      theDateArray.ensureCapacity((int) daysBetween);

      ArrayList<Integer> theCountArray = new ArrayList<>();

      for (int i = 0; i < (int) daysBetween; i++) {
        theNewArray.add(null);
        theCountArray.add(1);

        Calendar cTheCalendar = Calendar.getInstance();
        cTheCalendar.setTime(theStartDate);
        cTheCalendar.set(Calendar.HOUR_OF_DAY, 0);
        cTheCalendar.set(Calendar.MINUTE, 0);
        cTheCalendar.set(Calendar.SECOND, 0);
        cTheCalendar.set(Calendar.MILLISECOND, 0);
        cTheCalendar.add(Calendar.DAY_OF_MONTH, i);

        theDateArray.add(i, cTheCalendar.getTime());
      }

      // We MUST INCLUDE the actual end date.
      // For example, for one week: start date might be 01 Aug, end date might be 08 Aug ...
      // 01 02 03 04 05 06 07 08
      //   ^  ^  ^  ^  ^  ^  ^   = 7 days!!!

      JSONArray theResults = jsonData.getJSONArray("results");

      Calendar cForStartDate = Calendar.getInstance();
      cForStartDate.setTime(theStartDate);
      // Round-off the time data...
      // http://stackoverflow.com/questions/1908387/java-date-cut-off-time-information
      cForStartDate.set(Calendar.HOUR_OF_DAY, 0);
      cForStartDate.set(Calendar.MINUTE, 0);
      cForStartDate.set(Calendar.SECOND, 0);
      cForStartDate.set(Calendar.MILLISECOND, 0);

      int lItems = theResults.length();
      int lIndex = 0;
      for (lIndex = 0; lIndex < lItems; lIndex++) {
        JSONObject item = theResults.getJSONObject(lIndex);
        String value = item.getString("value");

        //value = "0.00499"; // TODO - for debug/testing only!!

        String datetime = item.getString("datetime");

        // Which day does this correspond to?!
        Calendar c = Calendar.getInstance();
        long milliseconds = Long.valueOf(datetime);
        Date theDate = new Date(milliseconds);
        c.setTime(theDate);
        // Round-off the time data...
        // http://stackoverflow.com/questions/1908387/java-date-cut-off-time-information
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        //Date theCDate = c.getTime();

        int dayIndex = 0;

//				boolean bCheckDateDayMatch = false;
//				int checkIndex = 0;
//				for (checkIndex = 0; checkIndex < daysBetween; checkIndex++) {
//					if (theCDate.compareTo(theDateArray.get(checkIndex)) == 0) {
//						bCheckDateDayMatch = true;
//						dayIndex = checkIndex;
//						break;
//					}
//				}
//				
//				if (bCheckDateDayMatch) {
//         	     	  Log.d(getClass().getName(), "Found date day match ("+dayIndex+")");
//				} else {
//       	     	    Log.e(getClass().getName(), "ERROR: did NOT find date/day match");
        long diff = c.getTimeInMillis() - cForStartDate.getTimeInMillis();
        dayIndex = (int) (diff / (24L * 60L * 60L * 1000L));

        // Debug / testing - could enable one of the following lines to verify that array bounds are handled appropriately.
        // dayIndex = -1;
        // dayIndex = 10011;

        if (dayIndex < 0) {
          SKLogger.sAssert(getClass(), false);
          dayIndex = 0;
        } else if (dayIndex >= theNewArray.size()) {
          SKLogger.sAssert(getClass(), false);
          dayIndex = theNewArray.size() - 1;
        }
//				}

        //SimpleDateFormat format = new SimpleDateFormat("dd/MM");
        //Log.d(getClass().getName(), "Read date for dayIndex=["+checkIndex+"], =" + format.format(c.getTime()) + ", value=" + value);

        // Get the double value safely, irrespective of Locale
        double doubleValue = SKCommon.sGetDecimalStringAnyLocaleAsDouble(value);

        if (theNewArray.get(dayIndex) == null) {
          // Nothing there yet!
          theNewArray.set(dayIndex, doubleValue);
          theCountArray.set(dayIndex, 1);
        } else {
          // Something there already found for this day - add up, and we convert to an average in a moment...
          double currentTotal = theNewArray.get(dayIndex);
          theNewArray.set(dayIndex, currentTotal + doubleValue);
          theCountArray.set(dayIndex, 1 + theCountArray.get(dayIndex));
        }
      }

      // Now, fix the values back to the averages...
      for (lIndex = 0; lIndex < daysBetween; lIndex++) {
        if (theCountArray.get(lIndex) > 1) {
          if (theNewArray.get(lIndex) != null) {
            theNewArray.set(lIndex, (theNewArray.get(lIndex) / (double) theCountArray.get(lIndex)));
          }
        }

//				// achartengine displays weird values if they are too small...
//				if (theNewArray.get(lIndex) != null) {
//					if (theNewArray.get(lIndex) < 0.01) {
//						theNewArray.set(lIndex,  0.01);
//					}
//				}
      }

//			final boolean bDoBackAndForwardFill = false;
//			
//			// To reach here, we have an array of items...
//			if (bDoBackAndForwardFill == true) {
//				// We must now interpolate!
//				int theLastNonNilNumberAtIndex = -1;
//				lItems = theNewArray.size();
//
//				// To reach here, we have an array of items...
//				// We must now interpolate!
//				for (lIndex = 0; lIndex < daysBetween; lIndex++) {
//
//					Double theObject = theNewArray.get(lIndex);
//					if (theObject == null) {
//						// This is our PLACEHOLDER!
//						if (theLastNonNilNumberAtIndex == -1) {
//							// Nothing we can do here!
//							continue;
//						}
//
//						Double theNumberAtLastNonNilIndex = theNewArray.get(theLastNonNilNumberAtIndex);
//
//						// Interpolate. Look FORWARD to the next number!
//						// If none found, then simply copy forward.
//						boolean bLookForwardFound = false;
//
//						int lLookForwardIndex;
//						for (lLookForwardIndex = lIndex + 1; ; lLookForwardIndex++) {
//							if (lLookForwardIndex >= lItems)
//							{
//								break;
//							}
//
//							Double theLookForwardObject = theNewArray.get(lLookForwardIndex);
//							if (theLookForwardObject != null) {
//								Double theLookForwardNumber = theLookForwardObject;
//								bLookForwardFound = true;
//
//								// Calculate the value to use!
//								double theDecimalLookForward = theLookForwardNumber;
//								double theDecimalNumberAtLastNonNilIndex = theNumberAtLastNonNilIndex;
//								double theInterpolatedValue = theDecimalNumberAtLastNonNilIndex + (theDecimalLookForward - theDecimalNumberAtLastNonNilIndex) * ((double)(lIndex - theLastNonNilNumberAtIndex)) / ((double)(lLookForwardIndex - theLastNonNilNumberAtIndex));
//								theNewArray.set(lIndex, theInterpolatedValue);
//								break;
//							}
//						}
//
//						if (bLookForwardFound == false) {
//							theNewArray.set(lIndex, theNewArray.get(theLastNonNilNumberAtIndex));
//						}
//
//					} else {
//						theLastNonNilNumberAtIndex = lIndex;
//					}
//
//				}
//			}

      // Finally, find the minimum and maximum values, for scaling the plot!
      corePlotMinValue = 0.0;
      //  bool bMinFound = false;
      corePlotMaxValue = 0.0;
      boolean bMaxFound = false;


      int items = theNewArray.size();
      for (lIndex = 0; lIndex < items; lIndex++) {
        Double theObject = theNewArray.get(lIndex);
        if (theObject != null) {
          Double theNumber = theObject;

          //theNumber = 0.00499; // TODO - this is for debug ONLY!
          // If the value is 0.00999 or less, then treat as 0.0!
          if (theNumber < 0.01) {
            if (theNumber > 0) {
              theNumber = 0.00;
              theNewArray.set(lIndex, theNumber);
            }
          }

          double theDouble = theNumber;
          if (bMaxFound == false) {
            corePlotMaxValue = theDouble;
            bMaxFound = true;
          }

          if (theDouble > corePlotMaxValue) {
            corePlotMaxValue = theDouble;
            bMaxFound = true;
          }
        }
      }

      //Log.d(this.getClass().getName(), "All values extracted!");

    } catch (JSONException e) {
      SKLogger.sAssert(getClass(), false);
    } catch (NullPointerException e) {
      SKLogger.sAssert(getClass(), false);
    }

    // Allow enough range for the plots note to be chopped-off in the Y range!
    corePlotMaxValue *= 1.10;

    mpCorePlotDataPoints = theNewArray;
    mpCorePlotDates = theDateArray;
  }

  private void extractPlotDataAveragedByHourFor24Hours() {
    // In the 24-hour case, we must actually average the values BY HOUR!
    // On Android, the data in jsonResults contains all the point
    // data in the specified period.

    // First we extract, and sort them in ascending order of date...

    ArrayList<Double> theNewArray = new ArrayList<>();
    ArrayList<Date> theDateArray = new ArrayList<>();

    Log.w(getClass().getName(), "TODO - 24-hour case!");
    //SKLogger.sAssert(this.getClass(),  "TODO - 24-hour case!", false);


    ArrayList<Pair<String, String>> arrayOfDateValues = new ArrayList<>();

    JSONArray theResults;
    try {
      theResults = jsonData.getJSONArray("results");
      mYAxisTitle = jsonData.getString("y_label");

      int lItems = theResults.length();
      int lIndex = 0;
      for (lIndex = 0; lIndex < lItems; lIndex++) {
        JSONObject item = theResults.getJSONObject(lIndex);
        String value = item.getString("value");

        //value = "0.00499"; // TODO - for debug/testing only!!
        String datetime = item.getString("datetime");

        Pair<String, String> dateValuePair = new Pair<>(datetime, value);
        arrayOfDateValues.add(dateValuePair);
      }

    } catch (JSONException e) {
      e.printStackTrace();
      SKLogger.sAssert(this.getClass(), false);
      return;
    }

    // To get here, we have all the 24-hour date items, stored as pairs,
    // but not necessarily sorted. Now sort them in ascending order of date.
    Collections.sort(arrayOfDateValues, new Comparator<Pair<String, String>>() {

      @Override
      public int compare(Pair<String, String> lhs, Pair<String, String> rhs) {
        Date theDateLsh = new Date((Long.valueOf(lhs.first)));
        Date theDateRsh = new Date((Long.valueOf(rhs.first)));
        return theDateLsh.compareTo(theDateRsh);
      }
    });

    // And just store this in a new value, as a reminder the values
    // are now sorted.
    ArrayList<Pair<String, String>> sortedArray24 = arrayOfDateValues;

    // Now, group the values by HOUR!
    ArrayList<Double> valuesByHour = new ArrayList<>();
    ArrayList<Integer> itemsByHour = new ArrayList<>();
    ArrayList<Date> datesByHour = new ArrayList<>();

    final double oneDay = 24.0 * 60.0 * 60.0;
    final double timeIntervalForOneHour = (oneDay / 24.0);

    // Our dates are calculated based on "NOW"

    Date dateNow = new Date();
    Calendar cTheCalendar = Calendar.getInstance();
    cTheCalendar.setTime(dateNow);
    cTheCalendar.add(Calendar.HOUR, -24);
    Date dateYesterday = cTheCalendar.getTime();

    {
      int hourIndex;
      for (hourIndex = 0; hourIndex < 24; hourIndex++) {
        datesByHour.add(cTheCalendar.getTime());
        itemsByHour.add(0);
        valuesByHour.add(null);

        cTheCalendar.add(Calendar.HOUR, +1);
      }
    }

    final boolean bDoBackAndForwardFill = false;

    for (Pair<String, String> theDay : sortedArray24) {
      Double theStartTimeInterval = SKCommon.sGetDecimalStringAnyLocaleAsDouble(theDay.first);
      Date theDate = new Date((Long.valueOf(theDay.first)));

      String theValue = theDay.second;
      // You can enable the following for localization testing...
      //theValue = theValue.replaceAll("\\.",",");
      Double theResult = SKCommon.sGetDecimalStringAnyLocaleAsDouble(theValue);

      // Milliseconds
      long timeIntervalSinceStartMs = theDate.getTime() - dateYesterday.getTime();
      // Seconds
      long timeIntervalSinceStart = timeIntervalSinceStartMs / 1000;

      // Are we in a new hour?
      // If so, save the last value before continuing!
      int hourIndex = (int) (timeIntervalSinceStart / timeIntervalForOneHour);
      SKLogger.sAssert(getClass(), hourIndex >= 0);
      SKLogger.sAssert(getClass(), hourIndex <= 23);
      if (hourIndex < 0) {
        hourIndex = 0;
      }
      if (hourIndex > 23) {
        hourIndex = 23;
      }

      itemsByHour.set(hourIndex, itemsByHour.get(hourIndex) + 1);

      Double valueAtHour = valuesByHour.get(hourIndex);
      if (valueAtHour == null) {
        valuesByHour.set(hourIndex, theResult);
      } else {
        valuesByHour.set(hourIndex, valueAtHour + theResult);
      }
    }

    // Now run through, and calculate the averages.
    {
      int hourIndex;
      for (hourIndex = 0; hourIndex < 24; hourIndex++) {
        if (itemsByHour.get(hourIndex) > 0) {
          Double theAverage = valuesByHour.get(hourIndex) / (double) (itemsByHour.get(hourIndex));
          valuesByHour.set(hourIndex, theAverage);
        }
      }
    }

    // Finally, find the minimum and maximum values, for scaling the plot!
    corePlotMinValue = 0.0;
    //  bool bMinFound = false;
    corePlotMaxValue = 0.0;
    boolean bMaxFound = false;

    {
      int hourIndex;
      for (hourIndex = 0; hourIndex < 24; hourIndex++) {
        Date theDate = datesByHour.get(hourIndex);
        theDateArray.add(theDate);

        Double theResult = valuesByHour.get(hourIndex);

        if (theResult == null) {
          theNewArray.add(theResult);
          continue;
        }

        //theResult = 0.00499; // TODO - this is for debug ONLY!
        // If the value is 0.00999 or less, then treat as 0.0!
        if (theResult < 0.01) {
          if (theResult > 0) {
            theResult = 0.00;
          }
        }

        if (bMaxFound == false) {
          corePlotMaxValue = theResult;
          bMaxFound = true;
        }

        if (theResult > corePlotMaxValue) {
          corePlotMaxValue = theResult;
          bMaxFound = true;
        }

        theNewArray.add(theResult);
      }
    }

    // Allow enough range for the plots note to be chopped-off in the Y range!
    corePlotMaxValue *= 1.10;

    mpCorePlotDataPoints = theNewArray;
    mpCorePlotDates = theDateArray;

    //
    // We now have all the 24-hour values!
    //
  }


  private void addCorePlotDataToGraphicalView() {

    int lDates = mpCorePlotDates.size();

    SKLogger.sAssert(getClass(), mpCorePlotDataPoints.size() == mpCorePlotDates.size());

    if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_DAY) {
      // 24-hour mode - just plot hourly averages!
      // However, with AChart engine there is no way to show labels
      // alternately, like we can on iOS.
      // multipleSeriesRenderer.setXLabels(6);
      multipleSeriesRenderer.setXLabels(12);
    } else if (mDateRange == DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_WEEK) {
      // If week period, force more label items...
      multipleSeriesRenderer.setXLabels(lDates);
    } else if (multipleSeriesRenderer.getXLabels() < 5) {
      multipleSeriesRenderer.setXLabels(5);
    }

    // With the chart engine used on iOS, if we don't provide items, the x-axis (time) labels are generated.
    // However, on Android, the x-axis labels are generated only where there are data items provided!
    // The best we can do on Android, is:
    // - start plotting well off-screen.
    // - Once we have a value, we can interpolate.
    // - We cannot draw points!

    int indexOfFirstFoundValue = -1;
    int lIndex = 0;
    for (lIndex = 0; lIndex < lDates; lIndex++) {
      Double theValue = mpCorePlotDataPoints.get(lIndex);
      if (theValue != null) {
        indexOfFirstFoundValue = lIndex;
        break;
      }
    }

    int indexOfLastFoundValue = -1;
    if (indexOfFirstFoundValue != -1) {
      // At least one item found... look backwards for last value.
      for (lIndex = lDates - 1; lIndex >= 0; lIndex--) {
        Double theValue = mpCorePlotDataPoints.get(lIndex);
        if (theValue != null) {
          indexOfLastFoundValue = lIndex;
          break;
        }
      }
    }

    for (lIndex = 0; lIndex < lDates; lIndex++) {
      Double theValue = mpCorePlotDataPoints.get(lIndex);
      if (theValue != null) {
        double theDouble = theValue;
        //mCurrentSeries.add(lIndex, theDouble);
        mTimeSeries.add(mpCorePlotDates.get(lIndex), theDouble);

      } else {
        // Nothing there - use a magic value that isn't plotted!
        // http://stackoverflow.com/questions/13025981/achartengine-renders-null-values

        // We can (and must) do this ONLY if
        // - there are not yet any values to plot from this point backwards
        // - OR if there are no values from here on to plot.
        // Otherwise, don't provide a data point - that allows the system to fill between plot points.
        // That allows the graph to start and end at "zero" with no points plotted,
        // and allows the graph to fill as required between the plotted points.
        if (lIndex <= indexOfFirstFoundValue) {
          // No values yet plotted.
          mTimeSeries.add(mpCorePlotDates.get(lIndex), MathHelper.NULL_VALUE);
        } else {
          // Are there any values to come in the future?
          if (lIndex >= indexOfLastFoundValue) {
            mTimeSeries.add(mpCorePlotDates.get(lIndex), MathHelper.NULL_VALUE);
          }
        }
      }
    }
  }

  private void attachAchartEngine(Context context, ViewGroup inContainerViewGroup) {

    // If the chart already exists, remove it!
    if (mGraphicalView != null) {
      inContainerViewGroup.removeView(mGraphicalView);
    }

    // Extract the query data from the jsonData!
    extractCorePlotData();

    // We can now create and add the chart!
    createChartRendererSeriesAndView(context);

    // And now put the data in the chart!
    addCorePlotDataToGraphicalView();

    //
    // Finally - add the new chart to our view group!
    //
    inContainerViewGroup.addView(mGraphicalView);
  }

  //
  // achartengine (end)
  //

  //
  // Update the graph, and set the caption text...
  //
  //Handler handler = new Handler();

  private void updateGraphAndCaption(Context context, ViewGroup inContainerViewGroup) {

    // Update the graph...
    attachAchartEngine(context, inContainerViewGroup);

    // And update the caption text... ensuring that it appears in bold.
    // For some reason, the Html.fromHtml... approach is required for this to work
    // reliably!
    // http://stackoverflow.com/questions/1529068/is-it-possible-to-have-multiple-styles-inside-a-textview
    // mCaptionView.setText(mYAxisTitle);
    mCaptionView.setText(Html.fromHtml("<b>" + mYAxisTitle + "</b>"));

    // TODO: on the simulator, the graph doesn't always display the background properly the first time!
//        handler.postDelayed(new Runnable() {
//
//			@Override
//			public void run() {
//				containerViewCroup.invalidate();
//				mGraphicalView.invalidate();
//
//			}
//        }, 100);
  }

  Context mContext = null;

  public SKGraphForResults(Context context, ViewGroup inViewGroup, TextView inCaptionView, String inTag) {
    mContext = context;

    containerViewCroup = inViewGroup;

    if (inViewGroup.getClass() == WebView.class) {
      // If we're embedded in a web view - ensure the scrollbars are disabled,
      // as they flash-up momentarily and are unsightly!
      inViewGroup.setVerticalScrollBarEnabled(false);
      inViewGroup.setHorizontalScrollBarEnabled(false);

      // http://stackoverflow.com/questions/2527899/disable-scrolling-in-webview
      // breakingart.com: This prevents the webview being scrolled by dragging!
      inViewGroup.setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
          return (event.getAction() == MotionEvent.ACTION_MOVE);
        }
      });
    }

    // Make the Y Axis Label appear manually - the one build into the library always
    // appears vertically, so we use our own, appearing horizontally in the top-left corner
    mCaptionView = inCaptionView;

    this.tag = inTag;
  }


  /**
   * Set the data to display
   *
   * @param data JSONObject
   */
  // The JSONObject is like this:
  //		"start_date":"1381499507861",
  //		"end_date":"1382104307862",
  //		"results":[{"value":"0.979312","datetime":"1382099660000"},{"value":"3.570656","datetime":"1382099068000"}],
  //		"type":0,
  //		"y_label":"Mbps"
  // For a one-day view, by default there will be just one value, which will be an average;
  // so, depending on how the SQL works we'll probably want to pass-in list of point items that applied for the last 24 hours.
  // Then, like on iOS, the graph processor will identify this, and specially extract those 24-hour point items for display.
  // Alternatively, the graph *could* query that data directly?
  public void updateGraphWithTheseResults(JSONObject data, DATERANGE_1w1m3m1y dateFilter) {

    if (data == null) {
      SKLogger.sAssert(getClass(), false);
      return;
    }

    Log.v(TAG, "setData()");
    json = data.toString();
    jsonData = data;

    mDateRange = dateFilter;

    // Attach a new, updated graph!
    updateGraphAndCaption(mContext, containerViewCroup);
  }

  public void updateGraphWithTheseResults(JSONObject data, DATERANGE_1w1m3m1y dateFilter,
                                          int backgroundColor, int fillColor) {

    // Set the fill colour BEFORE calling update graph...
    //mFillColorEnd = fillColor;

    updateGraphWithTheseResults(data, dateFilter);

    // Now that we have the graph, we can update the colours associated with it!
    multipleSeriesRenderer.setBackgroundColor(backgroundColor); // This is the graph BACKGROUND color
    multipleSeriesRenderer.setMarginsColor(backgroundColor); // This is the color that SURROUNDS the graph

    // TODO - use the fill colour etc.!
  }


  DATERANGE_1w1m3m1y mDateRange = DATERANGE_1w1m3m1y.DATERANGE_1w1m3m1y_ONE_WEEK;

  /**
   * Return the data in a json string
   *
   * @return
   */
  public String getData() {
    Log.v(TAG, "getData()");
    //Log.v(TAG, json);
    return json;
  }

  /**
   * Return the date as a string
   */
  public String getStartDate() {
    Log.v(TAG, "getStartDate()");
    return date;
  }
}
