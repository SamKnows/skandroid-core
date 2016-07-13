package com.samknows.tests;

// NOTE: This code is written as Pure Java.
// It is possible to modify it to have Android-specific calls.
// Look at the static methods at the top of the class, for the commented-out Android-specific
// code that can be re-enabled if required in HttpTest.java

//import android.os.Debug;
//import android.util.Log;

import com.samknows.libcore.SKCommon;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.List;

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
      //sDoAssert(data.length <= intoHere.length);
      //System.arraycopy(data,0,intoHere,0,data.length);
    } catch (UnsupportedEncodingException e) {
      SKCommon.sDoAssert(false);
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
      if (SKCommon.sIsDebuggerConnected()) {
        SKCommon.sDoLogD("DEBUG", "Upload - getTransmissionDone - cancel test!");
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
      SKCommon.sDoLogE("PassiveServerUploadTest", "Error in setting up output stream, exiting... thread: " + threadIndex);
      return false;
    }

    try {
      byte headerByteArray[] = getPostHeaderRequestStringAsByteArray(threadIndex);
      if (headerByteArray.length > 0) {
        //sDoLogD(this, "transmit() header write() ... thread:" + threadIndex);
        connOut.write(headerByteArray);
        //sDoLogD(this, "transmit() header flush() ... thread:" + threadIndex);
        connOut.flush();
        //sDoLogD(this, "transmit() header flush()! ... thread:" + threadIndex);
      }

      do {
        if (connOut == null) {
          break;
        }

        // Write buffer to output socket
        //sDoLogD(this, "transmit() calling write() ... thread:" + threadIndex);

        /*
        // Note that long delays (of approximately 0.5 to 1.5 seconds) can occcur quite often in the call to write.
        long startTimeWrite = System.currentTimeMillis();
        */
        try {
          connOut.write(buff);
        } catch (SocketTimeoutException e) {
          if (getIgnoreSocketTimeout()) {
            if (SKCommon.sIsDebuggerConnected()) {
              SKCommon.sDoLogD("DEBUG", "Upload ignore socket timeout on write");
            }
          } else {
            if (SKCommon.sIsDebuggerConnected()) {
              SKCommon.sDoLogD("DEBUG", "Upload socket timeout!! on write");
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
            if (SKCommon.sIsDebuggerConnected()) {
              SKCommon.sDoLogD("DEBUG", "Upload ignore socket timeout on flush");
            }
          } else {
            if (SKCommon.sIsDebuggerConnected()) {
              SKCommon.sDoLogD("DEBUG", "Upload socket timeout!! on flush");
            }
            throw e;
          }
        }
        //sDoLogD(this, "transmit() called flush()! ... thread:" + threadIndex);

        /*
        long endTimeFlush = System.currentTimeMillis();

        if ((endTimeWrite-startTimeWrite) > 100) {
          //if (BuildConfig.DEBUG) {
          if (Debug.isDebuggerConnected()) {
            sDoLogD("DEBUG", "Upload write warning - endTimeWrite-startTimeWrite=" + (endTimeWrite - startTimeWrite));
          }
        }
        if ((endTimeFlush-startTimeFlush) > 100) {
          //if (BuildConfig.DEBUG) {
          if (Debug.isDebuggerConnected()) {
            sDoLogD("DEBUG", "Upload write warning - endTimeFlush-startTimeFlush=" + (endTimeFlush - startTimeFlush));
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

        //sDoLogE(TAG(this), "DEBUG: speed in bytes per second" + getSpeedBytesPerSecond() + "<<<");
        //sDoLogE(TAG(this), "DEBUG: isTransferDone=" + isTransferDone + ", totalTransferBytesSent=>>>" + getTotalTransferBytes() + ", time" + (sGetMicroTime() - start) + "<<<");
      } while (!getTransmissionDone(isWarmup));

    } catch (Exception e) {
      SKCommon.sDoLogE("PassiveServerUploadTest", "Exception in setting up output stream, exiting... thread: " + threadIndex, e);

      // EXCEPTION: RECORD ERROR, AND SET BYTES TO 0!!!
      resetTotalTransferBytesToZero();
      getError().set(true);

      // Verify thta we've set everything to zero properly!
      SKCommon.sDoAssert(getTotalTransferBytes() == 0L);
      try {
        SKCommon.sDoAssert(getBytesPerSecond(isWarmup) == 0);
      } catch (Exception e1) {
        SKCommon.sDoAssert(false);
      }
      Double bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
      SKCommon.sDoAssert(bytesPerSecondMeasurement == 0);

      sSetLatestSpeedForExternalMonitorInterval(extMonitorUpdateInterval, "runUp1Err", getBytesPerSecond(isWarmup));
      //sDoLogE(TAG(this), "loop - break 3");
      return false;
    }

    //
    // If only 1 buffer "SENT": treat this as an error...
    //
    long btsTotal = getTotalTransferBytes();
    if (btsTotal == buff.length) {
      // ONLY 1 BUFFER "SENT": TREAT THIS AS AN ERROR, AND SET BYTES TO 0!!!
      SKCommon.sDoLogE("PassiveServerUploadTest", "Only one buffer sent - treat this as an upload failure");
      resetTotalTransferBytesToZero();
      getError().set(true);

      // Verify thta we've set everything to zero properly!
      SKCommon.sDoAssert(getTotalTransferBytes() == 0L);
      try {
        SKCommon.sDoAssert(getBytesPerSecond(isWarmup) == 0);
      } catch (Exception e1) {
        SKCommon.sDoAssert(false);
      }
      Double bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
      SKCommon.sDoAssert(bytesPerSecondMeasurement == 0);
      return false;
    }

    //
    // To get here, the test ran OK!
    //
    Double bytesPerSecondMeasurement = Math.max(0, getTransferBytesPerSecond());
    SKCommon.sDoAssert(bytesPerSecondMeasurement >= 0);

    // Do NOT send this, as it otherwise affects ALL thread test potentially?
    // It turns out that if this is *not* sent, then the app UI can keep spinning-through showing upload
    // speed data rather than Latency speed data!
    sSetLatestSpeedForExternalMonitor(bytesPerSecondMeasurement, cReasonUploadEnd);											/* Final external interface set up */

    return true;
  }

  @Override
  protected boolean warmup(ISKHttpSocket socket, int threadIndex) {
    //sDoLogD(this, "PassiveServerUploadTest, warmup()... thread: " + threadIndex);

    boolean isWarmup = true;
    boolean result = false;

    result = transmit(socket, threadIndex, isWarmup);

    if (getError().get()) {
    	// Warm up might have set a global error
      //sDoLogE(TAG(this), "WarmUp Exits: Result FALSE, totalWarmUpBytes=>>> " + getTotalWarmUpBytes());
      return false;
    }
    return result;
  }

  @Override
  protected boolean transfer(ISKHttpSocket socket, int threadIndex) {
    //sDoLogD(this, "PassiveServerUploadTest, transfer()... thread: " + threadIndex);

    boolean isWarmup = false;
    return transmit(socket, threadIndex, isWarmup);
  }
}