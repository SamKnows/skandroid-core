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

  // Make base class private!
  private SKTestRunner() {
  }

  SKTestRunner(SKTestRunnerObserver observer) {
    // This is ALLOWED to be null!
    mObserver = observer;

    // Don't do this, or it auto-stops the test running!
    //setStateChangeToUIHandler(TestRunnerState.STOPPED);
  }

  // The interface is used by systems using the test runner, to observe changes in test progress.
  // All methods in this handler, are guaranteed to be in the main UI thread.
  public interface SKTestRunnerObserver {
    public static final String kManualTest_UDPFailedSkipTests = "kManualTest_UDPFailedSkipTests";

    void onTestProgress(JSONObject pm);

    void onTestResult(JSONObject pm);
    void onPassiveMetric(JSONObject o);
    //void onCompleted();
    void OnChangedStateTo(TestRunnerState state);
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
      return;
    }

    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        mObserver.OnChangedStateTo(TestRunnerState.STOPPED);
      }
    }, milliDelay);
  }

  protected void setStateChangeToUIHandler(final TestRunnerState state) {
    if (mObserver != null) {

      mHandler.post(new Runnable() {
        @Override
        public void run() {
          mObserver.OnChangedStateTo(state);
        }
      });
    }
  }
}
