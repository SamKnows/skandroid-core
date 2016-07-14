package com.samknows.measurement.TestRunner;

import com.samknows.libcore.SKPorting;
import org.json.JSONObject;

public class SKTestRunner {

  public enum TestRunnerState {STOPPED, STARTING, EXECUTING, STOPPING}

  // This might be null.
  protected SKTestRunnerObserver mObserver;

  // Only ONE test runner is instantiated at any one time.
  // Which one is running, is managed privately by this class via synchronized access methods.
  private static SKTestRunner sRunningTestRunner = null;
  private static synchronized SKTestRunner sGetRunningTestRunner() {
    return sRunningTestRunner;
  }

  public static synchronized void sSetRunningTestRunner(SKTestRunner testRunner) {
    sRunningTestRunner = testRunner;
  }

  // Make base class private!
  public SKTestRunner() {
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
  }

  protected void sendTestResultToUIHandler(final JSONObject pm) {
  }

  protected void sendPassiveMetricToUIHandler(final JSONObject o) {
  }

  protected void sendCompletedMessageToUIHandlerWithMilliDelay(long milliDelay) {

    if (mObserver == null) {
      sSetRunningTestRunner(null);
      return;
    }
  }

  protected void setStateChangeToUIHandler(final TestRunnerState state) {
    if (state == TestRunnerState.STARTING) {
      sSetRunningTestRunner(this);
    } else if (state == TestRunnerState.STOPPED) {
      sSetRunningTestRunner(null);
    }
  }

  public static void sDoReportUDPFailedSkipTests() {
    final SKTestRunner runner = sGetRunningTestRunner();
    if (runner == null) {
      SKPorting.sAssert(false);
      return;
    }
  }

  public static void sDoReportCurrentLatencyCalculated(final long latencyMilli) {
    final SKTestRunner runner = sGetRunningTestRunner();
    if (runner == null) {
      SKPorting.sAssert(false);
      return;
    }
  }

  public static void sDoReportClosestTargetSelected(final String closestTarget) {
    final SKTestRunner runner = sGetRunningTestRunner();
    if (runner == null) {
      SKPorting.sAssert(false);
      return;
    }
  }
}
