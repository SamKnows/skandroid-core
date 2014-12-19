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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.content.Context;
//import android.util.Log;
import android.util.Pair;

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

public abstract class HttpTest extends Test {
	// @property (weak) SKTransferOperation *mpParentTransferOperation;
	// @property int mSocketFd;


	/*
	 * constants for creating a http test
	 */
	private static final String DOWNSTREAM = "downStream";
	private static final String UPSTREAM = "upStream";
	private static final String UPLOADSTRATEGY = "strategy";

	private static final String WARMUPMAXTIME = "warmupMaxTime";
	private static final String WARMUPMAXBYTES = "warmupMaxBytes";
	private static final String TRANSFERMAXTIME = "transferMaxTime";
	private static final String TRANSFERMAXBYTES = "transferMaxBytes";
	public static final String NTHREADS = "numberOfThreads";
	private static final String BUFFERSIZE = "bufferSize";
	private static final String SENDBUFFERSIZE = "sendBufferSize";
	private static final String RECEIVEBUFFERSIZE = "receiveBufferSize";
	private static final String POSTDATALENGTH = "postDataLength";
	private static final String SENDDATACHUNK = "sendDataChunk";

	public enum UploadStrategy { ACTIVE, PASSIVE}; 

	protected String TAG(Object param){ return param.getClass().getSimpleName();}						/* TAG is to be passed to SKLogger class */

	/*
	 * Http Status codes
	 */
	protected static final int HTTPOK = 200;
	protected static final int HTTPCONTINUE = 100;

	/*
	 * error codes and constraints
	 */
	protected static final int BYTESREADERR = -1;
	private static final int MAXNTHREADS = 100;

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
	protected static final String _DOWNSTREAM = "downstream";
	protected static final String _UPSTREAM = "upstream";

	public static final String JSON_TRANFERTIME = "transfer_time";
	public static final String JSON_TRANFERBYTES = "transfer_bytes";
	public static final String JSON_BYTES_SEC = "bytes_sec";
	public static final String JSON_WARMUPTIME = "warmup_time";
	public static final String JSON_WARMUPBYTES = "warmup_bytes";
	public static final String JSON_NUMBER_OF_THREADS = "number_of_threads";

	private	Thread[] mThreads = null;

	/*
	 * Time functions
	 */
	protected static long sGetMicroTime() {	return System.nanoTime() / 1000L; 		}
	protected static long sGetMilliTime() {	return System.nanoTime() / 1000000L; 	}

	private void doInitialize() {//TODO optimise
		sLatestSpeedReset();
	}

	public HttpTest() { doInitialize(); }

	public HttpTest(boolean downstream) {
		this.downstream = downstream;
		doInitialize();
	}

	public HttpTest(String direction) {
		setDirection(direction);

		doInitialize();
	}

	public static HttpTest getInstance(String direction, List<Param> params){
		boolean initialised = true;
		HttpTest result = null;

		try {
			for (Param curr : params) {
				String param = curr.getName();
				String value = curr.getValue();
				if (paramMatch(param, TARGET)) {
					target = value;
				} else if (paramMatch(param, PORT)) {
					port = Integer.parseInt(value);
				} else if (paramMatch(param, FILE)) {
					file = value;
				} else if (paramMatch(param, WARMUPMAXTIME)) {
					mWarmupMaxTimeMicro  = Integer.parseInt(value);
				} else if (paramMatch(param, WARMUPMAXBYTES)) {
					mWarmupMaxBytes  = Integer.parseInt(value);
				} else if (paramMatch(param, TRANSFERMAXTIME)) {
					mTransferMaxTimeMicro = Integer.parseInt(value);
				} else if (paramMatch(param, TRANSFERMAXBYTES)) {
					mTransferMaxBytes  = Integer.parseInt(value);
				} else if (paramMatch(param, NTHREADS)) {
					nThreads = Integer.parseInt(value);
				} else if (paramMatch(param, UPLOADSTRATEGY)){
					uploadStrategyServerBased = UploadStrategy.ACTIVE;
				}else if (paramMatch(param, BUFFERSIZE)) {
					downloadBufferSize  = Integer.parseInt(value);
				} else if (paramMatch(param, SENDBUFFERSIZE)) {
					socketBufferSize  = Integer.parseInt(value);
				} else if (paramMatch(param, RECEIVEBUFFERSIZE)) {
					desiredReceiveBufferSize = Integer.parseInt(value);
					downloadBufferSize = Integer.parseInt(value);
				} else if (paramMatch(param, SENDDATACHUNK)) {
					uploadBufferSize  = Integer.parseInt(value);
				} else if (paramMatch(param, POSTDATALENGTH)) {
					postDataLength = Integer.parseInt(value);
				} else {
					SKLogger.sAssert(TestFactory.class, false);
					initialised = false;
					break;
				}
			}
		} catch (NumberFormatException nfe) {
			initialised = false;
		}

		if ( !initialised )
			return null;

		if ( direction == DOWNSTREAM ){
			result = new DownloadTest();
		}
		else if ( direction == UPSTREAM ){
			if ( uploadStrategyServerBased ==  UploadStrategy.ACTIVE )
				result = new ActiveServerloadTest();
			else
				result = new PassiveServerUploadTest();
		}
		return result;
	}


	@Override
	public int getNetUsage() {
		return (int)(getTotalTransferBytes() + getTotalWarmUpBytes());
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
		if (mWarmupMaxTimeMicro == 0 && mWarmupMaxBytes == 0) {
			setError("No warmup parameter defined");
			return false;
		}
		if (mTransferMaxTimeMicro == 0 && mTransferMaxBytes == 0) {
			setError("No transfer parameter defined");
			return false;
		}
		if (downstream && downloadBufferSize == 0) {
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
	public boolean isSuccessful() { return testStatus.equals("OK");  }

	public int getSendBufferSize() 	  {	return sendBufferSize;		}
	public int getReceiveBufferSize() { return receiveBufferSize;	}
	public String getInfo() 		  {	return infoString; 			}

	@Override
	public void execute() {
		//     	smDebugSocketSendTimeMicroseconds.clear();

		//Context context = SKApplication.getAppInstance().getBaseContext();

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
	}

	protected Socket getSocket() {
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
			if (0 != socketBufferSize) {
				ret.setSendBufferSize(socketBufferSize);
			}
			sendBufferSize = ret.getSendBufferSize();

			if (downstream) {
				// Read / download
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


	abstract int getSpeedBytesPerSecond();											/* To be used in derived classes */

	/*	public synchronized int getTransferBytes() {
		return mWarmupBytesAcrossAllTestThreads + transferBytesAcrossAllTestThreads;
	}

	public synchronized int getWarmupBytes() {
		return mWarmupBytesAcrossAllTestThreads;
	}*/

	/*	public String getHumanReadableResult() {
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
	}*/


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
		o.add(Long.toString( getTransferTimeMicro()));//TODO check
		output.put(JSON_TRANFERTIME, getTransferTimeMicro());
		// transfer bytes
		o.add(Long.toString(getTotalTransferBytes()));
		output.put(JSON_TRANFERBYTES, getTotalTransferBytes());
		// byets_sec
		o.add(Integer.toString(getSpeedBytesPerSecond()));
		output.put(JSON_BYTES_SEC, getSpeedBytesPerSecond());
		// warmup time
		o.add(Long.toString(getWarmUpTimeMicro()));	//TODO check
		output.put(JSON_WARMUPTIME, getWarmUpTimeMicro());
		// warmup bytes
		o.add(Long.toString( getTotalWarmUpBytes()));
		output.put(JSON_WARMUPBYTES,  getTotalWarmUpBytes());
		// number of threads
		o.add(Integer.toString(nThreads));
		output.put(JSON_NUMBER_OF_THREADS, nThreads);

//TODO remove in production
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String, Object>> iter = output.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Object> entry = iter.next();
			sb.append(entry.getKey());
			sb.append('=').append('"');
			sb.append(entry.getValue());
			sb.append('"');
			if (iter.hasNext()) {
				sb.append(',').append(' ');
			}
		}

		SKLogger.d(TAG(this), "Output data: \n" + sb.toString());

		setOutput(o.toArray(new String[1]));
		setJSONOutput(output);
	}

/* The following set of methods relates to a  communication with the external UI TODO move prototypes to test */

	static private AtomicLong sLatestSpeedForExternalMonitorBytesPerSecond = new AtomicLong(0);
	static private AtomicLong sBytesPerSecondLast = new AtomicLong(0);
	
	static protected String sLatestSpeedForExternalMonitorTestId = "";
	
	public static void sLatestSpeedReset() {
		sLatestSpeedForExternalMonitorBytesPerSecond.set(0);
		sBytesPerSecondLast.set(0);
	}

	// Report-back a running average, to keep the UI moving...
	public static Pair<Double,String> sGetLatestSpeedForExternalMonitorAsMbps() {
		// use moving average of the last 2 items!
		double bytesPerSecondToUse = sBytesPerSecondLast.doubleValue() + sLatestSpeedForExternalMonitorBytesPerSecond.doubleValue();
		bytesPerSecondToUse /= 2;

		double mbps = (bytesPerSecondToUse * 8.0) / 1000000.0;
		return new Pair<Double,String>(mbps, sLatestSpeedForExternalMonitorTestId);
	}

	public static void sSetLatestSpeedForExternalMonitor(long bytesPerSecond, String testId) {
		sBytesPerSecondLast = sLatestSpeedForExternalMonitorBytesPerSecond;
		sLatestSpeedForExternalMonitorBytesPerSecond.set(bytesPerSecond);
	}

/* This is the end of the block related to communication with UI */
	
	
	/*synchronized*/ 
	boolean isWarmupDone(int bytes) {
		boolean ret = false;
		boolean bTimeWarmup = false;
		boolean bBytesWarmup = false;

		if (bytes == BYTESREADERR) {												/* if there is an error the test must stop and report it */
			setErrorIfEmpty("read error");
			bytes = 0; 																/* do not modify the bytes counters ??? */
			error.set(true);
		}

		addTotalWarmUpBytes(bytes);													/* increment atomic total bytes counter */

		if (mStartWarmupMicro == 0) {
			mStartWarmupMicro = sGetMicroTime();									/* record start up time should be recorded only by one thread */
		}

		setWarmUpTimeMicro(sGetMicroTime() - mStartWarmupMicro);					/* current warm up time should be atomic*/
					
		if (mWarmupMaxTimeMicro > 0) {												/*if warmup max time is set and time has exceeded its values set time warmup to true */
			bTimeWarmup = (mWarmupTimeMicro.get() >= mWarmupMaxTimeMicro);
		}

		if (mWarmupMaxBytes > 0) {													/* if warmup max bytes is set and bytes counter exceeded its value set bytesWarmup to true */
			bBytesWarmup = (getTotalWarmUpBytes() >= mWarmupMaxBytes);
		}

		if (bTimeWarmup) {															/* if a condition happened increment the warmupDoneCounter */
			warmupDoneCounter.addAndGet(1);
			ret = true;
		}

		if (bBytesWarmup) {
			warmupDoneCounter.addAndGet(1);
			ret = true;
		}
		return ret;
	}

	boolean isTransferDone(int bytes) {
		boolean ret = false;
		// In case an error occurred stop the test
		if (bytes == BYTESREADERR) {
			error.set(true);
			bytes = 0;
		}

		addTotalTransferBytes(bytes);

		// if startTransfer is 0 this is the first call to isTransferDone
		if (mStartTransferMicro == 0) {
			mStartTransferMicro = sGetMicroTime();
		}
		
		setTransferTimeMicro(sGetMicroTime() - mStartTransferMicro);
		// if transfermax time is
		if (mTransferMaxTimeMicro > 0) {
			if (getTransferTimeMicro() > mTransferMaxTimeMicro) {
				ret = true;
			}
		}
		if (mTransferMaxBytes > 0) {
			if (getTotalTransferBytes() + getTotalWarmUpBytes() > mTransferMaxBytes) {
				ret = true;
			}
		}

		if (getTotalTransferBytes() > 0) {
			testStatus = "OK";
		}

		if (error.get()) {
			ret = true;
		}
		//TODO debug, remove later
		//Log.d(TAG, "DEBUG: isTransferDone = " + ret + ", totalWarmUpBytes=>>>" + totalWarmUpBytes.get() + ", totalTransferBytes=>>>" + totalTransferBytes.get() + ", time" + transferTimeMicroseconds + "<<<");
		return ret;
	}

	protected int getThreadIndex(){
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
		return threadIndex;
	}


	//
	// Private class that performs the upload test.
	// This calculates upload speed from the client perspective.
	// It also tries to get an upload speed as a response from the server. ---> No way.
	//

	protected void closeConnection(Socket s, InputStream i, OutputStream o) {
		SKLogger.d(TAG(this), "closeConnection...");

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

	public void setDownstream() {								downstream = true;				}
	public void setUpstream() {									downstream = false;				}

	public void setDirection(String d) {
		if (Test.paramMatch(d, _DOWNSTREAM)) {
			downstream = true;
		} else if (Test.paramMatch(d, _UPSTREAM)) {
			downstream = false;
		}
	}

	public boolean isProgressAvailable() {//TODO check with new interface
		boolean ret = false;
		if (mTransferMaxTimeMicro > 0) {
			ret = true;
		} else if (mTransferMaxBytes > 0) {
			ret = true;
		}
		return ret;
	}

	public int getProgress() {//TODO check with new interface
		double ret = 0;

		synchronized (this) {
			if (mStartWarmupMicro == 0) {
				ret = 0;
			} else if (mTransferMaxTimeMicro != 0) {
				long currTime = sGetMicroTime() - mStartWarmupMicro;
				ret = (double) currTime / (mWarmupMaxTimeMicro + mTransferMaxTimeMicro);

			} else {
				long currBytes = getTotalWarmUpBytes() + getTotalTransferBytes();
				ret = (double) currBytes / (mWarmupMaxBytes + mTransferMaxBytes);
			}
		}
		ret = ret < 0 ? 0 : ret;
		ret = ret >= 1 ? 0.99 : ret;
		return (int) (ret * 100);
	}


	/*
	 * Atomic variables used as aggregate counters or (errors, etc. ) indicators  
	 */
	private AtomicLong totalWarmUpBytes = new AtomicLong(0);
	private AtomicLong totalTransferBytes = new AtomicLong(0);
	protected AtomicBoolean error = new AtomicBoolean(false);

	/*
	 * Accessors to atomic variables
	 */
	protected long getTotalWarmUpBytes(){ 				return totalWarmUpBytes.get();		}	
	protected long getTotalTransferBytes(){				return totalTransferBytes.get();	}	
	protected long getWarmUpTimeMicro(){				return mWarmupTimeMicro.get();		}
	protected long getTransferTimeMicro(){				return transferTimeMicroseconds.get();	}
	
	protected void addTotalTransferBytes(long bytes) {	totalTransferBytes.addAndGet(bytes); }	
	protected void addTotalWarmUpBytes(long bytes) {	totalWarmUpBytes.addAndGet(bytes);	}

	protected void setWarmUpTimeMicro(long uTime){		mWarmupTimeMicro.set(uTime);	}
	protected void setTransferTimeMicro(long uTime){	transferTimeMicroseconds.set(uTime);	}

	private String infoString = "";
	private String ipAddress = "";

	boolean randomEnabled = false;	
	boolean warmUpDone = false;

	static int postDataLength = 0;

	// warmup variables
	private long mStartWarmupMicro = 0;
	private AtomicLong mWarmupTimeMicro = new AtomicLong(0);
	private AtomicInteger warmupDoneCounter = new AtomicInteger(0);	
	static long mWarmupMaxTimeMicro = 0;
	static int mWarmupMaxBytes = 0;


	// transfer variables
	private long mStartTransferMicro = 0;
	private AtomicLong transferTimeMicroseconds = new AtomicLong(0);
	private AtomicInteger transferDoneCounter = new AtomicInteger(0);
	static long mTransferMaxTimeMicro = 0;
	static int mTransferMaxBytes = 0;
	


	// test variables
	static private int nThreads;
	static int downloadBufferSize = 0;
	static int desiredReceiveBufferSize = 0;
	static int socketBufferSize = 0;
	static int uploadBufferSize = 0;

	private int connectionCounter = 0;
	private int receiveBufferSize = 0;
	private int sendBufferSize = 0;

	protected int getThreadsNum(){ return nThreads; }	/* getter for number of threads */

	boolean noDelay = false;

	String testStatus = "FAIL";

	// Connection variables
	static String target = "";
	static String file = "";
	static int port = 0;
	static UploadStrategy uploadStrategyServerBased = UploadStrategy.PASSIVE;

	boolean downstream = true;

}


//For debug timings!
//	private static ArrayList<DebugTiming> smDebugSocketSendTimeMicroseconds = new ArrayList<DebugTiming>();



/*		private class DebugTiming {
public String description;
public int threadIndex;
public Long time;
public int currentSpeed;

public DebugTiming(String description, int threadIndex, Long time, int currentSpeed) {
	super();
	this.description = description;
	this.threadIndex = threadIndex;
	this.time = time;
	this.currentSpeed = currentSpeed;
}
}*/
