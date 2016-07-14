package com.samknows.ui2.activity;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.samknows.libcore.SKPorting;
import com.samknows.libcore.R;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.SKApplication.eNetworkTypeResults;
import com.samknows.libcore.SKTypeface;

/**
 * This class represents the custom adapter to the archive results list view.
 * <p/>
 * All rights reserved SamKnows
 *
 * @author pablo@samknows.com
 */

public class AdapterArchivedResultsListView extends ArrayAdapter<TestResult> {
  // *** VARIABLES *** //
  private ArrayList<TestResult> archivedResultsList;
  private boolean packetLossTestWasPerformed = false;

  // Other stuff
  private Context context;

  // Custom adapter constructor
  public AdapterArchivedResultsListView(Context pContext, ArrayList<TestResult> pArchivedResultsList) {
    super(pContext, R.layout.results_panel_shared_layout, pArchivedResultsList);

    context = pContext;
    archivedResultsList = pArchivedResultsList;
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

  // This is how each row view is calculated
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    // Get the row layout
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View rowView = inflater.inflate(R.layout.results_panel_shared_layout, parent, false);

    // Set up UI elements
    TextView testDateDay = (TextView) rowView.findViewById(R.id.archiveResultsListItemDateDay);
    TextView testDateTime = (TextView) rowView.findViewById(R.id.archiveResultsListItemDateTime);
    ImageView testNetworkType = (ImageView) rowView.findViewById(R.id.archiveResultsListItemNetworkType);

    TextView tv_Result_Download = (TextView) rowView.findViewById(R.id.archiveResultsListItemDownload);
    TextView testDownloadUnits = (TextView) rowView.findViewById(R.id.mbps_label_1);
    TextView tv_Result_Upload = (TextView) rowView.findViewById(R.id.archiveResultsListItemUpload);
    TextView testUploadUnits = (TextView) rowView.findViewById(R.id.mbps_label_2);
    TextView testLatency = (TextView) rowView.findViewById(R.id.archiveResultsListItemLatency);
    TextView testPacketLoss = (TextView) rowView.findViewById(R.id.archiveResultsListItemPacketLoss);
    TextView testJitter = (TextView) rowView.findViewById(R.id.archiveResultsListItemJitter);

    if (SKApplication.getAppInstance().hideJitter()) {
      rowView.findViewById(R.id.jitter_panel).setVisibility(View.GONE);
    }
    if (SKApplication.getAppInstance().hideLoss()) {
      rowView.findViewById(R.id.loss_panel).setVisibility(View.GONE);
    }

    View view = rowView; // findViewById(android.R.id.content);
    SKTypeface.sChangeChildrenToDefaultFontTypeface(view);

    // Set up fonts
    Typeface robotoThinTypeFace = SKTypeface.sGetTypefaceWithPathInAssets("fonts/roboto_thin.ttf");
    Typeface robotoLightTypeFace = SKTypeface.sGetTypefaceWithPathInAssets("fonts/roboto_light.ttf");
    Typeface robotoCondensedTypeface = SKTypeface.sGetTypefaceWithPathInAssets("fonts/roboto_condensed_regular.ttf");

    // Assign the fonts
    //((TextView)rowView.findViewById(R.id.mbps_label_1)).setTypeface(robotoThinTypeFace);
    //((TextView)rowView.findViewById(R.id.mbps_label_2)).setTypeface(robotoThinTypeFace);
    ((TextView) rowView.findViewById(R.id.mbps_label_1)).setTypeface(robotoLightTypeFace);
    ((TextView) rowView.findViewById(R.id.mbps_label_2)).setTypeface(robotoLightTypeFace);
    ((TextView) rowView.findViewById(R.id.downloadLabel)).setTypeface(robotoLightTypeFace);
    ((TextView) rowView.findViewById(R.id.uploadLabel)).setTypeface(robotoLightTypeFace);
    ((TextView) rowView.findViewById(R.id.latency_label)).setTypeface(robotoLightTypeFace);
    ((TextView) rowView.findViewById(R.id.loss_label)).setTypeface(robotoLightTypeFace);
    ((TextView) rowView.findViewById(R.id.jitter_label)).setTypeface(robotoLightTypeFace);
//
    testDateDay.setTypeface(robotoLightTypeFace);
    testDateTime.setTypeface(robotoLightTypeFace);
    // If we override the font, the text doesn't display with the correct alignment!
    tv_Result_Download.setTypeface(robotoCondensedTypeface);
    tv_Result_Upload.setTypeface(robotoCondensedTypeface);
    testLatency.setTypeface(robotoCondensedTypeface);
    testPacketLoss.setTypeface(robotoCondensedTypeface);
    testJitter.setTypeface(robotoCondensedTypeface);

    // If we have any result to show
    if (archivedResultsList.size() > 0) {
      packetLossTestWasPerformed = false;

      // Set the test time
      long resultDate = archivedResultsList.get(position).getDtime();
      if (resultDate == 0) {
        testDateDay.setText(rowView.getContext().getString(R.string.not_available));
      } else {
//        String value = new FormattedValues().getDate(resultDate, "dd/MM/yyyy");
//        if (testJitter.getVisibility() != View.GONE) {
        String value = new FormattedValues().getDate(resultDate, "dd/MM/yy");
//        }
        testDateDay.setText(value);
      }
      testDateTime.setText(resultDate != 0 ? new FormattedValues().getDate(resultDate, "HH:mm:ss") : rowView.getContext().getString(R.string.not_available));

      // Set the test network type icon
      eNetworkTypeResults resultNetworkType = archivedResultsList.get(position).getNetworkType();

      switch (resultNetworkType) {
        case eNetworkTypeResults_WiFi:
          testNetworkType.setImageResource(R.drawable.ic_swifi);
          break;

        case eNetworkTypeResults_Mobile:
          testNetworkType.setImageResource(R.drawable.ic_sgsm);
          break;

        default:
          testNetworkType.setImageResource(R.drawable.ic_swifi);
          SKPorting.sAssert(getClass(), false);
          break;
      }

      String failed = rowView.getContext().getString(R.string.failed);
      String failed0MBPS = rowView.getContext().getString(R.string.failed_0MBPS);

      // Set the test download result
      String downloadResult = archivedResultsList.get(position).getDownloadResult();

      if (downloadResult.equals(failed)) {
        tv_Result_Download.setText(failed);
        testDownloadUnits.setText("");
      } else if (downloadResult.equals("0")) {
        tv_Result_Download.setText(rowView.getContext().getString(R.string.slash));
      } else if (downloadResult.equals("-1"))    // The test failed
      {
        //testDownload.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
        //tv_Result_Download.setText(rowView.getContext().getString(R.string.failed_test));
        //testDownloadUnits.setVisibility(View.INVISIBLE);
        tv_Result_Download.setText(failed);
        testDownloadUnits.setText("");
      } else    // The test was OK
      {
        Pair<Float, String> valueUnits = FormattedValues.getFormattedSpeedValue(downloadResult);
        String theText = String.valueOf(FormattedValues.sGet3DigitsNumber(valueUnits.first));
        String textForZero = String.valueOf(FormattedValues.sGet3DigitsNumber(0.0F));
        String textForFailed = getContext().getString(R.string.failed);
        if (theText.equals(textForZero) || theText.equals(textForFailed)) {
          theText = failed0MBPS;
          tv_Result_Download.setText(theText);
          testDownloadUnits.setText("Mbps");
        }
        tv_Result_Download.setText(theText);
        testDownloadUnits.setText(valueUnits.second);
      }

      // Set the test upload result
      String uploadResult = archivedResultsList.get(position).getUploadResult();

      if (uploadResult.equals(failed)) {
        tv_Result_Upload.setText(failed);
        testUploadUnits.setText("");
      } else if (uploadResult.equals("0")) {
        tv_Result_Upload.setText(rowView.getContext().getString(R.string.slash));
      } else if (uploadResult.equals("-1")) {
        //testUpload.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
//        tv_Result_Upload.setText(rowView.getContext().getString(R.string.failed_test));
//        testUploadUnits.setVisibility(View.INVISIBLE);
        tv_Result_Upload.setText(failed);
        testUploadUnits.setText("");
      } else    // The test was OK
      {
        Pair<Float, String> valueUnits = FormattedValues.getFormattedSpeedValue(uploadResult);
        String theText = String.valueOf(FormattedValues.sGet3DigitsNumber(valueUnits.first));
        String textForZero = String.valueOf(FormattedValues.sGet3DigitsNumber(0.0F));
        String textForFailed = getContext().getString(R.string.failed);
        if (theText.equals(textForZero) || theText.equals(textForFailed)) {
          theText = failed0MBPS;
          tv_Result_Upload.setText(theText);
          testUploadUnits.setText("Mbps");
        } else {
          tv_Result_Upload.setText(theText);
          testUploadUnits.setText(valueUnits.second);
        }
      }

      // Set the test latency result
      String latencyResult = archivedResultsList.get(position).getLatencyResult();
      // The test was not performed
      if (latencyResult.equals("0")) {
        testLatency.setText(rowView.getContext().getString(R.string.slash));
      }
      // The test was performed
      else {
        packetLossTestWasPerformed = true;

        if (latencyResult.equals("-1") || latencyResult.equals("-")) {
          //testLatency.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
          testLatency.setText(rowView.getContext().getString(R.string.slash));
        } else {
          Pair<String, String> valueUnits = FormattedValues.sGetFormattedLatencyValue(latencyResult);
          testLatency.setText(valueUnits.first + " " + valueUnits.second);
//            		if (valueUnits.first < 1000)
//            		{            			
//            			testLatency.setText(new DecimalFormat("0").format(valueUnits.first) + " " + rowView.getContext().getString(R.string.units_ms));						
//					}
//            		else if (valueUnits.first >= 1000)
//            		{
//            			testLatency.setText(new DecimalFormat("0.0").format(valueUnits.first/1000) + " " + rowView.getContext().getString(R.string.units_s));
//            		}            		            		
        }
      }

      // Set the packet loss result
      String packetLossResult = archivedResultsList.get(position).getPacketLossResult();

      if (packetLossResult.equals("0")) {
        testPacketLoss.setText(packetLossTestWasPerformed ? String.valueOf(packetLossResult) + rowView.getContext().getString(R.string.units_percent) : rowView.getContext().getString(R.string.slash));
      } else {
        if (packetLossResult.equals("-1")) {
          //testPacketLoss.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
          testPacketLoss.setText(rowView.getContext().getString(R.string.failed_test));
        } else    // The test was OK
        {
          Pair<Integer, String> valueUnits = FormattedValues.sGetFormattedPacketLossValue(packetLossResult);
          testPacketLoss.setText(String.valueOf(valueUnits.first) + " " + valueUnits.second);
        }
      }

      // Set the jitter result
      String jitterResult = archivedResultsList.get(position).getJitterResult();

      if (jitterResult.equals("0")) {
        testJitter.setText(rowView.getContext().getString(R.string.slash));
      } else {
        if (jitterResult.equals("-1")) {
          //testJitter.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
          testJitter.setText(rowView.getContext().getString(R.string.failed_test));
        } else    // The test was OK
        {
          String stringValue = String.valueOf(jitterResult);

          Pair<Integer, String> valueUnits = FormattedValues.sGetFormattedJitter(stringValue);
          testJitter.setText(String.valueOf(valueUnits.first) + " " + valueUnits.second);
//            		if (stringValue.substring(stringValue.length()-1, stringValue.length()).equals("0"))
//            		{            			
//            			testJitter.setText(new DecimalFormat("0").format(jitterResult) + " " + rowView.getContext().getString(R.string.units_ms));						
//					}
//            		else
//            		{
//            			testJitter.setText(new DecimalFormat("0.0").format(jitterResult) + rowView.getContext().getString(R.string.units_s));            			
//            		}           		            		
        }
      }
    }

    return rowView;
  }
}
