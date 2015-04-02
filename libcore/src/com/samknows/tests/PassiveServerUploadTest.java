package com.samknows.tests;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.Callable;

import android.support.v4.BuildConfig;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.activity.components.Util;
import com.samknows.measurement.util.OtherUtils;

public class PassiveServerUploadTest extends UploadTest {

  public PassiveServerUploadTest(List<Param> params) {
    super(params);
  }

  private String formPostHeaderRequestString(int threadIndex) {
    StringBuilder sb = new StringBuilder();

    sb.append("POST /?UPTESTV1=" + threadIndex + " HTTP/1.1\r\n");
    sb.append("Host: ");
    sb.append(target + ":" + port + "\r\n");
    sb.append("User-Agent: SamKnows HTTP Client 1.1(2)\r\n");
    sb.append("Accept: */*\r\n");
    sb.append("Content-Length: 4294967295\r\n");
    sb.append("Content-Type: application/octet-stream\r\n");
    sb.append("Expect: 100-continue\r\n");
    sb.append("\r\n");

    return sb.toString();
  }

  private byte[] getPostHeaderRequestStringAsByteArray(int threadIndex) {
    String theString = formPostHeaderRequestString(threadIndex);
    byte data[] = new byte[0];
    try {
      data = theString.getBytes("UTF-8");
      //SKLogger.sAssert(data.length <= intoHere.length);
      //System.arraycopy(data,0,intoHere,0,data.length);
    } catch (UnsupportedEncodingException e) {
      SKLogger.sAssert(false);
    }

    return data;
  }

  private boolean transmit(Socket socket, int threadIndex, boolean isWarmup) {

    //SKLogger.d(this, "PassiveServerUploadTest, transmit(), isWarmup=" + isWarmup + ", thread: " + threadIndex);
//    InputStream connIn = null;
//
//    try {
//      connIn = getInput(socket);															/* Get input stream */
//    } catch (Exception e) {
//      SKLogger.e(this, "Failed to create connIn");
//      connIn = null;
//    }

    Callable<Integer> bytesPerSecond = null;																			/* Generic method returning the current average speed across all thread  since thread started */
    Callable<Boolean> transmissionDone = null;																			/* Generic method returning the transmission state */

    if (isWarmup) {
    	// If warmup mode is active
      bytesPerSecond = new Callable<Integer>() {
        public Integer call() {
          return getWarmupBytesPerSecond();
        }
      };
      transmissionDone = new Callable<Boolean>() {
        public Boolean call() {
          return isWarmupDone(buff.length);
        }
      };
    } else {
    	// If transmission mode is active
      bytesPerSecond = new Callable<Integer>() {
        public Integer call() {
          return getTransferBytesPerSecond();
        }
      };
      transmissionDone = new Callable<Boolean>() {
        public Boolean call() {
          return isTransferDone(buff.length);
        }
      };
    }

    // Access output stream
    OutputStream connOut = getOutput(socket);

    if (connOut == null) {
      closeConnection(socket);
      SKLogger.e(this, "Error in setting up output stream, exiting... thread: " + threadIndex);
      return false;
    }

    try {
      byte headerByteArray[] = getPostHeaderRequestStringAsByteArray(threadIndex);
      if (headerByteArray.length > 0) {
        //SKLogger.d(this, "transmit() header write() ... thread:" + threadIndex);
        connOut.write(headerByteArray);
        //SKLogger.d(this, "transmit() header flush() ... thread:" + threadIndex);
        connOut.flush();
        //SKLogger.d(this, "transmit() header flush()! ... thread:" + threadIndex);
      }

      do {
        if (connOut == null) {
          break;
        }

        // Write buffer to output socket
        //SKLogger.d(this, "transmit() calling write() ... thread:" + threadIndex);
        connOut.write(buff);
        //SKLogger.d(this, "transmit() calling flush() ... thread:" + threadIndex);
        connOut.flush();
        //SKLogger.d(this, "transmit() called flush()! ... thread:" + threadIndex);

        if (bytesPerSecond.call() >= 0) {
          // -1 would mean no result found (as not enough time yet spent measuring)
          sSetLatestSpeedForExternalMonitorInterval(extMonitorUpdateInterval, "runUp1Normal", bytesPerSecond);
        }

        //// DEBUG TESTING!
        //throw new SocketException();
        // break; // DEBUG force effective error, just one buffer!

        //SKLogger.e(TAG(this), "DEBUG: speed in bytes per second" + getSpeedBytesPerSecond() + "<<<");
        //SKLogger.e(TAG(this), "DEBUG: isTransferDone=" + isTransferDone + ", totalTransferBytesSent=>>>" + getTotalTransferBytes() + ", time" + (sGetMicroTime() - start) + "<<<");
      } while (!transmissionDone.call());

    } catch (Exception e) {
      SKLogger.e(this, "Exception in setting up output stream, exiting... thread: " + threadIndex, e);

      // EXCEPTION: RECORD ERROR, AND SET BYTES TO 0!!!
      resetTotalTransferBytesToZero();
      error.set(true);

      // Verify thta we've set everything to zero properly!
      SKLogger.sAssert(getTotalTransferBytes() == 0L);
      try {
        SKLogger.sAssert(bytesPerSecond.call() == 0);
      } catch (Exception e1) {
        SKLogger.sAssert(false);
      }
      int bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
      SKLogger.sAssert(bytesPerSecondMeasurement == 0);

      sSetLatestSpeedForExternalMonitorInterval(extMonitorUpdateInterval, "runUp1Err", bytesPerSecond);
      //SKLogger.e(TAG(this), "loop - break 3");//haha
      return false;
    }

    //
    // If only 1 buffer "SENT": treat this as an error...
    //
    long btsTotal = getTotalTransferBytes();
    if (btsTotal == buff.length) {
      // ONLY 1 BUFFER "SENT": TREAT THIS AS AN ERROR, AND SET BYTES TO 0!!!
      SKLogger.e(this, "Only one buffer sent - treat this as an upload failure");
      resetTotalTransferBytesToZero();
      error.set(true);

      // Verify thta we've set everything to zero properly!
      SKLogger.sAssert(getTotalTransferBytes() == 0L);
      try {
        SKLogger.sAssert(bytesPerSecond.call() == 0);
      } catch (Exception e1) {
        SKLogger.sAssert(false);
      }
      int bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
      SKLogger.sAssert(bytesPerSecondMeasurement == 0);
      return false;
    }

    //
    // To get here, the test ran OK!
    //
    int bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
    SKLogger.sAssert(bytesPerSecondMeasurement >= 0);
    //hahaSKLogger.e(TAG(this), "Result is from the BUILT-IN MEASUREMENT, bytesPerSecondMeasurement= " + bytesPerSecondMeasurement + " thread: " + threadIndex);

    sSetLatestSpeedForExternalMonitor(bytesPerSecondMeasurement, cReasonUploadEnd);											/* Final external interface set up */

//    if (connIn != null) {
//      try {
//        connIn.close();
//      } catch (Exception e) {
//        SKLogger.sAssert(false);
//      }
//    }

    return true;
  }

  @Override
  protected boolean warmup(Socket socket, int threadIndex) {
    //SKLogger.d(this, "PassiveServerUploadTest, warmup()... thread: " + threadIndex);

    boolean isWarmup = true;
    boolean result = false;

    result = transmit(socket, threadIndex, isWarmup);

    if (error.get()) {
    	// Warm up might have set a global error
      //SKLogger.e(TAG(this), "WarmUp Exits: Result FALSE, totalWarmUpBytes=>>> " + getTotalWarmUpBytes());//haha remove in production
      return false;
    }
    return result;
  }

  @Override
  protected boolean transfer(Socket socket, int threadIndex) {
    //SKLogger.d(this, "PassiveServerUploadTest, transfer()... thread: " + threadIndex);

    boolean isWarmup = false;
    return transmit(socket, threadIndex, isWarmup);
  }
}