package com.samknows.tests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.samknows.libcore.SKPorting;
import org.json.JSONObject;

// NOTE: This code is written as Pure Java.
// It is possible to modify it to have Android-specific calls.
// Look at the static methods at the top of the class, for the commented-out Android-specific
// code that can be re-enabled if required.
import com.samknows.libcore.SKPair;

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

public abstract class HttpTest extends SKAbstractBaseTest implements Runnable {



  // @property (weak) SKTransferOperation *mpParentTransferOperation;
  // @property int mSocketFd;

  public enum UploadStrategy {ACTIVE, PASSIVE}

  // Socket timeout parameters
  private final int CONNECTIONTIMEOUT = 10000; 	// 10 seconds connection timeout
  private final int READTIMEOUT  = 10000; 			// 10 seconds read timeout
  private final int WRITETIMEOUT = 10000; 			// 10 seconds write timeout

  // Http Status codes
  protected final int HTTPOK = 200;
  private final int HTTPCONTINUE = 100;

  // error codes and constraints
  protected final int BYTESREADERR = -1;				// Error occurred while reading from socket
  private final int MAXNTHREADS = 100;					// Max number of threads

  // Parameters name for the setParameter function.

  // HTTP test types. Static because called from constructors.
  protected final static String _DOWNSTREAM = "downstream";
  protected final static String _UPSTREAM = "upstream";

  // Parameters names for use in Settings XML files
  //private static final String DOWNSTREAM = "downStream";
  //private static final String UPSTREAM = "upStream";
  private static final String UPLOADSTRATEGY = "strategy";						// Use server side calculations, different type of server required
  public static final String WARMUPMAXTIME = "warmupMaxTime";					// Max warmup time in uSecs
  private static final String WARMUPMAXBYTES = "warmupMaxBytes";			// Max warmup bytes allowed to be transmitted
  public static final String TRANSFERMAXTIME = "transferMaxTime";			// Max transfer time in uSecs. Metrics, measured during this time period contribute to final result
  private static final String TRANSFERMAXBYTES = "transferMaxBytes";	// Max transfer bytes allowed to be transmitted
  public static final String NTHREADS = "numberOfThreads";						// Max number of threads allowed
  private static final String NTHREADSLOWERCASE = "numberofthreads";	// Max number of threads allowed
  public static final String BUFFERSIZE = "bufferSize";							  // Socket receive buffer size
  public static final String SENDBUFFERSIZE = "sendBufferSize";				// Socket send buffer size
  private static final String RECEIVEBUFFERSIZE = "receiveBufferSize";// Socket receive buffer size
  private static final String POSTDATALENGTH = "postDataLength";			// ???
  public static final String SENDDATACHUNK = "sendDataChunk";					// Application send buffer size

  // Messages regarding the status of the test
  private final String HTTPGETRUN = "Running download test";
  private final String HTTPGETDONE = "Download test completed";
  private final String HTTPPOSTRUN = "Running upload test";
  private final String HTTPPOSTDONE = "Upload completed";

  // Test strings for public use. JSON related
  public static final String DOWNSTREAMSINGLE = "JHTTPGET";
  public static final String DOWNSTREAMMULTI = "JHTTPGETMT";
  public static final String UPSTREAMSINGLE = "JHTTPPOST";
  public static final String UPSTREAMMULTI = "JHTTPPOSTMT";

  // "Direction" for constructor
  public static final String cReasonResetDownload = "Reset Download";
  public static final String cReasonResetUpload = "Reset Upload";
  public static final String cReasonUploadEnd = "Upload End";


  // Create an interface class, which will allow us to inject a test socket for mock testing.
  // Usage: Call open, then the set/get methods - then connect!
  // Then call getInputStream/getOutputStream...
  // Then call close()
  public interface ISKHttpSocketFactory {
    // Instance of factory used to "open" the socket, as a wrapper around the open method.
    // If null, we use the standard SKHttpSocket.
    ISKHttpSocket newSocket();
  }

  public interface ISKHttpSocket {

    void open();
    void setTcpNoDelay(boolean on) throws SocketException;
    void setReceiveBufferSize(int size) throws SocketException;
    int getReceiveBufferSize() throws SocketException;
    void setSendBufferSize(int size) throws SocketException;
    int getSendBufferSize()  throws SocketException;
    void setSoTimeout(int timeout) throws SocketException;

    //ipAddress = sockAddr.getAddress().getHostAddress();
    // Returns the ipaddress...
    String connect(String target, int port, int timeout) throws IOException;

    InputStream getInputStream() throws IOException;
    OutputStream getOutputStream() throws IOException;

    void close() throws IOException;
  }

  // Define a real instantiation of the ISKHttpSocket interface, which is used for "real" testing.
  public class SKHttpSocket implements ISKHttpSocket {
    private Socket socket = null;

    public SKHttpSocket() {
    }

    public void open() {
      socket = new Socket();
    }

    public void setTcpNoDelay(boolean on) throws SocketException {

      if (socket == null) {
        SKPorting.sAssert(false);
        return;
      }

      socket.setTcpNoDelay(on);
    }

    public synchronized void setReceiveBufferSize(int size) throws SocketException {

      if (socket == null) {
        SKPorting.sAssert(false);
        return;
      }

      socket.setReceiveBufferSize(size);
    }

    public int getReceiveBufferSize() throws SocketException {
      if (socket == null) {
        SKPorting.sAssert(false);
        return 0;
      }

      return socket.getReceiveBufferSize();
    }

    public synchronized void setSendBufferSize(int size) throws SocketException {

      if (socket == null) {
        SKPorting.sAssert(false);
        return;
      }

      socket.setSendBufferSize(size);
    }

    public synchronized  int getSendBufferSize()  throws SocketException {
      if (socket == null) {
        SKPorting.sAssert(false);
        return 0;
      }
      return socket.getSendBufferSize();
    }

    public void setSoTimeout(int timeout) throws SocketException {
      if (socket == null) {
        SKPorting.sAssert(false);
        return;
      }
      socket.setSoTimeout(timeout);
    }

    // Returns the ipaddress...
    public String connect(String target, int port, int timeout) throws IOException {
      InetSocketAddress sockAddr = new InetSocketAddress(target, port);
      if (sockAddr == null) {
        SKPorting.sAssert(false);
        return "";
      }
      socket.connect(sockAddr, timeout); // // 10 seconds connection timeout
      return sockAddr.getAddress().getHostAddress();
    }

    public InputStream getInputStream() throws IOException {
      if (socket == null) {
        SKPorting.sAssert(false);
        return null;
      }
      return socket.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
      if (socket == null) {
        SKPorting.sAssert(false);
        return null;
      }
      return socket.getOutputStream();
    }

    public void close() throws IOException {
      if (socket == null) {
        SKPorting.sAssert(false);
        return;
      }

      try {
        socket.close();
      } catch (IOException e1) {
        SKPorting.sAssert(false);
        throw e1;
      } finally {
        socket = null;
      }
    }

//    public long getStartTimeNanoseconds() {
//      return System.nanoTime();
//    }
//
//    public long getTimeNowNanoseconds() {
//      return System.nanoTime();
//    }
//
//    public InetAddress getInetAddressByName(String host) throws UnknownHostException {
//      return InetAddress.getByName(host);
//    }
  }

  private String TAG(Object param) {
    return param.getClass().getSimpleName();
  }							/* TAG is to be passed to SKLogger class. It outputs the human readable class name of the message logger */

  /* Abstract methods to be implemented in derived classes */
  protected abstract boolean transfer(ISKHttpSocket socket, int threadIndex);	/* Generate main traffic for metrics measurements */

  protected abstract boolean warmup(ISKHttpSocket socket, int threadIndex);		/* Generate initial traffic for setting optimal TCP parameters */
  //private abstract int getWarmupBytesPerSecond();						/* Initial traffic speed */
  //private abstract int getTransferBytesPerSecond();						/* Main traffic speed */

  private Thread[] mThreads = null;										/* Array of all running threads */

  /* Time helper functions */
  protected long sGetMicroTime() {
    return System.nanoTime() / 1000L;
  }

  private long sGetMilliTime() {
    return System.nanoTime() / 1000000L;
  }

  protected HttpTest(String direction, List<Param> params) {					/* Constructor. Accepts list of Param objects, each representing a certain parameter read from settings XML file */
    setDirection(direction);											/* Legacy. To be removed */
    sLatestSpeedReset(downstream ? cReasonResetDownload : cReasonResetUpload);

    setParams(params);													/* Initialisation */

    //SKLogger.d(this, "CREATING HTTP TEST - LOG TEST!");
  }

  private void setParams(List<Param> params) {								/* Initialisation helper function */
    initialised = true;
    try {
      for (Param param : params) {
        String value = param.getValue();
        if (param.contains(TARGET)) {
          target = value;
        } else if (param.contains(PORT)) {
          port = Integer.parseInt(value);
        } else if (param.contains(FILE)) {
          file = value;
        } else if (param.contains(WARMUPMAXTIME)) {
          mWarmupMaxTimeMicro = Integer.parseInt(value);
        } else if (param.contains(WARMUPMAXBYTES)) {
          mWarmupMaxBytes = Integer.parseInt(value);
        } else if (param.contains(TRANSFERMAXTIME)) {
          mTransferMaxTimeMicro = Integer.parseInt(value);
        } else if (param.contains(TRANSFERMAXBYTES)) {
          mTransferMaxBytes = Integer.parseInt(value);
        } else if (param.contains(NTHREADS)) {
          nThreads = Integer.parseInt(value);
        } else if (param.contains(NTHREADSLOWERCASE)) {
          nThreads = Integer.parseInt(value);
        } else if (param.contains(UPLOADSTRATEGY)) {
          uploadStrategyServerBased = UploadStrategy.ACTIVE;		/* If strategy parameter is present ActiveServerload class is used */
        } else if (param.contains(BUFFERSIZE)) {
          downloadBufferSize = Integer.parseInt(value);
        } else if (param.contains(SENDBUFFERSIZE)) {
          socketBufferSize = Integer.parseInt(value);
        } else if (param.contains(RECEIVEBUFFERSIZE)) {
          desiredReceiveBufferSize = Integer.parseInt(value);
          downloadBufferSize = Integer.parseInt(value);
        } else if (param.contains(SENDDATACHUNK)) {
          uploadBufferSize = Integer.parseInt(value);
        } else if (param.contains(POSTDATALENGTH)) {
          postDataLength = Integer.parseInt(value);
        } else {
          SKPorting.sLogE("HttpTest", "setParams()");
          initialised = false;
          break;
        }
      }
    } catch (NumberFormatException nfe) {
      SKPorting.sAssert(false);
      initialised = false;
    }
  }

  @Override
  public int getNetUsage() {												/* Total number of bytes transfered */
    return (int) (getTotalTransferBytes() + getTotalWarmUpBytes());
  }

  // @SuppressLint("NewApi")
  @Override
  public boolean isReady() {												/* Test sanity checker. Virtual */
    if (!initialised)
      return false;

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

//  void sendTestPing(String token) {
//    //
//    DatagramSocket socket = null;
//    try {
//      socket = new DatagramSocket();
//
//      try {
//        InetAddress address = InetAddress.getByName("192.168.2.105");
//        byte[] buf = token.getBytes(Charset.forName("UTF-8"));
//        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 90);
//        socket.send(packet);
//      } catch (Exception e) {
//        sAssert(false);
//      }
//    } catch (SocketException e2) {
//      socket.close();
//      sAssert(false);
//    } finally {
//      if (socket != null) {
//        socket.close();
//      }
//    }
//  }

  @Override
  public boolean isSuccessful() {
    return testStatus.equals("OK");
  }		/* Returns test run result */

//	public int getSendBufferSize() 	  {	return sendBufferSize;		}
//	public int getReceiveBufferSize() { return receiveBufferSize;	}
//	public String getInfo() 		  {	return infoString; 			}

  // Socket factory to support testing!
  ISKHttpSocketFactory mThisSocketFactory = null;

  @Override
  public void runBlockingTestToFinishInThisThread() {													/* Execute test */
    //smDebugSocketSendTimeMicroseconds.clear();
    //Context context = SKApplication.getAppInstance().getBaseContext();

    //sendTestPing("TIMING_Start");

    if (downstream) {
      //SKLogger.d(this, "DOWNLOAD HTTP TEST - execute()");
      infoString = HTTPGETRUN;
    } else {
      //SKLogger.d(this, "UPLOAD HTTP TEST - execute()");
      infoString = HTTPPOSTRUN;
    }

    setStateToRunning();

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
      SKPorting.sLogE("HttpTest", "Thread join exception()");
      testStatus = "FAIL";
    }

    if (downstream) {
      infoString = HTTPGETDONE;
    } else {
      infoString = HTTPPOSTDONE;
    }

    //sendTestPing("TIMING_Stop");

    if (getTotalTransferBytes() == 0) {
      // 30/03/2015 - note that if transferBytes is ZERO, we must also tag this with "success": false
      error.set(true);
    }
    finish();
  }

  public void runBlockingTestToFinishInThisThread(ISKHttpSocketFactory withThisSocketFactory) {													/* Execute test */
    if (mThisSocketFactory != null) {
      SKPorting.sAssert(false);
    } else {
      mThisSocketFactory = withThisSocketFactory;
    }

    runBlockingTestToFinishInThisThread();
  }

  private ISKHttpSocket makeSocket() {															/* Socket initialiser */
    //SKLogger.d(this, "HTTP TEST - makeSocket()");

    ISKHttpSocket retSocket = null;
    try {
      // Are we using a socket factory?
      if (mThisSocketFactory != null) {
        // Yes, use a specific socket factory (for testing!)
        retSocket = mThisSocketFactory.newSocket();
      } else {
        // No, use the built-in socket.
        retSocket = new SKHttpSocket();
      }
      retSocket.open();

      retSocket.setTcpNoDelay(noDelay);

      if (0 != desiredReceiveBufferSize) {
        retSocket.setReceiveBufferSize(desiredReceiveBufferSize);
      }
      receiveBufferSize = retSocket.getReceiveBufferSize();

      // Experimentation shows a *much* better settling-down on upload speed,
      // if we force a 32K send buffer size in bytes, rather than relying
      // on the default send buffer size.

      // When forcing value in bytes, you must actually divide by two!
      // https://code.google.com/p/android/issues/detail?id=13898
      // desiredSendBufferSize = 32768 / 2; // (2 ^ 15) / 2
      if (0 != socketBufferSize) {
        retSocket.setSendBufferSize(socketBufferSize);
      }
      sendBufferSize = retSocket.getSendBufferSize();

      if (downstream) {
        // Read / download
        retSocket.setSoTimeout(READTIMEOUT);
      } else {
        retSocket.setSoTimeout(getSocketTimeoutMilliseconds());
        //retSocket.setSoTimeout(1);
      }

      ipAddress = retSocket.connect(target, port, CONNECTIONTIMEOUT); // // 10 seconds connection timeout
      SKPorting.sAssert(ipAddress.length() > 0);

      //SKLogger.d(this, "HTTP TEST - getSocket() completed OK");
    } catch (Exception e) {
      SKPorting.sLogE("HttpTest", "getSocket()", e);
      retSocket = null;
    }
    return retSocket;
  }

  private Long mTimestampSeconds = SKAbstractBaseTest.sGetUnixTimeStampSeconds();
  @Override
  public synchronized void finish() {
    mTimestampSeconds = SKAbstractBaseTest.sGetUnixTimeStampSeconds();
    status = STATUS.DONE;
  }

  @Override
  public long getTimestamp() {
    return mTimestampSeconds;
  }

  @Override
  public void setTimestamp(long timestamp) {
    mTimestampSeconds = timestamp;
  }

  @Override
  public JSONObject getJSONResult() {
    //SKLogger.d(this, "HTTP TEST - output()");

    Map<String, Object> output = new HashMap<>();
    // string id
    output.put(JsonData.JSON_TYPE, getStringID());

    // time
    output.put(JsonData.JSON_TIMESTAMP, mTimestampSeconds);
    output.put(JsonData.JSON_DATETIME, SKPorting.sGetDateAsIso8601String(new java.util.Date(mTimestampSeconds * 1000)));

    long transferBytes = getTotalTransferBytes();
    //SKLogger.d(this, "HTTP TEST - output(), transferBytes=" + transferBytes);
    if (transferBytes == 0) {
      // 30/03/2015 - note that if transferBytes is ZERO, we must also tag this with "success": false
      error.set(true);
    }

    // status
    if (error.get()) {
      output.put(JsonData.JSON_SUCCESS, false);
    } else {
      output.put(JsonData.JSON_SUCCESS, true);
    }
    // target
    output.put(JsonData.JSON_TARGET, target);
    // target ip address
    output.put(JsonData.JSON_TARGET_IPADDRESS, ipAddress);
    // transfer time
    output.put(JsonData.JSON_TRANFERTIME, getTransferTimeMicro());
    // transfer bytes
    output.put(JsonData.JSON_TRANFERBYTES, totalTransferBytes);
    // byets_sec
    output.put(JsonData.JSON_BYTES_SEC, Math.max(0, getTransferBytesPerSecond()));
    // warmup time
    output.put(JsonData.JSON_WARMUPTIME, getWarmUpTimeMicro());
    // warmup bytes
    output.put(JsonData.JSON_WARMUPBYTES, getTotalWarmUpBytes());
    // number of threads
    output.put(JsonData.JSON_NUMBER_OF_THREADS, nThreads);

    JSONObject json_output = new JSONObject(output);
    return json_output;
  }

/* The following set of methods relates to a  communication with the external UI TODO move prototypes to test */

  static private AtomicReference<Double> sLatestSpeedForExternalMonitorBytesPerSecond = new AtomicReference<>(Double.valueOf(0.0));
  static private AtomicReference<Double> sBytesPerSecondLast = new AtomicReference<>(Double.valueOf(0.0));

  private static String sLatestSpeedForExternalMonitorTestId = "";

  private static void sLatestSpeedReset(String theReasonId) {
    sLatestSpeedForExternalMonitorBytesPerSecond.set(Double.valueOf(0));
    sBytesPerSecondLast.set(Double.valueOf(0));
    sLatestSpeedForExternalMonitorTestId = theReasonId;
  }

  public static void sLatestSpeedReset() {
    sLatestSpeedReset(cReasonResetDownload);
  }

  // Report-back a running average, to keep the UI moving...
  // Returns -1 if sample time too short.
  public static SKPair<Double, String> sGetLatestSpeedForExternalMonitorAsMbps() {
    // use moving average of the last 2 items!
    double bytesPerSecondToUse = sBytesPerSecondLast.get().doubleValue() + sLatestSpeedForExternalMonitorBytesPerSecond.get().doubleValue();
    bytesPerSecondToUse /= 2;

    double mbps1000Based = Conversions.sConvertBytesPerSecondToMbps1000Based(bytesPerSecondToUse);
    return new SKPair<>(mbps1000Based, sLatestSpeedForExternalMonitorTestId);
  }

  public static void sSetLatestSpeedForExternalMonitor(Double bytesPerSecond, String testId) {
    sBytesPerSecondLast = sLatestSpeedForExternalMonitorBytesPerSecond;
    if (bytesPerSecond == 0) {
      //sAssert(testId.equals(cReasonUploadEnd));
    }
    sLatestSpeedForExternalMonitorBytesPerSecond.set(bytesPerSecond);
    sLatestSpeedForExternalMonitorTestId = testId;
  }

  protected final int extMonitorUpdateInterval = 500000;

  protected void sSetLatestSpeedForExternalMonitorInterval(long pause, String id, Double transferSpeed) {
    long updateTime = /*timeElapsedSinceLastExternalMonitorUpdate.get() == 0 ? pause * 5 : */ pause;					/* first update is delayed 3 times of a given pause */

    if (timeElapsedSinceLastExternalMonitorUpdate.get() == 0) {
      timeElapsedSinceLastExternalMonitorUpdate.set(sGetMicroTime()); 										/* record update time */
    }

    if (sGetMicroTime() - timeElapsedSinceLastExternalMonitorUpdate.get() > updateTime/*uSec*/) {				/* update should be called only if 'pause' is expired */
      double currentSpeed;

      try {
        currentSpeed = transferSpeed;																/* current speed could be for warm up, transfer or possibly others processes */
      } catch (Exception e) {
        currentSpeed = 0.0;
      }

      sSetLatestSpeedForExternalMonitor(currentSpeed /*/ 1000000.0*/, id);							/* update speed parameter + indicative ID */

      timeElapsedSinceLastExternalMonitorUpdate.set(sGetMicroTime());											/* set new update time */

//			SKLogger.d(TAG(this), "External Monitor updated at " + (new java.text.SimpleDateFormat("HH:mm:ss.SSS")).format(new java.util.Date()) + 
//					" as " +  ( currentSpeed / 1000000.0) +
//					" thread: " + getThreadIndex());//haha remove in production
    }
  }

/* This is the end of the block related to communication with UI */


  protected boolean isWarmupDone(int bytes) {
    //SKLogger.d(this, "isWarmupDone("+ bytes+")");

    boolean timeExceeded = false;
    boolean bytesExceeded = false;

    if (bytes == BYTESREADERR) {													/* if there is an error the test must stop and report it */
      setErrorIfEmpty("read error");
      bytes = 0; 																	/* do not modify the bytes counters ??? */
      error.set(true);
      return true;
    }

    if (mWarmupMicroDuration.get() != 0)											/* if some other thread has already finished warmup there is no need to proceed */
      return true;

    addTotalWarmUpBytes(bytes);														/* increment atomic total bytes counter */

    if (mStartWarmupMicro.get() == 0) {
      mStartWarmupMicro.set(sGetMicroTime()); 									/* record start up time should be recorded only by one thread */
    }

    setWarmUpTimeMicro(sGetMicroTime() - mStartWarmupMicro.get());					/* current warm up time should be atomic*/

    if (mWarmupMaxTimeMicro > 0) {													/*if warmup max time is set and time has exceeded its values set time warmup to true */
      timeExceeded = (mWarmupTimeMicro.get() >= mWarmupMaxTimeMicro);
    }

    if (mWarmupMaxBytes > 0) {														/* if warmup max bytes is set and bytes counter exceeded its value set bytesWarmup to true */
      bytesExceeded = (getTotalWarmUpBytes() >= mWarmupMaxBytes);
    }

    if (timeExceeded) {																/* if maximum warmup time is reached */
      if (mWarmupMicroDuration.get() == 0) {
        mWarmupMicroDuration.set(sGetMicroTime() - mStartWarmupMicro.get());	/* Register the time duration up to this moment */
      }
      warmupDoneCounter.addAndGet(1);												/* and increment warmup counter */
      return true;
    }

    if (bytesExceeded) {																/* if max warmup bytes transferred */
      if (mWarmupMicroDuration.get() == 0) {
        mWarmupMicroDuration.set(sGetMicroTime() - mStartWarmupMicro.get());	/* Register the time duration up to this moment */
      }
      warmupDoneCounter.addAndGet(1);												/* and increment warmup counter */
      return true;
    }

    return false;
  }

  protected boolean isTransferDone(int bytes) {
    boolean timeExceeded = false;
    boolean bytesExceeded = false;

    //SKLogger.d(this, "isTransferDone("+ bytes+")");

    //boolean ret = false;
    if (bytes == BYTESREADERR) {														/* if there is an error the test must stop and report it */
      setErrorIfEmpty("read error");
      bytes = 0; 																		/* do not modify the bytes counters ??? */
      error.set(true);
      SKPorting.sLogE("HttpTest", "isTransferDone, bytes == BYTESREADERR!");
      return true;
    }

    if (mTransferMicroDuration.get() != 0) {
    	/* if some other thread has already finished warmup there is no need to proceed */
      //SKLogger.d(this, "isTransferDone, mTransferMicroDuration != 0");
      return true;
    }


    addTotalTransferBytes(bytes);														/* increment atomic total bytes counter */

    /* record start up time should be recorded only by one thread */
    mStartTransferMicro.compareAndSet(0, sGetMicroTime());
    //SKLogger.d(TAG(this), "Setting transfer start  == " + mStartTransferMicro.get() + " by thread: " + this.getThreadIndex());//TODO remove in production

    setTransferTimeMicro(sGetMicroTime() - mStartTransferMicro.get());					/* How much time transfer took up to now */

    if (mTransferMaxTimeMicro > 0) {													/* If transfer time is more than max time, then transfer is done */
      //SKLogger.d(this, "transfer Time so far milli =" + getTransferTimeMicro()/1000);

      timeExceeded = (getTransferTimeMicro() >= mTransferMaxTimeMicro);
    }

    //SKLogger.d(this, "transfer Bytes so far =" + getTotalTransferBytes());
    if (mTransferMaxBytes > 0) {
      bytesExceeded = (getTotalTransferBytes() >= mTransferMaxBytes);
    }

    if (getTotalTransferBytes() > 0) {
      testStatus = "OK";
    }

    if (timeExceeded) {																	/* if maximum transfer time is reached */
      /* Register the time duration up to this moment */
      mTransferMicroDuration.compareAndSet(0, sGetMicroTime() - mStartTransferMicro.get());
      transferDoneCounter.addAndGet(1);												/* and increment transfer counter */
      //SKLogger.d(this, "isTransferDone, timeExceeded");
      return true;
    }

    if (bytesExceeded) {																/* if max transfer bytes transferred */
      mTransferMicroDuration.compareAndSet(0, sGetMicroTime() - mStartTransferMicro.get());
      //SKLogger.d(this, "isTransferDone, bytesExceeded");
      transferDoneCounter.addAndGet(1);												/* and increment transfer counter */
      return true;
    }

    //SKLogger.d(this, "isTransferDone, still waiting...");
    return false;
  }


  protected int getThreadIndex() {
    int threadIndex = 0;

    synchronized (mThreads) {

      boolean bFound = false;

      int i;
      for (i = 0; i < mThreads.length; i++) {
        if (Thread.currentThread() == mThreads[i]) {
          threadIndex = i;
          bFound = true;
          break;
        }
      }

      if (bFound == false) {
        SKPorting.sLogE("HttpTest", "getThreadIndex()");
      }
    }
    return threadIndex;
  }


  protected OutputStream getOutput(ISKHttpSocket socket) {
    OutputStream conn = null;
    boolean err = false;								/*Initially there is not error */

    if (socket != null) {
      try {
        conn = socket.getOutputStream();			/* Try to get output stream */
      } catch (IOException io) {
        err = true;									/* Fails */
        SKPorting.sLogE("HttpTest", "getOutput() ... thread: " + this.getThreadIndex(), io);
      }
    } else {											/* if socket is null - fails */
      err = true;
      SKPorting.sLogE("HttpTest", "getOutput(), socket is null! ... thread: " + this.getThreadIndex());
    }

    if (err) {										/* return null if there is an error */
      SKPorting.sLogE("HttpTest", "Error occurred while getting output connection, returning null... thread: " + this.getThreadIndex());
      return null;
    }

    return conn;										/* return output stream */
  }

  protected InputStream getInput(ISKHttpSocket socket) {
    InputStream conn = null;
    boolean err = false;								/*Initially there is not error */

    if (socket != null) {
      try {
        conn = socket.getInputStream();			/* Try to get output stream */
      } catch (IOException io) {
        err = true;									/* Fails */
        SKPorting.sLogE("HttpTest", "getInput() ... thread: " + this.getThreadIndex(), io);
      }
    } else {											/* if socket is null - fails */
      err = true;
      SKPorting.sLogE("HttpTest", "getOutput(), socket is null! ... thread: " + this.getThreadIndex());
    }

    if (err) {										/* return null if there is an error */
      SKPorting.sLogE("HttpTest", "Error occurred while getting input connection, returning null... thread: " + this.getThreadIndex());
      return null;
    }

    return conn;										/* return output stream */
  }


  //public void setDownstream() {								downstream = true;				}
  //public void setUpstream() {									downstream = false;				}

  private void setDirection(String d) {
    if (d.equalsIgnoreCase(_DOWNSTREAM)) {
      downstream = true;
    } else if (d.equalsIgnoreCase(_UPSTREAM)) {
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

  // Return progress as integer value from 0 to 100.
  public int getProgress0To100() {
    double ret = 0;

    if (mStartWarmupMicro.get() == 0) {
      return 0;
    }

    if (mTransferMaxTimeMicro != 0) {
      long currTime = sGetMicroTime() - mStartWarmupMicro.get();
      ret = (double) currTime / (mWarmupMaxTimeMicro + mTransferMaxTimeMicro);

    } else {
      long currBytes = getTotalWarmUpBytes() + getTotalTransferBytes();
      ret = (double) currBytes / (mWarmupMaxBytes + mTransferMaxBytes);
    }

    int result = (int) (ret * 100);
    if (result < 0) {
      return 0;
    }
    if (result > 100) {
      return 100;
    }

    SKPorting.sAssert(result >= 0);
    SKPorting.sAssert(result <= 100);

    return result;
  }

  protected void closeConnection(ISKHttpSocket socket) {											/* Closes connections  and winds socket out*/
    //SKLogger.d(this, "closeConnection()");

    /*
		 * Should be run inside thread
		 */
    if (socket != null) {
      OutputStream outputStream = null;
      InputStream inputStream = null;
      try {
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
      } catch (IOException e) {
        SKPorting.sLogE("HttpTest", "closeConnection(), e", e);
      }


      if (inputStream != null) {
        //SKLogger.d(this, "inputStream.close()");
        try {
          inputStream.close();
        } catch (IOException ioe) {
          SKPorting.sLogE("HttpTest", "closeConnection(), ioe", ioe);
        }
      }

      if (outputStream != null) {
        //SKLogger.d(this, "outputStream.close()");
        try {
          outputStream.close();
        } catch (IOException ioe2) {
          SKPorting.sLogE("HttpTest", "closeConnection(), ioe2", ioe2);
        }
      }

      try {
        //SKLogger.d(this, "socket.close()");
        socket.close();
      } catch (IOException ioe3) {
        SKPorting.sLogE("HttpTest", "closeConnection(), ioe2", ioe3);
      }
    }
  }

  @Override
  public void run() {
    boolean result = false;
    int threadIndex = getThreadIndex();

    ISKHttpSocket socket = makeSocket();

    if (socket == null) {
      SKPorting.sLogE("HttpTest", "Socket initiation failed, thread: " + threadIndex);
      return;
    }

    result = warmup(socket, threadIndex);

    if (!result) {
      closeConnection(socket);
      return;
    }

    result = transfer(socket, threadIndex);

    closeConnection(socket);
  }

  /*
   * Atomic variables used as aggregate counters or (errors, etc. ) indicators updated from concurrently running threads
   */
  private AtomicLong totalWarmUpBytes = new AtomicLong(0);												/* Total num of bytes transmitted during warmup period */
  private AtomicLong totalTransferBytes = new AtomicLong(0);												/* Total num of bytes transmitted during trnasfer period */
  private AtomicBoolean error = new AtomicBoolean(false);												/* Global error indicator */

  protected AtomicBoolean getError() {
    return error;
  }

  /*
   * Accessors to atomic variables
   */
  protected long getTotalWarmUpBytes() {
    return totalWarmUpBytes.get();
  }

  protected long getTotalTransferBytes() {
    return totalTransferBytes.get();
  }

  protected long getWarmUpTimeMicro() {
    return mWarmupTimeMicro.get();
  }

  private long getWarmUpTimeDurationMicro() {
    return mWarmupMicroDuration.get();
  }

  protected long getTransferTimeMicro() {
    return transferTimeMicroseconds.get();
  }

  private long getTransferTimeDurationMicro() {
    return mTransferMicroDuration.get();
  }

  private long getStartTransferMicro() {
    return mStartTransferMicro.get();
  }

  private long getStartWarmupMicro() {
    return mStartWarmupMicro.get();
  }

  protected void addTotalTransferBytes(long bytes) {
    totalTransferBytes.addAndGet(bytes);
  }

  protected void resetTotalTransferBytesToZero() {
    totalTransferBytes.set(0L);
  }

  private void addTotalWarmUpBytes(long bytes) {
    totalWarmUpBytes.addAndGet(bytes);
  }

  private void setWarmUpTimeMicro(long uTime) {
    mWarmupTimeMicro.set(uTime);
  }

  private void setTransferTimeMicro(long uTime) {
    transferTimeMicroseconds.set(uTime);
  }

  private String infoString = "";
  private String ipAddress = "";

  private boolean randomEnabled = false;																			/* Upload buffer randomisation is required */

  protected boolean getRandomEnabled() {
    return randomEnabled;
  }
  //boolean warmUpDone = false;

  private int postDataLength = 0;

  protected int getPostDataLength() {
    return postDataLength;
  }

  // warmup variables
  private AtomicLong mStartWarmupMicro = new AtomicLong(0);												/* Point in time when warm up process starts, uSecs */
  private AtomicLong mWarmupMicroDuration = new AtomicLong(0);											/* Total duration of warm up period, uSecs */
  private AtomicLong mWarmupTimeMicro = new AtomicLong(0);												/* Time elapsed since warm up process started, uSecs */
  private AtomicInteger warmupDoneCounter = new AtomicInteger(0);											/* Counter shows how many threads completed warm up process */
  private long mWarmupMaxTimeMicro = 0;																	/* Max time warm up is allowed to continue, uSecs */

  protected long getWarmupMaxTimeMicro() {
    return mWarmupMaxTimeMicro;
  }

  private int mWarmupMaxBytes = 0;																		/* Max bytes warm up is allowed to send */

  protected int getWarmupMaxBytes() {
    return mWarmupMaxBytes;
  }


  // transfer variables
  private AtomicLong mStartTransferMicro = new AtomicLong(0);												/* Point in time when transfer process starts, uSecs */
  private AtomicLong mTransferMicroDuration = new AtomicLong(0);											/* Total duration of transfer period, uSecs */
  private AtomicLong transferTimeMicroseconds = new AtomicLong(0);										/* Time elapsed since transfer process started, uSecs */
  private AtomicInteger transferDoneCounter = new AtomicInteger(0);										/* Counter shows how many threads completed trnasfer process */
  private long mTransferMaxTimeMicro = 0;																/* Max time transfer is allowed to continue, uSecs*/

  protected long getTransferMaxTimeMicro() {
    return mTransferMaxTimeMicro;
  }

  private int mTransferMaxBytes = 0;																	/* Max bytes transfer is allowed to send */

  protected int getTransferMaxBytes() {
    return mTransferMaxBytes;
  }

  //external monitor variables
  private AtomicLong timeElapsedSinceLastExternalMonitorUpdate = new AtomicLong(0);						/* Time elapsed since external monitor counter was updated last time, uSecs */

  // Various HTTP tests variables
  private int nThreads;																					/* Number of send/receive threads */

  //various buffers
  private int downloadBufferSize = 0;

  protected int getDownloadBufferSize() {
    return downloadBufferSize;
  }

  private int desiredReceiveBufferSize = 0;
  private int socketBufferSize = 0;
  private int uploadBufferSize = 0;

  protected int getUploadBufferSize() {
    return uploadBufferSize;
  }

  //private int connectionCounter = 0;
  private int receiveBufferSize = 0;
  private int sendBufferSize = 0;

  protected int getThreadsNum() {
    return nThreads;
  }														/* Accessor for number of threads */

  private boolean noDelay = false;

  private String testStatus = "FAIL";																	/* Test status, could be 'OK' or 'FAIL' */

  protected String getTestStatus() {
    return testStatus;
  }

  protected void setTestStatus(String value) {
    testStatus = value;
  }

  // Connection variables
  private String target = "";

  public String getTarget() {
    return target;
  }

  private String file = "";

  protected String getFile() {
    return file;
  }

  private int port = 0;

  protected int getPort() {
    return port;
  }

  private UploadStrategy uploadStrategyServerBased = UploadStrategy.PASSIVE;							/* Upload type selection strategy: simple upload or with server side measurements */

  private boolean downstream = true;


  private static double sGetBytesPerSecondWithMicroDuration(long durationMicro, long btsTotal) {
    double btsPerSec = 0;

    if (durationMicro != 0) {
      double timeSeconds = ((double) durationMicro) / 1000000.0;
      btsPerSec =  (((double) btsTotal) / timeSeconds);
    }

    //SKLogger.d(TAG(this), "getWarmupSpeedBytesPerSecond, using CLIENT value = " + btsPerSec);//HAHA remove in production
    return btsPerSec;
  }


  protected double getWarmupBytesPerSecond() {
    long btsTotal = getTotalWarmUpBytes();
    long durationMicro = getWarmUpTimeDurationMicro() == 0 ? (sGetMicroTime() - getStartWarmupMicro()) : getWarmUpTimeDurationMicro();

    return sGetBytesPerSecondWithMicroDuration(durationMicro, btsTotal);
  }


  // Returns -1 if not enough time has passed for sensible measurement.
  public double getTransferBytesPerSecond() {
    long btsTotal = getTotalTransferBytes();
    long durationMicro = getTransferTimeDurationMicro() == 0 ? (sGetMicroTime() - getStartTransferMicro()) : getTransferTimeDurationMicro();

//    double durationSeconds = ((double)durationMicro) / 1000000.0;
    //if (durationMicro < 1000000.0) // Anything
    if (durationMicro == 0) // Anything
    {
      // Not yet possible to return a valid result!
      return -1.0;
    }

    return sGetBytesPerSecondWithMicroDuration(durationMicro, btsTotal);
  }


  private int mSocketTimeoutMilliseconds = WRITETIMEOUT;

  private int getSocketTimeoutMilliseconds() {
    return mSocketTimeoutMilliseconds;
  }

  public void setSocketTimeoutMilliseconds(int value) {
    mSocketTimeoutMilliseconds = value;
  }

  private boolean mbIgnoreSocketTimeout = false;

  public boolean getIgnoreSocketTimeout() {
    return mbIgnoreSocketTimeout;
  }

  public void setIgnoreSocketTimeout(boolean value) {
    mbIgnoreSocketTimeout = value;
  }
}
