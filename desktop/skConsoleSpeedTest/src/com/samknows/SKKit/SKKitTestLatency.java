package com.samknows.SKKit;

import com.samknows.libcore.SKPorting;
import com.samknows.measurement.TestRunner.SKTestRunner;
import com.samknows.tests.LatencyTest;
import com.samknows.tests.Param;
import com.samknows.tests.SKAbstractBaseTest;
import com.samknows.tests.TestFactory;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.samknows.measurement.TestRunner.SKTestRunner.sSetRunningTestRunner;

public class SKKitTestLatency {

  // The Test Descriptor allows you to override the settings of various properties used for the Latency test.
  static public class SKKitTestDescriptor_Latency { // : SKKitTestDescriptor
    private final String mTarget;

    public final Integer mPort;
    public final Double mInterPacketTimeSeconds;
    public final Double mDelayTimeoutSeconds;
    public Integer mNumberOfPackets;
    public final Integer mPercentile;
    public Double mMaxTimeSeconds;

    public SKKitTestDescriptor_Latency(String target) {
      if (target == null) {
        SKPorting.sAssert(false);
      } else {
        SKPorting.sAssert(!target.isEmpty());
      }

      mTarget = target;

      mPort = 6000;
      mInterPacketTimeSeconds = 0.1;
      mDelayTimeoutSeconds = 2.0;
      mNumberOfPackets = 60;
      mPercentile = 100;
      mMaxTimeSeconds = 15.0;
    }
  }

  public interface ISKLatencyTestProgressUpdate {

    void onTestProgress_OnMainThread(int progress0To100, double latency);
    void onTestCompleted_OnMainThread(double latency, double loss, double jitterMilliseconds);
  }

  private final SKKitTestDescriptor_Latency mTestDescriptor;
  private LatencyTest mLatencyTest;

  public SKKitTestLatency(SKKitTestDescriptor_Latency testDescriptor) {
    mTestDescriptor = testDescriptor;
    SKPorting.sAssert(!testDescriptor.mTarget.isEmpty());
  }

  // Adaptor for extracting JSON data for export!
  public JSONObject getJSONResult() {
    return mLatencyTest.getJSONResult();
  }

  private final SKPorting.MainThreadResultHandler mHandler = new SKPorting.MainThreadResultHandler();

  public void start(final ISKLatencyTestProgressUpdate progressUpdate) {
    SKPorting.sAssert(progressUpdate != null);

    // This requires a TestRunner for observing results...
    SKTestRunner.SKTestRunnerObserver observer = new SKTestRunner.SKTestRunnerObserver() {
      @Override
      public void onTestProgress(JSONObject pm) {
      }

      @Override
      public void onTestResult(JSONObject pm) {
      }

      @Override
      public void onPassiveMetric(JSONObject o) {
      }

      @Override
      public void OnChangedStateTo(SKTestRunner.TestRunnerState state) {

//        if (state == SKTestRunner.TestRunnerState.STOPPED) {
//        }
      }

      @Override
      public void OnUDPFailedSkipTests() {
      }

      @Override
      public void OnClosestTargetSelected(String closestTarget) {

      }

      @Override
      public void OnCurrentLatencyCalculated(long latencyMilli) {
        if (progressUpdate != null) {
          progressUpdate.onTestProgress_OnMainThread(getProgress0To100(), latencyMilli);
        }
      }
    };
    final SKTestRunner testRunner = new SKTestRunner(observer);
    sSetRunningTestRunner(testRunner);

    Thread latencyThread = new Thread() {
      @Override
      public void run() {
        super.run();

        List<Param> params = new ArrayList<>();
        params.add(new Param(SKAbstractBaseTest.PORT, mTestDescriptor.mPort.toString()));
        params.add(new Param(SKAbstractBaseTest.TARGET, mTestDescriptor.mTarget));

        params.add(new Param(TestFactory.INTERPACKETTIME, String.valueOf((int) (mTestDescriptor.mInterPacketTimeSeconds * 1000000.0)))); // Microseconds!
        params.add(new Param(TestFactory.DELAYTIMEOUT, String.valueOf((int) (mTestDescriptor.mDelayTimeoutSeconds * 1000000.0)))); // Microseconds!
        params.add(new Param(TestFactory.NUMBEROFPACKETS, String.valueOf(mTestDescriptor.mNumberOfPackets)));
        params.add(new Param(TestFactory.PERCENTILE, String.valueOf(mTestDescriptor.mPercentile)));
        params.add(new Param(TestFactory.MAXTIME, String.valueOf((int) (mTestDescriptor.mMaxTimeSeconds * 1000000.0)))); // Microseconds!

        SKPorting.sLogD("IHT", "START Running Latency test for milliseconds=" + mTestDescriptor.mMaxTimeSeconds * 1000.0);

        mLatencyTest = LatencyTest.sCreateLatencyTest(params);
        if (mLatencyTest == null) {
          SKPorting.sAssert(false);
          return;
        }
        final LatencyTest theTest = mLatencyTest;
        long timeStartMilli = System.currentTimeMillis();
        mLatencyTest.runBlockingTestToFinishInThisThread();
        long timeEndMilli = System.currentTimeMillis();

        long actualTimeTakenMilli = timeEndMilli - timeStartMilli;
        SKPorting.sLogD("IHT", "STOPPED Running Latency test for milliseconds=" + actualTimeTakenMilli + ", completed after " + actualTimeTakenMilli);

        // Finished the Latency test - extract the result values, and post them to the handler method in the main thread.
        final double latency = theTest.getResultLatencyMilliseconds();
        final double loss = theTest.getResultLossPercent0To100();
        final double jitterMilliseconds = theTest.getResultJitterMilliseconds();

        mHandler.callUsingMainThreadWhereSupported(new Runnable() {
          public void run() {

            if (progressUpdate != null) {
              progressUpdate.onTestCompleted_OnMainThread(latency, loss, jitterMilliseconds);
            }
          }
        });
      }
    };

    latencyThread.start();
  }

  public void cancel() {
    if (mLatencyTest != null) {
      mLatencyTest.setShouldCancel();
    }
  }

  public int getProgress0To100() {
    if (mLatencyTest == null) {
      //SKCommon.sAssert(false);
      return 0;
    }
    return mLatencyTest.getProgress0To100();
  }
}
