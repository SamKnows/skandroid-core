package com.samknows.tests;

//import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKLogger;
import com.samknows.tests.Param;
import com.samknows.tests.HttpTest.UploadStrategy;

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
	private static final String TARGET = "target";
	private static final String PORT = "port";
	private static final String FILE = "file";

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
	private static final String NTHREADS = "numberOfThreads";
	private static final String BUFFERSIZE = "bufferSize";
	private static final String SENDBUFFERSIZE = "sendBufferSize";
	private static final String RECEIVEBUFFERSIZE = "receiveBufferSize";
	private static final String POSTDATALENGTH = "postDataLength";
	private static final String SENDDATACHUNK = "sendDataChunk";

	private static final String[] HTTPTESTPARAMLIST = { TESTTYPE, TARGET, PORT,
			FILE, WARMUPMAXTIME, WARMUPMAXBYTES, TRANSFERMAXTIME,
			TRANSFERMAXBYTES, NTHREADS, BUFFERSIZE, SENDBUFFERSIZE,
			RECEIVEBUFFERSIZE, POSTDATALENGTH, SENDDATACHUNK };

	/*
	 * constants for creating a latency test
	 */
	public static final String NUMBEROFPACKETS = "numberOfPackets";
	public static final String DELAYTIMEOUT = "delayTimeout";
	public static final String INTERPACKETTIME = "interPacketTime";
	public static final String PERCENTILE = "percentile";
	public static final String MAXTIME = "maxTime";

	private static final String[] LATENCYTESTPARAMLIST = { TESTTYPE, TARGET,
			PORT, NUMBEROFPACKETS, DELAYTIMEOUT, INTERPACKETTIME, PERCENTILE,
			MAXTIME };

	//public static final String[] CLOSESTTARGETPARAMLIST = LATENCYTESTPARAMLIST;

	public static Test create(List<Param> params) {
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

	public static String getTestString(String testType, List<Param> params){
		String ret ="";
		if(testType.equalsIgnoreCase(DOWNSTREAMTHROUGHPUT)){
			for(Param p: params){
				
				if(p.contains(NTHREADS))
					ret = Integer.parseInt(p.getValue()) == 1 ? HttpTest.DOWNSTREAMSINGLE : HttpTest.DOWNSTREAMMULTI; 
			}
		}else if(testType.equalsIgnoreCase( UPSTREAMTHROUGHPUT)){
			for(Param p: params){
				if(p.contains(NTHREADS))
					ret = Integer.parseInt(p.getValue()) == 1 ? HttpTest.UPSTREAMSINGLE : HttpTest.UPSTREAMMULTI; 
			}
		}else if (testType.equalsIgnoreCase(LATENCY)){
			ret = LatencyTest.STRING_ID;
		}
		return ret;
	}
	
	public static Test create(String testType, List<Param> params) {
		Test ret = null;
		if (testType.equalsIgnoreCase(DOWNSTREAMTHROUGHPUT)) {
			ret = createHttpTest(DOWNSTREAM, params);
		} else if (testType.equalsIgnoreCase( UPSTREAMTHROUGHPUT)) {
			ret = createHttpTest(UPSTREAM, params);
		} else if (testType.equalsIgnoreCase(LATENCY)) {
			ret = createLatencyTest(params);
		} else if (testType.equalsIgnoreCase(CLOSESTTARGET)) {
			ret = createClosestTarget(params);
		} else if (testType.equalsIgnoreCase(PROXYDETECTOR)) {
			ret = createProxyDetector(params);
		}
		if (ret != null) {
			if (!ret.isReady()) {
				ret = null;
			}
		}
		return ret;
	}

	private static ClosestTarget createClosestTarget(List<Param> params) {
		return new ClosestTarget(params);
	}

	private static ProxyDetector createProxyDetector(List<Param> params) {
		ProxyDetector ret = new ProxyDetector();
		try {
			for (Param param : params) {
				String value = param.getValue();
				if (param.contains(TARGET)) {
					ret.setTarget(value);
				} else if (param.contains( PORT)) {
					ret.setPort(Integer.parseInt(value));
				} else if (param.contains( FILE)) {
					ret.setFile(value);
				} else {
					ret = null;
					break;
				}
			}
		} catch (NumberFormatException nfe) {
			ret = null;
		}
		return ret;
	}

	public static LatencyTest createLatencyTest(List<Param> params) {
		LatencyTest ret = new LatencyTest();

		try {
			for (Param param : params) {
				String value = param.getValue();
				if (param.contains(TARGET)) {
					ret.setTarget(value);
				} else if (param.contains( PORT)) {
					ret.setPort(Integer.parseInt(value));
				} else if (param.contains( NUMBEROFPACKETS)) {
					ret.setNumberOfDatagrams(Integer.parseInt(value));
				} else if (param.contains( DELAYTIMEOUT)) {
					ret.setDelayTimeout(Integer.parseInt(value));
				} else if (param.contains( INTERPACKETTIME)) {
					ret.setInterPacketTime(Integer.parseInt(value));
				} else if (param.contains( PERCENTILE)) {
					ret.setPercentile(Integer.parseInt(value));
				} else if (param.contains( MAXTIME)) {
					ret.setMaxExecutionTime(Long.parseLong(value));
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

	private static HttpTest createHttpTest(String direction, List<Param> params) {
		UploadStrategy uploadStrategyServerBased = UploadStrategy.PASSIVE;
		HttpTest result = null;

		if ( direction.equals(DOWNSTREAM)){
			result = new DownloadTest( params );
		}
		else if ( direction.equals(UPSTREAM)){

			for (Param param : params) {
				if (param.contains(UPLOADSTRATEGY)){
					uploadStrategyServerBased = UploadStrategy.ACTIVE;
					break;
				}
			}

			if ( uploadStrategyServerBased ==  UploadStrategy.ACTIVE ) {
				result = new ActiveServerloadTest(params);
			} else {
				result = new PassiveServerUploadTest(params);
			}
		}

		if( result != null ) {
      if (result.isReady()) {
        return result;
      }
    }

		return null;
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
	public static final long getMaxUsage(String type, List<Param> params){
		long ret = 0;
		if (type.equalsIgnoreCase(DOWNSTREAMTHROUGHPUT)|| type.equalsIgnoreCase(UPSTREAMTHROUGHPUT)){
			ret = getMaxUsageHttp(params);
		} else if(type.equalsIgnoreCase(LATENCY)){
			ret = getMaxUsageLatency(params);
		}
		return ret;
	}
	
	// TODO move to test?
	private static long getMaxUsageHttp(List<Param> params){
		long ret = 0;
		for (Param p:params){
			if (p.isName(WARMUPMAXBYTES) || p.isName(TRANSFERMAXBYTES )){
				ret += Long.parseLong(p.getValue());
			}
		}
		return ret;
	}
	
	private static long getMaxUsageLatency(List<Param> params){
		long ret = 0;
		for (Param p: params){
			if (p.isName(NUMBEROFPACKETS)){
				ret = Long.parseLong(p.getValue()) * LatencyTest.getPacketSize(); 
			}
		}
		return ret;
	}
	
}
