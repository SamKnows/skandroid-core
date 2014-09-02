package com.samknows.tests;

//import android.annotation.SuppressLint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.util.SKDateFormat;

public class LatencyTest extends Test {

	public static final String STRING_ID = "JUDPLATENCY";
	public static final int STATUSFIELD = 2;
	public static final int TARGETFIELD = 3;
	public static final int IPTARGETFIELD = 4;
	public static final int AVERAGEFIELD = 5;
	private static final String LATENCYRUN = "Running latency and loss tests";
	private static final String LATENCYDONE = "Latency and loss tests completed";

	public static final String JSON_RTT_AVG = "rtt_avg";
	public static final String JSON_RTT_MIN = "rtt_min";
	public static final String JSON_RTT_MAX = "rtt_max";
	public static final String JSON_RTT_STDDEV = "rtt_stddev";
	public static final String JSON_RECEIVED_PACKETS = "received_packets";
	public static final String JSON_LOST_PACKETS = "lost_packets";

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
     		Result r = new Result(inTarget, (long)inRttNanoseconds);
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
	
	public static int getPacketSize(){
		return UdpDatagram.PACKETSIZE;
	}

	@SuppressWarnings("serial")
	static private class PacketTimeOutException extends Exception{
		
	}
	
	static private class UdpDatagram {
		static final int PACKETSIZE = 16;
		static final int SERVERTOCLIENTMAGIC = 0x00006000;
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

	public LatencyTest() {
	}

	public String getStringID() {
		return STRING_ID;
	}

	public LatencyTest(String server, int port, int numdatagrams) {
		this.numdatagrams = numdatagrams;
		results = new long[numdatagrams];
	}

	public LatencyTest(String server, int port, int numdatagrams,
			int interPacketTime) {
		target = server;
		this.port = port;
		this.numdatagrams = numdatagrams;
		results = new long[numdatagrams];
		this.interPacketTime = interPacketTime * 1000; // nanoSeconds
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
			SKLogger.sAssert(getClass(),  false);
			return false;
		}
		if (port == 0) {
			SKLogger.sAssert(getClass(),  false);
			return false;
		}
		if (numdatagrams == 0 || results == null) {
			SKLogger.sAssert(getClass(),  false);
			return false;
		}
		if (delayTimeout == 0) {
			SKLogger.sAssert(getClass(),  false);
			return false;
		}
		if (interPacketTime == 0) {
			SKLogger.sAssert(getClass(),  false);
			return false;
		}
		if (percentile < 0 || percentile > 100) {
			SKLogger.sAssert(getClass(),  false);
			return false;
		}
		
		return true;
	}

	@Override
	public void execute() {
		run();
	}

	@Override
	public boolean isSuccessful() {
		return testStatus.equals("OK");
	}

	public String getInfo() {
		return infoString;
	}

	public String getHumanReadableResult() {
		String ret = "";
		if (testStatus.equals("FAIL")) {
			ret = String.format("The latency test has failed.");
		} else {
			// added cast otherwise it will always be 0 or 1;
			int packetLoss = (int) (100 * (((float) sentPackets - recvPackets) / sentPackets)); 
			
			int jitter = (int) ((averageNanoseconds - minimumNanoseconds) / 1000000);
			ret = String.format(
					"Latency is %d ms. Packet loss is %d %%. Jitter is %d ms",
					(int) (averageNanoseconds / 1000000), packetLoss, jitter);
		}
		return ret;
	}

	@Override
	public HumanReadable getHumanReadable() {
		HumanReadable ret = new HumanReadable();
		if (testStatus.equals("FAIL")) {
			ret.testString = TEST_STRING.LATENCY_FAILED;
		} else {
			ret.testString = TEST_STRING.LATENCY_SUCCESS;
			ret.values = new String[3];
			ret.values[0] = "" + ((int) (averageNanoseconds / 1000000));
			ret.values[1] = ""
					+ ((int) (100 * (((float) sentPackets - recvPackets) / sentPackets)));
			ret.values[2] = "" + ((int) ((averageNanoseconds - minimumNanoseconds) / 1000000));
		}
		return ret;
	}

	private void output() {
		Map<String, Object> output = new HashMap<String, Object>();
		ArrayList<String> o = new ArrayList<String>();
		// test string id
		o.add(STRING_ID);
		output.put(Test.JSON_TYPE, STRING_ID);
		// time
		Long time_stamp = unixTimeStamp();
		o.add(Long.toString(time_stamp));
		output.put(Test.JSON_TIMESTAMP, time_stamp);
		output.put(Test.JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(time_stamp*1000)));
		// test status
		o.add(testStatus);
		output.put(Test.JSON_SUCCESS, isSuccessful());
		// target
		o.add(target);
		output.put(Test.JSON_TARGET, target);
		// target ipaddress
		o.add(ipAddress);
		output.put(Test.JSON_TARGET_IPADDRESS, ipAddress);
		// average
		o.add(Long.toString(((long)(averageNanoseconds / 1000))));
		output.put(JSON_RTT_AVG, (long) (averageNanoseconds / 1000));
		// minimum
		o.add(Long.toString(minimumNanoseconds / 1000));
		output.put(JSON_RTT_MIN, minimumNanoseconds / 1000);
		// maximum
		o.add(Long.toString(maximumNanoseconds / 1000));
		output.put(JSON_RTT_MAX, maximumNanoseconds /1000);
		// standard deviation
		o.add(Long.toString((long) (stddeviationNanoseconds / 1000)));
		
		output.put(JSON_RTT_STDDEV,(long) (stddeviationNanoseconds / 1000));
		// recvPackets
		o.add(Integer.toString(recvPackets));
		output.put(JSON_RECEIVED_PACKETS, recvPackets);
		// lost packets
		o.add(Integer.toString(sentPackets - recvPackets));
		output.put(JSON_LOST_PACKETS, sentPackets - recvPackets);
		setOutput(o.toArray(new String[1]));
		setJSONOutput(output);
	}

	@Override
	public void run() {
		start();
		//set to zero internal variables in case the same test object is executed severals times 
		sentPackets=0;
		recvPackets=0;
		startTimeNanonseconds = System.nanoTime();
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
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
			address = InetAddress.getByName(target);
			ipAddress = address.getHostAddress();
		} catch (UnknownHostException e) {
			failure();
     		socket.close();
     		socket = null;
			return;
		}
		for (int i = 0; i < numdatagrams; ++i) {
			
			if ((maxExecutionTimeNanoseconds > 0)
					&& (System.nanoTime() - startTimeNanonseconds > maxExecutionTimeNanoseconds)) {
				break;
			}

			UdpDatagram data = new UdpDatagram(i, UdpDatagram.CLIENTTOSERVERMAGIC);
			byte[] buf = data.byteArray();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
			long answerTime = 0;

			// It isn't the current time as in the original but a random value.
			// Let's hope nobody changes the server to make this important...
			long time = System.nanoTime();
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
					long now = System.nanoTime();
					long timeout = delayTimeout - (now - time)/1000000;
					if(timeout<0){
						throw new PacketTimeOutException();
					}
					socket.setSoTimeout((int) timeout);
					socket.receive(packet);
					answer = new UdpDatagram(buf);
				
				} while (answer.magic != UdpDatagram.SERVERTOCLIENTMAGIC || answer.datagramid != i);
				answerTime = System.nanoTime();
				recvPackets++;
			} catch (SocketTimeoutException e) {
				continue;
			} catch (IOException e) {
				continue;
			} catch (PacketTimeOutException e){
				continue;
			}
			

			long rtt = answerTime - time;
			results[recvPackets - 1] = rtt;
			
			// *** Pablo's modifications *** //
			// Local Broadcast receiver to inform about the current speed to the speedTestActivity			
			Intent intent = new Intent("currentLatencyIntent");			
			intent.putExtra("currentLatencyValue", String.valueOf(rtt/1000000));			
			LocalBroadcastManager.getInstance(SKApplication.getAppInstance().getBaseContext()).sendBroadcast(intent);
			// *** End Pablo's modifications *** //

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

			}
		}
	}

	private void failure() {
		testStatus = "FAIL";
		output();
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
		output();
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

	public void setMaxExecutionTime(long time) {
		maxExecutionTimeNanoseconds = time * 1000; // nanoSeconds
	}

	public boolean isProgressAvailable() {
		return true;
	}

	public int getProgress() {
		double retTime = 0;
		double retPackets = 0;
		if (maxExecutionTimeNanoseconds > 0) {
			long currTime = (System.nanoTime() - startTimeNanonseconds);
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
