package com.samknows.tests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import android.util.Log;

import com.samknows.libcore.SKLogger;

public abstract class UploadTest extends HttpTest {
	
	// This is used for upload tests.	
	private static final int maxSendDataChunkSize = 32768;
	
	private Socket[] sockets =	null;								/* 1 Socket per thread */ 		
	
	//protected Socket socket = 						null;			/* TCP Socket to server */
	//protected OutputStream connOut = 				null;			/* Output stream to server */
	//protected InputStream connIn = 					null;			/* Input stream from Server*/
	protected boolean bGotValidResponseFromServer = false;
	protected boolean bReadThreadIsRunning = 		true;			/* True if thread from Server still running */
	protected double bitrateMpbs1024Based = 		-1.0;			/* ???? Scale coefficient */

	protected byte[] buff;											/* buffer to send values */

	protected Socket getSocket(int threadNum){
		return sockets[threadNum];
	}
	
	UploadTest(){
		super(_UPSTREAM);
		sockets = new Socket[getThreadsNum()];
	}
	
	protected OutputStream getOutputStream(){
		try {
			return sockets[getThreadIndex()].getOutputStream();
		} catch (IOException e) {
			SKLogger.sAssert(OutputStream.class, false);
			//e.printStackTrace();
			return null;
		}
	}
	
	protected InputStream getInputStream(){
		try {
			return sockets[getThreadIndex()].getInputStream();
		} catch (IOException e) {
			SKLogger.sAssert(InputStream.class, false);
			//e.printStackTrace();
			return null;
		}
	}
	
	protected boolean  warmUp(){										/* "Warms up" traffic to progress TCP slow start */
		long start = sGetMicroTime();									/* Memorize time when process starts */

		boolean isNotTimeToBreak = false;								/* We check this value to see if we can break the loop */

		OutputStream connOut = getOutputStream();
		do {
			try {					
				if (connOut == null)
					break;

				connOut.write(buff);									/* Write buffer to output socket */
				connOut.flush();

			} catch (IOException ioe) {									/* If something is wrong break from the cycle */
				SKLogger.sAssert(getClass(), false);
				error.set(true);
				break;
			}

			addTotalWarmUpBytes( buff.length );							/* This is an atomic variable. This thread modifies it, the other threads will do just that. No locking here */

			/* Break if we run out of time or bytes that we were allowed to send  */

			isNotTimeToBreak = ((sGetMicroTime() - start) < mWarmupMaxTimeMicro) || ( getTotalWarmUpBytes() < mWarmupMaxBytes ) ;

				//Log.d(TAG(this), "DEBUG: speed in bytes per second" + getSpeedBytesPerSecond() + "<<<");
				//Log.d(TAG(this), "DEBUG: isNotTimeToBreak=" + isNotTimeToBreak + ", totalWarmUpBytes=>>>" + totalWarmUpBytes.get() + ", time" + (sGetMicroTime() - start) + "<<<");

		} while ( isNotTimeToBreak ); // Upload warmup...


		if (error.get()) {
			// Let the thread do this! closeConnection(socket, connIn, connOut);
			// No need to wait for the thread to complete, however.		
			SKLogger.d(TAG(this), "WarmUp Exits: Result FALSE, isNotTimeToBreak=" + isNotTimeToBreak + ", totalWarmUpBytes=>>>" + getTotalWarmUpBytes() + ", time" + (sGetMicroTime() - start) + "<<<");
			return false;
		}
		
		SKLogger.d(TAG(this), "WarmUp Exits: Result TRUE, isNotTimeToBreak=" + isNotTimeToBreak + ", totalWarmUpBytes=>>>" + getTotalWarmUpBytes() + ", time" + (sGetMicroTime() - start) + "<<<");
		return true;
	}
	
	protected AtomicLong runUpStartTime = new AtomicLong(0);
	protected abstract boolean  runUp();							/* Traffic actually being measured */
		
	protected void init(){											/* don't forget to check error state after this method */
												/* getSocket() is a method from the parent class */

		for ( int i = 0; i < sockets.length; i++){
			sockets[i] = getSocket();

			if ( sockets[i] == null ){										
				error.set(true);
				SKLogger.sAssert(getClass(), false);
			}
		}
		
		// Generate this value in case we need it.
		// It is a random value from [0...2^32-1]
		Random sRandom = new Random();								/* Used for initialisation of upload array */
				
		if ( uploadBufferSize > 0 &&  uploadBufferSize <= maxSendDataChunkSize){
			buff = new byte[uploadBufferSize];
		}
		else{
			buff = new byte[maxSendDataChunkSize];
			SKLogger.sAssert(getClass(), false);
		}				

		if (randomEnabled){											/* randomEnabled comes from the parent (HTTPTest) class */
			sRandom = new Random();									/* Used for initialisation of upload array */	
			sRandom.nextBytes(buff);
		}
		
	}

//	public void release(){											/* Closes connections  and winds socket out*/
//		closeConnection(socket, connIn, connOut);
//	}

	

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
	
	
	
	/*public class HumanReadable {
	public TEST_STRING testString;
	public String[] values;

	public String getString(String locale) {
		switch (testString) {
		case DOWNLOAD_SINGLE_SUCCESS:
		case DOWNLOAD_MULTI_SUCCESS:
		case UPLOAD_SINGLE_SUCCESS:
		case UPLOAD_MULTI_SUCCESS:
			return String.format(locale, values[0]);
		case LATENCY_SUCCESS:
			return String.format(locale, values[0], values[1], values[2]);
		case DOWNLOAD_FAILED:
		case UPLOAD_FAILED:
		case LATENCY_FAILED:
			return locale;
		case NONE:
			return "";
		}
		return "";
	}

	public HashMap<String, String> getValues() {
		HashMap<String, String> ret = new HashMap<String, String>();
		switch (testString) {
		case DOWNLOAD_SINGLE_SUCCESS:
		case DOWNLOAD_MULTI_SUCCESS:
			ret.put("downspeed", values[0]);
			break;
		case UPLOAD_SINGLE_SUCCESS:
		case UPLOAD_MULTI_SUCCESS:
			ret.put("upspeed", values[0]);
			break;
		case LATENCY_SUCCESS:
			ret.put("latency", values[0]);
			ret.put("packetloss", values[1]);
			ret.put("jitter", values[2]);
			break;
		default:
		}
		return ret;
	}
	}*/
	
			
	
	private String[] formValuesArr(){
		String[] values = new String[1];			
		values = new String[1];
		values[0] = String.format("%.2f", (getSpeedBytesPerSecond() * 8d / 1000000));

		return values;
	}
	
	@Override
	public String getResultsAsString(){							/* New Human readable implementation */
		if (testStatus.equals("FAIL")){
			return "";
		}else{
			String[] values = formValuesArr();			
			return String.format(Locale.UK, values[0]);
		}		
	}
	@Override
	public String getResultsAsString(String locale){			/* New Human readable implementation */
		if (testStatus.equals("FAIL")){
			return locale;
		}else{
			String[] values = formValuesArr();			
			return String.format(locale, values[0]);
		}		
	}
	@Override
	public HashMap<String, String> getResultsAsHash(){
		HashMap<String, String> ret = new HashMap<String, String>();
		if (!testStatus.equals("FAIL")) {
			String[] values = formValuesArr();
			ret.put("upspeed", values[0]);
		}
		return ret;		
	}
		
	
	@Override
	public boolean isReady() {
		super.isReady();
		
		if ( uploadBufferSize == 0 || postDataLength == 0) {
			setError("Upload parameter missing");
			return false;
		}
		
		return true;
	}
	
	public String getHumanReadableResult() {
		String ret = "";
		String direction = "upload";
		String type = getThreadsNum() == 1 ? "single connection" : "multiple connection";
		if (testStatus.equals("FAIL")) {
			ret = String.format("The %s has failed.", direction);
		} else {
			ret = String.format(Locale.UK,"The %s %s test achieved %.2f Mbps.", type, direction, (getSpeedBytesPerSecond() * 8d / 1000000));
		}
		return ret;
	}
	
	
	/*private void uploadTest(int threadIndex) {

		IConcreteTest uploadTest;

		if ( !uploadStrategyServerBased ){													 If we don't use server base measurements 
			Log.d(TAG, "DEBUG: app does not use server-based upload speed testing...");
			uploadTest = new PassiveServerUploadTest(threadIndex);
		}
		else{
			Log.d(TAG, "DEBUG: app uses server-based upload speed testing...!");
			uploadTest = new ActiveServerloadTest(threadIndex);								 If we use server base measurements 
		}

		if( error.get() )																	 Something went wrong during test initialisation 	
			return;

		waitForAllConnections();				
		if (error.get()) {																	 SOmething went wrong during other threads initialisations 
			uploadTest.release();
			return;
		}

		uploadTest.execute();
		Log.d(TAG, "Completed uploadTest, threadIndex = " + threadIndex);
	}*/
	
	
	
//	@Override
//	int getSpeedBytesPerSecond() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
}
