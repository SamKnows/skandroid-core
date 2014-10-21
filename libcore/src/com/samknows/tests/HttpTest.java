package com.samknows.tests;

//import android.annotation.SuppressLint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.util.OtherUtils;
import com.samknows.measurement.util.SKDateFormat;

/*
NOTES: See also https://svn.samknows.com/svn/tests/http_server/trunk/docs/protocol.txt ... where the protocol
for the new service is defined.
 
create socket
connect
 
 ** Start by trying to do this in JUST ONE THREAD! **
 
boolean bQuit = false
 
 Thread 1:
 write header :
    Example header as follows:
*/

//  POST /?CONTROL=1&UNITID=1&SESSIONID=281541010&NUM_CONNECTIONS=2&CONNECTION=1&AGGREGATE_WARMUP=0&RESULTS_INTERVAL_PERIOD=10&RESULT_NUM_INTERVALS=1&TEST_DATA_CAP=4294967295&TRANSFER_MAX_SIZE=4294967295&WARMUP_SAMPLE_TIME=5000&NUM_WARMUP_SAMPLES=1&MAX_WARMUP_SIZE=4294967295&MAX_WARMUP_SAMPLES=1&WARMUP_FAIL_ON_MAX=0&WARMUP_TOLERANCE=5 HTTP/1.1
//  Host: n1-the1.samknows.com:6500
//  Accept: */*
//  Content-Length: 4294967295
//  Content-Type: application/x-www-form-urlencoded
//  Expect: 100-continue

/*
 }
 
 The server expects a query string with a "CONTROL" field. It's value should be set to "1.0", which specifies the version of the protocol.
 
 - UNITID
 Mandatory.
 
 - SESSIONID
 Mandatory.
 
 - NUM_CONNECTIONS
 Mandatory.
 
 - CONNECTION
 Mandatory.
 
 - WARMUP_SAMPLE_TIME
 Mandatory for POST requests.
 
 - NUM_WARMUP_SAMPLES
 Mandatory for POST requests.
 
 - MAX_WARMUP_SAMPLES
 Mandatory for POST requests.
 
 - WARMUP_TOLERANCE
 Mandatory for POST requests.
 
 - RESULTS_INTERVAL_PERIOD
 Mandatory for POST requests.
 
 - RESULT_NUM_INTERVALS
 Mandatory for POST requests.
 
 - AGGREGATE_WARMUP
 Optional. Default: false.
 
 - TEST_DATA_CAP
 Optional. Default: no limit.
 
 - TRANSFER_MAX_SIZE
 Optional. Default: no limit.
 
 - MAX_WARMUP_SIZE
 Optional. Default: no limit.
 
 - WARMUP_FAIL_ON_MAX
 Optional. Default: false.
 
 - TRACE_INTERVAL
 Optional. Default: no tracing.
 
 - TCP_CONG
 Optional. Default: server system default.
 
   The values must be sent to the server (e.g TEST_DATA_CAP).
   Once the server reaches the limits it will send the result up to that point.
   This makes the limit "maximum bytes to *RECEIVE*".
   To keep it as "maximum bytes to *SENT*" you could keep your own counter
   and close the connection when you have sent a fixed amount of bytes...
 }
 while (bQuit == false) {
   write(socket, buffer...)
 }
 
 Thread 2:
 while (bQuit == false) {
   if (read (socket, intobuffer, timeout)) succeeds:
   {
     Extract upload speed results from server response, which will be in this format:
SAMKNOWS_HTTP_REPLY\n
VERSION: <major>.<minor>\n
RESULT: <OK|FAIL>\n
END_TIME: <end time>
SECTION: WARMUP\n
NUM_WARMUP: <num>\n
WARMUP_SESSION <seconds> <nanoseconds> <bytes>\n
WARMUP_SESSION <seconds> <nanoseconds> <bytes>\n
SECTION: MEASUR\n
NUM_MEASUR: <num>\n
MEASUR_SESSION <seconds> <nanoseconds> <bytes>\n
MEASUR_SESSION <seconds> <nanoseconds> <bytes>\n
 
     In terms of sample data etc., the server will give you something like this:
...
NUM_MEASUR: 2\n
MEASUR_SESSION 5 0 5000000\n
MEASUR_SESSION 10 0 15000000\n
...
     ... It's the client job to calculate that during the first five seconds the speed has been 5,000,000 / 5 = 1,000,000 bytes/sec; and during the next 10 seconds 15,000,000 / 10 = 1,500,000 bytes/sec. Meaning that the speed between seconds 5 and 10 has been (15,000,000 - 5,000,000) / (10 - 5) = 2,000,000 bytes/sec.
     ... send this "final upload speed result value from the server" to the application.
     bQuit = true;
   }
 }
*/

public class HttpTest extends Test {
	
	static Random sRandom = new Random();
	
	static final String TAG = HttpTest.class.getSimpleName();

	// This is used for upload tests.
	private long mSESSIONID_ForServerUploadTest = -1L;
		
	private class MyHttpReadThread extends Thread {

		// @property (weak) SKTransferOperation *mpParentTransferOperation;
		// @property int mSocketFd;

		public Socket mSocket = null;
		public InputStream mConnIn = null;
		public boolean mbIsCancelled = false;
		
		public void doStop() {
			mbIsCancelled = true;
		}
		
		public boolean getIsCancelled() {
			return mbIsCancelled;
		}


		public MyHttpReadThread(Socket inSocket, InputStream inConnIn) {
			super();
			
			mSocket = inSocket;
			mConnIn = inConnIn;
			//  self.mpParentTransferOperation = inSKTransferOperation;
			//  self.mSocketFd = inSocketFd;
			//  self.mCallOnStopOrCancel = inCallOnStopOrCancel;
		}

		@Override
		public void run() {

			byte[] buffer = new byte[4000];

			String response = new String();
			int responseCode = 0;

			for (;;) {
				if (mbIsCancelled == true) {
					Log.w(getClass().getName(), "mbIsCancelled=true, stop the read thread");
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
						response = response + bufferAsUtf8String;

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
										Log.w(getClass().getName(), "Error in response, code " + responseCode);
										break;
									}
								}
							}

							// Have we got everything we need yet?
							if (response.contains("SAMKNOWS_HTTP_REPLY")) {
								// Got the header!
								if (response.contains("MEASUR_SESSION")) {
									// Assume we have the lot!
									Log.w(getClass().getName(), "Got MEASUR_SESSION");
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


				// Continue the for ... loop!
				// Give other threads a chance, otherwise we're locked a hard loop...
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					SKLogger.sAssert(getClass(), false);
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

	/*
	 * Http Status codes
	 */
	private static final int HTTPOK = 200;
	private static final int HTTPCONTINUE = 100;

	/*
	 * error codes and contraints
	 */
	private static final int BYTESREADERR = -1;
	public static final int MAXNTHREADS = 100;

	/*
	 * Messages regarding the status of the test
	 */
	private static final String HTTPGETRUN = "Running download test";
	private static final String HTTPGETDONE = "Download test completed";
	private static final String HTTPPOSTRUN = "Running upload test";
	private static final String HTTPPOSTDONE = "Upload completed";

	/*
	 * Test strings
	 */
	public static final String DOWNSTREAMSINGLE = "JHTTPGET";
	public static final String DOWNSTREAMMULTI = "JHTTPGETMT";
	public static final String UPSTREAMSINGLE = "JHTTPPOST";
	public static final String UPSTREAMMULTI = "JHTTPPOSTMT";
	/*
	 * Parameters name for the setParameter function
	 */
	private static final String DOWNSTREAM = "downstream";
	private static final String UPSTREAM = "upstream";

	public static final String JSON_TRANFERTIME = "transfer_time";
	public static final String JSON_TRANFERBYTES = "transfer_bytes";
	public static final String JSON_BYTES_SEC = "bytes_sec";
	public static final String JSON_WARMUPTIME = "warmup_time";
	public static final String JSON_WARMUPBYTES = "warmup_bytes";
	public static final String JSON_NUMBER_OF_THREADS = "number_of_threads";
	
//	private class DebugTiming {
//		public String description;
//		public int threadIndex;
//		public Long time;
//		public int currentSpeed;
//		
//		public DebugTiming(String description, int threadIndex, Long time, int currentSpeed) {
//			super();
//			this.description = description;
//			this.threadIndex = threadIndex;
//			this.time = time;
//			this.currentSpeed = currentSpeed;
//		}
//	}

	// For debug timings!
//	private static ArrayList<DebugTiming> smDebugSocketSendTimeMicroseconds = new ArrayList<DebugTiming>();
	
	private	Thread[] mThreads = null;
	
	/*
	 * Time in microseconds
	 */
	private static long sGetMicroTime() {
		return System.nanoTime() / 1000L;
	}

	private static long sGetMilliTime() {
		return System.nanoTime() / 1000000L;
	}
	
	private void doInitialize() {
	    // Generate this value in case we need it.
	    // It is a random value from [0...2^32-1]
    	mSESSIONID_ForServerUploadTest = sRandom.nextLong() & 0xffffffffL;
		SKLogger.sAssert(getClass(), mSESSIONID_ForServerUploadTest >= 0);

		// static values that need resetting!
		sServerUploadBytesPerSecond.clear();
		sSetLatestSpeedForExternalMonitor(0);
	}

	public HttpTest() {
		doInitialize();
	}

	public HttpTest(boolean downstream) {
		this.downstream = downstream;
		
		doInitialize();
	}

	public HttpTest(String direction) {
		setDirection(direction);
		
		doInitialize();
	}

	public String getStringID() {
		String ret = "";
		if (downstream) {
			if (nThreads == 1) {
				ret = DOWNSTREAMSINGLE;
			} else {
				ret = DOWNSTREAMMULTI;
			}
		} else {
			if (nThreads == 1) {
				ret = UPSTREAMSINGLE;
			} else {
				ret = UPSTREAMMULTI;
			}
		}
		return ret;
	}

	/*
	 * 
	 */
	@Override
	public int getNetUsage() {
		return transferBytesAcrossAllTestThreads + warmupBytesAcrossAllTestThreads;
	}

	// @SuppressLint("NewApi")
	@Override
	public boolean isReady() {
		if (target.length() == 0) {
			setError("Target empty");
			return false;
		}
		if (port == 0) {
			setError("Port is zero");
			return false;
		}
		if (warmupMaxTime == 0 && warmupMaxBytes == 0) {
			setError("No warmup parameter defined");
			return false;
		}
		if (transferMaxTime == 0 && transferMaxBytes == 0) {
			setError("No transfer parameter defined");
			return false;
		}
		if (!downstream && (sendDataChunkSize == 0 || postDataLength == 0)) {
			setError("Upload parameter missing");
			return false;
		}
		if (downstream && bufferSize == 0) {
			setError("Buffer size missing for download");
			return false;
		}
		if (nThreads < 1 && nThreads > MAXNTHREADS) {
			setError("Number of threads error, current is: " + nThreads
					+ ". Min " + 1 + " Max " + MAXNTHREADS + ".");
			return false;
		}
		return true;
	}

	@Override
	public boolean isSuccessful() {
		return testStatus.equals("OK");
	}

	private String getHeaderRequest() {
		String request = "GET /%s HTTP/1.1\r\nHost: %s \r\nACCEPT: */*\r\n\r\n";
		return String.format(request, file, target);
	}

	private String getPostHeaderRequestStringForUploadTest(int numThreads, int threadIndex, int transferMaxBytes, double warmupMaxTimeMicro, int warmupMaxBytes) {
		StringBuilder sb = new StringBuilder();
	
		// Verify that the session_id was properly initialized (it will have come from a random value, shared by all threads
		// for this HttpTest instance...)
		SKLogger.sAssert(getClass(), mSESSIONID_ForServerUploadTest >= 0);
		
		// Use the correct parameters in the header... INCLUDING THE UNIT ID!
		// c.f. instructions at the top of this file.
		sb.append("POST /?CONTROL=1&UNITID=1");
		sb.append("&SESSIONID=");
		sb.append(mSESSIONID_ForServerUploadTest);
		sb.append("&NUM_CONNECTIONS=");
		sb.append(numThreads);
        sb.append("&CONNECTION=");
		sb.append(threadIndex);
        sb.append("&AGGREGATE_WARMUP=0&RESULTS_INTERVAL_PERIOD=10&RESULT_NUM_INTERVALS=1&TEST_DATA_CAP=4294967295");
        sb.append("&TRANSFER_MAX_SIZE=");
		sb.append(transferMaxBytes);
        sb.append("&WARMUP_SAMPLE_TIME=");
        // The system will reject a header with "WARMUP_SAMPLE_TIME=0".
        // If that happens, set WARMUP_SAMPLE_TIME to UINT32_MAX instead of zero.
        //long millisecondsWarmupSampleTime = (long)(warmupMaxTimeMicro/1000.0);
        long millisecondsWarmupSampleTime = (long)(warmupMaxTime/1000.0);
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
		sb.append(warmupMaxBytes);
        sb.append("&MAX_WARMUP_SAMPLES=1&WARMUP_FAIL_ON_MAX=0&WARMUP_TOLERANCE=5 HTTP/1.1\r\n");
		
		sb.append("Host: ");
		sb.append(this.target + ":" + this.port);
		sb.append("\r\n");
		sb.append("Accept: */*\r\n");
		sb.append("Content-Length: 4294967295\r\n");
		sb.append("Content-Type: application/x-www-form-urlencoded\r\n");
		sb.append("Expect: 100-continue\r\n");
		sb.append("\r\n");
		
		String result = sb.toString();
		return result;
	}

	public int getSendBufferSize() {
		return sendBufferSize;
	}

	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}
	
	@Override
	public void execute() {
//     	smDebugSocketSendTimeMicroseconds.clear();
		
		Context context = SKApplication.getAppInstance().getBaseContext();
	
		if (downstream) {
			infoString = HTTPGETRUN;
		} else {
			infoString = HTTPPOSTRUN;
		}
		start();
		mThreads = new Thread[nThreads];
		for (int i = 0; i < nThreads; i++) {
			mThreads[i] = new Thread(this);
		}
		for (int i = 0; i < nThreads; i++) {
			mThreads[i].start();
		}
		try {
			for (int i = 0; i < nThreads; i++) {
				mThreads[i].join();
			}
		} catch (Exception e) {
			setErrorIfEmpty("Thread join exception: ", e);
			SKLogger.sAssert(getClass(),  false);
			testStatus = "FAIL";
		}

		if (downstream) {
			infoString = HTTPGETDONE;
		} else {
			infoString = HTTPPOSTDONE;
		}

		output();

		finish();
		
//		// Debug - dump timings
//		synchronized (HttpTest.class) {
//			for (DebugTiming value : smDebugSocketSendTimeMicroseconds) {
//				Log.d("HttpTest DUMP", "HttpTest DUMP - threadIndex:" + value.threadIndex + " description:"+ value.description + " time:" + value.time + " microsec speed:" + value.currentSpeed);
//			}
//			smDebugSocketSendTimeMicroseconds.clear();
//		}
		
	}

	public String getInfo() {
		return infoString;
	}

	private Socket getSocket() {
		Socket ret = null;
		try {
			InetSocketAddress sockAddr = new InetSocketAddress(target, port);
			ipAddress = sockAddr.getAddress().getHostAddress();
			ret = new Socket();
			ret.setTcpNoDelay(noDelay);

			if (0 != desiredReceiveBufferSize) {
				ret.setReceiveBufferSize(desiredReceiveBufferSize);
			}
			receiveBufferSize = ret.getReceiveBufferSize();

			// Experimentation shows a *much* better settling-down on upload speed,
			// if we force a 32K send buffer size in bytes, rather than relying
			// on the default send buffer size.

			// When forcing value in bytes, you must actually divide by two!
			// https://code.google.com/p/android/issues/detail?id=13898
			// desiredSendBufferSize = 32768 / 2; // (2 ^ 15) / 2
			if (0 != desiredSendBufferSize) {
				ret.setSendBufferSize(desiredSendBufferSize);
			}
			sendBufferSize = ret.getSendBufferSize();

			if (downstream) {
				ret.setSoTimeout(Test.READTIMEOUT);
			} else {
				ret.setSoTimeout(100); // Just 100 ms while polling server for upload test response!
			}

			ret.connect(sockAddr, Test.CONNECTIONTIMEOUT); // // 10 seconds connection timeout
		} catch (Exception e) {
			SKLogger.sAssert(getClass(),  false);
			ret = null;
		}
		return ret;
	}

//	private SocketAddress InetSocketAddress(String target2, int port2) {
//		SKLogger.sAssert(getClass(),  false);
//		return null;
//	}
	
	static ArrayList<Double> sServerUploadBytesPerSecond = new ArrayList<Double>();

	// Bytes per second
	public int getSpeedBytesPerSecond() {
		
		// If we have a figure from the upload server - then return the best average.
		// Otherwise, return a value calculated by the client!
	
		int bytesPerSecondFromClient = 0;
		if (transferTimeMicroseconds != 0) {
			double transferTimeSeconds = ((double) transferTimeMicroseconds) / 1000000.0;
			bytesPerSecondFromClient = (int) (((double)transferBytesAcrossAllTestThreads) / transferTimeSeconds);
			// Log.w(TAG, "DEBUG: getSpeedBytesPerSecond, candidate client value = " + bytesPerSecondFromClient);
		}
		
		synchronized (HttpTest.class) {

			if (sServerUploadBytesPerSecond.size() > 0) {
				double total = 0.0;
				for (Double theValue : sServerUploadBytesPerSecond) {
					total += theValue.doubleValue();
				}
				
				double theResult = total / ((double) sServerUploadBytesPerSecond.size());
				
				// Log.w(TAG, "DEBUG: getSpeedBytesPerSecond, using SERVER value (result/thread count=" + sServerUploadBytesPerSecond.size() + ") = " + theResult);
				
				return (int) theResult;
			}
		}
		
		// Log.w(TAG, "DEBUG: getSpeedBytesPerSecond, using CLIENT value = " + bytesPerSecondFromClient);
		
		return bytesPerSecondFromClient;
	}

	public synchronized int getTransferBytes() {
		return warmupBytesAcrossAllTestThreads + transferBytesAcrossAllTestThreads;
	}

	public synchronized int getWarmupBytes() {
		return warmupBytesAcrossAllTestThreads;
	}

	public String getHumanReadableResult() {
		String ret = "";
		String direction = downstream ? "download" : "upload";
		String type = nThreads == 1 ? "single connection" : "multiple connection";
		if (testStatus.equals("FAIL")) {
			ret = String.format("The %s has failed.", direction);
		} else {
			ret = String.format("The %s %s test achieved %.2f Mbps.", type,
					direction, (getSpeedBytesPerSecond() * 8d / 1000000));
		}
		return ret;
	}

	@Override
	public HumanReadable getHumanReadable() {
		HumanReadable ret = new HumanReadable();
		if (downstream) {
			if (testStatus.equals("FAIL")) {
				ret.testString = TEST_STRING.DOWNLOAD_FAILED;
			} else if (nThreads == 1) {
				ret.testString = TEST_STRING.DOWNLOAD_SINGLE_SUCCESS;
				ret.values = new String[1];
				ret.values[0] = String.format("%.2f",
						(getSpeedBytesPerSecond() * 8d / 1000000));
			} else {
				ret.testString = TEST_STRING.DOWNLOAD_MULTI_SUCCESS;
				ret.values = new String[1];
				ret.values[0] = String.format("%.2f",
						(getSpeedBytesPerSecond() * 8d / 1000000));
			}
		} else {
			if (testStatus.equals("FAIL")) {
				ret.testString = TEST_STRING.UPLOAD_FAILED;
			} else if (nThreads == 1) {
				ret.testString = TEST_STRING.UPLOAD_SINGLE_SUCCESS;
				ret.values = new String[1];
				ret.values[0] = String.format("%.2f",
						(getSpeedBytesPerSecond() * 8d / 1000000));
			} else {
				ret.testString = TEST_STRING.UPLOAD_MULTI_SUCCESS;
				ret.values = new String[1];
				ret.values[0] = String.format("%.2f",
						(getSpeedBytesPerSecond() * 8d / 1000000));
			}
		}
		return ret;
	}

	private void output() {
		ArrayList<String> o = new ArrayList<String>();
		Map<String, Object> output = new HashMap<String, Object>();
		// string id
		o.add(getStringID());
		output.put(Test.JSON_TYPE, getStringID());
		// time
		long time_stamp = unixTimeStamp();
		o.add(time_stamp + "");
		output.put(Test.JSON_TIMESTAMP, time_stamp);
		output.put(Test.JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(time_stamp*1000)));
		// status
		if (error.get()) {
			o.add("FAIL");
			output.put(Test.JSON_SUCCESS, false);
		} else {
			o.add("OK");
			output.put(Test.JSON_SUCCESS, true);
		}
		// target
		o.add(target);
		output.put(Test.JSON_TARGET, target);
		// target ip address
		o.add(ipAddress);
		output.put(Test.JSON_TARGET_IPADDRESS, ipAddress);
		// transfer time
		o.add(Long.toString(transferTimeMicroseconds));
		output.put(JSON_TRANFERTIME, transferTimeMicroseconds);
		// transfer bytes
		o.add(Integer.toString(transferBytesAcrossAllTestThreads));
		output.put(JSON_TRANFERBYTES, transferBytesAcrossAllTestThreads);
		// byets_sec
		o.add(Integer.toString(getSpeedBytesPerSecond()));
		output.put(JSON_BYTES_SEC, getSpeedBytesPerSecond());
		// warmup time
		o.add(Long.toString(warmupTime));
		output.put(JSON_WARMUPTIME, warmupTime);
		// warmup bytes
		o.add(Integer.toString(warmupBytesAcrossAllTestThreads));
		output.put(JSON_WARMUPBYTES, warmupBytesAcrossAllTestThreads);
		// number of threads
		o.add(Integer.toString(nThreads));
		output.put(JSON_NUMBER_OF_THREADS, nThreads);

		setOutput(o.toArray(new String[1]));
		setJSONOutput(output);
	}

	private void downloadTest(int threadIndex) {

		OutputStream connOut = null;
		InputStream connIn = null;
		byte[] buff = new byte[bufferSize];
		int readBytes = 0;
		
		Socket socket = getSocket();
		if (socket != null) {

			try {
				int receiveBufferSizeBytes = socket.getReceiveBufferSize();
				Log.d(getClass().getName(), "HttpTest: download: receiveBufferSizeBytes=" + receiveBufferSizeBytes);
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
		waitForAllConnections();
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
		} while (!isWarmupDone(readBytes));
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
			
			sSetLatestSpeedForExternalMonitor(getSpeedBytesPerSecond());

		} while (!isTransferDone(readBytes));

		closeConnection(socket, connIn, connOut);

	}

	static private int sLatestSpeedForExternalMonitor = 0;
			
	public static int sGetLatestSpeedForExternalMonitor() {
		return sLatestSpeedForExternalMonitor;
	}

	public static void sSetLatestSpeedForExternalMonitor(int bytesPerSecond) {
  	    sLatestSpeedForExternalMonitor = bytesPerSecond;
	}

	synchronized void waitForAllConnections() {
		connectionCounter++;
		if (connectionCounter < nThreads) {
			try {
				wait();
			} catch (InterruptedException e) {
				error.set(true);
				SKLogger.sAssert(getClass(), false);
			}
		} else {
			notifyAll();
		}
	}

	// the warmup ends also if there is a problem on any connection
	synchronized boolean isWarmupDone(int bytes) {
		boolean ret = false;
		boolean timeWarmup = false;
		boolean bytesWarmup = false;
		// if there is an error the test must stop and report it
		if (bytes == BYTESREADERR) {
			setErrorIfEmpty("read error");
			bytes = 0; // do not modify the bytes counters
			error.set(true);
		}

		warmupBytesAcrossAllTestThreads += bytes;
		if (startWarmup == 0) {
			startWarmup = sGetMicroTime();
		}
		warmupTime = sGetMicroTime() - startWarmup;
		// if warmup max time is set and time has exceeded its values set time
		// warmup to true
		timeWarmup = warmupMaxTime > 0 && warmupTime >= warmupMaxTime;

		// if warmup max bytes is set and bytes counter exceeded its value set
		// bytesWarmup to true
		bytesWarmup = warmupMaxBytes > 0 && warmupBytesAcrossAllTestThreads >= warmupMaxBytes;

		// if a condition happened increment the warmupDoneCounter
		if (timeWarmup || bytesWarmup) {
			warmupDoneCounter++;
			ret = true;
		}
		// if there is an error notify all, some thread might be waiting for
		// other to finish the warmup, and exit
		if (error.get()) {
			notifyAll();
			ret = true;

		}// warmup is finished, wait for other threads if any
		else if (ret && warmupDoneCounter < nThreads) {
			startTransfer = sGetMicroTime();
			try {
				wait();
			} catch (InterruptedException ie) {
				error.set(true);
				ret = true;
				notifyAll();
      			SKLogger.sAssert(getClass(), false);
			}
		}// warmup is finished, last thread, notify all
		else if (ret) {
			notifyAll();
		}
		return ret;

	}

	synchronized boolean isTransferDone(int bytes) {
		boolean ret = false;
		// In case an error occurred stop the test
		if (bytes == BYTESREADERR) {
			error.set(true);
			bytes = 0;
		}
		transferBytesAcrossAllTestThreads += bytes;

		// if startTransfer is 0 this is the first call to isTransferDone
		if (startTransfer == 0) {
			startTransfer = sGetMicroTime();
		}
		transferTimeMicroseconds = sGetMicroTime() - startTransfer;
		// if transfermax time is
		if ((transferMaxTime > 0) && (transferTimeMicroseconds > transferMaxTime)) {
			ret = true;
		}
		if ((transferMaxBytes > 0)
				&& (transferBytesAcrossAllTestThreads + warmupBytesAcrossAllTestThreads > transferMaxBytes)) {
			ret = true;
		}
		if (transferBytesAcrossAllTestThreads > 0) {
			testStatus = "OK";
		}

		if (error.get()) {
			ret = true;
		}

		return ret;
	}

	@Override
	public void run() {
	
		int threadIndex = 0;
		
		synchronized (HttpTest.class) {
			
			boolean bFound = false;

			int i;
			for (i = 0; i < mThreads.length; i++) {
				if (Thread.currentThread() == mThreads[i]) {
					threadIndex = i;
					bFound = true;
					break;
				}
			}
			SKLogger.sAssert(getClass(), bFound);
		}

		if (downstream) {
			downloadTest(threadIndex);
		} else {
			uploadTest(threadIndex);
		}
	}

	// Reads the http response and returns the http status code
	private int readResponse(InputStream is) {
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
	
	//
	// Private class that performs the upload test.
	// This calculates upload speed from the client perspective.
	// It also tries to get an upload speed as a response from the server.
	//
	private class CUploadTest {

		Socket socket = null;
		OutputStream connOut = null;
		InputStream connIn = null;
		
		public CUploadTest() {
			super();
		}

		boolean bGotValidResponseFromServer = false;
		boolean bReadThreadIsRunning = true;
		double bitrateMpbs1024Based = -1.0;
			
		private void uploadTestBlocking(int threadIndex) {
			Random generator = new Random();
			byte[] buff = new byte[sendDataChunkSize];

			socket = getSocket();
			if (socket != null) {

				try {
					int sendBufferSizeBytes = socket.getSendBufferSize();
					Log.d(getClass().getName(), "HttpTest: upload: sendBufferSizeBytes=" + sendBufferSizeBytes);
					//socket.setReceiveBufferSize(16000);
					//sendBufferSizeBytes = socket.getReceiveBufferSize();
					//Log.d(getClass().getName(), "HttpTest: upload: sendBufferSizeBytes=" + sendBufferSizeBytes);
				} catch (SocketException e1) {
					SKLogger.sAssert(getClass(),  false);
				}

				try {
					connOut = socket.getOutputStream();
					connIn = socket.getInputStream();

					PrintWriter writerOut = new PrintWriter(connOut, false);

					String postRequestHeaderString = getPostHeaderRequestStringForUploadTest(nThreads, threadIndex, transferMaxBytes, warmupTime, warmupMaxBytes);
					writerOut.print(postRequestHeaderString);
					writerOut.flush();

				} catch (IOException ioe) {
					SKLogger.sAssert(getClass(), false);
					error.set(true);
				}
			} else {
				error.set(true);
         		SKLogger.sAssert(getClass(), false);
			}
			waitForAllConnections();
			if (error.get()) {
				closeConnection(socket, connIn, connOut);
				return;
			}
		
			//
			// Now - decide if we can use server-based upload speed testing, or not!
        	// Note: we only run the new server-side upload speed tests WHERE THE APP SPECFICALLY ALLOWS IT.
			//
			MyHttpReadThread readThread = null;
		
			if (SKApplication.getAppInstance().getDoesAppSupportServerBasedUploadSpeedTesting() == false) {
				// No, we are on an older app, that does not use server-based upload speed testing...
				Log.d("TAG", "DEBUG: app does not use server-based upload speed testing...");
         		bGotValidResponseFromServer = false;
         		bReadThreadIsRunning = false;
			} else {
				// Yes, we can use server-based upload speed testing!
				Log.d("TAG", "DEBUG: app uses server-based upload speed testing...!");

				// Create a read thread, that starts monitor for a response from the server.

				readThread = new MyHttpReadThread(socket, connIn) {

					@Override
					public void callOnStopOrCancel(String responseString, int responseCode) {

						synchronized(HttpTest.this) {
							HttpTest.this.closeConnection(socket,  connIn,  connOut);
							connIn = null;
							connOut = null;
							socket = null;
						}

						if ((responseCode != 100) && (responseCode != 200)) {
							SKLogger.sAssert(getClass(), false);
							// TODO - [self connection:nil didFailWithError:nil];
						} else {
							Log.d("TAG", "DEBUG: reponseCode=" + responseCode + ", responseString=>>>" + responseString + "<<<");

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

							double finalBytesPerSecond = 0.0;
							double finalBytesMilliseconds = 0.0;
							double finalBytes = 0.0;

							String[] items = responseString.split("\n");
							if (items.length == 0) {
								SKLogger.sAssert(getClass(),  false);
							} else {
								int itemCount = items.length;
								int itemIndex;
								for (itemIndex = 0; itemIndex < itemCount; itemIndex++) {
									String item = items[itemIndex];
									// Locate the MEASURE_SESSION items.
									if (item.contains("MEASUR_SESSION")) {
										// Use the final calculated value!
										String[] items2 = item.split(" ");
										if (items2.length != 4) {
											SKLogger.sAssert(getClass(),  false);
										} else {
											double seconds = Double.valueOf(items2[1]);

											if (seconds <= 0) {
												SKLogger.sAssert(getClass(),  false);
											} else {
												bGotValidResponseFromServer = true;

												double bytesThusFar = Double.valueOf(items2[3]);
												SKLogger.sAssert(getClass(), bytesThusFar > 0);

												double bytesThisTime = bytesThusFar; // - bytesAtLastMeasurement;
												SKLogger.sAssert(getClass(), bytesThisTime > 0);

												double bytesPerSecond = bytesThisTime / seconds;
												SKLogger.sAssert(getClass(), bytesPerSecond > 0);

												finalBytesPerSecond = bytesPerSecond;
												finalBytesMilliseconds = seconds * 1000.0;
												finalBytes = bytesThusFar;
											}
										}
									}
								}

							}

							//SKLogger.sAssert(getClass(), bGotValidResponseFromServer == true);

							// bGotValidResponseFromServer = false; // TODO - debug hack for testing!

							if (bGotValidResponseFromServer == true)
							{
								synchronized (HttpTest.class) {

									sServerUploadBytesPerSecond.add(Double.valueOf(finalBytesPerSecond));
								}

								Log.w(TAG, "DEBUG: BYTES CALCULATED FROM SERVER, PER SECOND = " + finalBytesPerSecond);
								bitrateMpbs1024Based = OtherUtils.sConvertBytesPerSecondToMbps1024Based(finalBytesPerSecond);
								Log.w(TAG, "DEBUG: bitsPerSecond CALCULATED FROM SERVER = " + OtherUtils.sBitrateMbps1024BasedToString(bitrateMpbs1024Based));
							}

							// TODO [self doSendUpdateStatus:self.status threadId:threadId];

							// bGotValidResponseFromServer = false; // DEBUG ONLY TESTING!
						}
						bReadThreadIsRunning = false;
					}
				};
				readThread.start();
			}


			//
			// Send WARM-UP data!
			//

			do {
				if (randomEnabled) {
					generator.nextBytes(buff);
				}
				try {
					//synchronized(HttpTest.this) {
						if (connOut == null) {
							break;
						}
						//             	long start = sGetMicroTime();
						connOut.write(buff);
						//             	long end = sGetMicroTime();

						//         		synchronized (HttpTest.class) {
						//             		smDebugSocketSendTimeMicroseconds.add(new DebugTiming("warmup", threadIndex, end-start, getSpeed()));
						//         		}

						connOut.flush();
					//}
					
					// Give other threads a chance, otherwise we're locked a hard loop...
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						SKLogger.sAssert(getClass(), false);
					}
				} catch (IOException ioe) {
					SKLogger.sAssert(getClass(), false);
					error.set(true);
				}
			} while (!isWarmupDone(sendDataChunkSize));

			if (error.get()) {
				// Let the thread do this! closeConnection(socket, connIn, connOut);
				// No need to wait for the thread to complete, however.
				if (readThread != null) {
					readThread.doStop();
				}
				return;
			}

			//
			// Send the ACTUAL TEST data!
			//

			boolean isTransferDone = false;
            long waitUntilTime = Long.MAX_VALUE;
            long waitFromTime = Long.MAX_VALUE;
            
            //Log.w("MPC", "loop - start 1");
            
            long actuallyTransferredBytes = 0L;
            
            for (;;) {
            	if (randomEnabled) {
            		generator.nextBytes(buff);
            	}
            	try {
            		//synchronized(HttpTest.this) {
            			if (connOut == null) {
            				//Log.w("MPC", "loop - break 2");
            				break;
            			}
            			//             	long start = sGetMicroTime();
            			//             	long start = sGetMicroTime();
            			connOut.write(buff);
                        actuallyTransferredBytes += buff.length;
                        
            			//             	long end = sGetMicroTime();

            			//         		synchronized (HttpTest.class) {
            			//             		smDebugSocketSendTimeMicroseconds.add(new DebugTiming("testit", threadIndex, end-start, getSpeed()));
            			//         		}
            			connOut.flush();
            		//}

            	} catch (IOException ioe) {
            		SKLogger.sAssert(getClass(), false);
            		error.set(true);
            		// And break out of the loop....
            		Log.w("MPC", "loop - break 3");
            		break;
            	}
            	
    			sSetLatestSpeedForExternalMonitor(getSpeedBytesPerSecond());

            	final long waitForTimeMs = 20000L;
            	if (isTransferDone == false) {
            		isTransferDone = isTransferDone(sendDataChunkSize);

            		if (isTransferDone == true) {
            			waitFromTime = sGetMilliTime();
            			waitUntilTime = waitFromTime + waitForTimeMs;
                		Log.w("MPC", "waituntiltime set=" + waitUntilTime);
            		}

            	}

            	// Stop EITHER if:
            	// 1) the read thread tells us!
            	if ((readThread != null) && (readThread.getIsCancelled() == true)) {
            		Log.w("MPC", "loop - break 4");
            		break;
            	}
            	
            	// 2) we at least 10 seconds AFTER the detection of "isTransferDone" - giving server long enough to respond, or until we've written enough bytes!
            	//if (transferMaxBytes == 0) {
            	if (readThread != null) {
            		// Server-based upload speed test in operation...
            		if (isTransferDone == true) {
            			if (sGetMilliTime() > waitUntilTime) {
            				Log.w("MPC", "loop - break 5a, waituntiltime=" + waitUntilTime + ", waited for " + (sGetMilliTime() - waitFromTime) + " ms");
            				break;
            			}
            		} else if (actuallyTransferredBytes >= transferMaxBytes) {
            			Log.w("MPC", "loop - break 5b");
            			break;
            		}

            		// Give other threads a chance, otherwise we're locked a hard loop...
            		try {
            			if (isTransferDone) {
            				Thread.sleep(10);
            			} else {
            				Thread.sleep(1);
            			}
            		} catch (InterruptedException e) {
            			SKLogger.sAssert(getClass(), false);
            		}
            	} else {
            		// Old-style upload speed test - simple condition to end the loop.
            		if (isTransferDone == true) {
                    	//Log.d("MPC", "loop - break as isTransferDone is true");
            			break;
            		}
            	}
            	
            	//Log.w("MPC", "loop - continue 6 - actualBytesTransferred = " + actuallyTransferredBytes);
            }

			//
			// To reach here, the (blocking) test is finished.
			//
            //Log.w("MPC", "loop - ended continue7");
			
			// Ask the thread to stop... and wait for it to stop!
			// Note that in the event of an error when transferring data, we'll have
			// already requested it to stop; however, it is fine to call doStop as many times as you want.
			
			// Has the server *already* finished?
            if (readThread != null) {
            	if (readThread.getIsCancelled() == true) {
            		// Already got a response!
            	} else {
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

			// Close the connection / tidy-up once the thread has finished?
			// No - the thread does this itself.
			// closeConnection(socket, connIn, connOut);
            
            int bytesPerSecondMeasurement = getSpeedBytesPerSecond();

			if (bGotValidResponseFromServer == true) {
				// BEST RESULT is from the SERVER!
         		Log.d(TAG, "Best result is from the SERVER, bytesPerSecondMeasurement=" + bytesPerSecondMeasurement);
				// TODO! [self doSendtodDidCompleteTransferOperation:0 transferBytes:0 totalBytes:0 ForceThisBitsPerSecondFromServer:bitrateMpbs1024Based threadId:threadId];
			} else {
         		Log.d(TAG, "Best result is from the BUILT-IN MEASUREMENT, bytesPerSecondMeasurement=" + bytesPerSecondMeasurement);
				// Best result is from the built-in measurement.
				// TODO! [self doSendtodDidCompleteTransferOperation:transferTimeMicroseconds transferBytes:transferBytes totalBytes:totalBytes ForceThisBitsPerSecondFromServer:-1.0  threadId:threadId];
			}
			
    		sSetLatestSpeedForExternalMonitor(bytesPerSecondMeasurement);

			return;
		}
	};
	
	private void uploadTest(int threadIndex) {
		CUploadTest theUploadTest = new CUploadTest();

		// This is blocking
		theUploadTest.uploadTestBlocking(threadIndex);
		
		Log.d(TAG, "Completed uploadTest, threadIndex = " + threadIndex);
	}

	private void closeConnection(Socket s, InputStream i, OutputStream o) {
		Log.w(getClass().getName(), "closeConnection...");
		
		if(i != null){
			try {
				i.close();
			} catch (IOException ioe) {
				SKLogger.sAssert(getClass(),  false);
			}
		}

		if(o != null){
			try {
				o.close();
			} catch (IOException ioe) {
				SKLogger.sAssert(getClass(),  false);
			}
		}

		if(s != null){
			try {
				s.close();
			} catch (IOException ioe) {
				SKLogger.sAssert(getClass(),  false);
			}
		}
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public void setWarmupMaxTime(int time) {
		warmupMaxTime = time;
	}

	public void setWarmupMaxBytes(int bytes) {
		warmupMaxBytes = bytes;
	}

	public void setTransferMaxTime(int time) {
		transferMaxTime = time;
		//transferMaxTime *= 4; Log.d("TODO", "TOOD _ REMOVE ME 2");
	}

	public void setTransferMaxBytes(int bytes) {
		transferMaxBytes = bytes;
		//transferMaxBytes *= 2; Log.d("TODO", "TOOD _ REMOVE ME 2");
	}

	public void setBufferSize(int size) {
		bufferSize = size;
	}

	public void setSendBufferSize(int size) {
		desiredSendBufferSize = size;
	}

	public void setReceiveBufferSize(int size) {
		desiredReceiveBufferSize = size;
		bufferSize = size;
	}

	public void setSendDataChunk(int size) {
		sendDataChunkSize = size;
	}

	public void setPostDataLenght(int l) {
		postDataLength = l;
	}

	public void setDownstream() {
		downstream = true;
	}

	public void setUpstream() {
		downstream = false;
	}

	public void setNumberOfThreads(int n) {
		nThreads = n;
		
		//nThreads = 1; Log.w(TAG, "TODO - do not check this in - forcing to just one thread!");
	}

	public void setDirection(String d) {
		if (Test.paramMatch(d, DOWNSTREAM)) {
			downstream = true;
		} else if (Test.paramMatch(d, UPSTREAM)) {
			downstream = false;
		}
	}

	public boolean isProgressAvailable() {
		boolean ret = false;
		if (transferMaxTime > 0) {
			ret = true;
		} else if (transferMaxBytes > 0) {
			ret = true;
		}
		return ret;
	}

	public int getProgress() {
		double ret = 0;

		synchronized (this) {
			if (startWarmup == 0) {
				ret = 0;
			} else if (transferMaxTime != 0) {
				long currTime = sGetMicroTime() - startWarmup;
				ret = (double) currTime / (warmupMaxTime + transferMaxTime);

			} else {
				int currBytes = warmupBytesAcrossAllTestThreads + transferBytesAcrossAllTestThreads;
				ret = (double) currBytes / (warmupMaxBytes + transferMaxBytes);
			}
		}
		ret = ret < 0 ? 0 : ret;
		ret = ret >= 1 ? 0.99 : ret;
		return (int) (ret * 100);
	}

	//boolean upload = true;
	boolean randomEnabled = false;
	String infoString = "";
	String ipAddress = "";
	boolean warmUpDone = false;
	int postDataLength = 0;

	// warmup variables
	long startWarmup = 0;
	long warmupTime = 0;
	long warmupMaxTime = 0;
	int warmupBytesAcrossAllTestThreads = 0;
	int warmupMaxBytes = 0;
	int warmupDoneCounter = 0;

	// transfer variables
	long startTransfer = 0;
	long transferTimeMicroseconds = 0;
	int transferBytesAcrossAllTestThreads = 0;
	long transferMaxTime = 0;
	int transferMaxBytes = 0;
	int transferDoneCounter = 0;

	// test variables
	int connectionCounter = 0;
	private int nThreads;
	boolean noDelay = false;
	int receiveBufferSize = 0;
	int sendBufferSize = 0;
	int bufferSize = 0;
	int desiredReceiveBufferSize = 0;
	int desiredSendBufferSize = 0;
	int sendDataChunkSize = 0;
	String testStatus = "FAIL";

	// Connection variables
	String target = "";
	String file = "";
	int port = 0;
	boolean downstream = true;

	// Test Status variable
	private AtomicBoolean error = new AtomicBoolean(false);

}
