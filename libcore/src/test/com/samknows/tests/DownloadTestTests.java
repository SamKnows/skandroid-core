package com.samknows.tests;

import android.os.ConditionVariable;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.samknows.XCT;
import com.samknows.libcore.SKLogger;
import com.samknows.measurement.TestRunner.SKTestRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.samknows.tests.DownloadTest.*;

@RunWith(RobolectricTestRunner.class)

public class DownloadTestTests {

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

  public class MyMockInputStream extends InputStream {

    int mBlockSize = 0;

    MyMockInputStream(int blockSize) {
      mBlockSize = blockSize;
    }

    @Override
    public int available() throws IOException {
      return mBlockSize;
    }

    String header = "HTTP/1.1 200 OK\n\n\n";
    byte[] headerBytes = header.getBytes();

//    boolean bReadHeader = false;

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {

      // We need to return THIS to start...
      // then anything!
      Arrays.fill(buffer, (byte) 0);

//      if (bReadHeader == false) {
//        bReadHeader = true;
        SKLogger.sAssert(headerBytes.length <= byteCount);
        System.arraycopy(headerBytes, 0, buffer, byteOffset, headerBytes.length);
//      }

      return byteCount;

//      int bytesLeftToCopy = byteCount;
//      while (bytesLeftToCopy > 0) {
//        int bytesThisTime = Math.min(bytesLeftToCopy, headerBytes.length);
//        System.arraycopy(headerBytes, 0, buffer, byteOffset, bytesThisTime);
//        byteOffset += bytesThisTime;
//        bytesLeftToCopy -= bytesThisTime;
//      }

//      return byteCount;
    }

    @Override
    public int read() throws IOException {
      // We keep this running for ever!
      // Just return one byte with zero!
      return 0;
    }
  };

  public class MockSKSocket implements ISKHttpSocket {

    public int mOpenCalledCount = 0;
    public int mSetTcpNoDelayCalledCount = 0;
    public int mSetReceiveBufferSizeCount = 0;
    public int mSetSendBufferSizeCount = 0;
    public int mSetSoTimeoutCalledCount = 0;
    public int mConnectCalledCount = 0;
    public int mGetInputStreamCalledCount = 0;
    public int mGetOutputStreamCalledCount = 0;
    public int mCloseCalledCount = 0;
    private int mReceiveBufferSize = 512;
    private int mSendBufferSize = 512;

    static final String cDummyIpAddress = "127.0.0.1";

    private ByteArrayOutputStream outputStream;

    @Override
    public void open() {
      mOpenCalledCount++;
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
      mSetTcpNoDelayCalledCount++;
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
      mSetReceiveBufferSizeCount++;
      mReceiveBufferSize = size;
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
      return mReceiveBufferSize;
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
      mSetSendBufferSizeCount++;
      mSendBufferSize = size;
    }

    @Override
    public int getSendBufferSize() throws SocketException {
      return mSendBufferSize;
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
      mSetSoTimeoutCalledCount++;
    }

    @Override
    public String connect(String target, int port, int timeout) throws IOException {
      mConnectCalledCount++;
      return cDummyIpAddress;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      mGetInputStreamCalledCount++;
      // byte[] buf = new byte[]();
      // ByteArrayInputStream stubInputStream = new ByteArrayInputStream(buf);
      //InputStream stubInputStream = IOUtils.toInputStream("HTTP/1.1 200 OK\n");
      InputStream stubInputStream = new MyMockInputStream(512);
      return stubInputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      mGetOutputStreamCalledCount++;
      outputStream = new ByteArrayOutputStream();
      return outputStream;
    }

    @Override
    public void close() {
      mCloseCalledCount++;
    }
  }

	public DownloadTestTests() {
	}

  static final String cLiveServer = "all-the1.samknows.com";
  static final String cLivePort = "80";
  static final String mFile = "1000MB.bin";
  static final Double mWarmupMaxTimeSeconds = 2.0;
  static final Double mTransferMaxTimeSeconds = 10.0;
  static final Integer mNumberOfThreads = 1;
  static final Integer mBufferSizeBytes = 1048576;

  private static DownloadTest sCreateDownloadTest() {

    List<Param> params = new ArrayList<>();
    params.add(new Param(SKAbstractBaseTest.PORT, cLivePort));
    params.add(new Param(SKAbstractBaseTest.TARGET, cLiveServer));
    params.add(new Param(SKAbstractBaseTest.FILE, mFile));
    params.add(new Param(HttpTest.WARMUPMAXTIME, String.valueOf((int) (mWarmupMaxTimeSeconds * 1000000.0)))); // Microseconds!
    params.add(new Param(HttpTest.TRANSFERMAXTIME, String.valueOf((int) (mTransferMaxTimeSeconds * 1000000.0)))); // Microseconds
    params.add(new Param(HttpTest.NTHREADS, String.valueOf(mNumberOfThreads)));
    params.add(new Param(HttpTest.BUFFERSIZE, String.valueOf(mBufferSizeBytes)));

    DownloadTest theTest = DownloadTest.sCreateDownloadTest(params);
    return theTest;
  }


  // Try a MOCK test. This is proving problematic, as the response required isn't a simple one.
  /*
  @Test
  public void testDownloadTest() throws Exception{

    final boolean[] responseCalled = {false};

    final ConditionVariable cv = new ConditionVariable();

    final DownloadTest downloadTest = sCreateDownloadTest();
    final MockSKSocket mockSocket = new MockSKSocket();

    // Set dummy SKTestRunner instance, or the tests will give lots of assertions...
    SKTestRunner.sSetRunningTestRunner(new SKTestRunner());

    Thread thread = new Thread() {
      @Override
      public void run() {

        // We should now be able to test the test has worked as expected.
        downloadTest.runBlockingTestToFinishInThisThread(new ISKHttpSocketFactory() {
          @Override
          public ISKHttpSocket newSocket() {
            return mockSocket;
          }
        });

        responseCalled[0] = true;

        cv.open();
      }

    };
    thread.start();

    cv.block(20000);

    // And verify results!
    XCT.Assert(responseCalled[0] == true);
    XCT.Assert(downloadTest.isSuccessful() == true);
    XCT.Assert(downloadTest.getTransferBytesPerSecond() > 0);

    XCT.Assert(mockSocket.mOpenCalledCount == 1); // Depends on nThreads
    XCT.Assert(mockSocket.mSetTcpNoDelayCalledCount > 0);
    XCT.Assert(mockSocket.mSetReceiveBufferSizeCount > 0);
    XCT.Assert(mockSocket.mSetSendBufferSizeCount > 0);
    XCT.Assert(mockSocket.mSetSoTimeoutCalledCount > 0);
    XCT.Assert(mockSocket.mConnectCalledCount > 0);
    XCT.Assert(mockSocket.mGetInputStreamCalledCount == 1); // Depends on nThreads
    XCT.Assert(mockSocket.mGetOutputStreamCalledCount == 0);
    XCT.Assert(mockSocket.mCloseCalledCount == 1); // Depends on nThreads
    XCT.Assert(mockSocket.mReceiveBufferSize == 512);
    XCT.Assert(mockSocket.mSendBufferSize == 512);
  }
  */

  // Include a LIVE test, to verify that basic behaviour of the non-mocked system is still fundamentally OK.
  // This is less t able to make specific assertions.
  @Test
  public void testDownloadTest_Live() throws Exception{

    final boolean[] responseCalled = {false};

    final ConditionVariable cv = new ConditionVariable();

    final DownloadTest downloadTest = sCreateDownloadTest();

    // Set dummy SKTestRunner instance, or the tests will give lots of assertions...
    SKTestRunner.sSetRunningTestRunner(new SKTestRunner());

    Thread thread = new Thread() {
      @Override
      public void run() {

        // We should now be able to test the test has worked as expected.
        downloadTest.runBlockingTestToFinishInThisThread();

        responseCalled[0] = true;

        cv.open();
      }

    };
    thread.start();

    cv.block(20000);

    // And verify results!
    XCT.Assert(responseCalled[0] == true);
    XCT.Assert(downloadTest.isSuccessful() == true);
    XCT.Assert(downloadTest.getTransferBytesPerSecond() > 0);
  }
}
