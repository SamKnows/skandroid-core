package com.samknows.ska.activity.components;

import com.samknows.libcore.R;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.SKApplication.eNetworkTypeResults;
import com.samknows.measurement.activity.components.FontFitTextView;
import com.samknows.measurement.activity.components.StatRecord;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class StatView extends ScrollView {
	private Context ctx;
    public StatView(Context ctx) {
        super(ctx);
        this.ctx=ctx;
     
    }
    
    public void setData(StatRecord sr){
    	setActiveNetworkType(sr.active_network_type);
    	//setTestsLocation(sr.tests_location);
    	setUploadLocation(sr.upload_location);
    	setUploadResult(sr.upload_result);
    	setDownloadLocation(sr.download_location);
    	setDownloadResult(sr.download_result);
    	setLatencyLocation(sr.latency_location);
    	setLatencyResult(sr.latency_result);
    	setPacketlossLocation(sr.packetloss_location);
    	setPacketlossResult(sr.packetloss_result);
    	setJitterLocation(sr.jitter_location);
    	setJitterResult(sr.jitter_result);
    	setTimestamp(sr.time_stamp);
    	
    	if (   (findViewById(R.id.download_archive_panel).getVisibility() == View.GONE)
    	    && (findViewById(R.id.upload_archive_panel).getVisibility() == View.GONE)
    	    && (findViewById(R.id.latency_archive_panel).getVisibility() == View.GONE)
    	    && (findViewById(R.id.packetloss_archive_panel).getVisibility() == View.GONE)
    	    && (findViewById(R.id.jitter_archive_panel).getVisibility() == View.GONE)
    	   )
    	{
    		// No results AT ALL to show!
    	    ((TextView)findViewById(R.id.download_label_text_view)).setText("No results");
    	    ((FontFitTextView)findViewById(R.id.download_result)).setText("No data found");
    	    findViewById(R.id.download_archive_panel).setVisibility(View.VISIBLE);
    	}

    	setPassiveMetric(R.id.pm_tablerow1,R.id.passivemetric1,sr.passivemetric1,sr.passivemetric1_type);
    	setPassiveMetric(R.id.pm_tablerow2,R.id.passivemetric2,sr.passivemetric2,sr.passivemetric2_type);
    	setPassiveMetric(R.id.pm_tablerow3,R.id.passivemetric3,sr.passivemetric3,sr.passivemetric3_type);
    	setPassiveMetric(R.id.pm_tablerow4,R.id.passivemetric4,sr.passivemetric4,sr.passivemetric4_type);
    	setPassiveMetric(R.id.pm_tablerow5,R.id.passivemetric5,sr.passivemetric5,sr.passivemetric5_type);
    	setPassiveMetric(R.id.pm_tablerow6,R.id.passivemetric6,sr.passivemetric6,sr.passivemetric6_type);
    	setPassiveMetric(R.id.pm_tablerow7,R.id.passivemetric7_networkoperatorname,sr.passivemetric7_networkoperatorname,sr.passivemetric7_networkoperatorname_type);
    	setPassiveMetric(R.id.pm_tablerow8,R.id.passivemetric8,sr.passivemetric8,sr.passivemetric8_type);
    	setPassiveMetric(R.id.pm_tablerow9,R.id.passivemetric9,sr.passivemetric9,sr.passivemetric9_type);
    	setPassiveMetric(R.id.pm_tablerow10,R.id.passivemetric10,sr.passivemetric10,sr.passivemetric10_type);
    	
    	setPassiveMetric(R.id.pm_tablerow11,R.id.passivemetric11,sr.passivemetric11,sr.passivemetric11_type);
    	setPassiveMetric(R.id.pm_tablerow12,R.id.passivemetric12,sr.passivemetric12,sr.passivemetric12_type);
    	setPassiveMetric(R.id.pm_tablerow13,R.id.passivemetric13,sr.passivemetric13,sr.passivemetric13_type);
    	setPassiveMetric(R.id.pm_tablerow14,R.id.passivemetric14,sr.passivemetric14,sr.passivemetric14_type);
    	setPassiveMetric(R.id.pm_tablerow15,R.id.passivemetric15,sr.passivemetric15,sr.passivemetric15_type);
    	setPassiveMetric(R.id.pm_tablerow16,R.id.passivemetric16,sr.passivemetric16,sr.passivemetric16_type);
    	setPassiveMetric(R.id.pm_tablerow17,R.id.passivemetric17,sr.passivemetric17,sr.passivemetric17_type);
    	setPassiveMetric(R.id.pm_tablerow18,R.id.passivemetric18,sr.passivemetric18,sr.passivemetric18_type);
    	setPassiveMetric(R.id.pm_tablerow19,R.id.passivemetric19,sr.passivemetric19,sr.passivemetric19_type);
    	
    	setPassiveMetric(R.id.pm_tablerow20,R.id.passivemetric20,sr.passivemetric20,sr.passivemetric20_type);
    	setPassiveMetric(R.id.pm_tablerow21,R.id.passivemetric21,sr.passivemetric21,sr.passivemetric21_type);
    	setPassiveMetric(R.id.pm_tablerow22,R.id.passivemetric22,sr.passivemetric22,sr.passivemetric22_type);
    	
		//
		// Show or hide the passive results, depending on whether
		// we're looking at mobile (show) or WiFi (hide!)
		//
	    TextView activeMetricsTitle = (TextView) findViewById(R.id.active_metric_title);
		LinearLayout passiveResultsLinearLayout = (LinearLayout)findViewById(R.id.passive_results_linearlayout);
		//if (SKApplication.getNetworkTypeResults() == eNetworkTypeResults.eNetworkTypeResults_WiFi) {
		if (sr.active_network_type.equals("(Mobile)")) {
			// Show for Mobile results!
			passiveResultsLinearLayout.setVisibility(View.VISIBLE);
	        activeMetricsTitle.setText(ctx.getString(R.string.active_metrics_mobile));
		} else {
			// Hide for WiFi results!
			passiveResultsLinearLayout.setVisibility(View.GONE);
	        activeMetricsTitle.setText(ctx.getString(R.string.active_metrics_wifi));
		}

    	setPassiveMetric(R.id.pm_tablerow23,R.id.passivemetric23,sr.passivemetric23,sr.passivemetric23_type);
    	setPassiveMetric(R.id.pm_tablerow24,R.id.passivemetric24,sr.passivemetric24,sr.passivemetric24_type);
    	setPassiveMetric(R.id.pm_tablerow25,R.id.passivemetric25,sr.passivemetric25,sr.passivemetric25_type);
    	setPassiveMetric(R.id.pm_tablerow26,R.id.passivemetric26,sr.passivemetric26,sr.passivemetric26_type);
    	setPassiveMetric(R.id.pm_tablerow27,R.id.passivemetric27,sr.passivemetric27,sr.passivemetric27_type);
    	setPassiveMetric(R.id.pm_tablerow28,R.id.passivemetric28,sr.passivemetric28,sr.passivemetric28_type);
    	setPassiveMetric(R.id.pm_tablerow29,R.id.passivemetric29,sr.passivemetric29,sr.passivemetric29_type);
    	setPassiveMetric(R.id.pm_tablerow30,R.id.passivemetric30,sr.passivemetric30,sr.passivemetric30_type);
    	
    	setPassiveMetric(R.id.pm_tablerow31,R.id.passivemetric31,sr.passivemetric31,sr.passivemetric31_type);
    	setPassiveMetric(R.id.pm_tablerow32,R.id.passivemetric32,sr.passivemetric32,sr.passivemetric32_type);
    	
    }
    
    
    // active metrics setter

    public void setDownloadLocation(String text){
    	TextView tv = (TextView) findViewById(R.id.download_location);
		tv.setText(text);
    }
    public void setDownloadResult(String text){
    	FontFitTextView fftv = (FontFitTextView) findViewById(R.id.download_result);
		fftv.setText(text);
		if (text.contains("Fail")){
			fftv.setTextColor(Color.RED);
		}
		
		if (text.equals("")){
			TableLayout tl = (TableLayout) findViewById(R.id.download_archive_panel);
			tl.setVisibility(View.GONE);
		}
		
    }
    
    public void setActiveNetworkType(String text){
    	TextView tv = (TextView) findViewById(R.id.active_metric_title);
    	
    	if (text.equals("(Network)")) {
    		text = "(" + ctx.getString(R.string.wifi) + ")";
    	} else if (text.equals("Network")) {
    		text = ctx.getString(R.string.wifi);
    	}
    	tv.setText(ctx.getString(R.string.active_metrics)+" "+text);
    }
    
    /*
    public void setTestsLocation(String text){
    	TextView tv = (TextView) findViewById(R.id.tests_location);
    	tv.setText(text);
    }*/
    
    public void setUploadLocation(String text){
    	TextView tv = (TextView) findViewById(R.id.upload_location);
		tv.setText(text);
    }
    public void setUploadResult(String text){
    	FontFitTextView fftv = (FontFitTextView) findViewById(R.id.upload_result);
		fftv.setText(text);
		if (text.contains("Fail")){
			fftv.setTextColor(Color.RED);
		}
		if (text.equals("")) {
			TableLayout tl = (TableLayout) findViewById(R.id.upload_archive_panel);
			tl.setVisibility(View.GONE);
		}
    }
    public void setLatencyLocation(String text){
    	TextView tv = (TextView) findViewById(R.id.latency_location);
		tv.setText(text);
    }
    public void setLatencyResult(String text){
    	FontFitTextView fftv = (FontFitTextView) findViewById(R.id.latency_result);
		fftv.setText(text);
		if (text.contains("Fail")){
			fftv.setTextColor(Color.RED);
		}
		if (text.equals("")) {
			TableLayout tl = (TableLayout) findViewById(R.id.latency_archive_panel);
			tl.setVisibility(View.GONE);
		}
    }
    public void setPacketlossLocation(String text){
    	TextView tv = (TextView) findViewById(R.id.packetloss_location);
		tv.setText(text);
    }
    public void setPacketlossResult(String text){
    	FontFitTextView fftv = (FontFitTextView) findViewById(R.id.packetloss_result);
		fftv.setText(text);
		if (text.contains("Fail")){
			fftv.setTextColor(Color.RED);
		}
		if (text.equals("")) {
			TableLayout tl = (TableLayout) findViewById(R.id.packetloss_archive_panel);
			tl.setVisibility(View.GONE);
		}
    }
    public void setJitterLocation(String text){
    	TextView tv = (TextView) findViewById(R.id.jitter_location);
		tv.setText(text);
    }
    public void setJitterResult(String text){
    	FontFitTextView tv = (FontFitTextView) findViewById(R.id.jitter_result);
		tv.setText(text);
		if (text.contains("Fail")){
			tv.setTextColor(Color.RED);
		}
		if (text.equals("")) {
			TableLayout tl = (TableLayout) findViewById(R.id.jitter_archive_panel);
			tl.setVisibility(View.GONE);
		}
    }
 
    public void setTimestamp(String text){
    	TextView tv = (TextView) findViewById(R.id.timestamp);
		tv.setText(text);
    }
    
public void setPassiveMetric(int table_id,int text_view,String text, String type){
		
		if (type.equals("boolean")){
			ImageView iv = new ImageView(ctx);
			iv.setImageResource(R.drawable.check);
			iv.setPadding(5,5,5,5);
			TableRow tr = (TableRow) findViewById(table_id);
			tr.addView(iv);
		}
		else
		{
	    	TextView tv = (TextView) findViewById(text_view);
			tv.setText(text);
			
			if (text.equals("")){
				
				TableLayout tl=(TableLayout) tv.getParent().getParent();
				tl.setVisibility(View.GONE);
			}
		}
    }
    

    
    public void setRightPageIndicator(boolean setting)
    {
//    	ImageView iv= (ImageView) findViewById(R.id.page_right_indicater);
//		if (setting){
//			iv.setBackgroundResource(R.drawable.page_turn_right);
//		}
//		else
//		{
//			iv.setBackgroundResource(R.drawable.page_corner_blank);
//		}
    }
    

}
