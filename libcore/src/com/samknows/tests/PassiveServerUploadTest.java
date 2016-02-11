package com.samknows.tests;

import android.os.Debug;
import android.util.Log;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

import com.samknows.libcore.SKLogger;

public class PassiveServerUploadTest extends UploadTest {

  private PassiveServerUploadTest(List<Param> params) {
    super(params);
  }

  static public PassiveServerUploadTest sCreatePassiveServerUploadTest(List<Param> params) {
    return new PassiveServerUploadTest(params);
  }

  private String formPostHeaderRequestString(int threadIndex) {
    StringBuilder sb = new StringBuilder();

    sb.append("POST /?UPTESTV1=" + threadIndex + " HTTP/1.1\r\n");
    sb.append("Host: ");
    sb.append(getTarget() + ":" + getPort() + "\r\n");
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

  private Integer getBytesPerSecond(boolean isWarmup) {
    if (isWarmup) {
      // If warmup mode is active
      return getWarmupBytesPerSecond();
    } else {
      // If transmission mode is active
      return getTransferBytesPerSecond();
    }
  }

  private boolean getTransmissionDone(boolean isWarmup) {
    if (getShouldCancel()) {
      if (Debug.isDebuggerConnected()) {
        Log.d("DEBUG", "Upload - getTransmissionDone - cancel test!");
      }
      return true;
    }

    if (isWarmup) {
      return isWarmupDone(buff.length);
    } else {
      return isTransferDone(buff.length);
    }
  }

  private boolean transmit(Socket socket, int threadIndex, boolean isWarmup) {

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

        /*
        // Note that long delays (of approximately 0.5 to 1.5 seconds) can occcur quite often in the call to write.
        long startTimeWrite = System.currentTimeMillis();
        */
        try {
          connOut.write(buff);
        } catch (SocketTimeoutException e) {
          if (getIgnoreSocketTimeout()) {
            if (Debug.isDebuggerConnected()) {
              Log.d("DEBUG", "Upload ignore socket timeout on write");
            }
          } else {
            if (Debug.isDebuggerConnected()) {
              Log.d("DEBUG", "Upload socket timeout!! on write");
            }
            throw e;
          }
        }
        /*
        long endTimeWrite = System.currentTimeMillis();

        long startTimeFlush = System.currentTimeMillis();
        */
        try {
          connOut.flush();
        } catch (SocketTimeoutException e) {
          if (getIgnoreSocketTimeout()) {
            if (Debug.isDebuggerConnected()) {
              Log.d("DEBUG", "Upload ignore socket timeout on flush");
            }
          } else {
            if (Debug.isDebuggerConnected()) {
              Log.d("DEBUG", "Upload socket timeout!! on flush");
            }
            throw e;
          }
        }
        //SKLogger.d(this, "transmit() called flush()! ... thread:" + threadIndex);

        /*
        long endTimeFlush = System.currentTimeMillis();

        if ((endTimeWrite-startTimeWrite) > 100) {
          //if (BuildConfig.DEBUG) {
          if (Debug.isDebuggerConnected()) {
            Log.d("DEBUG", "Upload write warning - endTimeWrite-startTimeWrite=" + (endTimeWrite - startTimeWrite));
          }
        }
        if ((endTimeFlush-startTimeFlush) > 100) {
          //if (BuildConfig.DEBUG) {
          if (Debug.isDebuggerConnected()) {
            Log.d("DEBUG", "Upload write warning - endTimeFlush-startTimeFlush=" + (endTimeFlush - startTimeFlush));
          }
        }
        */

        if (getBytesPerSecond(isWarmup) >= 0) {
          // -1 would mean no result found (as not enough time yet spent measuring)
          sSetLatestSpeedForExternalMonitorInterval(extMonitorUpdateInterval, "runUp1Normal", getBytesPerSecond(isWarmup));
        }

        //// DEBUG TESTING!
        //throw new SocketException();
        // break; // DEBUG force effective error, just one buffer!

        //SKLogger.e(TAG(this), "DEBUG: speed in bytes per second" + getSpeedBytesPerSecond() + "<<<");
        //SKLogger.e(TAG(this), "DEBUG: isTransferDone=" + isTransferDone + ", totalTransferBytesSent=>>>" + getTotalTransferBytes() + ", time" + (sGetMicroTime() - start) + "<<<");
      } while (!getTransmissionDone(isWarmup));

    } catch (Exception e) {
      SKLogger.e(this, "Exception in setting up output stream, exiting... thread: " + threadIndex, e);

      // EXCEPTION: RECORD ERROR, AND SET BYTES TO 0!!!
      resetTotalTransferBytesToZero();
      getError().set(true);

      // Verify thta we've set everything to zero properly!
      SKLogger.sAssert(getTotalTransferBytes() == 0L);
      try {
        SKLogger.sAssert(getBytesPerSecond(isWarmup) == 0);
      } catch (Exception e1) {
        SKLogger.sAssert(false);
      }
      int bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
      SKLogger.sAssert(bytesPerSecondMeasurement == 0);

      sSetLatestSpeedForExternalMonitorInterval(extMonitorUpdateInterval, "runUp1Err", getBytesPerSecond(isWarmup));
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
      getError().set(true);

      // Verify thta we've set everything to zero properly!
      SKLogger.sAssert(getTotalTransferBytes() == 0L);
      try {
        SKLogger.sAssert(getBytesPerSecond(isWarmup) == 0);
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

    return true;
  }

  @Override
  protected boolean warmup(Socket socket, int threadIndex) {
    //SKLogger.d(this, "PassiveServerUploadTest, warmup()... thread: " + threadIndex);

    boolean isWarmup = true;
    boolean result = false;

    result = transmit(socket, threadIndex, isWarmup);

    if (getError().get()) {
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