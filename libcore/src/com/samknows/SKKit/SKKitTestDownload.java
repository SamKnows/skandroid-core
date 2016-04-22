package com.samknows.SKKit;

import android.os.Handler;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.tests.Conversions;
import com.samknows.tests.DownloadTest;
import com.samknows.tests.HttpTest;
import com.samknows.tests.Param;
import com.samknows.tests.SKAbstractBaseTest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SKKitTestDownload {

  // The Test Descriptor allows you to override the settings of various properties used for the download test.
  static public class SKKitTestDescriptor_Download { // : SKKitTestDescriptor
    private final String mTarget;

    public final Integer mPort;
    public final String mFile;
    public final Double mWarmupMaxTimeSeconds;
    public Double mTransferMaxTimeSeconds;
    //public Integer mWarmupMaxBytes;
    //public Integer mTransferMaxBytes;
    public Integer mNumberOfThreads;
    public final Integer mBufferSizeBytes;

    public SKKitTestDescriptor_Download(String target) {
      if (target == null) {
        SKLogger.sAssert(false);
      } else {
        SKLogger.sAssert(!target.isEmpty());
      }

      mTarget = target;

      mPort = 80;
      mFile = "1000MB.bin";
      mWarmupMaxTimeSeconds = 2.0;
      mTransferMaxTimeSeconds = 10.0;
      mNumberOfThreads = 1;
      mBufferSizeBytes = 1048576;
    }
  }

  public interface ISKDownloadTestProgressUpdate {

    void onTestCompleted_OnMainThread(double mbpsPerSecond1024Based);
  }

  private final SKKitTestDescriptor_Download mTestDescriptor;
  private DownloadTest mDownloadTest;

  public SKKitTestDownload(SKKitTestDescriptor_Download testDescriptor) {
    mTestDescriptor = testDescriptor;
    SKLogger.sAssert(!testDescriptor.mTarget.isEmpty());
  }


  // Adaptor for extracting JSON data for export!
  public JSONObject getJSONResult() {
    return mDownloadTest.getJSONResult();
  }

  public void setTimestamp (long timestamp) {
    if (mDownloadTest != null) {
      mDownloadTest.setTimestamp(timestamp);
    }
  }

  private final Handler mHandler = new Handler();

  public double getTransferBytesPerSecond() {
    if (mDownloadTest != null) {
      return mDownloadTest.getTransferBytesPerSecond();
    }

    //SKLogger.sAssert(false);
    return 0;
  }

  public void start(final ISKDownloadTestProgressUpdate progressUpdate) {
    SKLogger.sAssert(progressUpdate != null);

    Thread downloadThread = new Thread() {
      @Override
      public void run() {
        super.run();

        List<Param> params = new ArrayList<>();
        params.add(new Param(SKAbstractBaseTest.PORT, mTestDescriptor.mPort.toString()));
        params.add(new Param(SKAbstractBaseTest.TARGET, mTestDescriptor.mTarget));
        params.add(new Param(SKAbstractBaseTest.FILE, mTestDescriptor.mFile));
        params.add(new Param(HttpTest.WARMUPMAXTIME, String.valueOf((int) (mTestDescriptor.mWarmupMaxTimeSeconds * 1000000.0)))); // Microseconds!

        params.add(new Param(HttpTest.TRANSFERMAXTIME, String.valueOf((int) (mTestDescriptor.mTransferMaxTimeSeconds * 1000000.0)))); // Microseconds
        params.add(new Param(HttpTest.NTHREADS, String.valueOf(mTestDescriptor.mNumberOfThreads)));
        params.add(new Param(HttpTest.BUFFERSIZE, String.valueOf(mTestDescriptor.mBufferSizeBytes)));

        Log.d("IHT", "START Running download test for milliseconds=" + mTestDescriptor.mTransferMaxTimeSeconds * 1000.0);

        mDownloadTest = DownloadTest.sCreateDownloadTest(params);
        final DownloadTest theTest = mDownloadTest;
        long timeStartMilli = System.currentTimeMillis();
        mDownloadTest.runBlockingTestToFinishInThisThread();
        long timeEndMilli = System.currentTimeMillis();

        long actualTimeTakenMilli = timeEndMilli - timeStartMilli;
        Log.d("IHT", "STOPPED Running download test for milliseconds=" + actualTimeTakenMilli + ", completed after " + actualTimeTakenMilli);


        mHandler.post(new Runnable() {
          public void run() {

            // Finished the download test!

            if (progressUpdate != null) {
              double bytesPerSecond = theTest.getTransferBytesPerSecond();

              // To reach here, we've finished the download test!
              double mbpsPerSecond1024Based = Conversions.sConvertBytesPerSecondToMbps1024Based(bytesPerSecond);
              progressUpdate.onTestCompleted_OnMainThread(mbpsPerSecond1024Based);
            }
            mDownloadTest = null;
          }
        });
      }
    };

    downloadThread.start();
  }

  public void cancel() {
    if (mDownloadTest != null) {
      mDownloadTest.setShouldCancel();
    }
  }

  public int getProgress0To100() {
    if (mDownloadTest == null) {
      SKLogger.sAssert(false);
      return 0;
    }
    return mDownloadTest.getProgress0To100();
  }
}
