package com.samknows.SKKit;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.TestRunner.SKTestRunner;
import com.samknows.tests.ClosestTarget;
import com.samknows.tests.Param;
import com.samknows.tests.SKAbstractBaseTest;
import com.samknows.tests.TestFactory;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.samknows.measurement.TestRunner.SKTestRunner.sSetRunningTestRunner;

public class SKKitTestClosestTarget {

  public static class ClosestTargetHostDescriptor {
    public String mHostAddress;
    public String mHostName;

    public ClosestTargetHostDescriptor( final String hostAddress, final String hostName) {
      mHostAddress = hostAddress;
      mHostName = hostName;
    }
  }

  // The Test Descriptor allows you to override the settings of various properties used for the ClosestTarget test.
  static public class SKKitTestDescriptor_ClosestTarget { // : SKKitTestDescriptor
    private List<ClosestTargetHostDescriptor> mTargetList = null;

//    public final Integer mPort;
//    public final Double mInterPacketTimeSeconds;
//    public final Double mDelayTimeoutSeconds;
//    public final Integer mNumberOfPackets;

    public SKKitTestDescriptor_ClosestTarget(@NonNull List<ClosestTargetHostDescriptor> targetList) {
      if (targetList == null) {
        SKLogger.sAssert(false);
        return;
      }

      if (targetList.size() == 0) {
        SKLogger.sAssert(false);
        return;
      }

      for (ClosestTargetHostDescriptor target : targetList) {
        SKLogger.sAssert(!target.mHostAddress.isEmpty());
        SKLogger.sAssert(!target.mHostName.isEmpty());
      }

      mTargetList = targetList;
//      mPort = 6000;
//      mInterPacketTimeSeconds = 0.1;
//      mDelayTimeoutSeconds = 2.0;
//      mNumberOfPackets = 60;
    }
  }

  public interface ISKClosestTargetTestProgressUpdate {

    void onTestProgress_OnMainThread(int progress0To100);
    void onTestCompleted_OnMainThread(String closestTarget);
  }

  private final SKKitTestDescriptor_ClosestTarget mTestDescriptor;
  private ClosestTarget mClosestTargetTest;

  public SKKitTestClosestTarget(SKKitTestDescriptor_ClosestTarget testDescriptor) {
    mTestDescriptor = testDescriptor;
    SKLogger.sAssert(testDescriptor.mTargetList.size() > 0);
  }

  // Adaptor for extracting JSON data for export!
  public JSONObject getJSONResult() {
    return mClosestTargetTest.getJSONResult();
  }

  private final Handler mHandler = new Handler();

  private String mFoundClosestTarget = null;

  public void start(@NonNull final ISKClosestTargetTestProgressUpdate progressUpdate) {
    SKLogger.sAssert(progressUpdate != null);

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
//        mFoundClosestTarget = closestTarget;
//        if (progressUpdate != null) {
//          progressUpdate.onTestCompleted_OnMainThread(closestTarget);
//        }
      }

      @Override
      public void OnCurrentLatencyCalculated(long latencyMilli) {
      }
    };

    final SKTestRunner testRunner = new SKTestRunner(observer);
    sSetRunningTestRunner(testRunner);

    Thread testThread = new Thread() {
      @Override
      public void run() {
        super.run();

        List<Param> params = new ArrayList<>();

        //params.add(new Param(SKAbstractBaseTest.PORT, mTestDescriptor.mPort.toString()));
        for (ClosestTargetHostDescriptor target : mTestDescriptor.mTargetList) {
          params.add(new Param(SKAbstractBaseTest.TARGET, target.mHostAddress));
        }

//        params.add(new Param(TestFactory.INTERPACKETTIME, String.valueOf((int) (mTestDescriptor.mInterPacketTimeSeconds * 1000000.0)))); // Microseconds!
//        params.add(new Param(TestFactory.DELAYTIMEOUT, String.valueOf((int) (mTestDescriptor.mDelayTimeoutSeconds * 1000000.0)))); // Microseconds!
//        params.add(new Param(TestFactory.NUMBEROFPACKETS, String.valueOf(mTestDescriptor.mNumberOfPackets)));

        //Log.d("IHT", "START Running ClosestTarget test ..." + mTestDescriptor.mDelayTimeoutSeconds * 1000.0);
        Log.d("IHT", "START Running ClosestTarget test ...");

        mClosestTargetTest = ClosestTarget.sCreateClosestTarget(params);
        if (mClosestTargetTest == null) {
          SKLogger.sAssert(false);
          return;
        }
        long timeStartMilli = System.currentTimeMillis();
        mClosestTargetTest.runBlockingTestToFinishInThisThread();
        long timeEndMilli = System.currentTimeMillis();

        long actualTimeTakenMilli = timeEndMilli - timeStartMilli;
        Log.d("IHT", "STOPPED Running ClosestTarget test for milliseconds=" + actualTimeTakenMilli + ", completed after " + actualTimeTakenMilli);

        // Finished the ClosestTarget test - extract the result values, and post them to the handler method in the main thread.
        mFoundClosestTarget = mClosestTargetTest.getClosest();

        mHandler.post(new Runnable() {
          public void run() {

            if (progressUpdate != null) {
              //progressUpdate.onTestCompleted_OnMainThread(latency, loss, jitterMilliseconds);
              progressUpdate.onTestCompleted_OnMainThread(mFoundClosestTarget);
            }
          }
        });
      }
    };

    testThread.start();
  }

  public void cancel() {
    if (mClosestTargetTest != null) {
      mClosestTargetTest.setShouldCancel();
    }
  }

  public int getProgress0To100() {
    if (mClosestTargetTest == null) {
      //SKLogger.sAssert(false);
      return 0;
    }
    return mClosestTargetTest.getProgress0To100();
  }
}
