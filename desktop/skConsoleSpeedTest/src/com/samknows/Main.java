package com.samknows;

import com.samknows.SKKit.SKKitTestClosestTarget;
import com.samknows.SKKit.SKKitTestDownload;
import com.samknows.SKKit.SKKitTestLatency;
import com.samknows.SKKit.SKKitTestUpload;
import com.samknows.libcore.SKPorting;
import com.samknows.tests.DownloadTest;
import com.samknows.tests.Param;

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

  public static void doClosestTargetTest() {

    System.out.println("Start ClosestTarget Test");

    ArrayList<SKKitTestClosestTarget.ClosestTargetHostDescriptor> targetList = new ArrayList<>();
    targetList.add(new SKKitTestClosestTarget.ClosestTargetHostDescriptor("samknows1.dal1.level3.net", "Dallas, USA"));
    targetList.add(new SKKitTestClosestTarget.ClosestTargetHostDescriptor("samknows2.nyc2.level3.net", "New York, USA"));
    targetList.add(new SKKitTestClosestTarget.ClosestTargetHostDescriptor("samknows1.nyc2.level3.net", "New York, USA"));
    targetList.add(new SKKitTestClosestTarget.ClosestTargetHostDescriptor("samknows1.sjo1.level3.net", "San Jose, USA"));
    targetList.add(new SKKitTestClosestTarget.ClosestTargetHostDescriptor("samknows1.wdc4.level3.net", "Washington D.C., USA"));
    targetList.add(new SKKitTestClosestTarget.ClosestTargetHostDescriptor("samknows1.chi2.level3.net", "Chicago, USA"));
    targetList.add(new SKKitTestClosestTarget.ClosestTargetHostDescriptor("samknows1.lax1.level3.net", "Los Angeles, USA"));

    targetList.add(new SKKitTestClosestTarget.ClosestTargetHostDescriptor("n1-the1.samknows.com", "London, UK"));

    SKKitTestClosestTarget.SKKitTestDescriptor_ClosestTarget closestTargetTestDescriptor;
    closestTargetTestDescriptor = new SKKitTestClosestTarget.SKKitTestDescriptor_ClosestTarget(targetList);

    final SKKitTestClosestTarget closestTargetTest = new SKKitTestClosestTarget(closestTargetTestDescriptor);

    //
    // Use a Condition/Lock to allow this method to block until the test completes.
    //
    final Lock lock = new ReentrantLock();
    final Condition cv = lock.newCondition();
    lock.lock();

    //
    // Kick-off the background test thread.
    //
    closestTargetTest.start(new SKKitTestClosestTarget.ISKClosestTargetTestProgressUpdate() {
      @Override
      public void onTestProgress_OnMainThread(int progress0To100) {

      }

      @Override
      public void onTestCompleted_OnMainThread(String closestTarget) {
        System.out.println("ClosestTarget test result: " + closestTarget);

        // We arrive here with the mutex unlocked (the await() causes the unlock to happen).
        // We must grab the lock, call signal, and then unlock.
        // The await() will then unblock and continue.
        lock.lock();
        cv.signal();
        lock.unlock();

      }
    });

    //
    // Wait for the background test to complete.
    //
    try {
      // The call to await releases the lock, and we then wait for the condition to be signalled (and the mutex unlocked)
      cv.await();
    } catch (InterruptedException e) {
      SKPorting.sAssert(false);
    }

    System.out.println("Completed ClosestTarget Test");
  }

  public static void doDownloadTest() {

    System.out.println("Start Download Test");

    SKKitTestDownload.SKKitTestDescriptor_Download downloadTestDescriptor;
    downloadTestDescriptor = new SKKitTestDownload.SKKitTestDescriptor_Download("speedtestsk1.ofca.gov.hk");
    downloadTestDescriptor.mWarmupMaxTimeSeconds = 2.0;
    downloadTestDescriptor.mTransferMaxTimeSeconds = 5.0;
    downloadTestDescriptor.mNumberOfThreads = 16;
    downloadTestDescriptor.mBufferSizeBytes = 4096*4;

    final SKKitTestDownload downloadTest = new SKKitTestDownload(downloadTestDescriptor);

    //
    // Use a Condition/Lock to allow this method to block until the test completes.
    //
    final Lock lock = new ReentrantLock();
    final Condition cv = lock.newCondition();
    lock.lock();

    //
    // Kick-off the background test thread.
    //
    downloadTest.start(new SKKitTestDownload.ISKDownloadTestProgressUpdate() {
      @Override
      public void onTestCompleted_OnMainThread(double mbpsPerSecond1024Based) {
        System.out.println("Download test result: " + mbpsPerSecond1024Based + " Mbps");

        // We arrive here with the mutex unlocked (the await() causes the unlock to happen).
        // We must grab the lock, call signal, and then unlock.
        // The await() will then unblock and continue.
        lock.lock();
        cv.signal();
        lock.unlock();
      }
    });

    //
    // Wait for the background test to complete.
    //
    try {
      // The call to await releases the lock, and we then wait for the condition to be signalled (and the mutex unlocked)
      cv.await();
    } catch (InterruptedException e) {
      SKPorting.sAssert(false);
    }

    System.out.println("Completed Download Test");
  }

  public static void doUploadTest() {
    System.out.println("Start Upload Test");

    SKKitTestUpload.SKKitTestDescriptor_Upload uploadTestDescriptor;
    uploadTestDescriptor = new SKKitTestUpload.SKKitTestDescriptor_Upload("n1-the1.samknows.com");
    uploadTestDescriptor.mPort = 8080;
    uploadTestDescriptor.mWarmupMaxTimeSeconds = 2.0;
    uploadTestDescriptor.mTransferMaxTimeSeconds = 10.0;
    uploadTestDescriptor.mNumberOfThreads = 3;
    uploadTestDescriptor.mSendDataChunkSizeBytes = 8192; // 4096*4; // 16K!

    SKKitTestUpload uploadTest = new SKKitTestUpload(uploadTestDescriptor);

    //
    // Use a Condition/Lock to allow this method to block until the test completes.
    //
    Lock lock = new ReentrantLock();
    final Condition cv = lock.newCondition();
    lock.lock();

    //
    // Kick-off the background test thread.
    //
    uploadTest.start(mbpsPerSecond1024Based -> {
      System.out.println("Upload test result: " + mbpsPerSecond1024Based + " Mbps");

      // Note that on desktop Java, this will not be on the main thread.
      // We arrive here with the mutex unlocked (the await() causes the unlock to happen).
      // We must grab the lock, call signal, and then unlock.
      // The await() will then unblock and continue.
      lock.lock();
      cv.signal();
      lock.unlock();
    });

    //
    // Wait for the background test to complete.
    //
    try {
      // The call to await releases the lock, and we then wait for the condition to be signalled (and the mutex unlocked)
      cv.await();
    } catch (InterruptedException e) {
      SKPorting.sAssert(false);
    }

    System.out.println("Completed Upload Test");
  }

  public static void doLatencyTest() {

    System.out.println("Start Latency Test");

    SKKitTestLatency.SKKitTestDescriptor_Latency latencyTestDescriptor;
    latencyTestDescriptor = new SKKitTestLatency.SKKitTestDescriptor_Latency("speedtestsk1.ofca.gov.hk");
    latencyTestDescriptor.mMaxTimeSeconds = 5.0;
    latencyTestDescriptor.mNumberOfPackets = 10;
    final SKKitTestLatency latencyTest = new SKKitTestLatency(latencyTestDescriptor);

    //
    // Use a Condition/Lock to allow this method to block until the test completes.
    //
    final Lock lock = new ReentrantLock();
    final Condition cv = lock.newCondition();
    lock.lock();

    //
    // Kick-off the background test thread.
    //
    latencyTest.start(new SKKitTestLatency.ISKLatencyTestProgressUpdate() {
      @Override
      public void onTestProgress_OnMainThread(int progress0To100, double latency) {

      }

      @Override
      public void onTestCompleted_OnMainThread(double latency, double loss, double jitterMilliseconds) {
        System.out.println("Latency test result: latency=" + latency + ", loss=" + loss + ", jitter=" + jitterMilliseconds + " ms");

        // We arrive here with the mutex unlocked (the await() causes the unlock to happen).
        // We must grab the lock, call signal, and then unlock.
        // The await() will then unblock and continue.
        lock.lock();
        cv.signal();
        lock.unlock();
      }
    });

    //
    // Wait for the background test to complete.
    //
    try {
      // The call to await releases the lock, and we then wait for the condition to be signalled (and the mutex unlocked)
      cv.await();
    } catch (InterruptedException e) {
      SKPorting.sAssert(false);
    }

    System.out.println("Completed Latency Test");
  }

  public static void main(String[] args) {
//    System.out.println("Start Tests...!");

//    doClosestTargetTest();
//    doDownloadTest();
    doUploadTest();
//    doLatencyTest();

//    System.out.println("Tests completed!");
  }
}
