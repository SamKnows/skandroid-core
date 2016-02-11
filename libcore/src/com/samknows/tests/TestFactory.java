package com.samknows.tests;

//import com.samknows.libcore.SKLogger;

import com.samknows.libcore.SKLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class TestFactory {

  public static final String DOWNSTREAMTHROUGHPUT = "downstreamthroughput";
  public static final String UPSTREAMTHROUGHPUT = "upstreamthroughput";
  public static final String LATENCY = "latency";
  private static final String PROXYDETECTOR = "proxydetector";
  public static final String CLOSESTTARGET = "closesttarget";
  /*
	 * constants shared among different tests
	 */
  private static final String TESTTYPE = "testType";
  public static final String TARGET = "target";
  public static final String PORT = "port";
  public static final String FILE = "file";

  /*
	 * constants for creating a http test
	 */
  public static final String UPLOADSTRATEGY = "strategy";

  private static final String WARMUPMAXTIME = "warmupMaxTime";
  private static final String WARMUPMAXBYTES = "warmupMaxBytes";
  private static final String TRANSFERMAXTIME = "transferMaxTime";
  private static final String TRANSFERMAXBYTES = "transferMaxBytes";
  private static final String NTHREADS = "numberOfThreads";
  private static final String BUFFERSIZE = "bufferSize";
  private static final String SENDBUFFERSIZE = "sendBufferSize";
  private static final String RECEIVEBUFFERSIZE = "receiveBufferSize";
  private static final String POSTDATALENGTH = "postDataLength";
  private static final String SENDDATACHUNK = "sendDataChunk";

  private static final String[] HTTPTESTPARAMLIST = {TESTTYPE, TARGET, PORT,
      FILE, WARMUPMAXTIME, WARMUPMAXBYTES, TRANSFERMAXTIME,
      TRANSFERMAXBYTES, NTHREADS, BUFFERSIZE, SENDBUFFERSIZE,
      RECEIVEBUFFERSIZE, POSTDATALENGTH, SENDDATACHUNK};

  /*
	 * constants for creating a latency test
	 */
  public static final String NUMBEROFPACKETS = "numberOfPackets";
  public static final String DELAYTIMEOUT = "delayTimeout";
  public static final String INTERPACKETTIME = "interPacketTime";
  public static final String PERCENTILE = "percentile";
  public static final String MAXTIME = "maxTime";

  private static final String[] LATENCYTESTPARAMLIST = {TESTTYPE, TARGET,
      PORT, NUMBEROFPACKETS, DELAYTIMEOUT, INTERPACKETTIME, PERCENTILE,
      MAXTIME};

  //public static final String[] CLOSESTTARGETPARAMLIST = LATENCYTESTPARAMLIST;

  public static SKAbstractBaseTest create(List<Param> params) {
    Param testType = null;
    for (Param p : params) {
      if (p.contains(TESTTYPE)) {
        testType = p;
      }
    }
    if (testType == null) {
      return null;
    }
    params.remove(testType);
    return create(testType.getValue(), params);
  }

  public static String getTestString(String testType, List<Param> params) {
    String ret = "";
    if (testType.equalsIgnoreCase(DOWNSTREAMTHROUGHPUT)) {
      for (Param p : params) {

        if (p.contains(NTHREADS))
          ret = Integer.parseInt(p.getValue()) == 1 ? HttpTest.DOWNSTREAMSINGLE : HttpTest.DOWNSTREAMMULTI;
      }
    } else if (testType.equalsIgnoreCase(UPSTREAMTHROUGHPUT)) {
      for (Param p : params) {
        if (p.contains(NTHREADS))
          ret = Integer.parseInt(p.getValue()) == 1 ? HttpTest.UPSTREAMSINGLE : HttpTest.UPSTREAMMULTI;
      }
    } else if (testType.equalsIgnoreCase(LATENCY)) {
      ret = LatencyTest.STRING_ID;
    }
    return ret;
  }

  public static SKAbstractBaseTest create(String testType, List<Param> params) {
    SKAbstractBaseTest ret = null;
    if (testType.equalsIgnoreCase(DOWNSTREAMTHROUGHPUT)) {
      ret = DownloadTest.sCreateDownloadTest(params);
    } else if (testType.equalsIgnoreCase(UPSTREAMTHROUGHPUT)) {
      ret = UploadTest.sCreateUploadTest(params);
    } else if (testType.equalsIgnoreCase(LATENCY)) {
      ret = LatencyTest.sCreateLatencyTest(params);
    } else if (testType.equalsIgnoreCase(CLOSESTTARGET)) {
      ret = ClosestTarget.sCreateClosestTarget(params);
    } else if (testType.equalsIgnoreCase(PROXYDETECTOR)) {
      ret = ProxyDetector.sCreateProxyDetector(params);
    }
    if (ret != null) {
      if (!ret.isReady()) {
        SKLogger.sAssert(false);
        ret = null;
      }
    } else {
      SKLogger.sAssert(false);
    }
    return ret;
  }

  public static final ArrayList<Param> testConfiguration(List<Param> allParam) {
    ArrayList<Param> ret = null;
    Param testType = null;
    for (Param p : allParam) {
      if (p.getName().equals("testid")) {
        testType = p;
      }
    }
    if (testType != null) {
      allParam.remove(testType);
      ret = testConfiguration(allParam, testType.getValue());
    }
    return ret;
  }

  private static ArrayList<Param> testConfiguration(
      List<Param> allParam, String testType) {
    ArrayList<Param> ret = new ArrayList<>();
    if (testType.equalsIgnoreCase(DOWNSTREAMTHROUGHPUT)) {
      ret = testConfiguration(allParam, HTTPTESTPARAMLIST);
    } else if (testType.equalsIgnoreCase(LATENCY)) {
      ret = testConfiguration(allParam, LATENCYTESTPARAMLIST);
    } else {
      ret = null;
    }
    return ret;
  }

  private static ArrayList<Param> testConfiguration(
      List<Param> allParam, String[] configKey) {
    HashSet<String> toInclude = new HashSet<>();
    ArrayList<Param> ret = new ArrayList<>();
    Collections.addAll(toInclude, configKey);
    for (Param curr : allParam) {
      if (toInclude.contains(curr.getName())) {
        ret.add(curr);
      }
    }
    return ret;
  }

  //TODO Refactor this
  public static final long getMaxUsage(String type, List<Param> params) {
    long ret = 0;
    if (type.equalsIgnoreCase(DOWNSTREAMTHROUGHPUT) || type.equalsIgnoreCase(UPSTREAMTHROUGHPUT)) {
      ret = getMaxUsageHttp(params);
    } else if (type.equalsIgnoreCase(LATENCY)) {
      ret = getMaxUsageLatency(params);
    }
    return ret;
  }

  // TODO move to test?
  private static long getMaxUsageHttp(List<Param> params) {
    long ret = 0;
    for (Param p : params) {
      if (p.isName(WARMUPMAXBYTES) || p.isName(TRANSFERMAXBYTES)) {
        ret += Long.parseLong(p.getValue());
      }
    }
    return ret;
  }

  private static long getMaxUsageLatency(List<Param> params) {
    long ret = 0;
    for (Param p : params) {
      if (p.isName(NUMBEROFPACKETS)) {
        ret = Long.parseLong(p.getValue()) * LatencyTest.getPacketSize();
      }
    }
    return ret;
  }
}
