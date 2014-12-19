package com.samknows.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Locale;

import android.util.Log;

import com.samknows.libcore.SKLogger;

public final class DownloadTest extends HttpTest {
	private String[] formValuesArr(){
		String[] values = new String[1];			
		values[0] = String.format("%.2f", (getSpeedBytesPerSecond() * 8d / 1000000));

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
	private void downloadTest(int threadIndex) {

		OutputStream connOut = null;
		InputStream connIn = null;
		byte[] buff = new byte[downloadBufferSize];
		int readBytes = 0;

		Socket socket = getSocket();
	
		if (socket != null) {

			try {
				int receiveBufferSizeBytes = socket.getReceiveBufferSize();
				Log.d(TAG(this), "HttpTest: download: receiveBufferSizeBytes=" + receiveBufferSizeBytes);
			} catch (SocketException e1) {
				SKLogger.sAssert(getClass(),  false);
			}

			try {
				connOut = socket.getOutputStream();
				connIn = socket.getInputStream();
				PrintWriter writerOut = new PrintWriter(connOut, false);
				writerOut.print(getHeaderRequest());
				writerOut.flush();
				int httpResponse = readResponse(connIn);
				if (httpResponse != HTTPOK) {
					setErrorIfEmpty("Http response received: " + httpResponse);
					error.set(true);
				}
			} catch (IOException io) {
				error.set(true);
				SKLogger.sAssert(getClass(),  false);
			}
		} else {
			error.set(true);
			SKLogger.sAssert(getClass(),  false);
		}
		//waitForAllConnections();
		if (error.get()) {
			closeConnection(socket, connIn, connOut);
			SKLogger.sAssert(getClass(),  false);
			return;
		}
		// warmup, can be based on time constraint or data usage constraint
		do {
			try {
				readBytes = connIn.read(buff, 0, buff.length);
			} catch (IOException io) {
				readBytes = BYTESREADERR;
				error.set(true);
				SKLogger.sAssert(getClass(), false);
			}
		} while (!isWarmupDone(readBytes)); // Download warmup...

		if (error.get()) {
			closeConnection(socket, connIn, connOut);
			SKLogger.sAssert(getClass(),  false);
			return;
		}
		do {
			try {
				readBytes = connIn.read(buff, 0, buff.length);
			} catch (IOException io) {
				readBytes = BYTESREADERR;
				SKLogger.sAssert(getClass(),  false);
			}

			sSetLatestSpeedForExternalMonitor(getSpeedBytesPerSecond(),"Download");

		} while (!isTransferDone(readBytes));

		closeConnection(socket, connIn, connOut);

	}

	@Override
	int getSpeedBytesPerSecond() {
		// If we have a figure from the upload server - then return the best average.
		// Otherwise, return a value calculated by the client!

		int bytesPerSecond = 0;
		if (getTransferTimeMicro() != 0) {
			double transferTimeSeconds = ((double) getTransferTimeMicro()) / 1000000.0;
			
			bytesPerSecond = (int) (((double)getTotalTransferBytes()) / transferTimeSeconds);
		}

		//Log.d(TAG(this), "DEBUG: getSpeedBytesPerSecond, using CLIENT value = " + bytesPerSecondFromClient);
		return bytesPerSecond;
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
	public void run() {
		int threadIndex = getThreadIndex();
		downloadTest(threadIndex);
	}
	@Override
	public String getHumanReadableResult() {
		String ret = "";
		String direction = "download";
		String type = getThreadsNum() == 1 ? "single connection" : "multiple connection";
		if (testStatus.equals("FAIL")) {
			ret = String.format("The %s has failed.", direction);
		} else {
			ret = String.format(Locale.UK, "The %s %s test achieved %.2f Mbps.", type,	direction, (getSpeedBytesPerSecond() * 8d / 1000000));
		}
		return ret;
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
