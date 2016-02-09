package com.samknows.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;


import com.samknows.libcore.SKLogger;

public abstract class UploadTest extends HttpTest {
		
	protected double bitrateMpbs1024Based = 		-1.0;			/* ???? Scale coefficient */
	protected byte[] buff;											/* buffer to send values */
	
	protected UploadTest(List<Param> params){
		super(_UPSTREAM, params);
		this.init();
	}
		
	private String[] formValuesArr(){
		String[] values = new String[1];			
		values = new String[1];
		values[0] = String.format("%.2f", (Math.max(0,getTransferBytesPerSecond()) * 8d / 1000000));

		return values;
	}
					
	private void init(){											/* don't forget to check error state after this method */
																	/* getSocket() is a method from the parent class */
		int maxSendDataChunkSize = 32768;
			
		// Generate this value in case we need it.
		// It is a random value from [0...2^32-1]
		Random sRandom = new Random();								/* Used for initialisation of upload array */
				
		if ( getUploadBufferSize() > 0 &&  getUploadBufferSize() <= maxSendDataChunkSize){
			buff = new byte[getUploadBufferSize()];
		}
		else{
			buff = new byte[maxSendDataChunkSize];
			SKLogger.sAssert(getClass(), false);
		}				

		if (getRandomEnabled()){											/* randomEnabled comes from the parent (HTTPTest) class */
			sRandom = new Random();									/* Used for initialisation of upload array */	
			sRandom.nextBytes(buff);
		}	
	}

	@Override
	public String getStringID() {
		String ret = "";
		if (getThreadsNum() == 1) {
			ret = UPSTREAMSINGLE;
		} else {
			ret = UPSTREAMMULTI;
		}
		return ret;
	}
	
	@Override
	public int getNetUsage() {
		return (int)(getTotalTransferBytes() + getTotalWarmUpBytes());
	}
			
	@Override
	public HashMap<String, String> getResults(){
		HashMap<String, String> ret = new HashMap<>();
		if (!getTestStatus().equals("FAIL")) {
			String[] values = formValuesArr();
			ret.put("upspeed", values[0]);
		}
		return ret;		
	}
		
	@Override
	public boolean isReady() {
		super.isReady();
		
		if ( getUploadBufferSize() == 0 || getPostDataLength() == 0) {
			setError("Upload parameter missing");
			return false;
		}
		
		return true;
	}
	
	public String getHumanReadableResult() {
		String ret = "";
		String direction = "upload";
		String type = getThreadsNum() == 1 ? "single connection" : "multiple connection";
		if (getTestStatus().equals("FAIL")) {
			ret = String.format("The %s has failed.", direction);
		} else {
			ret = String.format(Locale.UK,"The %s %s test achieved %.2f Mbps.", type, direction, (Math.max(0, getTransferBytesPerSecond()) * 8d / 1000000));
		}
		return ret;
	}
}
