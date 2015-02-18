package com.samknows.tests;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Callable;

import com.samknows.libcore.SKLogger;

public class PassiveServerUploadTest extends UploadTest {

	public PassiveServerUploadTest(List<Param> params){
		super(params);
	}
	
	private boolean transmit(Socket socket, int threadIndex, boolean isWarmup){
		Callable<Integer> bytesPerSecond = null;																			/* Generic method returning the current average speed across all thread  since thread started */
		Callable<Boolean> transmissionDone = null;																			/* Generic method returning the transmission state */

		if (isWarmup){																										/* If warmup mode is active */
			bytesPerSecond = new Callable<Integer>(){ public Integer call() { return getWarmupBytesPerSecond();} };
			transmissionDone = new Callable<Boolean>(){ public Boolean call() { return isWarmupDone(buff.length);} };
		}
		else{																												/* If transmission mode is active */
			bytesPerSecond = new Callable<Integer>(){ public Integer call() { return getTransferBytesPerSecond();} };
			transmissionDone = new Callable<Boolean>(){ public Boolean call() { return isTransferDone(buff.length);} };
		}

		OutputStream connOut = getOutput(socket);																			/* Access output stream */

		if ( connOut == null) {
			closeConnection(socket);
			SKLogger.sAssert(getClass(),  false);
			SKLogger.e(TAG(this), "Error in setting up output stream, exiting... thread: " + threadIndex);
			return false;
		}

		try {
			do {
				if (connOut == null) 
					break;

				connOut.write(buff);																						/* Write buffer to output socket */
				connOut.flush();

				sSetLatestSpeedForExternalMonitorInterval( extMonitorUpdateInterval, "runUp1Normal", bytesPerSecond );

				//SKLogger.e(TAG(this), "DEBUG: speed in bytes per second" + getSpeedBytesPerSecond() + "<<<");
				//SKLogger.e(TAG(this), "DEBUG: isTransferDone=" + isTransferDone + ", totalTransferBytesSent=>>>" + getTotalTransferBytes() + ", time" + (sGetMicroTime() - start) + "<<<");
			}while(!transmissionDone.call());

		}catch(Exception e){
			SKLogger.sAssert(getClass(), false);
			sSetLatestSpeedForExternalMonitorInterval( extMonitorUpdateInterval, "runUp1Err", bytesPerSecond);
			//SKLogger.e(TAG(this), "loop - break 3");//haha
			return false;
		}

		int bytesPerSecondMeasurement = getTransferBytesPerSecond();
		//hahaSKLogger.e(TAG(this), "Result is from the BUILT-IN MEASUREMENT, bytesPerSecondMeasurement= " + bytesPerSecondMeasurement + " thread: " + threadIndex);

		sSetLatestSpeedForExternalMonitor(bytesPerSecondMeasurement, "UploadEnd");											/* Final external interface set up */
		return true;
	}
	
	@Override
	protected boolean  warmup(Socket socket, int threadIndex){
		boolean isWarmup = true;
		boolean result = false;

		result = transmit(socket, threadIndex, isWarmup);

		if (error.get()) {										/* Warm up might have set a global error */		 
			//SKLogger.e(TAG(this), "WarmUp Exits: Result FALSE, totalWarmUpBytes=>>> " + getTotalWarmUpBytes());//haha remove in production
			return false;
		}
		return result;
	}

	@Override
	protected boolean transfer(Socket socket, int threadIndex){
		boolean isWarmup = false;
		return transmit(socket, threadIndex, isWarmup);
	}
}