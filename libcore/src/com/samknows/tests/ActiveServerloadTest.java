package com.samknows.tests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.annotation.SuppressLint;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.test.TestExecutor;
import com.samknows.measurement.util.OtherUtils;

@SuppressLint("UseSparseArrays")
public final class ActiveServerloadTest extends UploadTest {
	private AtomicInteger threadsCount = new AtomicInteger(0);

	private AtomicLong bytesPerSecondTotal = new AtomicLong(0);
	private AtomicLong bytesTransferTotal = new AtomicLong(0);
	private AtomicLong bytesWarmUpTotal  = new AtomicLong(0);
	private AtomicLong timeWarmUpTotal = new AtomicLong(0);
	private AtomicLong timeTransferTotal = new  AtomicLong(0);


	private long mSessionID = -1L;	
	private HashMap<Integer, ServerInStreamThread> readThreads = new HashMap<Integer, ServerInStreamThread>();

	public ActiveServerloadTest( ) {
		super();
		super.init();										/*TODO : check error state*/

		Random sRandom = new Random();
		mSessionID = sRandom.nextLong() & 0xffffffffL;
		SKLogger.sAssert(getClass(), mSessionID >= 0);

		SKLogger.d(TAG(this), "Session ID = " + mSessionID);//TODO remove in production
	}


	private String formPostHeaderRequest(int numThreads, int threadIndex ) {
		StringBuilder sb = new StringBuilder();

		// Verify that the session_id was properly initialized (it will have come from a random value, shared by all threads
		// for this HttpTest instance...)
		SKLogger.sAssert(getClass(), mSessionID >= 0);

		// Use the correct parameters in the header... INCLUDING THE UNIT ID!
		// c.f. instructions at the top of this file.
		sb.append("POST /?CONTROL=1&UNITID=1");
		sb.append("&SESSIONID=");		
		sb.append(mSessionID);
		sb.append("&NUM_CONNECTIONS=");
		sb.append(numThreads);
		sb.append("&CONNECTION=");
		sb.append(threadIndex);
		sb.append("&AGGREGATE_WARMUP=0&RESULTS_INTERVAL_PERIOD=");

		long resultsIntervalPeriod = (mWarmupMaxTimeMicro + mTransferMaxTimeMicro) / 1000000 + 1;
		sb.append(resultsIntervalPeriod);

		sb.append("&RESULT_NUM_INTERVALS=1&TEST_DATA_CAP=4294967295");
		sb.append("&TRANSFER_MAX_SIZE=");
		sb.append(mTransferMaxBytes);
		sb.append("&WARMUP_SAMPLE_TIME=");
		// The system will reject a header with "WARMUP_SAMPLE_TIME=0".
		// If that happens, set WARMUP_SAMPLE_TIME to UINT32_MAX instead of zero.
		//long millisecondsWarmupSampleTime = (long)(warmupMaxTimeMicro/1000.0);
		long millisecondsWarmupSampleTime = (long)(mWarmupMaxTimeMicro/1000.0);
		if (millisecondsWarmupSampleTime == 0) {
			// There is no unsigned 32 bit int in Java. You have to use long (signed 64-bit) instead.
			// Not expected - and might cause the server-based test to timeout!
			SKLogger.sAssert(getClass(),  false);
			millisecondsWarmupSampleTime = 4294967295L;
			//millisecondsWarmupSampleTime = 5000L; // Hack!
		}
		sb.append(millisecondsWarmupSampleTime); // WARMUP_SAMPLE_TIME=%d (milli) - from Micro!
		sb.append("&NUM_WARMUP_SAMPLES=1");
		sb.append("&MAX_WARMUP_SIZE=");
		sb.append(mWarmupMaxBytes);
		sb.append("&MAX_WARMUP_SAMPLES=1&WARMUP_FAIL_ON_MAX=0&WARMUP_TOLERANCE=5 HTTP/1.1\r\n");

		sb.append("Host: ");
		sb.append(target + ":" + port);
		sb.append("\r\n");
		sb.append("Accept: */*\r\n");
		sb.append("Content-Length: 4294967295\r\n");
		sb.append("Content-Type: application/x-www-form-urlencoded\r\n");
		sb.append("Expect: 100-continue\r\n");
		sb.append("\r\n");

		String result = sb.toString();

		SKLogger.d(TAG(this), "Request string: \n" + result);//TODO remove in production
		return result;
	}
	private void requestHeader(){				

		OutputStream connOut = getOutputStream();
		if( connOut != null){
			PrintWriter writerOut = new PrintWriter(connOut, false);

			String postRequestHeader = formPostHeaderRequest(getThreadsNum(), getThreadIndex());
			writerOut.print(postRequestHeader);
			writerOut.flush();
		}
	}
	private class ServerInStreamThread extends Thread {		

		//Socket mSocket = null;
		InputStream mConnIn = null;
		boolean mbIsCancelled = false;
		int threadIndex = 0;

		public void doStop() {
			mbIsCancelled = true;
		}

		public boolean getIsCancelled() {
			return mbIsCancelled;
		}


		public ServerInStreamThread( InputStream inConnIn, int threadIndex) {
			super();

			//mSocket = inSocket;
			mConnIn = inConnIn;
			this.threadIndex = threadIndex;
		}

		@Override
		public void run() {

			byte[] buffer = new byte[4000];

			String response = new String();
			int responseCode = 0;

			for (;;) {
				if (mbIsCancelled == true) {
					SKLogger.d(TAG(this), "mbIsCancelled=true, stop the read thread");
					break;
				}

				try {

					int approxBytesAvailable = mConnIn.available();
					if (approxBytesAvailable <= 0) {
						// continue the for loop!
						continue;
					}

					int bytes = mConnIn.read(buffer, 0, buffer.length-1);

					if (bytes > 0) {
						buffer[bytes] = '\0';
						String bufferAsUtf8String = new String(buffer, "UTF-8");
						response = bufferAsUtf8String;//.substring(0, bytes - 1);
						SKLogger.d(TAG(this), "Server response: \n " + response);//TODO remove in production						

						String[] items = response.split(" ");

						if (items.length > 0) {
							if (items[0].equals("HTTP/1.1")) {
								if (items.length > 1) {
									responseCode = Integer.valueOf(items[1]);
									if ( (responseCode == 100) || // Continue
											(responseCode == 200)    // OK
											)
									{
										// OK!
									} else {
										SKLogger.d(TAG(this), "Error in response, code " + responseCode);
										break;
									}
								}
							}

							// Have we got everything we need yet?
							if (response.contains("SAMKNOWS_HTTP_REPLY")) {
								// Got the header!
								if (response.contains("MEASUR_SESSION")) {
									// Assume we have the lot!
									//SKLogger.d(TAG(this), "Got MEASUR_SESSION");
									break;
								}
							}

						}
					}
				} catch (SocketTimeoutException e) {
					// Keep going!
				} catch (IOException e) {
					SKLogger.sAssert(getClass(),  false);
					break;
				}			
			}

			callOnStopOrCancel(response, responseCode);

			mbIsCancelled = true;
		}

		public void callOnStopOrCancel(String responseString, int responseCode) {
			// This must be overridden!
			SKLogger.sAssert(getClass(),  false);
		}
	};
	private int setReadThread(){
		InputStream inputStream = getInputStream();

		ServerInStreamThread readThread = new ServerInStreamThread(inputStream, getThreadIndex()) {

			@Override
			public void callOnStopOrCancel(String responseString, int responseCode) {

				SKLogger.d(TAG(this), "DEBUG: callOnStopOrCancel");

				//					synchronized(HttpTest.this) {
				//						HttpTest.this.closeConnection(socket,  connIn,  connOut);
				//						connIn = null;
				//						connOut = null;
				//						socket = null;
				//					}

				if ((responseCode != 100) && (responseCode != 200)) {
					SKLogger.sAssert(getClass(), false);

					SKLogger.d(TAG(this), "DEBUG: upload server did fail with error=" + responseCode);
				} else {
					if (responseCode == 100) {
						SKLogger.d(TAG(this), "DEBUG: reponseCode=" + responseCode);
					} else {
						SKLogger.d(TAG(this), "DEBUG: reponseCode=" + responseCode + ", responseString=>>>" + responseString + "<<<");
					}

					// Example
					/*
	       HTTP/1.1 100 Continue
	       X-SamKnows: 1

	       SAMKNOWS_HTTP_REPLY
	       VERSION: 1.0
	       RESULT: OK
	       END_TIME: 1402570650
	       SECTION: WARMUP
	       NUM_WARMUP: 1
	       WARMUP_SESSION: 5 1030000 3994048
	       SECTION: MEASUR
	       NUM_MEASUR: 1
	       MEASUR_SESSION: 15 1666000 8293952

	       That is 829352/15 = 552930.13333 bytes per second.
					 */

					long transferBytes = 0;
					long transferBytesPerSecond = 0;

					long warmUpBytesPerSecond = 0;
					long warmUpBytes = 0;

					long warmupnSecTime = 0;
					long transfernSecTime = 0;

					String[] items = responseString.split("\n");
					if (items.length == 0) {
						SKLogger.sAssert(getClass(),  false);
					} else {
						int itemCount = items.length;
						int itemIndex;

						for (itemIndex = 0; itemIndex < itemCount; itemIndex++) {
							String item = items[itemIndex];
							//WARMUP_SESSION: 5 1030000 3994048
							// Locate the WARMUP_SESSION items.
							// Locate the MEASURE_SESSION items.

							SKLogger.e(TAG(this), item);//TODO haha
							if ( item.contains("WARMUP_SESSION")){
								String[] warmUpItems = item.split(" ");
								if( warmUpItems.length != 4 ){
									SKLogger.sAssert(getClass(),  false);
								}else{
									warmupnSecTime = Long.valueOf(warmUpItems[1])/*seconds*/ * 1000000000 + Long.valueOf(warmUpItems[2])/*nanoseconds*/;
									if( warmupnSecTime <= 0){
										SKLogger.sAssert(getClass(),  false);
									}else{
										warmUpBytes = Long.valueOf(warmUpItems[3]);
										SKLogger.sAssert(getClass(), warmUpBytes > 0);

										warmUpBytesPerSecond = (long)(warmUpBytes / ( warmupnSecTime / 1000000000.0));
										SKLogger.sAssert(getClass(), warmUpBytesPerSecond > 0);
									}
								}
							}


							if (item.contains("MEASUR_SESSION")) {
								// Use the final calculated value!
								String[] transferItems = item.split(" ");
								if (transferItems.length != 4) {
									SKLogger.sAssert(getClass(),  false);
								} else {
									transfernSecTime = Long.valueOf(transferItems[1])/*seconds*/ * 1000000000 + Long.valueOf(transferItems[2])/*nanoseconds*/;
									transfernSecTime -= warmupnSecTime;
									if (transfernSecTime <= 0) {
										SKLogger.sAssert(getClass(),  false);
									} else {
										bGotValidResponseFromServer = true;
										SKLogger.d(TAG(this), "*** bGotValidResponseFromServer set to TRUE!");//TODO remove in production

										transferBytes = Long.valueOf(transferItems[3]);
										SKLogger.sAssert(getClass(), transferBytes > 0);

										transferBytesPerSecond = (long)(transferBytes / ( transfernSecTime / 1000000000.0));
										SKLogger.sAssert(getClass(), transferBytesPerSecond > 0);
									}
								}
							}
						}
					}

					if (bGotValidResponseFromServer == true)
					{
						threadsCount.addAndGet(1);
						bytesPerSecondTotal.addAndGet(transferBytesPerSecond);
						bytesTransferTotal.addAndGet(transferBytes);
						bytesWarmUpTotal.addAndGet(warmUpBytes);
						timeTransferTotal.addAndGet((long)(transfernSecTime / 1000.0));
						timeWarmUpTotal.addAndGet((long)(warmupnSecTime / 1000.0));


						SKLogger.d(TAG(this), "DEBUG: BYTES CALCULATED FROM SERVER, PER SECOND = " + transferBytesPerSecond);//TODO remove in production
						bitrateMpbs1024Based = OtherUtils.sConvertBytesPerSecondToMbps1024Based(transferBytesPerSecond);
						SKLogger.d(TAG(this), "DEBUG: bitsPerSecond CALCULATED FROM SERVER = " + OtherUtils.sBitrateMbps1024BasedToString(bitrateMpbs1024Based));
					}
				}
				SKLogger.d(TAG(this), "Closing thread 'INPUT FROM SERVER' ("+ threadIndex +") at " + (new java.text.SimpleDateFormat("HH:mm:ss.SSS")).format(new java.util.Date()) );//TODO remove in production
				bReadThreadIsRunning = false;
			}
		};

		requestHeader();										/* inform Server about this connection parameters */

		readThreads.put(this.getThreadIndex(), readThread);		/* Memorize this input thread */

		SKLogger.d(TAG(this), "Starting thread 'INPUT FROM SERVER at ' " + (new java.text.SimpleDateFormat("HH:mm:ss.SSS")).format(new java.util.Date()) );//TODO remove in production

		try{
			readThread.start();									/* Start input thread */
		}
		catch(Exception io){
			SKLogger.d(TAG(this), "Thread 'INPUT FROM SERVER has already started " + (new java.text.SimpleDateFormat("HH:mm:ss.SSS")).format(new java.util.Date()) );//TODO remove in production
			return 1;
		}
		return 0;
	}

	@Override
	final protected boolean runUp(){
		SKLogger.d(TAG(this), "Starting thread 'UPLOAD TO SERVER at ' " + (new java.text.SimpleDateFormat("HH:mm:ss.SSS")).format(new java.util.Date()) );//TODO remove in production
		//
		// Send the ACTUAL TEST data!
		//

		boolean isTransferDone = false;
		long waitUntilTime = 0;
		long waitFromTime = 0;

		long bytesTransferredInThisThread = 0L;												/* bytes transferred by this thread */
		long timeElapsedSinceLastExternalMonitorUpdate = 0;

		long currentTime = sGetMicroTime();
		if (runUpStartTime.get() <= 0 )
			runUpStartTime.set(currentTime);											/* Memorize time when process starts */	

		waitUntilTime = currentTime + mTransferMaxTimeMicro + 1;
		waitFromTime = currentTime;

		ServerInStreamThread readThread = readThreads.get(getThreadIndex());
		OutputStream connOut = getOutputStream();
		for (;;) {//do while suits us better???
			long timeElapsed = sGetMicroTime() - runUpStartTime.get();

			try {
				if (connOut == null) 
					break;

				connOut.write(buff);														/* Write buffer to output socket */
				connOut.flush();

				bytesTransferredInThisThread += buff.length;
				addTotalTransferBytes( buff.length );

			} catch (IOException ioe) {
				SKLogger.sAssert(getClass(), false);
				error.set(true);
				// And break out of the loop....
				sSetLatestSpeedForExternalMonitor((long) (super.getTotalTransferBytes( )/(timeElapsed / 1000000.0))  , "Upload write exception");
				SKLogger.d(TAG(this), "Upload write exception");		//TODO remove this later
				break;
			}

			long updateTime = 0;
			updateTime = timeElapsedSinceLastExternalMonitorUpdate == 0 ? 1000000 : /*40000*/500000;
			if( timeElapsed - timeElapsedSinceLastExternalMonitorUpdate > updateTime/*uSec*/ ){													/* should be triggered 25 times a second after the 1st second */
				sSetLatestSpeedForExternalMonitor((long) (super.getTotalTransferBytes( )/(timeElapsed / 1000000.0))  , "Normal upload cycle");		/* update speed parameter + indicative ID */

				timeElapsedSinceLastExternalMonitorUpdate = timeElapsed;
				SKLogger.d(TAG(this), "External Monitor updated at " + (new java.text.SimpleDateFormat("HH:mm:ss.SSS")).format(new java.util.Date()) + 
						" as " +  (super.getTotalTransferBytes( )/(timeElapsed / 1000000.0)) +
						" thread: " + getThreadIndex());//TODO remove in production
			}


			/* Transfer done if: */
			if (mTransferMaxTimeMicro > 0) 																	/* we run out of time */
				if (timeElapsed > mTransferMaxTimeMicro) 
					isTransferDone = true;


			if (mTransferMaxBytes > 0) {
				if (getTotalWarmUpBytes() + super.getTotalTransferBytes() > mTransferMaxBytes) {					/* we transferred more bytes that was initially allowed */
					isTransferDone = true;
				}
			}

			if (getTotalTransferBytes() > 0) {																/* if we managed just to transfer something set status from parent class to OK */
				testStatus = "OK";
			}


			//SKLogger.d(TAG(this), "DEBUG: speed in bytes per second" + getSpeedBytesPerSecond() + "<<<");
			//SKLogger.d(TAG(this), "DEBUG: isTransferDone=" + isTransferDone + ", totalTransferBytesSent=>>>" + totalTransferBytesSent.get() + ", time" + (sGetMicroTime() - start) + "<<<");

			// Stop EITHER if:
			// 1) the read thread tells us!
			if ((readThread != null) && (readThread.getIsCancelled() == true)) {
				sSetLatestSpeedForExternalMonitor((long) (super.getTotalTransferBytes( )/(timeElapsed / 1000000.0)), "Server Read thread has stopped");
				SKLogger.d(TAG(this), "Server Read thread has stopped");
				break;
			}

			// 2) we at least 10 seconds AFTER the detection of "isTransferDone" - giving server long enough to respond, or until we've written enough bytes!
			//if (transferMaxBytes == 0) {
			if (readThread != null) {
				// Server-based upload speed test in operation...
				if (isTransferDone == true) {
					if (sGetMilliTime() > waitUntilTime) {
						SKLogger.d(TAG(this), "loop - break 5a, waituntiltime=" + waitUntilTime + ", waited for " + (sGetMilliTime() - waitFromTime) + " ms");
						break;
					}
				} else if (mTransferMaxBytes > 0) {
					if (bytesTransferredInThisThread >= mTransferMaxBytes) {
						sSetLatestSpeedForExternalMonitor((long) (super.getTotalTransferBytes( )/(timeElapsed / 1000000.0)), "Upload5b, mTransferMaxBytes=" + mTransferMaxBytes);
						SKLogger.d(TAG(this), "loop - break 5b");
						break;
					}
				}
			}
			//SKLogger.d(TAG(this), "loop - continue 6 - actualBytesTransferred = " + actuallyTransferredBytes);
		}//for(;;)


		//
		// To reach here, the (blocking) test is finished.
		//
		//SKLogger.d(TAG(this), "loop - ended continue7");


		// Ask the thread to stop... and wait for it to stop!
		// Note that in the event of an error when transferring data, we'll have
		// already requested it to stop; however, it is fine to call doStop as many times as you want.

		// Has the server *already* finished?
		if (readThread != null) {
			if (readThread.getIsCancelled() == false) {												/* For some reason thread has not been completed */
				readThread.doStop();

				// Once the read thread has completed, send our best known result.
				while (bReadThreadIsRunning == true) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						SKLogger.sAssert(getClass(), false);
					}
				}
			} 
		}

		int bytesPerSecondFinalMeasurement = getSpeedBytesPerSecond();

		sSetLatestSpeedForExternalMonitor(bytesPerSecondFinalMeasurement, "UploadEnd");
		SKLogger.d(TAG(this), "Stopping thread 'UPLOAD TO SERVER' (" + this.getThreadIndex() +") at" + (new java.text.SimpleDateFormat("HH:mm:ss.SSS")).format(new java.util.Date()) );//TODO remove in production
		return true;
	}
	@Override
	public long getWarmUpTimeMicro(){
		if (threadsCount.get() > 0) {
			return (long)(timeWarmUpTotal.doubleValue() / threadsCount.get());
		}
		return 0;
	}
	@Override
	public long getTransferTimeMicro(){
		if (threadsCount.get() > 0) {
			return (long)(timeTransferTotal.doubleValue() / threadsCount.get());
		}
		return 0;
	}
	@Override
	public long getTotalWarmUpBytes(){
		if (threadsCount.get() > 0) {
			return (long)(bytesWarmUpTotal.doubleValue() / threadsCount.get());
		}
		return 0;
	}
	@Override	
	public long getTotalTransferBytes(){
		if (threadsCount.get() > 0) {
			return (long)(bytesTransferTotal.doubleValue() / threadsCount.get());
		}
		return 0;

		//addTotalTransferBytes(transferBytes);
		//addTotalWarmUpBytes(warmUpBytes);

		//setTransferTimeMicro((long)(transfernSecTime / 1000.0));
		//setWarmUpTimeMicro((long)(  warmupnSecTime   / 1000.0));
	}
	@Override	
	public int getSpeedBytesPerSecond() {
		int res = 0;

		if (threadsCount.get() > 0) {
			long total = bytesPerSecondTotal.get();
			double theResult = total / threadsCount.get();

			SKLogger.d(TAG(this), "DEBUG: getSpeedBytesPerSecond, using SERVER value (result/thread count=" + threadsCount.get() + ") = " + (int)theResult);
			return (int) theResult;
		}
		return res;
	}
	@Override
	public void run() {
		if ( setReadThread() == 0) 
			runUp();
	}

}
