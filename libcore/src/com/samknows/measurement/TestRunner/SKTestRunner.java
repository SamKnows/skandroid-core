package com.samknows.measurement.TestRunner;

import android.os.Handler;
import android.os.Message;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.storage.PassiveMetric;
import com.samknows.measurement.storage.StorageTestResult;

import org.json.JSONException;
import org.json.JSONObject;

public class SKTestRunner {

  public enum TestRunnerState {STOPPED, STARTING, EXECUTING, STOPPING}

  // Handler used for making the callbacks through the observer.
  protected Handler mHandler = new Handler();

  // This might be null.
  protected SKTestRunnerObserver mObserver;

  // Only ONE test runner is instantiated at any one time.
  // Which one is running, is managed privately by this class via synchronized access methods.
  private static SKTestRunner sRunningTestRunner = null;
  private static synchronized SKTestRunner sGetRunningTestRunner() {
    SKLogger.sAssert(sRunningTestRunner != null);
    return sRunningTestRunner;
  }

  public static synchronized void sSetRunningTestRunner(SKTestRunner testRunner) {
    sRunningTestRunner = testRunner;
  }

  // Make base class private!
  private SKTestRunner() {
  }

  public SKTestRunner(SKTestRunnerObserver observer) {
    // This is ALLOWED to be null!
    mObserver = observer;

    // Don't do this, or it auto-stops the test running!
    //setStateChangeToUIHandler(TestRunnerState.STOPPED);
  }

  // The interface is used by systems using the test runner, to observe changes in test progress.
  // All methods in this handler, are guaranteed to be in the main UI thread.
  public interface SKTestRunnerObserver {

    void onTestProgress(JSONObject pm);

    void onTestResult(JSONObject pm);
    void onPassiveMetric(JSONObject o);
    //void onCompleted();
    void OnChangedStateTo(TestRunnerState state);

    void OnUDPFailedSkipTests();
    void OnClosestTargetSelected(String closestTarget);
    void OnCurrentLatencyCalculated(long latencyMilli);
  }

  protected void sendTestProgressToUIHandler(final JSONObject pm) {
    if (mObserver != null) {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          mObserver.onTestProgress(pm);
        }
      });
    }
  }

  protected void sendTestResultToUIHandler(final JSONObject pm) {
    if (mObserver != null) {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          mObserver.onTestResult(pm);
        }
      });
    }
  }

  protected void sendPassiveMetricToUIHandler(final JSONObject o) {
    if (mObserver != null) {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          mObserver.onPassiveMetric(PassiveMetric.passiveMetricToCurrentTest(o));
        }
      });
    }
  }

  protected void sendCompletedMessageToUIHandlerWithMilliDelay(long milliDelay) {

    if (mObserver == null) {
      sSetRunningTestRunner(null);
      return;
    }

    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        sSetRunningTestRunner(null);
        mObserver.OnChangedStateTo(TestRunnerState.STOPPED);
      }
    }, milliDelay);
  }

  protected void setStateChangeToUIHandler(final TestRunnerState state) {
    if (state == TestRunnerState.STARTING) {
      sSetRunningTestRunner(this);
    } else if (state == TestRunnerState.STOPPED) {
      sSetRunningTestRunner(null);
    }

    if (mObserver != null) {

      mHandler.post(new Runnable() {
        @Override
        public void run() {
          mObserver.OnChangedStateTo(state);
        }
      });
    }
  }

  public static void sDoReportUDPFailedSkipTests() {
    final SKTestRunner runner = sGetRunningTestRunner();
    if (runner == null) {
      SKLogger.sAssert(false);
      return;
    }

    if (runner.mObserver != null) {

      runner.mHandler.post(new Runnable() {
        @Override
        public void run() {
          runner.mObserver.OnUDPFailedSkipTests();
        }
      });
    }
  }

  public static void sDoReportCurrentLatencyCalculated(final long latencyMilli) {
    final SKTestRunner runner = sGetRunningTestRunner();
    if (runner == null) {
      SKLogger.sAssert(false);
      return;
    }

    if (runner.mObserver != null) {

      runner.mHandler.post(new Runnable() {
        @Override
        public void run() {
          runner.mObserver.OnCurrentLatencyCalculated(latencyMilli);
        }
      });
    }
  }

  public static void sDoReportClosestTargetSelected(final String closestTarget) {
    final SKTestRunner runner = sGetRunningTestRunner();
    if (runner == null) {
      SKLogger.sAssert(false);
      return;
    }

    if (runner.mObserver != null) {

      runner.mHandler.post(new Runnable() {
        @Override
        public void run() {
          runner.mObserver.OnClosestTargetSelected(closestTarget);
        }
      });
    }
  }
}
