package com.samknows.tests;

import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

import com.samknows.libcore.SKLogger;

public class PassiveServerUploadTest extends UploadTest {

	protected boolean runUp(){
		//
		// Send the ACTUAL TEST data!
		//

		boolean isTransferDone = false;
		//long waitUntilTime = Long.MAX_VALUE;
		//long waitFromTime = Long.MAX_VALUE;


		long actuallyTransferredBytes = 0L;											/* bytes transferred by this thread */

		long start = sGetMicroTime();												/* Memorize time when process starts */

		OutputStream connOut = getOutputStream();
		for (;;) {//do while suits us better???
			try {
				if (connOut == null) 
					break;

				connOut.write(buff);												/* Write buffer to output socket */
				connOut.flush();

				actuallyTransferredBytes += buff.length;

			} catch (IOException ioe) {
				SKLogger.sAssert(getClass(), false);
				error.set(true);
				// And break out of the loop....
				sSetLatestSpeedForExternalMonitor(getSpeedBytesPerSecond(), "Upload3");
				Log.d(TAG(this), "loop - break 3");
				break;
			}

			sSetLatestSpeedForExternalMonitor(getSpeedBytesPerSecond(), "Upload3b");

			if (isTransferDone == false) 
				isTransferDone = isTransferDone(uploadBufferSize);

			//Log.d(TAG(this), "DEBUG: speed in bytes per second" + getSpeedBytesPerSecond() + "<<<");
			//Log.d(TAG(this), "DEBUG: isTransferDone=" + isTransferDone + ", totalTransferBytesSent=>>>" + totalTransferBytesSent.get() + ", time" + (sGetMicroTime() - start) + "<<<");

			if (isTransferDone == true) {
				//Log.d(TAG(this), "loop - break as isTransferDone is true");
				break;
			}
		}


		//
		// To reach here, the (blocking) test is finished.
		//
		//Log.d(TAG(this), "loop - ended continue7");


		// Ask the thread to stop... and wait for it to stop!
		// Note that in the event of an error when transferring data, we'll have
		// already requested it to stop; however, it is fine to call doStop as many times as you want.


		// Close the connection / tidy-up once the thread has finished?
		// No - the thread does this itself.
		// closeConnection(socket, connIn, connOut);

		int bytesPerSecondMeasurement = getSpeedBytesPerSecond();
		Log.d(TAG(this), "Best result is from the BUILT-IN MEASUREMENT, bytesPerSecondMeasurement=" + bytesPerSecondMeasurement);

		//if (bGotValidResponseFromServer == true) {
			// BEST RESULT is from the SERVER!
			//Log.d(TAG(this), "Best result is from the SERVER, bytesPerSecondMeasurement=" + bytesPerSecondMeasurement);
			// TODO! [self doSendtodDidCompleteTransferOperation:0 transferBytes:0 totalBytes:0 ForceThisBitsPerSecondFromServer:bitrateMpbs1024Based threadId:threadId];
		//} else {
			//Log.d(TAG(this), "Best result is from the BUILT-IN MEASUREMENT, bytesPerSecondMeasurement=" + bytesPerSecondMeasurement);
			// Best result is from the built-in measurement.
			// TODO! [self doSendtodDidCompleteTransferOperation:transferTimeMicroseconds transferBytes:transferBytes totalBytes:totalBytes ForceThisBitsPerSecondFromServer:-1.0  threadId:threadId];
		//}

		sSetLatestSpeedForExternalMonitor(bytesPerSecondMeasurement, "UploadEnd");

		return true;
	}
	protected boolean warmUp(){
		return super.warmUp();
	}

	public PassiveServerUploadTest( ) {
		super();

		Log.d(TAG(this), "DEBUG: app does not use server-based upload speed testing...");  /*TODO remove in production*/
		//bGotValidResponseFromServer = false;
		bReadThreadIsRunning = false;

		super.init();										/*TODO : check error state*/
		//super.requestHeader(1);
	}

	public int getSpeedBytesPerSecond() {
		// If we have a figure from the upload server - then return the best average.
		// Otherwise, return a value calculated by the client!

		int bytesPerSecondFromClient = 0;
		if ( getTransferTimeMicro() != 0) {
			double transferTimeSeconds = ((double) getTransferTimeMicro()) / 1000000.0;
			
			bytesPerSecondFromClient = (int) (((double)getTotalTransferBytes()) / transferTimeSeconds);
		}

		// Log.d(TAG(this), "DEBUG: getSpeedBytesPerSecond, using CLIENT value = " + bytesPerSecondFromClient);
		return bytesPerSecondFromClient;
	}

	
	@Override
	public void run() {
		if( warmUp() )										/* warmUp completed successfully */
			runUp();				
	}
}