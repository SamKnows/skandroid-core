package com.samknows.ui2.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samknows.libui2.R;
import com.samknows.measurement.SKApplication;

/**
 * This activity is responsible for sharing the a test result.
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */

public class ActivityShareResult extends Activity
{
	// *** VARIABLES *** //
	private String path;
	// UI elements
	private LinearLayout layout_ll_main;
	private TextView tv_Download_Result, tv_Upload_Result, tv_Latency_Result, tv_Packet_Loss_Result, tv_Jitter_Result, tv_Date_Result, tv_Connectivity_Result;
	private ImageView iv_Connectivity_Icon;	
	
	/**
	 * Activity life cycle
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share_result);
		setUpResources();		
	}
	
	// *** INNER CLASSES *** //
	private class GenerateImageAndShare extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			View view = layout_ll_main.getRootView();
			view.setDrawingCacheEnabled(true);
			Bitmap b = view.getDrawingCache();
			String extr = Environment.getExternalStorageDirectory().toString();
			File myPath = new File(extr, "TestResultToShare " + ".jpg");
			FileOutputStream fos = null;
			
			try
			{
			    fos = new FileOutputStream(myPath);
			    b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			    fos.flush();
			    fos.close();
			    path = MediaStore.Images.Media.insertImage( getContentResolver(), b, "Screen", "screen");
			}
			catch (FileNotFoundException e)
			{
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			}
			catch (Exception e)
			{
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);
			
			shareImage();
		}
	}
	
	// *** CUSTOM METHODS *** //
	/**
	 * Create, bind and set up resources
	 */
	private void setUpResources()
	{
		layout_ll_main = (LinearLayout)findViewById(R.id.activity_share_result_ll_main);
		
		tv_Download_Result = (TextView)findViewById(R.id.activity_share_result_tv_result_download);
		tv_Upload_Result = (TextView)findViewById(R.id.activity_share_result_tv_result_upload);
		tv_Latency_Result = (TextView)findViewById(R.id.activity_share_result_tv_result_latency);
		tv_Packet_Loss_Result = (TextView)findViewById(R.id.activity_share_result_tv_result_packet_loss);
		tv_Jitter_Result = (TextView)findViewById(R.id.activity_share_result_tv_result_jitter);
		tv_Date_Result = (TextView)findViewById(R.id.activity_share_result_tv_result_date);
		tv_Connectivity_Result = (TextView)findViewById(R.id.activity_share_result_tv_connectivity_text);
		iv_Connectivity_Icon = (ImageView)findViewById(R.id.activity_share_result_iv_connectivity_icon);
		
		// Initialise fonts		
		Typeface typeface_Roboto_Light = Typeface.createFromAsset(getAssets(), "fonts/roboto_light.ttf");
		Typeface typeface_Roboto_Thin = Typeface.createFromAsset(getAssets(), "fonts/roboto_thin.ttf");
		Typeface typeface_DIN_Condensed = Typeface.createFromAsset(getAssets(), "fonts/roboto_condensed_regular.ttf");
		
		((TextView)findViewById(R.id.activity_share_result_tv_label_download)).setTypeface(typeface_Roboto_Thin);
		((TextView)findViewById(R.id.activity_share_result_tv_label_upload)).setTypeface(typeface_Roboto_Thin);
		((TextView)findViewById(R.id.activity_share_result_tv_label_latency)).setTypeface(typeface_Roboto_Thin);
		((TextView)findViewById(R.id.activity_share_result_tv_label_packet_loss)).setTypeface(typeface_Roboto_Thin);
		((TextView)findViewById(R.id.activity_share_result_tv_label_jitter)).setTypeface(typeface_Roboto_Thin);
		((TextView)findViewById(R.id.activity_share_result_tv_title)).setTypeface(typeface_Roboto_Light);
		
		if (SKApplication.getAppInstance().hideJitter()) {
			tv_Jitter_Result.setVisibility(View.GONE);
			if (findViewById(R.id.archiveResultsListItemJitter) != null) {
				findViewById(R.id.archiveResultsListItemJitter).setVisibility(View.GONE);
			}
		}
		if (SKApplication.getAppInstance().hideLoss()) {
			tv_Packet_Loss_Result.setVisibility(View.GONE);
			if (findViewById(R.id.archiveResultsListItemPacketLoss) != null) {
        		findViewById(R.id.archiveResultsListItemPacketLoss).setVisibility(View.GONE);
			}
		}
		
		tv_Download_Result.setTypeface(typeface_DIN_Condensed);
		tv_Upload_Result.setTypeface(typeface_DIN_Condensed);
		tv_Latency_Result.setTypeface(typeface_DIN_Condensed);
		tv_Packet_Loss_Result.setTypeface(typeface_DIN_Condensed);
		tv_Jitter_Result.setTypeface(typeface_DIN_Condensed);
		tv_Date_Result.setTypeface(typeface_Roboto_Light);
		tv_Connectivity_Result.setTypeface(typeface_Roboto_Light);
		
		float downloadResult = (float)getIntent().getExtras().getFloat("downloadResult");
		if (downloadResult == 0)
		{
			tv_Download_Result.setText(getString(R.string.slash));						
		}
		else
		{
			tv_Download_Result.setText(getIntent().getExtras().get("downloadResult") + " " + getString(R.string.units_Mbps));
		}	
		
		float uploadResult = (float)getIntent().getExtras().getFloat("uploadResult");
		if (uploadResult == 0)
		{
			tv_Upload_Result.setText(getString(R.string.slash));			
		}
		else
		{
			tv_Upload_Result.setText(getIntent().getExtras().get("uploadResult") + " " + getString(R.string.units_Mbps));
		}
		
		int latencyResult = (int)getIntent().getExtras().getFloat("latencyResult");		
		if (latencyResult == 0)
		{
			tv_Latency_Result.setText(getString(R.string.slash));
			tv_Packet_Loss_Result.setText(getString(R.string.slash));
			tv_Jitter_Result.setText(getString(R.string.slash));						
		}
		else
		{
			tv_Latency_Result.setText((int)getIntent().getExtras().getFloat("latencyResult") + " " + getString(R.string.units_ms));
			tv_Packet_Loss_Result.setText(getIntent().getExtras().get("packetLossResult") + " " + getString(R.string.units_percent));
			tv_Jitter_Result.setText((int)getIntent().getExtras().getFloat("jitterResult") + " " + getString(R.string.units_ms));
		}		
		
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy, HH:mm");
		String date = sdf.format(new Date(getIntent().getExtras().getLong("dateResult")));
		tv_Date_Result.setText(date);							
		
		if (getIntent().getExtras().getInt("networkType") == 0)
		{
			iv_Connectivity_Icon.setImageDrawable(getResources().getDrawable(R.drawable.image_big_wifi));	
			tv_Connectivity_Result.setText(getString(R.string.wifi));
		}
		else
		{
			iv_Connectivity_Icon.setImageDrawable(getResources().getDrawable(R.drawable.image_big_mobile));
			tv_Connectivity_Result.setText(getString(R.string.mobile));
		}	
		
		ViewTreeObserver viewTreeObserver = layout_ll_main.getViewTreeObserver();
		viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{		
			@Override
			public void onGlobalLayout()
			{
				layout_ll_main.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				
				new GenerateImageAndShare().execute();				
			}
		});
		
	}
	
	/**
	 * Start the intent to share the image
	 */
	private void shareImage()
	{
		Uri URI_Image = Uri.parse(path);		
		Intent intent_Share_Image = new Intent(Intent.ACTION_SEND);		
		intent_Share_Image.putExtra(Intent.EXTRA_STREAM, URI_Image);
		intent_Share_Image.setType("image/jpeg");
		startActivity(Intent.createChooser(intent_Share_Image, "Share image using..."));
	}
}
