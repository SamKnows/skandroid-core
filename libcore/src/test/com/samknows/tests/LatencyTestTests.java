package com.samknows.tests;

import android.app.Activity;
import android.os.ConditionVariable;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.samknows.XCT;
import com.samknows.measurement.TestRunner.SKTestRunner;
import com.samknows.tests.LatencyTest.SKUDPSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static com.samknows.tests.LatencyTest.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)

public class LatencyTestTests {

  final long cMicrosecondInNanoSeconds = 1000;
  final long cMillisecondInNanoSeconds = cMicrosecondInNanoSeconds * 1000;
  final long cSecondInNanoSeconds = cMillisecondInNanoSeconds * 1000;
  final long cTenthOfASecondInNanoSeconds = cSecondInNanoSeconds / 10;

  @org.junit.Before
  public void setUp() throws Exception {
  }

  @org.junit.After
  public void tearDown() throws Exception {
    // https://github.com/robolectric/robolectric/issues/1890
    // "Robolectric testing has been designed on the assumption that no state is preserved
    // between tests & you're always starting from scratch.
    // Singletons & other static state maintained by your
    // application and/or test can break that assumption"
    SKTestRunner.sSetRunningTestRunner(null);
  }

  final String cFakeIpAddress = "127.0.0.1";

  public class MockSKUDPSocket implements ISKUDPSocket {

    public int mSendCalledCount = 0;
    public int mReceiveCalledCount = 0;
    public int mSetSoTimeoutCalledCount = 0;
    public int mCloseCalledCount = 0;

    @Override
    public void open() throws SocketException {
    }

    byte[] lastSent = null;
    @Override
    public void send(DatagramPacket pack) throws IOException {
      mSendCalledCount++;

      // Copy the "sent" data, so we can "return" it via the next receive method call.
      lastSent = pack.getData().clone();
    }

    @Override
    public void receive(DatagramPacket pack) throws IOException {
      mReceiveCalledCount++;

      // Return what we sent... BUT WITH THE SERVER MAGIC applied!
      // The SamKnows server *always* sets the magic value this way - the LatencyTest implementation relies on this.
      byte[] returningArrayRepresentation = pack.getData();
      System.arraycopy(lastSent, 0, returningArrayRepresentation, 0, lastSent.length);

      // BUT use a MAGIC value in the response (as used by the server!)
      final int serverMagic = LatencyTest.UdpDatagram.SERVERTOCLIENTMAGIC;
      returningArrayRepresentation[12] = (byte) (serverMagic >>> 24);
      returningArrayRepresentation[13] = (byte) (serverMagic >>> 16);
      returningArrayRepresentation[14] = (byte) (serverMagic >>> 8);
      returningArrayRepresentation[15] = (byte) (serverMagic);
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
      mSetSoTimeoutCalledCount++;
    }

    @Override
    public void close() {
      mCloseCalledCount++;
    }

    @Override
    public long getStartTimeNanoseconds() {
      return 0;
    }

    // For purposes of testing, to give useful standard deviation etc.; ensure that the packets have a spread of timings.
    long mInterpacketDelay = cTenthOfASecondInNanoSeconds;

    long timeLast = 0;
    @Override
    public long getTimeNowNanoseconds() {

      long result = timeLast;
      timeLast += mInterpacketDelay;

      mInterpacketDelay += cTenthOfASecondInNanoSeconds;

      return result;
    }

    @Override
    public InetAddress getInetAddressByName(String host) throws UnknownHostException {
      // This will always work, as a dummy value.
      InetAddress result = InetAddress.getByName(cFakeIpAddress);
      return result;
    }
  }

	public LatencyTestTests() {
	}

  final static Integer cPort = 80;
  final static String  cTarget = "TestTarget";
  final static Double  cInterPacketTimeSeconds = 0.1;
  final static Double  cDelayTimeoutSeconds = 2.0;
  final static Integer cNumberOfPackets = 10;
  final static Integer cPercentile = 10;

  private static LatencyTest sCreateLatencyTest(Integer optionalTimeoutInMicroseconds) {
    List<Param> params = new ArrayList<>();

    params.add(new Param(SKAbstractBaseTest.PORT, String.valueOf(cPort)));
    params.add(new Param(SKAbstractBaseTest.TARGET, cTarget));
    params.add(new Param(TestFactory.INTERPACKETTIME, String.valueOf((int) (cInterPacketTimeSeconds * 1000000.0)))); // 0.1 seconds - in Microseconds!
    params.add(new Param(TestFactory.DELAYTIMEOUT, String.valueOf((int) (cDelayTimeoutSeconds * 1000000.0)))); // 0.1 seconds - in Microseconds!
    params.add(new Param(TestFactory.NUMBEROFPACKETS, String.valueOf(cNumberOfPackets)));
    params.add(new Param(TestFactory.PERCENTILE, String.valueOf(cPercentile)));
    if (optionalTimeoutInMicroseconds != null) {
      params.add(new Param(TestFactory.MAXTIME, String.valueOf(optionalTimeoutInMicroseconds))); // Microseconds!
    }

    LatencyTest theTest = LatencyTest.sCreateLatencyTest(params);
    return theTest;
  }


  static final String cLiveServer = "all-the1.samknows.com";
  static final String cLivePort = "6000";
  private static LatencyTest sCreateLatencyTest_Live() {
    List<Param> params = new ArrayList<>();

    params.add(new Param(SKAbstractBaseTest.PORT, cLivePort));
    params.add(new Param(SKAbstractBaseTest.TARGET,  cLiveServer));
    params.add(new Param(TestFactory.INTERPACKETTIME, String.valueOf((int) (cInterPacketTimeSeconds * 1000000.0)))); // 0.1 seconds - in Microseconds!
    params.add(new Param(TestFactory.DELAYTIMEOUT, String.valueOf((int) (cDelayTimeoutSeconds * 1000000.0)))); // 0.1 seconds - in Microseconds!
    params.add(new Param(TestFactory.NUMBEROFPACKETS, String.valueOf(cNumberOfPackets)));
    params.add(new Param(TestFactory.PERCENTILE, "100")); // Use "all" results!
    params.add(new Param(TestFactory.MAXTIME, String.valueOf((int) (10 * 1000000.0)))); // 10 seconds - in Microseconds!

    LatencyTest theTest = LatencyTest.sCreateLatencyTest(params);
    return theTest;
  }

	@Test
	public void testLatencyTestWithNoTimeLimit() throws Exception{

    MockSKUDPSocket mockSKUDPSocket = new MockSKUDPSocket();

    // Create latency test with no time limit!
    LatencyTest latencyTest = sCreateLatencyTest(null);

    // Set dummy SKTestRunner instance, or the tests will give lots of assertions...
    SKTestRunner.sSetRunningTestRunner(new SKTestRunner());

    // We should now be able to test the test has worked as expected.
    latencyTest.executeWithSKUDPSocket(mockSKUDPSocket);

    XCT.Assert(mockSKUDPSocket.mSendCalledCount > 0);
    XCT.AssertEquals(mockSKUDPSocket.mReceiveCalledCount, cNumberOfPackets);
    XCT.AssertEquals(mockSKUDPSocket.mSetSoTimeoutCalledCount, cNumberOfPackets+1);
    XCT.AssertEquals(mockSKUDPSocket.mCloseCalledCount, 1);

    // And verify results!
    XCT.AssertEquals(latencyTest.getAverageMicroseconds(), 3000000);
    XCT.AssertEquals(latencyTest.getMinimumMicroseconds(), 300000);
    XCT.AssertEquals(latencyTest.getMaximumMicroseconds(), 5700000);
    XCT.AssertEquals(latencyTest.getStdDeviationMicroseconds(), 1816590);
    XCT.AssertEquals(latencyTest.getIpAddress(), cFakeIpAddress);
    XCT.AssertEquals(latencyTest.getJitter(), 2700000);
    XCT.AssertEquals(latencyTest.getSentPackets(), cNumberOfPackets);
    XCT.AssertEquals(latencyTest.getReceivedPackets(), cNumberOfPackets);
    XCT.AssertEquals(latencyTest.getLostPackets(), 0);
	}

  @Test
  public void testLatencyTestWithTimeLimit() throws Exception{

    MockSKUDPSocket mockSKUDPSocket = new MockSKUDPSocket();

    // Create latency test with time limit!
    final int cTimeoutAfterSecondsInMicroseconds = 3 * 1000000;
    LatencyTest latencyTest = sCreateLatencyTest(new Integer(cTimeoutAfterSecondsInMicroseconds));

    // Set dummy SKTestRunner instance, or the tests will give lots of assertions...
    SKTestRunner.sSetRunningTestRunner(new SKTestRunner());

    // We should now be able to test the test has worked as expected.
    latencyTest.executeWithSKUDPSocket(mockSKUDPSocket);

    XCT.Assert(mockSKUDPSocket.mSendCalledCount > 0);
    XCT.AssertEquals(mockSKUDPSocket.mReceiveCalledCount, 4);
    XCT.AssertEquals(mockSKUDPSocket.mSetSoTimeoutCalledCount, 4+1);
    XCT.AssertEquals(mockSKUDPSocket.mCloseCalledCount, 1);

    // And verify results!
    XCT.AssertEquals(latencyTest.getAverageMicroseconds(), 1700000);
    XCT.AssertEquals(latencyTest.getMinimumMicroseconds(), 500000);
    XCT.AssertEquals(latencyTest.getMaximumMicroseconds(), 2900000);
    XCT.AssertEquals(latencyTest.getStdDeviationMicroseconds(), 1032795);
    XCT.AssertEquals(latencyTest.getIpAddress(), cFakeIpAddress);
    XCT.AssertEquals(latencyTest.getJitter(), 1200000);
    XCT.AssertEquals(latencyTest.getSentPackets(), 4);
    XCT.AssertEquals(latencyTest.getReceivedPackets(), 4);
    XCT.AssertEquals(latencyTest.getLostPackets(), 0);
  }

  // TODO - need cases for lost packets, timeouts ... and any other special cases.

  // Include a LIVE test, to verify that basic behaviour of the non-mocked system is still fundamentally OK.
  // This is less t able to make specific assertions.
  @Test
  public void testLatencyTest_Live() throws Exception{

    final boolean[] responseCalled = {false};

    final ConditionVariable cv = new ConditionVariable();

    final int cTimeoutAfterSecondsInMicroseconds = 10 * 1000000;
    final LatencyTest latencyTest = sCreateLatencyTest_Live();

    // Set dummy SKTestRunner instance, or the tests will give lots of assertions...
    SKTestRunner.sSetRunningTestRunner(new SKTestRunner());

    Thread thread = new Thread() {
      @Override
      public void run() {

        // We should now be able to test the test has worked as expected.
        latencyTest.execute();

        responseCalled[0] = true;

        cv.open();
      }

    };
    thread.start();

    cv.block(20000);

    // And verify results!
    XCT.Assert(responseCalled[0] == true);
    XCT.Assert(latencyTest.getAverageMicroseconds() > 0);
    XCT.Assert(latencyTest.getMinimumMicroseconds() > 0);
    XCT.Assert(latencyTest.getMinimumMicroseconds() <= latencyTest.getAverageMicroseconds());
    XCT.Assert(latencyTest.getMaximumMicroseconds() > 0);
    XCT.Assert(latencyTest.getMaximumMicroseconds() >= latencyTest.getAverageMicroseconds());
    XCT.Assert(latencyTest.getStdDeviationMicroseconds() > 0);
    XCT.Assert(latencyTest.getIpAddress().equals(cFakeIpAddress) == false);
    XCT.Assert(latencyTest.getJitter() > 0);
    // Note: in GENERAL operation, this MIGHT be less - but generally shouldn't be!
    XCT.Assert(latencyTest.getSentPackets() > 0);
    XCT.Assert(latencyTest.getSentPackets() <= cNumberOfPackets);
    // Note: in GENERAL operation, this MIGHT  be less - but generally shouldn't be!
    XCT.Assert(latencyTest.getReceivedPackets() > 0);
    // Should never get back more packets than we sent!
    XCT.Assert(latencyTest.getReceivedPackets() <= latencyTest.getSentPackets());
    // Note: in GENERAL operation, lost packets are quite common. Should never be negative, however!
    XCT.Assert(latencyTest.getLostPackets() >= 0);
  }
}
