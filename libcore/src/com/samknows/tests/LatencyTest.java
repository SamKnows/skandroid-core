package com.samknows.tests;

//import android.annotation.SuppressLint;
import android.os.Debug;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.TestRunner.SKTestRunner;
import com.samknows.measurement.util.SKDateFormat;

import org.json.JSONObject;

public class LatencyTest extends SKAbstractBaseTest implements Runnable {

  public static final String STRING_ID = "JUDPLATENCY";
  private static final String LATENCYRUN = "Running latency and loss tests";
  private static final String LATENCYDONE = "Latency and loss tests completed";

  public static final String JSON_RTT_AVG = "rtt_avg";
  public static final String JSON_RTT_MIN = "rtt_min";
  public static final String JSON_RTT_MAX = "rtt_max";
  public static final String JSON_RTT_STDDEV = "rtt_stddev";
  public static final String JSON_RECEIVED_PACKETS = "received_packets";
  public static final String JSON_LOST_PACKETS = "lost_packets";

  // Create an interface class, which will allow us to inject a test socket for mock testing.
  public interface ISKUDPSocket {
    void open() throws SocketException;
    void send(DatagramPacket pack) throws IOException;
    void receive(DatagramPacket pack) throws IOException;
    void setSoTimeout(int timeout) throws SocketException;
    void close();

    long getStartTimeNanoseconds();
    long getTimeNowNanoseconds();

    InetAddress getInetAddressByName(String host) throws UnknownHostException;
  }

  // Define a real instantiation of the ISKUDPSocket interface, which is used for "real" testing.
  public class SKUDPSocket implements ISKUDPSocket {
    private DatagramSocket socket = null;

    public SKUDPSocket() {
    }

    public void open() throws SocketException {
      SKLogger.sAssert(socket == null);
      socket = new DatagramSocket();
      SKLogger.sAssert(socket != null);
    }

    public void send(DatagramPacket pack) throws IOException {
      if (socket == null) {
        SKLogger.sAssert(false);
        return;
      }
      socket.send(pack);
    }

    public void receive(DatagramPacket pack) throws IOException {
      if (socket == null) {
        SKLogger.sAssert(false);
        return;
      }
      socket.receive(pack);
    }

    public void setSoTimeout(int timeout) throws SocketException {
      if (socket == null) {
        SKLogger.sAssert(false);
        return;
      }
      socket.setSoTimeout(timeout);
    }

    public void close() {
      if (socket == null) {
        SKLogger.sAssert(false);
        return;
      }

      socket.close();
      socket = null;
    }

    public long getStartTimeNanoseconds() {
      return System.nanoTime();
    }

    public long getTimeNowNanoseconds() {
      return System.nanoTime();
    }

    public InetAddress getInetAddressByName(String host) throws UnknownHostException {
      return InetAddress.getByName(host);
    }
  }

  public static LatencyTest sCreateLatencyTest(List<Param> params) {
    LatencyTest ret = new LatencyTest();

    try {
      for (Param param : params) {
        String value = param.getValue();
        if (param.contains(TestFactory.TARGET)) {
          ret.setTarget(value);
        } else if (param.contains(TestFactory.PORT)) {
          ret.setPort(Integer.parseInt(value));
        } else if (param.contains(TestFactory.NUMBEROFPACKETS)) {
          ret.setNumberOfDatagrams(Integer.parseInt(value));
        } else if (param.contains(TestFactory.DELAYTIMEOUT)) {
          ret.setDelayTimeout(Integer.parseInt(value));
        } else if (param.contains(TestFactory.INTERPACKETTIME)) {
          ret.setInterPacketTime(Integer.parseInt(value));
        } else if (param.contains(TestFactory.PERCENTILE)) {
          ret.setPercentile(Integer.parseInt(value));
        } else if (param.contains(TestFactory.MAXTIME)) {
          ret.setMaxExecutionTimeMicroseconds(Long.parseLong(value));
        } else {
          SKLogger.sAssert(false);
          ret = null;
          break;
        }
      }
    } catch (NumberFormatException nfe) {
      SKLogger.sAssert(false);
      ret = null;
    }
    return ret;
  }

  static public class Result {
    public String target;
    public long rttMicroseconds;

    public Result(String _target, long nanoseconds) {
      target = _target;
      rttMicroseconds = nanoseconds / 1000;
    }
  }

  // Used internally ... and externally, by the HttpTest fallback for ClosestTarget testing.
  static void sCreateAndPushLatencyResultNanoseconds(BlockingQueue<Result> bq_results, String inTarget, double inRttNanoseconds) {
    if (bq_results != null) {
      // Pass-in the value in nanoseconds
      Result r = new Result(inTarget, (long) inRttNanoseconds);
      try {
        bq_results.put(r);
      } catch (InterruptedException e) {
        SKLogger.sAssert(LatencyTest.class, false);
      }
    }
  }


  // Used internally ...
  private void setLatencyValueNanoseconds(double inRttNanoseconds) {
    sCreateAndPushLatencyResultNanoseconds(bq_results, target, inRttNanoseconds);
  }

  public static int getPacketSize() {
    return UdpDatagram.PACKETSIZE;
  }

  @SuppressWarnings("serial")
  static private class PacketTimeOutException extends Exception {

  }

  public class UdpDatagram {
    static final int PACKETSIZE = 16;
    public static final int SERVERTOCLIENTMAGIC = 0x00006000;
    static final int CLIENTTOSERVERMAGIC = 0x00009000;

    int datagramid;
    @SuppressWarnings("unused")
    int starttimesec;
    @SuppressWarnings("unused")
    int starttimeusec;
    int magic;

    // When we make the "ping" we don't want to lose any time in memory
    // allocations, as much as possible should be ready (I miss structs...)
    byte[] arrayRepresentation;

    UdpDatagram(byte[] byteArray) {
      arrayRepresentation = byteArray;
      ByteBuffer bb = ByteBuffer.wrap(byteArray);
      datagramid = bb.getInt();
      starttimesec = bb.getInt();
      starttimeusec = bb.getInt();
      magic = bb.getInt();
    }

    UdpDatagram(int datagramid, int magic) {
      this.datagramid = datagramid;
      this.magic = magic;
      arrayRepresentation = new byte[PACKETSIZE];
      arrayRepresentation[0] = (byte) (datagramid >>> 24);
      arrayRepresentation[1] = (byte) (datagramid >>> 16);
      arrayRepresentation[2] = (byte) (datagramid >>> 8);
      arrayRepresentation[3] = (byte) (datagramid);
      arrayRepresentation[12] = (byte) (magic >>> 24);
      arrayRepresentation[13] = (byte) (magic >>> 16);
      arrayRepresentation[14] = (byte) (magic >>> 8);
      arrayRepresentation[15] = (byte) (magic);
    }

    byte[] byteArray() {
      return arrayRepresentation;
    }

    void setTime(long time) {
      int starttimesec = (int) (time / (int) 1e9);
      int starttimeusec = (int) ((time / (int) 1e3) % (int) 1e6);

      arrayRepresentation[4] = (byte) (starttimesec >>> 24);
      arrayRepresentation[5] = (byte) (starttimesec >>> 16);
      arrayRepresentation[6] = (byte) (starttimesec >>> 8);
      arrayRepresentation[7] = (byte) (starttimesec);
      arrayRepresentation[8] = (byte) (starttimeusec >>> 24);
      arrayRepresentation[9] = (byte) (starttimeusec >>> 16);
      arrayRepresentation[10] = (byte) (starttimeusec >>> 8);
      arrayRepresentation[11] = (byte) (starttimeusec);
    }
  }

  private LatencyTest() {
  }

  public String getStringID() {
    return STRING_ID;
  }

  public LatencyTest(String server, int port, int numdatagrams,
                     int interPacketTime, int delayTimeout) {
    target = server;
    this.port = port;
    this.numdatagrams = numdatagrams;
    results = new long[numdatagrams];
    this.interPacketTime = interPacketTime * 1000; // nanoSeconds
    this.delayTimeout = delayTimeout / 1000; // mSeconds
  }

  public void setBlockingQueueResult(BlockingQueue<Result> queue) {
    bq_results = queue;
  }

  @Override
  public int getNetUsage() {
    return UdpDatagram.PACKETSIZE * (sentPackets + recvPackets);
  }

  // @SuppressLint("NewApi")
  @Override
  public boolean isReady() {
    if (target.length() == 0) {
      SKLogger.sAssert(getClass(), false);
      return false;
    }
    if (port == 0) {
      SKLogger.sAssert(getClass(), false);
      return false;
    }
    if (numdatagrams == 0 || results == null) {
      SKLogger.sAssert(getClass(), false);
      return false;
    }
    if (delayTimeout == 0) {
      SKLogger.sAssert(getClass(), false);
      return false;
    }
    if (interPacketTime == 0) {
      SKLogger.sAssert(getClass(), false);
      return false;
    }
    if (percentile < 0 || percentile > 100) {
      SKLogger.sAssert(getClass(), false);
      return false;
    }

    return true;
  }

  ISKUDPSocket  mSKUDPSocket = null;

  @Override
  public void runBlockingTestToFinishInThisThread() {
    SKLogger.sAssert(mSKUDPSocket == null);
    mSKUDPSocket = new SKUDPSocket();

    // Note that we do NOT run a separate thread, when execute is called!
    runInCurrentThread();
  }

  public void executeWithSKUDPSocket(ISKUDPSocket skUDPSocket) {
    mSKUDPSocket = skUDPSocket;

    runInCurrentThread();
  }

  @Override
  public boolean isSuccessful() {
    return testStatus.equals("OK");
  }

  public String getInfo() {
    return infoString;
  }

  public String getTestStatus() {
    return testStatus;
  }

  public int getResultLatencyMilliseconds() {
    int result = ((int) (averageNanoseconds / 1000000));
    return result;
  }

  public int getResultLossPercent0To100() {
    int result = ((int) (100 * (((float) sentPackets - recvPackets) / sentPackets)));
    return result;
  }

  public long getResultJitterMilliseconds() {
    long jitterMicroseconds =  getAverageMicroseconds() - getMinimumMicroseconds();
    long jitterMilliseconds =  jitterMicroseconds / 1000;
    return jitterMilliseconds;
  }

  public long getAverageMicroseconds() {
    return (long) (averageNanoseconds / 1000);
  }

  public long getMinimumMicroseconds() {
    return (long) (minimumNanoseconds / 1000);
  }

  public long getMaximumMicroseconds() {
    return (long) (maximumNanoseconds / 1000);
  }

  public long getStdDeviationMicroseconds() {
    return (long) (stddeviationNanoseconds / 1000);
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public long getJitter() {
    return getAverageMicroseconds() - getMinimumMicroseconds();
  }

  public int getSentPackets() {
    return sentPackets;
  }

  public int getReceivedPackets() {
    return recvPackets;
  }

  public int getLostPackets() {
    return sentPackets - recvPackets;
  }

  private Long mTimestamp = SKAbstractBaseTest.sGetUnixTimeStamp();
  @Override
  public synchronized void finish() {
    mTimestamp = SKAbstractBaseTest.sGetUnixTimeStamp();
    status = STATUS.DONE;
  }

  @Override
  public long getTimestamp() {
    return mTimestamp;
  }

  @Override
  public JSONObject getJSONResult() {
    Map<String, Object> output = new HashMap<>();

    // 0 - test string id
    output.put(JsonData.JSON_TYPE, STRING_ID);

    // 1- time
    output.put(JsonData.JSON_TIMESTAMP, mTimestamp);

    // 2- date
    output.put(JsonData.JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(mTimestamp * 1000)));

    // 3 - test status
    output.put(JsonData.JSON_SUCCESS, isSuccessful());

    // 4 - target
    output.put(JsonData.JSON_TARGET, target);

    // 5 - target ipaddress
    output.put(JsonData.JSON_TARGET_IPADDRESS, ipAddress);

    // 6- average
    output.put(JSON_RTT_AVG, getAverageMicroseconds());

    // 7 -minimum
    output.put(JSON_RTT_MIN, getMinimumMicroseconds());

    // 8 - maximum
    output.put(JSON_RTT_MAX, getMaximumMicroseconds());

    // 9 - standard deviation
    output.put(JSON_RTT_STDDEV, getStdDeviationMicroseconds());

    // 10 - recvPackets
    output.put(JSON_RECEIVED_PACKETS, recvPackets);

    // 11 - lost packets
    output.put(JSON_LOST_PACKETS, getLostPackets());

    //setOutput(o.toArray(new String[1]));
    JSONObject json_output = new JSONObject(output);
    return json_output;
  }


  // The test can ALSO get run via ClosestTarget, via a new Thread(theLatencyTest), knowing that LatencyTest
  // is a runnable; using this method!
  @Override
  public void run() {
    runInCurrentThread();
  }

  boolean mbAlreadyRunning = false;
  private void runInCurrentThread() {
    SKLogger.sAssert(mbAlreadyRunning == false);
    mbAlreadyRunning = true;

    setStateToRunning();

    if (mSKUDPSocket == null) {
      // This should happen ONLY in the "live" app - it should not happen in the unit test.
      mSKUDPSocket = new SKUDPSocket();
    }

    setStateToRunning();
    //set to zero internal variables in case the same test object is executed severals times
    sentPackets = 0;
    recvPackets = 0;
    startTimeNanonseconds = mSKUDPSocket.getStartTimeNanoseconds();

    ISKUDPSocket socket = null;

    try {
      socket = mSKUDPSocket;
      socket.open();
      socket.setSoTimeout(delayTimeout);
    } catch (SocketException e) {
      failure();
      return;
    }

//		try {
//			int sendBufferSizeBytes = socket.getSendBufferSize();
//			Log.d(getClass().getName(), "LatencyTest: sendBufferSizeBytes=" + sendBufferSizeBytes);
//			int receiveBufferSizeBytes = socket.getReceiveBufferSize();
//			Log.d(getClass().getName(), "LatencyTest: receiveBufferSizeBytes=" + receiveBufferSizeBytes);
//		} catch (SocketException e1) {
//			SKLogger.sAssert(getClass(),  false);
//		}

    InetAddress address = null;
    try {
      address = mSKUDPSocket.getInetAddressByName(target);
      ipAddress = address.getHostAddress();
    } catch (UnknownHostException e) {
      SKLogger.sAssert(false);
      failure();
      socket.close();
      socket = null;
      return;
    }

    for (int i = 0; i < numdatagrams; ++i) {

      if (maxExecutionTimeNanoseconds > 0) {
        long timeSoFarNano = socket.getTimeNowNanoseconds() - startTimeNanonseconds;
        if (timeSoFarNano > maxExecutionTimeNanoseconds) {
          break;
        }
      }

      UdpDatagram data = new UdpDatagram(i, UdpDatagram.CLIENTTOSERVERMAGIC);
      byte[] buf = data.byteArray();
      DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
      long answerTime = 0;

      // It isn't the current time as in the original but a random value.
      // Let's hope nobody changes the server to make this important...
      long time = mSKUDPSocket.getTimeNowNanoseconds();
      data.setTime(time);

      try {
        socket.send(packet);
        sentPackets++;
      } catch (IOException e) {
        continue;
      }

      try {
        UdpDatagram answer;
        do {
          //Checks for the current time and set the SoTimeout accordingly
          //because of duplicate packets or packets received after delayTimeout
          long now = mSKUDPSocket.getTimeNowNanoseconds();
          long timeout = delayTimeout - (now - time) / 1000000;
          if (timeout < 0) {
            throw new PacketTimeOutException();
          }
          socket.setSoTimeout((int) timeout);
          socket.receive(packet);
          answer = new UdpDatagram(buf);

          if (answer.magic == UdpDatagram.SERVERTOCLIENTMAGIC) {
            if (answer.datagramid == i) {
              break;
            }
          }

        } while (true);

        answerTime = mSKUDPSocket.getTimeNowNanoseconds();
        recvPackets++;

        if (getShouldCancel()) {
          if (Debug.isDebuggerConnected()) {
            Log.d("DEBUG", "Latency - run - cancel test!");
          }
          break;
        }
      } catch (SocketTimeoutException e) {
        continue;
      } catch (IOException e) {
        continue;
      } catch (PacketTimeOutException e) {
        continue;
      }


      long rtt = answerTime - time;
      results[recvPackets - 1] = rtt;

      long latencyMilli = rtt / 1000000;
      SKTestRunner.sDoReportCurrentLatencyCalculated(latencyMilli);

      sleep(rtt);
    }

    socket.close();

    getStats();

    setLatencyValueNanoseconds(averageNanoseconds);
  }


  private void sleep(long rtt) {
    long sleepPeriod = interPacketTime - rtt;

    if (sleepPeriod > 0) {
      long millis = (long) Math.floor(sleepPeriod / 1000000);
      int nanos = (int) sleepPeriod % 1000000;
      try {
        Thread.sleep(millis, nanos);
      } catch (InterruptedException e) {
        SKLogger.sAssert(false);
      }
    }
  }

  private void failure() {
    SKLogger.sAssert(false);
    testStatus = "FAIL";
    finish();
  }

  private void getStats() {
    if (recvPackets <= 0) {
      failure();
      return;
    }
    testStatus = "OK";

    // Calculate statistics
    // Results sorted in order to take into account the percentile
    int nResults = 0;
    if (recvPackets < 100) {
      nResults = recvPackets;
    } else {
      nResults = (int) Math.ceil(percentile / 100.0 * recvPackets);
    }
    Arrays.sort(results, 0, recvPackets);
    minimumNanoseconds = results[0];
    maximumNanoseconds = results[nResults - 1];
    averageNanoseconds = 0;
    for (int i = 0; i < nResults; i++) {
      averageNanoseconds += results[i];
    }
    averageNanoseconds /= nResults;

    stddeviationNanoseconds = 0;

    for (int i = 0; i < nResults; ++i) {
      stddeviationNanoseconds += Math.pow(results[i] - averageNanoseconds, 2);
    }

    if (nResults - 1 > 0) {
      stddeviationNanoseconds = Math.sqrt(stddeviationNanoseconds / (nResults - 1));
    } else {
      stddeviationNanoseconds = 0;
    }

    // Return results
    finish();
    infoString = LATENCYDONE;

  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setNumberOfDatagrams(int n) {
    numdatagrams = n;
    results = new long[numdatagrams];
  }

  public void setDelayTimeout(int delay) {
    delayTimeout = delay / 1000;
  }

  public void setInterPacketTime(int time) {
    interPacketTime = time * 1000; // nanoSeconds
  }

  public void setPercentile(int n) {
    percentile = n;
  }

  public void setMaxExecutionTimeMicroseconds(long time) {
    maxExecutionTimeNanoseconds = time * 1000; // convert Microsecodns to NanoSeconds
  }

  public boolean isProgressAvailable() {
    return true;
  }

  @Override
  public int getProgress0To100() {

    if (mSKUDPSocket == null) {
      // Not yet prepared!
      return 0;
    }

    double retTime = 0;
    double retPackets = 0;
    if (maxExecutionTimeNanoseconds > 0) {
      long currTime = (mSKUDPSocket.getTimeNowNanoseconds() - startTimeNanonseconds);
      retTime = (double) currTime / maxExecutionTimeNanoseconds;
    }
    retPackets = (double) sentPackets / numdatagrams;

    double ret = retTime > retPackets ? retTime : retPackets;
    ret = ret > 1 ? 1 : ret;
    return (int) (ret * 100);
  }

  public String getTarget() {
    return target;
  }

  private String target = "";
  private int port = 0;
  private String infoString = LATENCYRUN;
  private String ipAddress;
  private String testStatus = "FAIL";

  private double averageNanoseconds = 0.0;
  private double stddeviationNanoseconds = 0.0;
  private long minimumNanoseconds = 0;
  private long maximumNanoseconds = 0;
  private long startTimeNanonseconds = 0;
  private long maxExecutionTimeNanoseconds = 0;

  private double percentile = 100;
  private int numdatagrams = 0;
  private int delayTimeout = 0;
  private int sentPackets = 0;
  private int recvPackets = 0;
  private int interPacketTime = 0;
  private long[] results = null;
  private BlockingQueue<Result> bq_results = null;
}
