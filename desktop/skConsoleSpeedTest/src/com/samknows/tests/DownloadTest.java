package com.samknows.tests;

// NOTE: This code is written as Pure Java.
// It is possible to modify it to have Android-specific calls.
// Look at the static methods at the top of the class, for the commented-out Android-specific
// code that can be re-enabled if required.

import com.samknows.libcore.SKPorting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.util.List;

public final class DownloadTest extends HttpTest {

  //
  // END: code that you can make behave differently if you so wish on Android...
  //

  private byte[] buff = new byte[getDownloadBufferSize()];
  private int readBytes = 0;

  private DownloadTest(List<Param> params) {
    super(_DOWNSTREAM, params);
  }

  public static DownloadTest sCreateDownloadTest(List<Param> params) {

    DownloadTest result = new DownloadTest(params);

    if (result != null) {
      if (result.isReady()) {
        return result;
      } else {
        SKPorting.sAssert(false);
        return null;
      }
    } else {
      SKPorting.sAssert(false);
    }

    return result;
  }

  private String[] formValuesArr() {
    String[] values = new String[1];
    values[0] = String.format("%.2f", (getTransferBytesPerSecond() * 8d / 1000000));

    return values;
  }

  private String getHeaderRequest() {
    String request = "GET /%s HTTP/1.1\r\nHost: %s \r\nACCEPT: */*\r\n\r\n";
    return String.format(request, getFile(), getTarget());
  }

  private int readResponse(InputStream is) {                  // Reads the http response and returns the http status code
    int ret = 0;
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String line = reader.readLine();
      if (line != null && line.length() != 0) {
        // Should be of the form:
        //   HTTP/1.1 200 OK
        String[] words = line.split(" ");
        if (words.length >= 2) {
          ret = Integer.parseInt(words[1]);
        } else {
          SKPorting.sAssert(getClass(), false);
        }
      }

      while ((line = reader.readLine()) != null) {
        if (line.length() == 0) {
          break;
        }
      }
    } catch (IOException IOe) {
      SKPorting.sAssert(getClass(), false);
      setErrorIfEmpty("IOexception while reading http header: ", IOe);
      ret = 0;
    } catch (NumberFormatException nfe) {
      SKPorting.sAssert(getClass(), false);
      setErrorIfEmpty("Error in converting the http code: ", nfe);
      ret = 0;
    } catch (Exception e) {
      SKPorting.sAssert(getClass(), false);
      setErrorIfEmpty("Error in converting the http code: ", e);
      ret = 0;
    }
    return ret;
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

  private boolean getTransmissionDone(boolean isWarmup, int readBytes) {
    if (getShouldCancel()) {
      if (SKPorting.sIsDebuggerConnected()) {
        SKPorting.sLogD("DEBUG", "Download - getTransmissionDone - cancel test!");
      }
      return true;
    }

    if (isWarmup) {
      return isWarmupDone(readBytes);
    } else {
      return isTransferDone(readBytes);
    }
  }

  private boolean transmit(ISKHttpSocket socket, int threadIndex, boolean isWarmup) {
//		Callable<Integer> bytesPerSecond = null;																			/* Generic method returning the current average speed across all thread  since thread started */
//		Callable<Boolean> transmissionDone = null;																			/* Generic method returning the transmission state */

    if (isWarmup) {																										/* If warmup mode is active */

      sendHeaderRequest(socket);																					/* Send download request is the part of the warm up process */
      if (getError().get()) {																								/* Error relates to sendHeader procedure */
        SKPorting.sAssert(getClass(), false);
        return false;
      }
    }

    InputStream connIn = getInput(socket);

    if (connIn == null) {
      closeConnection(socket);
      SKPorting.sAssert(getClass(), false);
      //hahaSKLogger.e(TAG(this), "Error in setting up input stream, exiting... thread: " + this.getThreadIndex());
      return false;
    }

    try {
      do {
        readBytes = connIn.read(buff, 0, buff.length);

        if (readBytes == -1) {
          // This can happen if we reach the end of the stream more quickly than expected...
          readBytes = 0;
        }

        SKPorting.sAssert(readBytes > 0);

        if (getBytesPerSecond(isWarmup) >= 0) {
          // -1 would mean no result found (as not enough time yet spent measuring)
          sSetLatestSpeedForExternalMonitorInterval(extMonitorUpdateInterval, "Download", getBytesPerSecond(isWarmup));
        }
      } while (!getTransmissionDone(isWarmup, readBytes));
    } catch (SocketTimeoutException e) {
      // This happens so often - that we just log it (but only when debugger in use)
      //if (OtherUtils.isDebuggable(SKApplication.getAppInstance()))
      if (SKPorting.sIsDebug()) {
        SKPorting.sLogE("DownloadTest", e.getMessage());
      }
      readBytes = BYTESREADERR;
      //sAssert(getClass(),  false);
      return false;
    } catch (IOException io) {
      readBytes = BYTESREADERR;
      SKPorting.sAssert(getClass(), false);
      return false;
    } catch (Exception io) {
      readBytes = BYTESREADERR;
      SKPorting.sAssert(getClass(), false);
      return false;
    }
    return true;
  }

  private void sendHeaderRequest(ISKHttpSocket socket) {
    InputStream connIn = getInput(socket);
    OutputStream connOut = getOutput(socket);

    if (connOut == null || connIn == null) {
      closeConnection(socket);
      SKPorting.sAssert(getClass(), false);
      getError().set(true);
      //hahaSKLogger.e(TAG(this), "Error in setting up output stream, exiting... thread: " + getThreadIndex());
      return;
    }

    try {
      PrintWriter writerOut = new PrintWriter(connOut, false);
      writerOut.print(getHeaderRequest());
      writerOut.flush();
      int httpResponse = readResponse(connIn);
      if (httpResponse != HTTPOK) {
        setErrorIfEmpty("Http response received: " + httpResponse);
        getError().set(true);
      }
    } catch (Exception io) {
      getError().set(true);
      SKPorting.sAssert(getClass(), false);
    }
  }

  @Override
  protected boolean transfer(ISKHttpSocket socket, int threadIndex) {
    boolean isWarmup = false;
    return transmit(socket, threadIndex, isWarmup);
  }

  @Override
  protected boolean warmup(ISKHttpSocket socket, int threadIndex) {
    boolean isWarmup = true;
    return transmit(socket, threadIndex, isWarmup);
  }

  @Override
  public String getStringID() {
    String ret = "";

    if (getThreadsNum() == 1) {
      ret = DOWNSTREAMSINGLE;
    } else {
      ret = DOWNSTREAMMULTI;
    }
    return ret;
  }

  @Override
  public int getNetUsage() {
    return (int) (getTotalTransferBytes() + getTotalWarmUpBytes());
  }
}
