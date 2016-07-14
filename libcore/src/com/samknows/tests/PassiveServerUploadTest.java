package com.samknows.tests;

import android.os.Debug;
import android.util.Log;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.List;

import com.samknows.libcore.SKPorting;

public class PassiveServerUploadTest extends UploadTest {

  private PassiveServerUploadTest(List<Param> params) {
    super(params);
  }

  static public PassiveServerUploadTest sCreatePassiveServerUploadTest(List<Param> params) {
    return new PassiveServerUploadTest(params);
  }

  @Override
  public void runBlockingTestToFinishInThisThread() {
    super.runBlockingTestToFinishInThisThread();
  }

  private String formPostHeaderRequestString(int threadIndex) {
    StringBuilder sb = new StringBuilder();

    sb.append("POST /?UPTESTV1=").append(threadIndex).append(" HTTP/1.1\r\n");
    sb.append("Host: ");
    sb.append(getTarget()).append(":").append(getPort()).append("\r\n");
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
      SKPorting.sAssert(false);
    }

    return data;
  }

  private Double getBytesPerSecond(boolean isWarmup) {
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

  private boolean transmit(ISKHttpSocket socket, int threadIndex, boolean isWarmup) {

    // Access output stream
    OutputStream connOut = getOutput(socket);

    if (connOut == null) {
      closeConnection(socket);
      SKPorting.sAssertE(this, "Error in setting up output stream, exiting... thread: " + threadIndex);
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

        Double bytesPerSecond = getBytesPerSecond(isWarmup);
        if (bytesPerSecond >= 0.0) {
          // -1 would mean no result found (as not enough time yet spent measuring)
          sSetLatestSpeedForExternalMonitorInterval(extMonitorUpdateInterval, "runUp1Normal", bytesPerSecond);
        }

        //// DEBUG TESTING!
        //throw new SocketException();
        // break; // DEBUG force effective error, just one buffer!

        //SKLogger.e(TAG(this), "DEBUG: speed in bytes per second" + getSpeedBytesPerSecond() + "<<<");
        //SKLogger.e(TAG(this), "DEBUG: isTransferDone=" + isTransferDone + ", totalTransferBytesSent=>>>" + getTotalTransferBytes() + ", time" + (sGetMicroTime() - start) + "<<<");
      } while (!getTransmissionDone(isWarmup));

    } catch (Exception e) {
      SKPorting.sAssertE(this, "Exception in setting up output stream, exiting... thread: " + threadIndex, e);

      // EXCEPTION: RECORD ERROR, AND SET BYTES TO 0!!!
      resetTotalTransferBytesToZero();
      getError().set(true);

      // Verify thta we've set everything to zero properly!
      SKPorting.sAssert(getTotalTransferBytes() == 0L);
      try {
        SKPorting.sAssert(getBytesPerSecond(isWarmup) == 0);
      } catch (Exception e1) {
        SKPorting.sAssert(false);
      }
      Double bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
      SKPorting.sAssert(bytesPerSecondMeasurement == 0);

      sSetLatestSpeedForExternalMonitorInterval(extMonitorUpdateInterval, "runUp1Err", getBytesPerSecond(isWarmup));
      //SKLogger.e(TAG(this), "loop - break 3");
      return false;
    }

    //
    // If only 1 buffer "SENT": treat this as an error...
    //
    long btsTotal = getTotalTransferBytes();
    if (btsTotal == buff.length) {
      // ONLY 1 BUFFER "SENT": TREAT THIS AS AN ERROR, AND SET BYTES TO 0!!!
      SKPorting.sAssertE(this, "Only one buffer sent - treat this as an upload failure");
      resetTotalTransferBytesToZero();
      getError().set(true);

      // Verify thta we've set everything to zero properly!
      SKPorting.sAssert(getTotalTransferBytes() == 0L);
      try {
        SKPorting.sAssert(getBytesPerSecond(isWarmup) == 0);
      } catch (Exception e1) {
        SKPorting.sAssert(false);
      }
      Double bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
      SKPorting.sAssert(bytesPerSecondMeasurement == 0);
      return false;
    }

    //
    // To get here, the test ran OK!
    //
    Double bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
    SKPorting.sAssert(bytesPerSecondMeasurement >= 0);

    // Do NOT send this, as it otherwise affects ALL thread test potentially?
    // It turns out that if this is *not* sent, then the app UI can keep spinning-through showing upload
    // speed data rather than Latency speed data!
    sSetLatestSpeedForExternalMonitor(bytesPerSecondMeasurement, cReasonUploadEnd);											/* Final external interface set up */

    return true;
  }

  @Override
  protected boolean warmup(ISKHttpSocket socket, int threadIndex) {
    //SKLogger.d(this, "PassiveServerUploadTest, warmup()... thread: " + threadIndex);

    boolean isWarmup = true;
    boolean result = false;

    result = transmit(socket, threadIndex, isWarmup);

    if (getError().get()) {
    	// Warm up might have set a global error
      //SKLogger.e(TAG(this), "WarmUp Exits: Result FALSE, totalWarmUpBytes=>>> " + getTotalWarmUpBytes());
      return false;
    }
    return result;
  }

  @Override
  protected boolean transfer(ISKHttpSocket socket, int threadIndex) {
    //SKLogger.d(this, "PassiveServerUploadTest, transfer()... thread: " + threadIndex);

    boolean isWarmup = false;
    return transmit(socket, threadIndex, isWarmup);
  }
}