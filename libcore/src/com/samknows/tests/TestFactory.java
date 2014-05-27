package com.samknows.tests;

import com.samknows.tests.Param;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class TestFactory {

	public static final String DOWNSTREAMTHROUGHPUT = "downstreamthroughput";
	public static final String UPSTREAMTHROUGHPUT = "upstreamthroughput";
	public static final String LATENCY = "latency";
	public static final String PROXYDETECTOR = "proxydetector";
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

	public static final String[] HTTPTESTPARAMLIST = { TESTTYPE, TARGET, PORT,
			FILE, WARMUPMAXTIME, WARMUPMAXBYTES, TRANSFERMAXTIME,
			TRANSFERMAXBYTES, NTHREADS, BUFFERSIZE, SENDBUFFERSIZE,
			RECEIVEBUFFERSIZE, POSTDATALENGTH, SENDDATACHUNK };

	/*
	 * constants for creating a latency test
	 */
	private static final String NUMBEROFPACKETS = "numberOfPackets";
	private static final String DELAYTIMEOUT = "delayTimeout";
	private static final String INTERPACKETTIME = "interPacketTime";
	private static final String PERCENTILE = "percentile";
	private static final String MAXTIME = "maxTime";

	public static final String[] LATENCYTESTPARAMLIST = { TESTTYPE, TARGET,
			PORT, NUMBEROFPACKETS, DELAYTIMEOUT, INTERPACKETTIME, PERCENTILE,
			MAXTIME };

	public static final String[] CLOSESTTARGETPARAMLIST = LATENCYTESTPARAMLIST;

	public static Test create(List<Param> params) {
		Param testType = null;
		for (Param p : params) {
			if (Test.paramMatch(p.getName(), TESTTYPE)) {
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
		if(Test.paramMatch(testType, DOWNSTREAMTHROUGHPUT)){
			for(Param p: params){
				
				if(Test.paramMatch(p.getName(), NTHREADS))
					ret = Integer.parseInt(p.getValue()) == 1 ? HttpTest.DOWNSTREAMSINGLE : HttpTest.DOWNSTREAMMULTI; 
			}
		}else if(Test.paramMatch(testType, UPSTREAMTHROUGHPUT)){
			for(Param p: params){
				if(Test.paramMatch(p.getName(), NTHREADS))
					ret = Integer.parseInt(p.getValue()) == 1 ? HttpTest.UPSTREAMSINGLE : HttpTest.UPSTREAMMULTI; 
			}
		}else if (Test.paramMatch(testType, LATENCY)){
			ret = LatencyTest.STRING_ID;
		}
		return ret;
	}
	
	public static Test create(String testType, List<Param> params) {
		Test ret = null;
		if (Test.paramMatch(testType, DOWNSTREAMTHROUGHPUT)) {
			ret = createHttpTest(DOWNSTREAM, params);
		} else if (Test.paramMatch(testType, UPSTREAMTHROUGHPUT)) {
			ret = createHttpTest(UPSTREAM, params);
		} else if (Test.paramMatch(testType, LATENCY)) {
			ret = createLatencyTest(params);
		} else if (Test.paramMatch(testType, CLOSESTTARGET)) {
			ret = createClosestTarget(params);
		} else if (Test.paramMatch(testType, PROXYDETECTOR)) {
			ret = createProxyDetector(params);
		}
		if (ret != null && !ret.isReady()) {
			ret = null;
		}
		return ret;
	}

	public static ClosestTarget createClosestTarget(List<Param> params) {
		ClosestTarget ret = new ClosestTarget();
		try {
			for (Param curr : params) {
				String param = curr.getName();
				String value = curr.getValue();
				if (Test.paramMatch(param, TARGET)) {
					ret.addTarget(value);
				} else if (Test.paramMatch(param, PORT)) {
					ret.setPort(Integer.parseInt(value));
				} else if (Test.paramMatch(param, NUMBEROFPACKETS)) {
					ret.setNumberOfDatagrams(Integer.parseInt(value));
				} else if (Test.paramMatch(param, DELAYTIMEOUT)) {
					ret.setDelayTimeout(Integer.parseInt(value));
				} else if (Test.paramMatch(param, INTERPACKETTIME)) {
					ret.setInterPacketTime(Integer.parseInt(value));
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

	private static ProxyDetector createProxyDetector(List<Param> params) {
		ProxyDetector ret = new ProxyDetector();
		try {
			for (Param curr : params) {
				String param = curr.getName();
				String value = curr.getValue();
				if (Test.paramMatch(param, TARGET)) {
					ret.setTarget(value);
				} else if (Test.paramMatch(param, PORT)) {
					ret.setPort(Integer.parseInt(value));
				} else if (Test.paramMatch(param, FILE)) {
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

	private static LatencyTest createLatencyTest(List<Param> params) {
		LatencyTest ret = new LatencyTest();

		try {
			for (Param curr : params) {
				String param = curr.getName();
				String value = curr.getValue();
				if (Test.paramMatch(param, TARGET)) {
					ret.setTarget(value);
				} else if (Test.paramMatch(param, PORT)) {
					ret.setPort(Integer.parseInt(value));
				} else if (Test.paramMatch(param, NUMBEROFPACKETS)) {
					ret.setNumberOfDatagrams(Integer.parseInt(value));
				} else if (Test.paramMatch(param, DELAYTIMEOUT)) {
					ret.setDelayTimeout(Integer.parseInt(value));
				} else if (Test.paramMatch(param, INTERPACKETTIME)) {
					ret.setInterPacketTime(Integer.parseInt(value));
				} else if (Test.paramMatch(param, PERCENTILE)) {
					ret.setPercentile(Integer.parseInt(value));
				} else if (Test.paramMatch(param, MAXTIME)) {
					ret.setMaxExecutionTime(Long.parseLong(value));
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

	private static HttpTest createHttpTest(String direction, List<Param> params) {
		HttpTest ret = new HttpTest(direction);
		try {
			for (Param curr : params) {
				String param = curr.getName();
				String value = curr.getValue();
				if (Test.paramMatch(param, TARGET)) {
					ret.setTarget(value);
				} else if (Test.paramMatch(param, PORT)) {
					ret.setPort(Integer.parseInt(value));
				} else if (Test.paramMatch(param, FILE)) {
					ret.setFile(value);
				} else if (Test.paramMatch(param, WARMUPMAXTIME)) {
					ret.setWarmupMaxTime(Integer.parseInt(value));
				} else if (Test.paramMatch(param, WARMUPMAXBYTES)) {
					ret.setWarmupMaxBytes(Integer.parseInt(value));
				} else if (Test.paramMatch(param, TRANSFERMAXTIME)) {
					ret.setTransferMaxTime(Integer.parseInt(value));
				} else if (Test.paramMatch(param, TRANSFERMAXBYTES)) {
					ret.setTransferMaxBytes(Integer.parseInt(value));
				} else if (Test.paramMatch(param, NTHREADS)) {
					ret.setNumberOfThreads(Integer.parseInt(value));
				} else if (Test.paramMatch(param, BUFFERSIZE)) {
					ret.setBufferSize(Integer.parseInt(value));
				} else if (Test.paramMatch(param, SENDBUFFERSIZE)) {
					ret.setSendBufferSize(Integer.parseInt(value));
				} else if (Test.paramMatch(param, RECEIVEBUFFERSIZE)) {
					ret.setReceiveBufferSize(Integer.parseInt(value));
				} else if (Test.paramMatch(param, SENDDATACHUNK)) {
					ret.setSendDataChunk(Integer.parseInt(value));
				} else if (Test.paramMatch(param, POSTDATALENGTH)) {
					ret.setPostDataLenght(Integer.parseInt(value));
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

	public static final ArrayList<Param> testConfiguration(
			List<Param> allParam, String testType) {
		ArrayList<Param> ret = new ArrayList<Param>();
		if (testType.equalsIgnoreCase(DOWNSTREAMTHROUGHPUT)) {
			ret = testConfiguration(allParam, HTTPTESTPARAMLIST);
		} else if (testType.equalsIgnoreCase(LATENCY)) {
			ret = testConfiguration(allParam, LATENCYTESTPARAMLIST);
		} else {
			ret = null;
		}
		return ret;
	}

	public static final ArrayList<Param> testConfiguration(
			List<Param> allParam, String[] configKey) {
		HashSet<String> toInclude = new HashSet<String>();
		ArrayList<Param> ret = new ArrayList<Param>();
		for (String k : configKey) {
			toInclude.add(k);
		}
		for (Param curr : allParam) {
			if (toInclude.contains(curr.getName())) {
				ret.add(curr);
			}
		}
		return ret;
	}
	
	public static final long getMaxUsage(String type, List<Param> params){
		long ret = 0;
		if(type.equalsIgnoreCase(DOWNSTREAMTHROUGHPUT)|| type.equalsIgnoreCase(UPSTREAMTHROUGHPUT)){
			ret = getMaxUsageHttp(params);
		}else if(type.equalsIgnoreCase(LATENCY)){
			ret = getMaxUsageLatency(params);
		}
		return ret;
	}
	
	private static long getMaxUsageHttp(List<Param> params){
		long ret = 0;
		for(Param p:params){
			if(p.isName(WARMUPMAXBYTES) || p.isName(TRANSFERMAXBYTES )){
				ret += Long.parseLong(p.getValue());
			}
		}
		return ret;
	}
	
	private static long getMaxUsageLatency(List<Param> params){
		long ret = 0;
		for(Param p: params){
			if(p.isName(NUMBEROFPACKETS)){
				ret = Long.parseLong(p.getValue()) * LatencyTest.getPacketSize(); 
			}
		}
		return ret;
	}
	
}
