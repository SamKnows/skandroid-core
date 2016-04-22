package com.samknows.tests;

import android.os.ConditionVariable;

import org.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.samknows.XCT;
import com.samknows.measurement.TestRunner.SKTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)

public class PassiveServerUploadTestTests {

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

  /*
  public class MockSKSocket implements ISKSocket {
    // TODO!

    public int mSendCalledCount = 0;
    public int mReceiveCalledCount = 0;
    public int mSetSoTimeoutCalledCount = 0;
    public int mCloseCalledCount = 0;

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
      mSetSoTimeoutCalledCount++;
    }

    @Override
    public void close() {
      mCloseCalledCount++;
    }
  }
  */

	public PassiveServerUploadTestTests() {
	}

  static final String cLiveServer = "all-the1.samknows.com";
  static final String cLivePort = "8080";

  static final Double mWarmupMaxTimeSeconds = 0.001;
  static final Double mTransferMaxTimeSeconds = 10.0;
  static final Integer mNumberOfThreads = 1;
  static final Integer mSendDataChunkSizeBytes = 512;

  private static PassiveServerUploadTest sCreatePassiveServerUploadTest_Live() {

    List<Param> params = new ArrayList<>();
    params.add(new Param(SKAbstractBaseTest.PORT, cLivePort));
    params.add(new Param(SKAbstractBaseTest.TARGET, cLiveServer));
    params.add(new Param(HttpTest.WARMUPMAXTIME, String.valueOf((int) (mWarmupMaxTimeSeconds * 1000000.0)))); // Microseconds!

    params.add(new Param(HttpTest.TRANSFERMAXTIME, String.valueOf((int) (mTransferMaxTimeSeconds * 1000000.0)))); // Microseconds
    params.add(new Param(HttpTest.NTHREADS, String.valueOf(mNumberOfThreads)));
    params.add(new Param(HttpTest.SENDDATACHUNK, String.valueOf(mSendDataChunkSizeBytes)));

    PassiveServerUploadTest theTest = PassiveServerUploadTest.sCreatePassiveServerUploadTest(params);
    return theTest;
  }

  // Include a LIVE test, to verify that basic behaviour of the non-mocked system is still fundamentally OK.
  // This is less t able to make specific assertions.
  @Test
  public void testPassiveServerUploadTest_Live() throws Exception{

    final boolean[] responseCalled = {false};

    final ConditionVariable cv = new ConditionVariable();

    final PassiveServerUploadTest uploadTest = sCreatePassiveServerUploadTest_Live();

    // Set dummy SKTestRunner instance, or the tests will give lots of assertions...
    SKTestRunner.sSetRunningTestRunner(new SKTestRunner());

    Thread thread = new Thread() {
      @Override
      public void run() {

        // We should now be able to test the test has worked as expected.
        uploadTest.runBlockingTestToFinishInThisThread();

        responseCalled[0] = true;

        cv.open();
      }

    };
    thread.start();

    cv.block(20000);

    // And verify results!
    XCT.Assert(responseCalled[0] == true);
    XCT.Assert(uploadTest.isSuccessful() == true);
    XCT.Assert(uploadTest.getTransferBytesPerSecond() > 0);
  }
}
