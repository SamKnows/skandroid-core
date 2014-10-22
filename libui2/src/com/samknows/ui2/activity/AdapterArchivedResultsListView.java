package com.samknows.ui2.activity;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.samknows.libui2.R;
import com.samknows.measurement.activity.components.FontFitTextView;

/**
 * This class represents the custom adapter to the archive results list view.
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */

public class AdapterArchivedResultsListView extends ArrayAdapter<TestResult>
{
    // *** VARIABLES *** //		
    private ArrayList<TestResult> archivedResultsList; 
    private boolean packetLossTestWasPerformed = false;
    
    // Other stuff
    private Context context;
    
    // Custom adapter constructor
    public AdapterArchivedResultsListView(Context pContext, ArrayList<TestResult> pArchivedResultsList)
    {
        super(pContext, R.layout.results_panel_shared_layout, pArchivedResultsList);
        
        context = pContext;
        archivedResultsList = pArchivedResultsList;
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

    // This is how each row view is calculated
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
    	// Get the row layout
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.results_panel_shared_layout, parent, false);

        // Set up UI elements
        TextView testDate = (TextView) rowView.findViewById(R.id.archiveResultsListItemDate);
        ImageView testNetworkType = (ImageView) rowView.findViewById(R.id.archiveResultsListItemNetworkType);

        TextView testDownload = (TextView) rowView.findViewById(R.id.archiveResultsListItemDownload);
        TextView testUpload = (TextView) rowView.findViewById(R.id.archiveResultsListItemUpload);
        TextView testLatency = (TextView) rowView.findViewById(R.id.archiveResultsListItemLatency);
        TextView testPacketLoss = (TextView) rowView.findViewById(R.id.archiveResultsListItemPacketLoss);
        TextView testJitter = (TextView) rowView.findViewById(R.id.archiveResultsListItemJitter);

        // Set up fonts
    	Typeface robotoThinTypeFace = Typeface.createFromAsset(context.getAssets(), "fonts/roboto_thin.ttf");
    	Typeface robotoLightTypeFace = Typeface.createFromAsset(context.getAssets(), "fonts/roboto_light.ttf");
    	Typeface robotoCondensedTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/roboto_condensed_regular.ttf");

    	// Assign the fonts
    	((TextView)rowView.findViewById(R.id.mbps_label_1)).setTypeface(robotoThinTypeFace);
    	((TextView)rowView.findViewById(R.id.mbps_label_2)).setTypeface(robotoThinTypeFace);
    	((TextView)rowView.findViewById(R.id.downloadLabel)).setTypeface(robotoLightTypeFace);
    	((TextView)rowView.findViewById(R.id.uploadLabel)).setTypeface(robotoLightTypeFace);
    	((TextView)rowView.findViewById(R.id.latency_label)).setTypeface(robotoLightTypeFace);
    	((TextView)rowView.findViewById(R.id.loss_label)).setTypeface(robotoLightTypeFace);
    	((TextView)rowView.findViewById(R.id.jitter_label)).setTypeface(robotoLightTypeFace);

        testDate.setTypeface(robotoLightTypeFace); 
        testDownload.setTypeface(robotoCondensedTypeface);
        testUpload.setTypeface(robotoCondensedTypeface);
        testLatency.setTypeface(robotoCondensedTypeface);
        testPacketLoss.setTypeface(robotoCondensedTypeface);
        testJitter.setTypeface(robotoCondensedTypeface);
        
        // If we have any result to show
        if (archivedResultsList.size() > 0)
        {
        	packetLossTestWasPerformed = false;
        	
        	// Set the test time
        	long resultDate = archivedResultsList.get(position).getDtime();
            testDate.setText(resultDate != 0 ? new FormattedValues().getDate(resultDate, "dd/MM/yy HH:mm") : rowView.getContext().getString(R.string.not_available));
            
            // Set the test network type icon
            int resultNetworkType = archivedResultsList.get(position).getNetworkType();
            
            if (resultNetworkType == 0)			// Network type is WiFi
            {        	
            	testNetworkType.setBackgroundResource(R.drawable.ic_swifi);
    		}
            else if (resultNetworkType == 1)	// Network type is mobile
            {
            	testNetworkType.setBackgroundResource(R.drawable.ic_sgsm);        	
            }
            
            // Set the test download result
            float downloadResult = archivedResultsList.get(position).getDownloadResult();
            
            if (downloadResult == 0)
            {
            	testDownload.setText(rowView.getContext().getString(R.string.slash));        	        				
    		}
            else if (downloadResult == -1)		// The test failed
            {
            	testDownload.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
            	testDownload.setTextSize(rowView.getResources().getDimension(R.dimen.text_size_large));
            	testDownload.setPadding(0, (int)convertDpToPixel(4, getContext()), 0,(int) convertDpToPixel(4, getContext()));
            	testDownload.setText(rowView.getContext().getString(R.string.failed_test));            	
            }
            else		// The test was OK
            {
            	testDownload.setText(String.valueOf(downloadResult));        	
            }
            
            // Set the test upload result
            float uploadResult = archivedResultsList.get(position).getUploadResult();
            
            if (uploadResult == 0)
            {
            	testUpload.setText(rowView.getContext().getString(R.string.slash));			
    		}
            else if (uploadResult == -1)		// The test failed
            {
            	testUpload.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
            	testUpload.setTextSize(rowView.getResources().getDimension(R.dimen.text_size_large));     
            	testUpload.setPadding(0, (int)convertDpToPixel(4, getContext()), 0,(int) convertDpToPixel(4, getContext()));
            	testUpload.setText(rowView.getContext().getString(R.string.failed_test));            	
    		}
            else		// The test was OK
            {
            	testUpload.setText(String.valueOf(uploadResult));
            }
            
            // Set the test latency result
            float latencyResult = archivedResultsList.get(position).getLatencyResult();
            // The test was not performed
            if (latencyResult == 0)
            {
            	testLatency.setText(rowView.getContext().getString(R.string.slash));        				
    		}
            // The test was performed
            else
            {
            	packetLossTestWasPerformed = true;
            	
            	if (latencyResult == -1)
            	{
            		testLatency.setTextColor(context.getResources().getColor(R.color.holo_red_dark));            		
            		testLatency.setText(context.getString(R.string.failed_test));
				}
            	else
            	{            		
            		if (latencyResult < 1000)
            		{            			
            			testLatency.setText(new DecimalFormat("0").format(latencyResult) + " " + rowView.getContext().getString(R.string.units_ms));						
					}
            		else if (latencyResult >= 1000)
            		{
            			testLatency.setText(new DecimalFormat("0.0").format(latencyResult/1000) + " " + rowView.getContext().getString(R.string.units_s));            			
            		}            		            		
            	}            	        	
            }       
            
            // Set the packet loss result
            int packetLossResult = archivedResultsList.get(position).getPacketLossResult();
            
            if (packetLossResult == 0)
            {
            	testPacketLoss.setText(packetLossTestWasPerformed ? String.valueOf(packetLossResult) + rowView.getContext().getString(R.string.units_percent) : rowView.getContext().getString(R.string.slash));
    		}
            else
            {
            	if (packetLossResult == -1)		// The test failed
            	{
            		testPacketLoss.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
            		testPacketLoss.setText(rowView.getContext().getString(R.string.failed_test));					
				}
            	else		// The test was OK
            	{
            		testPacketLoss.setText(packetLossResult + rowView.getContext().getString(R.string.units_percent));            		
            	}            	
            }
            
            // Set the jitter result
            float jitterResult = archivedResultsList.get(position).getJitterResult();
            
            if (jitterResult == 0)
            {
            	testJitter.setText(rowView.getContext().getString(R.string.slash));			
    		}
            else
            {
            	if (jitterResult == -1)			// The test failed
            	{
            		testJitter.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
            		testJitter.setText(rowView.getContext().getString(R.string.failed_test));					
				}
            	else		// The test was OK
            	{
            		String stringValue = String.valueOf(jitterResult); 
            		
            		if (stringValue.substring(stringValue.length()-1, stringValue.length()).equals("0"))
            		{            			
            			testJitter.setText(new DecimalFormat("0").format(jitterResult) + " " + rowView.getContext().getString(R.string.units_ms));						
					}
            		else
            		{
            			testJitter.setText(new DecimalFormat("0.0").format(jitterResult) + rowView.getContext().getString(R.string.units_s));            			
            		}           		            		
            	}            	
            }
		}        

        return rowView;
    }
}
