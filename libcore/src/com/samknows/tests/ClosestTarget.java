package com.samknows.tests;

import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.util.Log;
import android.util.Pair;

import com.samknows.libcore.SKPorting;
import com.samknows.measurement.TestRunner.SKTestRunner;
import com.samknows.measurement.util.SKDateFormat;

public class ClosestTarget extends SKAbstractBaseTest implements Runnable {

  /*
   * constants for creating a ClosestTarget test
   */
  private final String NUMBEROFPACKETS = "numberOfPackets";
  private final String DELAYTIMEOUT = "delayTimeout";
  private final String INTERPACKETTIME = "interPacketTime";

  private final static String VALUE_NOT_KNOWN = "-";

  public static final String TESTSTRING = "CLOSESTTARGET";

  /*
   * Default values for the LatencyTest
   */
  private final int _NPACKETS = 5;
  private final int _INTERPACKETTIME = 1000000;
  private final int _DELAYTIMEOUT = 2000000;
  private final int _PORT = 6000;

  /*
   * Constraints for the test parameters This values are needed to avoid to
   * misconfigure the latency test and hence to make the test useless or worst
   * to get stuck with the closest target test execution
   */
  private final int NUMBEROFPACKETSMAX = 100;
  private final int NUMBEROFPACKETSMIN = 5;
  private final int INTERPACKETIMEMAX = 60000000;
  private final int INTERPACKETIMEMIN = 10000;
  private final int DELAYTIMEOUTMIN = 1000000;
  private final int DELAYTIMEOUTMAX = 5000000;
  private final int NUMBEROFTARGETSMAX = 50;
  private final int NUMBEROFTARGETSMIN = 1;

  public static final String JSON_CLOSETTARGET = "closest_target";
  public static final String JSON_IPCLOSESTTARGET = "ip_closest_target";

  private ClosestTarget(List<Param> params) {
    synchronized (ClosestTarget.this) {
      sClosestTarget = "";
      setParams(params);
    }
  }

  public static ClosestTarget sCreateClosestTarget(List<Param> params) {
    return new ClosestTarget(params);
  }

  public class Result {
    public int total;
    public int completed;
    public String currbest_target;
    public long curr_best_timeNanoseconds;
  }

  //Used to collect the results from the individual LatencyTests as soon as the finish
  private final BlockingQueue<LatencyTest.Result> bq_results = new LinkedBlockingQueue<>();

  //public ClosestTarget() {
  //	synchronized (ClosestTarget.this) {
  //		sClosestTarget = "";
  //	}
  //}

  private boolean between(int x, int a, int b) {
    return (x >= a && x <= b);
  }

  @Override
  public boolean isReady() {
    if (!between(nPackets, NUMBEROFPACKETSMIN, NUMBEROFPACKETSMAX)) {
      SKPorting.sAssert(getClass(), false);
      return false;
    }
    if (!between(interPacketTime, INTERPACKETIMEMIN, INTERPACKETIMEMAX)) {
      SKPorting.sAssert(getClass(), false);
      return false;
    }
    if (!between(delayTimeout, DELAYTIMEOUTMIN, DELAYTIMEOUTMAX)) {
      SKPorting.sAssert(getClass(), false);
      return false;
    }
    if (!between(targets.size(), NUMBEROFTARGETSMIN, NUMBEROFTARGETSMAX)) {
      SKPorting.sAssert(getClass(), false);
      return false;
    }

    return true;
  }

  @Override
  public void runBlockingTestToFinishInThisThread() {
    find();
  }

  @Override
  public void run() {
    find();
  }

  @Override
  public int getNetUsage() {
    return 0;
  }

  @Override
  public boolean isSuccessful() {
    return success;
  }

  private void setNumberOfDatagrams(int n) {
    nPackets = n;
  }

  private void setInterPacketTime(int t) {
    interPacketTime = t;
  }

  private void setDelayTimeout(int t) {
    delayTimeout = t;
  }

  private void setPort(int p) {
    port = p;
  }

  private void addTarget(String target) {
    targets.add(target);
  }

  public void setTargetListEmpty() {
    targets = new ArrayList<>();
  }

  /*
  Some networks block UDP traffic; and some might even block raw TCP traffic!
  GIVEN: performing a closest target test
  WHEN:  UDP fails
  THEN:  we need use HTTP as the ultimate failsafe.
  Therefore, as a fall-back from the UDP best-target-selection process:
  1. Make three HTTP requests to "/" on each server. Set a 2 second timeout on each request.
     Ideally, you should parallelise them (maybe allow up to 6 concurrent requests).
  2. Choose the server with the lowest non-zero response time
     (not an average of the three requests - just take the one with the absolute lowest)
  */

  private static final int CHttpQueryTimeoutSeconds = 2;

  // Query the specified URL, and return <status,latency>
  private static Pair<Boolean, Long> sGetHttpResponseAndReturnLatencyMilliseconds(String urlString) {
    //AsyncHttpClient client = new AsyncHttpClient();
    //client.setTimeout(5000); // This is in milliseconds!

    Log.d(ClosestTarget.sGetTAG(), "DEBUG: fire http closest target query at (" + urlString + ")");

    // https://stackoverflow.com/questions/3000214/java-http-client-request-with-defined-timeout
    HttpParams httpParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(httpParams, CHttpQueryTimeoutSeconds * 1000);
    HttpConnectionParams.setSoTimeout(httpParams, CHttpQueryTimeoutSeconds * 1000);

    HttpClient httpclient = new DefaultHttpClient(httpParams);

    HttpGet httpGet = new HttpGet(urlString);
    HttpResponse response;

    // Fire the query!
    final Date startTime = new Date();

    try {
      response = httpclient.execute(httpGet);

      // Check if server response is valid
      StatusLine status = response.getStatusLine();
      if (status.getStatusCode() != 200) {
        Log.d("TAG", "Invalid response from server: " + status.toString());
        SKPorting.sAssert(ClosestTarget.class, false);
      } else {
        // Successful query, handle the response!
        final Date timeNow = new Date();

        long measuredLatencyMilliseconds = timeNow.getTime() - startTime.getTime();
        SKPorting.sAssert(ClosestTarget.class, measuredLatencyMilliseconds > 0);

        Log.d(ClosestTarget.sGetTAG(), "DEBUG: HTTP/Closest target test - success - measuredLatencyMilliseconds = " + measuredLatencyMilliseconds);

        return new Pair<>(true, measuredLatencyMilliseconds);
      }

    } catch (SocketTimeoutException ste) {
      // Don't fire a debug-time assertion if we have a simple timeout!
      Log.d(ClosestTarget.sGetTAG(), "DEBUG: HTTP/Closest target test - SocketTimeoutException");
    } catch (ConnectTimeoutException cte) {
      // Don't fire a debug-time assertion if we have a simple timeout!
      Log.d(ClosestTarget.sGetTAG(), "DEBUG: HTTP/Closest target test - ConnectTimeoutException");
    } catch (UnknownHostException uhe) {
      // Don't fire a debug-time assertion if the host cannot be found - it might just be down.
      Log.d(ClosestTarget.sGetTAG(), "DEBUG: HTTP/Closest target test - UnknownHostException");
    } catch (Exception e) {
      // This might show up if e.g. all network connections are disabled.
      SKPorting.sAssert(ClosestTarget.class, false);
    }

    return new Pair<>(false, -100L);
  }

  private static final String TAG = ClosestTarget.class.getName();

  private static String sGetTAG() {
    return TAG;
  }

  private final int cQueryCountPerServer = 3;
  private final ArrayList<Integer> finishedTestsPerServer = new ArrayList<>();

  private boolean mbInHttpTestingFallbackMode = false;
  private boolean mbUdpClosestTargetTestSucceeded = false;

  public boolean getUdpClosestTargetTestSucceeded() {
    return mbUdpClosestTargetTestSucceeded;
  }

  // http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/CountDownLatch.html
  class WorkerRunner extends Thread {
    private final int serverIndex;
    private final String target;
    private final String urlString;

    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;

    private long measuredLatencyMilliseconds = -100L;

    LatencyTest latencyTest = null;

    public long getMeasuredLatencyMilliseconds() {
      return measuredLatencyMilliseconds;
    }


    WorkerRunner(int inServerIndex, String inTarget, String inUrlString, CountDownLatch inStartSignal, CountDownLatch inDoneSignal) {
      super();

      serverIndex = inServerIndex;
      target = inTarget;
      urlString = inUrlString;
      startSignal = inStartSignal;
      doneSignal = inDoneSignal;

      latencyTest = latencyTests[serverIndex];
      SKPorting.sAssert(getClass(), latencyTest.getTarget().equals(target));
    }


    @Override
    public void run() {
      try {
        startSignal.await();
        doWork();

      } catch (InterruptedException ex) {
        SKPorting.sAssert(getClass(), false);
      }

      Log.d("RUN()", "Finished run() in WorkerRunner!");
    }

    void doWork() {

      Pair<Boolean, Long> latencyQueryResult = ClosestTarget.sGetHttpResponseAndReturnLatencyMilliseconds(urlString);
      if (latencyQueryResult.first) {
        // Succeeded!
        measuredLatencyMilliseconds = latencyQueryResult.second;
        SKPorting.sAssert(getClass(), measuredLatencyMilliseconds > 0L);
      } else {
        // Failed to get a latency measurement!
        measuredLatencyMilliseconds = -100L;
      }

      doneSignal.countDown();

      synchronized (ClosestTarget.this) {
        // This allows the UI to update itself, according to our current best guess...

        // used for UI reporting!
        if (measuredLatencyMilliseconds > 0) {
          if (measuredLatencyMilliseconds * 1000000 < curr_best_Nanoseconds) {
            curr_best_Nanoseconds = measuredLatencyMilliseconds * 1000000;
            curr_best_target = target;
            closestTarget = target;
          }
        }

        // Increment "finished" only when the last async test for the server is completed.
        int value = finishedTestsPerServer.get(serverIndex);
        value++;
        finishedTestsPerServer.set(serverIndex, value);

        if (value == cQueryCountPerServer) {
          finished++;
        }

        latencyTest.status = SKAbstractBaseTest.STATUS.DONE;
        latencyTest.finished = true;

        // Add a new result to the queue for display...
        // Will be ignored if not the best yet!
        LatencyTest.sCreateAndPushLatencyResultNanoseconds(bq_results, closestTarget, curr_best_Nanoseconds);
      }
    }
  }

  // This only returns when done...
  private boolean blockingTryHttpClosestTargetTestIfUdpTestFails() {

    mbInHttpTestingFallbackMode = true;
    mbUdpClosestTargetTestSucceeded = false;

    long timeAtStart = new Date().getTime();

    // Prevent weird warnings from the outer UI query thread...
    success = false;
    finished = 0;
    closestTarget = VALUE_NOT_KNOWN;

    String TAG = getClass().getName();

    Log.d(TAG, "DEBUG: tryHttpClosestTargetTestIfUdpTestFails");


    // 3 Threads per server!
    int serverCount = targets.size();

    Log.d(TAG, "DEBUG: tryHttpClosestTargetTestIfUdpTestFails - serverCount=" + serverCount);

    {
      int tempIndex;
      for (tempIndex = 0; tempIndex < serverCount; tempIndex++) {
        Log.d(TAG, "DEBUG: tryHttpClosestTargetTestIfUdpTestFails - targets[" + tempIndex + "]=" + targets.get(tempIndex));
        finishedTestsPerServer.add(0);
      }
    }

    int queriesToRun = serverCount * cQueryCountPerServer;
    Log.d(TAG, "DEBUG: tryHttpClosestTargetTestIfUdpTestFails - queriesToRun=" + queriesToRun);

    // http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/CountDownLatch.html
    CountDownLatch startSignal = new CountDownLatch(1);
    CountDownLatch queryCompleteCountdown = new CountDownLatch(queriesToRun);
    //queryCompleteCountdown = queriesToRun;
    ArrayList<Long> bestLatencyMillisecondsPerServer = new ArrayList<>();

    //
    // Fire this off in separate async tasks, and block waiting for them all to complete!
    //

    int serverIndex;
    for (serverIndex = 0; serverIndex < serverCount; serverIndex++) {
      // -100 means - no successful response - yet!
      bestLatencyMillisecondsPerServer.add(-100L);
    }

    ArrayList<WorkerRunner> workerRunnableArray = new ArrayList<>();

    for (serverIndex = 0; serverIndex < serverCount; serverIndex++) {
      String target = targets.get(serverIndex);
      String urlString = "http://" + target + "/";

      int queryIndexForServer;
      for (queryIndexForServer = 0; queryIndexForServer < cQueryCountPerServer; queryIndexForServer++) {
        // http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/CountDownLatch.html
        WorkerRunner workerRunnable = new WorkerRunner(serverIndex, target, urlString, startSignal, queryCompleteCountdown);
        workerRunnableArray.add(workerRunnable);
        workerRunnable.start();
      }
    }

    //
    // Block until all the async task queries have completed!
    //

    // Tell the async tasks to start working!
    startSignal.countDown();

    // Block, waiting for all the async task queries to complete.
    try {
      queryCompleteCountdown.await();
    } catch (InterruptedException e) {
      SKPorting.sAssert(getClass(), false);
    }

    //
    // We have finished the tests, for all servers!
    //
    long now = new Date().getTime();
    Log.d("HTTPClosestTarget", "time in seconds taken to run all HTTP tests = " + ((double) (now - timeAtStart)) / 1000.0);

    // Return true if we succeed - in which case :
    // closestTarget = set to the right target
    // ipClosestTarget = the ip address of the closest target
    if (closestTarget.equals(VALUE_NOT_KNOWN)) {
      // This can happen e.g. if all networking is off... but it is quite unlikely.
      SKPorting.sAssert(getClass(), false);
    } else {
      success = true;

      InetAddress address = null;
      try {
        address = InetAddress.getByName(closestTarget);
        ipClosestTarget = address.getHostAddress();
      } catch (UnknownHostException e) {
        SKPorting.sAssert(getClass(), false);
      }
    }

    // This tells the UI monitor thread to finish.
    finished = targets.size() + 1;

    return success;
  }


  private boolean find() {
    boolean ret = false;
    if (targets.size() == 0) {
      return ret;
    }
    ArrayList<Thread> threads = new ArrayList<>();
    latencyTests = new LatencyTest[targets.size()];

    for (int i = 0; i < targets.size(); i++) {
      LatencyTest lt = new LatencyTest(targets.get(i), port, nPackets,
          interPacketTime, delayTimeout);
      lt.setBlockingQueueResult(bq_results);
      latencyTests[i] = lt;
      if (latencyTests[i].isReady()) {
        Thread t = new Thread(latencyTests[i]);
        threads.add(t);
        t.start();
      }
    }

    for (int i = 0; i < targets.size(); i++) {
      try {
        threads.get(i).join();

      } catch (InterruptedException ie) {
        ie.printStackTrace();
        SKPorting.sAssert(getClass(), false);
      }
    }
    int minDist = Integer.MAX_VALUE;
    for (int i = 0; i < targets.size(); i++) {

      if (latencyTests[i].isSuccessful()) {
        success = true;
        int avg = (int) latencyTests[i].getAverageMicroseconds();
        if (avg < minDist) {
          closestTarget = targets.get(i);
          ipClosestTarget = latencyTests[i].getIpAddress();
          minDist = avg;
        }
      }
    }


    if (closestTarget.equals(VALUE_NOT_KNOWN)) {
      // Run the Http-based Closest Target test as a fall-back condition!
      // This will return only when fully done...
      mbUdpClosestTargetTestSucceeded = false;
      finished = 0;

      // WHEN:
      // - if closest target UDP test failed (NB: this is ALWAYS run first in manual testing)
      // THEN:
      // - notify the app to display test as UDP skipped
      // NOTE: Doesn't actually matter if we're running a manual test or not - if we're not running a manual test,
      // the user interface will ignore this event.
      SKTestRunner.sDoReportUDPFailedSkipTests();

      ret = blockingTryHttpClosestTargetTestIfUdpTestFails();

      // HTTP (blocking) test succeeded!
      mbFinishedSelectingClosestTarget = true;
    } else {
      // UDP test succeeded!
      mbUdpClosestTargetTestSucceeded = true;
      mbFinishedSelectingClosestTarget = true;
      ret = true;
    }
    return ret;
  }

  public String getClosest() {
    if (closestTarget.equals(VALUE_NOT_KNOWN)) {
      return null;
    }
    return closestTarget;
  }

  private Long mTimestamp = SKAbstractBaseTest.sGetUnixTimeStampSeconds();
  @Override
  public synchronized void finish() {
    mTimestamp = SKAbstractBaseTest.sGetUnixTimeStampSeconds();
    status = STATUS.DONE;
  }

  @Override
  public long getTimestamp() {
    return mTimestamp;
  }

  @Override
  public void setTimestamp(long timestamp) {
    mTimestamp = timestamp;
  }

  @Override
  public JSONObject getJSONResult() {
    ArrayList<String> o = new ArrayList<>();
    Map<String, Object> output = new HashMap<>();
    //string id
    o.add(TESTSTRING);
    output.put(JsonData.JSON_TYPE, TESTSTRING);
    //TIME
    o.add(mTimestamp + "");
    output.put(JsonData.JSON_TIMESTAMP, mTimestamp);
    output.put(JsonData.JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(mTimestamp * 1000)));
    //status
    boolean status = true;
    if (closestTarget.equals(VALUE_NOT_KNOWN)) {
      status = false;
    }

    synchronized (ClosestTarget.class) {
      sClosestTarget = closestTarget;
    }

    o.add(status ? "OK" : "FAIL");
    output.put(JsonData.JSON_SUCCESS, status);
    //closest target - might be VALUE_NOT_KNOWN...
    o.add(closestTarget);
    output.put(JSON_CLOSETTARGET, closestTarget);
    //ip closest target
    o.add(ipClosestTarget);
    output.put(JSON_IPCLOSESTTARGET, ipClosestTarget);

    //setOutput(o.toArray(new String[1]));
    JSONObject json_output = new JSONObject(output);
    return json_output;
  }

  private LatencyTest[] latencyTests = null;
  private ArrayList<String> targets = new ArrayList<>();
  private int nPackets = _NPACKETS;
  private int interPacketTime = _INTERPACKETTIME;
  private int delayTimeout = _DELAYTIMEOUT;
  private int port = _PORT;
  private String closestTarget = VALUE_NOT_KNOWN;
  private boolean success = false;
  private String ipClosestTarget = VALUE_NOT_KNOWN;

  // This is used purely by the UI, to display a count-down of the target servers latency test.
  // it is accessed via getPartialResults()...
  private int finished = 0;

  private boolean mbFinishedSelectingClosestTarget = false;

  private long curr_best_Nanoseconds = Long.MAX_VALUE;
  private String curr_best_target;

  private static String sClosestTarget = VALUE_NOT_KNOWN;

  public static String sGetClosestTarget() {
    synchronized (ClosestTarget.class) {
      return sClosestTarget;
    }
  }

  public static void sSetClosestTarget(String inTarget) {
    synchronized (ClosestTarget.class) {
      sClosestTarget = inTarget;

      SKTestRunner.sDoReportClosestTargetSelected(inTarget);
    }
  }

  @Override
  public int getProgress0To100() {
    if (latencyTests == null) {
      return 0;
    }
    int min = 100;
    for (LatencyTest t : latencyTests) {
      if (t != null) {
        int curr = t.getProgress0To100();
        min = curr < min ? curr : min;
      }
    }
    return min;
  }

  public Result getPartialResults() {
    //first result when no test is finished yet
    if (finished == 0) {
      Result ret = new Result();
      ret.completed = 0;
      ret.total = targets.size();
      ret.curr_best_timeNanoseconds = 0;
      ret.currbest_target = "";

      if (mbInHttpTestingFallbackMode == false) {
        finished++; // Do this ONLY if no in HTTP-based testing!
      }

      return ret;
    }

    if (mbFinishedSelectingClosestTarget == true) { // (finished == targets.size() + 1){
      // This tells the UI monitor to finish!
      return null;
    }

    Result ret = new Result();
    try {
      LatencyTest.Result r = bq_results.take();
      ret.completed = finished;

      if (mbInHttpTestingFallbackMode == false) {
        finished++; // Do this ONLY if no in HTTP-based testing!
      }

      ret.total = targets.size();
      if (r.rttMicroseconds > 0 && (r.rttMicroseconds * 1000L) < curr_best_Nanoseconds) {
        curr_best_Nanoseconds = (r.rttMicroseconds * 1000L);
        curr_best_target = r.target;
      }
      ret.curr_best_timeNanoseconds = curr_best_Nanoseconds;
      ret.currbest_target = curr_best_target;
    } catch (InterruptedException ie) {
      ie.printStackTrace();
      ret = null;
    }

    return ret;
  }

//	@Override
//	public HumanReadable getHumanReadable() {
//		// TODO Auto-generated method stub
//		return null;
//	}

  @Override
  public String getStringID() {
    return TESTSTRING;
  }

  private void setParams(List<Param> params) {
    try {
      for (Param param : params) {
        String value = param.getValue();
        if (param.contains(TARGET)) {
          addTarget(value);
        } else if (param.contains(PORT)) {
          setPort(Integer.parseInt(value));
        } else if (param.contains(NUMBEROFPACKETS)) {
          setNumberOfDatagrams(Integer.parseInt(value));
        } else if (param.contains(DELAYTIMEOUT)) {
          setDelayTimeout(Integer.parseInt(value));
        } else if (param.contains(INTERPACKETTIME)) {
          setInterPacketTime(Integer.parseInt(value));
        } else {
          initialised = false;
          break;
        }
      }
    } catch (NumberFormatException nfe) {
      initialised = false;
    }
  }
}
