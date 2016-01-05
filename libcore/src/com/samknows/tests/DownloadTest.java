package com.samknows.tests;

import android.support.v4.BuildConfig;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import com.samknows.libcore.SKLogger;

public final class DownloadTest extends HttpTest {
	private byte[] buff = new byte[downloadBufferSize];
	private int readBytes = 0;

	public DownloadTest(List<Param> params){
		super(_DOWNSTREAM, params);
	}

	private String[] formValuesArr(){
		String[] values = new String[1];			
		values[0] = String.format("%.2f", (getTransferBytesPerSecond() * 8d / 1000000));

		return values;
	}
	private String getHeaderRequest() {
		String request = "GET /%s HTTP/1.1\r\nHost: %s \r\nACCEPT: */*\r\n\r\n";
		return String.format(request, file, target);
	}
	private int readResponse(InputStream is) {									// Reads the http response and returns the http status code
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
					SKLogger.sAssert(getClass(), false);
				}
			}

			while ((line = reader.readLine()) != null) {
				if (line.length() == 0) {
					break;
				}
			}
		} catch (IOException IOe) {
			SKLogger.sAssert(getClass(),  false);
			setErrorIfEmpty("IOexception while reading http header: ", IOe);
			ret = 0;
		} catch (NumberFormatException nfe) {
			SKLogger.sAssert(getClass(),  false);
			setErrorIfEmpty("Error in converting the http code: ", nfe);
			ret = 0;
		} catch (Exception e) {
			SKLogger.sAssert(getClass(),  false);
			setErrorIfEmpty("Error in converting the http code: ", e);
			ret = 0;
		}
		return ret;
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

	private boolean getTransmissionDone(boolean isWarmup, int readBytes) {
		if (getShouldCancel()) {
			Log.d("REMOVE ME", "Download - getTransmissionDone - cancel test!");
			return true;
		}

		if (isWarmup) {
			return isWarmupDone(readBytes);
		} else {
			return isTransferDone(readBytes);
		}
	}

	private boolean transmit(Socket socket, int threadIndex, boolean isWarmup){
//		Callable<Integer> bytesPerSecond = null;																			/* Generic method returning the current average speed across all thread  since thread started */
//		Callable<Boolean> transmissionDone = null;																			/* Generic method returning the transmission state */

		if (isWarmup){																										/* If warmup mode is active */

			sendHeaderRequest( socket );																					/* Send download request is the part of the warm up process */
			if (error.get()) {																								/* Error relates to sendHeader procedure */
				SKLogger.sAssert(getClass(),  false);
				return false;
			}
		}

		InputStream connIn = getInput(socket);

		if ( connIn == null) {
			closeConnection(socket);
			SKLogger.sAssert(getClass(),  false);
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

				SKLogger.sAssert(readBytes > 0);

        if (getBytesPerSecond(isWarmup) >= 0) {
          // -1 would mean no result found (as not enough time yet spent measuring)
          sSetLatestSpeedForExternalMonitorInterval(extMonitorUpdateInterval, "Download", getBytesPerSecond(isWarmup));
        }
			} while (!getTransmissionDone(isWarmup, readBytes));
    } catch (SocketTimeoutException e) {
      // This happens so often - that we just log it (but only when debugger in use)
      //if (OtherUtils.isDebuggable(SKApplication.getAppInstance()))
      if (BuildConfig.DEBUG) {
        Log.e("DownloadTest", e.getMessage());
      }
      readBytes = BYTESREADERR;
      //SKLogger.sAssert(getClass(),  false);
      return false;
		} catch (IOException io) {
			readBytes = BYTESREADERR;
			SKLogger.sAssert(getClass(),  false);
			return false;
		} catch (Exception io) {
			readBytes = BYTESREADERR;	
			SKLogger.sAssert(getClass(),  false);
			return false;
		}
		return true;
	}
		
	private void sendHeaderRequest(Socket socket) {
		InputStream connIn = getInput(socket);
		OutputStream connOut = getOutput(socket);

		if ( connOut == null || connIn == null) {
			closeConnection(socket);
			SKLogger.sAssert(getClass(),  false);
			error.set(true);
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
				error.set(true);
			}
		} catch (Exception io) {
			error.set(true);
			SKLogger.sAssert(getClass(),  false);
		}
	}
	
	@Override
	protected boolean transfer(Socket socket, int threadIndex) {
		boolean isWarmup = false;
		return transmit(socket, threadIndex, isWarmup);	
	}
	
	@Override
	protected boolean warmup(Socket socket, int threadIndex) {
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
	public String getHumanReadableResult() {
		String ret = "";
		String direction = "download";
		String type = getThreadsNum() == 1 ? "single connection" : "multiple connection";
		if (testStatus.equals("FAIL")) {
			ret = String.format("The %s has failed.", direction);
		} else {
			ret = String.format(Locale.UK, "The %s %s test achieved %.2f Mbps.", type,	direction, (Math.max(0,getTransferBytesPerSecond()) * 8d / 1000000));
		}
		return ret;
	}
	@Override
	public HashMap<String, String> getResults(){
		HashMap<String, String> ret = new HashMap<String, String>();
		if (!testStatus.equals("FAIL")) {
			String[] values = formValuesArr();
			ret.put("downspeed", values[0]);
		}
		return ret;		
	}
	@Override
	public int getNetUsage() {
		// TODO Auto-generated method stub
		return 0;
	}
}


/*	@Override
public String getResultsAsString(){							 New Human readable implementation 
	if (testStatus.equals("FAIL")){
		return "";
	}else{
		String[] values = formValuesArr();			
		return String.format(Locale.UK, values[0]);
	}		
}*/
/*@Override
public String getResults(String locale){			 New Human readable implementation 
	if (testStatus.equals("FAIL")){
		return locale;
	}else{
		String[] values = formValuesArr();			
		return String.format(locale, values[0]);
	}		
}*/



//private void closeConnection(Socket s, InputStream i, OutputStream o) {
//SKLogger.e(TAG(this), "closeConnection...");//TODO remove in production 
//
//if(i != null){
//	try {
//		i.close();
//	} catch (IOException ioe) {
//		SKLogger.sAssert(getClass(),  false);
//	}
//}
//
//if(o != null){
//	try {
//		o.close();
//	} catch (IOException ioe) {
//		SKLogger.sAssert(getClass(),  false);
//	}
//}
//
//if(s != null){
//	try {
//		s.close();
//	} catch (IOException ioe) {
//		SKLogger.sAssert(getClass(),  false);
//	}
//}
//}




//private void downloadTest(int threadIndex) {
//
//	OutputStream connOut = null;
//	InputStream connIn = null;
//	byte[] buff = new byte[downloadBufferSize];
//	int readBytes = 0;
//
//	Socket socket = getSocket();
//
//	if (socket != null) {
//
//		try {
//			int receiveBufferSizeBytes = socket.getReceiveBufferSize();
//			SKLogger.e(TAG(this), "HttpTest: download: receiveBufferSizeBytes=" + receiveBufferSizeBytes);
//		} catch (SocketException e1) {
//			SKLogger.sAssert(getClass(),  false);
//		}
//
//		try {
//			connOut = socket.getOutputStream();
//			connIn = socket.getInputStream();
//			PrintWriter writerOut = new PrintWriter(connOut, false);
//			writerOut.print(getHeaderRequest());
//			writerOut.flush();
//			int httpResponse = readResponse(connIn);
//			if (httpResponse != HTTPOK) {
//				setErrorIfEmpty("Http response received: " + httpResponse);
//				error.set(true);
//			}
//		} catch (IOException io) {
//			error.set(true);
//			SKLogger.sAssert(getClass(),  false);
//		}
//	} else {
//		error.set(true);
//		SKLogger.sAssert(getClass(),  false);
//	}
//	//waitForAllConnections();
//	if (error.get()) {
//		closeConnection(socket, connIn, connOut);
//		SKLogger.sAssert(getClass(),  false);
//		return;
//	}
//	
//	
//
//	closeConnection(socket, connIn, connOut);
//
//}
