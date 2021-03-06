package com.samknows.tests;

//import java.util.Vector;

import org.json.JSONObject;

//
// Base class for the tests
//

abstract public class SKAbstractBaseTest {

  //
  // END: code that you can make behave differently if you so wish on Android...
  //

  //private String[] outputFields = null;
  private String errorString = "";
  //private JSONObject json_output = null;

  public static final String TARGET = "target";
  public static final String PORT = "port";
  public static final String FILE = "file";

  protected enum STATUS {WAITING, RUNNING, DONE}

  protected STATUS status;
  protected boolean finished;
  protected boolean initialised;

  public SKAbstractBaseTest() {
    status = STATUS.WAITING;
  }

  protected synchronized void setStateToRunning() {
    status = STATUS.RUNNING;
  }

  // THIS IS THE PUBLIC METHOD USED TO TO START THE TEST RUNNING...
  public abstract void runBlockingTestToFinishInThisThread();

  protected abstract String getStringID();
  abstract public boolean isSuccessful();
  abstract public int getProgress0To100(); 										/* from 0 to 100 */
  abstract public boolean isReady();										/* Checks if the test is ready to run */
  abstract public int getNetUsage();										/* The test has to provide the amount of data used */
  abstract public JSONObject getJSONResult();
  abstract public long getTimestamp();
  abstract public void setTimestamp(long timestamp); // Used only for special cases
  abstract public void finish();

  public static long sGetUnixTimeStampSeconds() {
    return System.currentTimeMillis() / 1000;
  }

  public static long sGetUnixTimeStampMilli() {
    return System.currentTimeMillis();
  }

  protected boolean setErrorIfEmpty(String error, Exception e) {
    String exErr = e.getMessage() == null ? "No exception message" : e.getMessage();
    return setErrorIfEmpty(error + " " + exErr);
  }

  protected boolean setErrorIfEmpty(String error) {
    boolean ret = false;
    synchronized (errorString) {
      if (errorString.equals("")) {
        errorString = error;
        ret = true;
      }
    }
    return ret;
  }

  protected void setError(String error) {
    synchronized (errorString) {
      errorString = error;
    }
  }

  //region Test Cancel control
  // The PassiveServerUploadTest, DownloadTest, LatencyTest classes all detect this stage and allow quick Cancelling of the test
  // even while it is running.
  // Other implements of Test (e.g. ActivServerloadTest) do not yet support this approach.
  private boolean mbShouldCancel = false;

  public boolean getShouldCancel() {
    return mbShouldCancel;
  }

  public void setShouldCancel() {
    mbShouldCancel = true;
  }
  //endregion Test Cancel control
}
