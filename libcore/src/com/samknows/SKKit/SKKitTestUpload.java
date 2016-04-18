package com.samknows.SKKit;

import android.os.Handler;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.tests.Conversions;
import com.samknows.tests.PassiveServerUploadTest;
import com.samknows.tests.UploadTest;
import com.samknows.tests.HttpTest;
import com.samknows.tests.Param;
import com.samknows.tests.SKAbstractBaseTest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SKKitTestUpload {

  // The Test Descriptor allows you to override the settings of various properties used for the Upload test.
  static public class SKKitTestDescriptor_Upload { // : SKKitTestDescriptor
    private final String mTarget;

    public final Integer mPort;
    public final Double mWarmupMaxTimeSeconds;
    public Double mTransferMaxTimeSeconds;
    //public Integer mWarmupMaxBytes;
    //public Integer mTransferMaxBytes;
    public final Integer mNumberOfThreads;
    public final Integer mSendDataChunkSizeBytes;
    //public Integer mPostDataLengthBytes;

    public SKKitTestDescriptor_Upload(String target) {
      if (target == null) {
        SKLogger.sAssert(false);
      } else {
        SKLogger.sAssert(!target.isEmpty());
      }

      mTarget = target;

      mPort = 8080;
      mWarmupMaxTimeSeconds = 0.001;
      mTransferMaxTimeSeconds = 10.0;
      mNumberOfThreads = 1;
      mSendDataChunkSizeBytes = 512;
    }
  }

  public interface ISKUploadTestProgressUpdate {

    void onTestCompleted_OnMainThread(double mbpsPerSecond1024Based);
  }

  private final SKKitTestDescriptor_Upload mTestDescriptor;
  private UploadTest mUploadTest;

  // Adaptor for extracting JSON data for export!
  public JSONObject getJSONResult() {
    return mUploadTest.getJSONResult();
  }

  public SKKitTestUpload(SKKitTestDescriptor_Upload testDescriptor) {
    mTestDescriptor = testDescriptor;
    SKLogger.sAssert(!testDescriptor.mTarget.isEmpty());
  }

  private final Handler mHandler = new Handler();

  public double getTransferBytesPerSecond() {
    if (mUploadTest != null) {
      return mUploadTest.getTransferBytesPerSecond();
    }

    SKLogger.sAssert(false);
    return 0;
  }

  public void start(final ISKUploadTestProgressUpdate progressUpdate) {
    SKLogger.sAssert(progressUpdate != null);

    Thread uploadThread = new Thread() {
      @Override
      public void run() {
        super.run();

        List<Param> params = new ArrayList<>();
        params.add(new Param(SKAbstractBaseTest.PORT, mTestDescriptor.mPort.toString()));
        params.add(new Param(SKAbstractBaseTest.TARGET, mTestDescriptor.mTarget));
        params.add(new Param(HttpTest.WARMUPMAXTIME, String.valueOf((int) (mTestDescriptor.mWarmupMaxTimeSeconds * 1000000.0)))); // Microseconds!

        params.add(new Param(HttpTest.TRANSFERMAXTIME, String.valueOf((int) (mTestDescriptor.mTransferMaxTimeSeconds * 1000000.0)))); // Microseconds
        params.add(new Param(HttpTest.NTHREADS, String.valueOf(mTestDescriptor.mNumberOfThreads)));
        params.add(new Param(HttpTest.SENDDATACHUNK, String.valueOf(mTestDescriptor.mSendDataChunkSizeBytes)));

        // Without setting the following value, we can observe very long delays while performing the final write
        // in PassiveServerUploadTest.java ...!
        // e.g. more than 30 seconds!
        // Note that long delays (of approximately 0.5 to 1.5 seconds) can occcur quite often.
        params.add(new Param(HttpTest.SENDBUFFERSIZE, String.valueOf(mTestDescriptor.mSendDataChunkSizeBytes)));

        Log.d("IHT", "START Running upload test for milliseconds=" + mTestDescriptor.mTransferMaxTimeSeconds * 1000.0);

        mUploadTest = PassiveServerUploadTest.sCreatePassiveServerUploadTest(params);
//        // The default behaviour is to fail if socket write timout occurs (which is 10 seconds by default!)
//        mUploadTest.setSocketTimeoutMilliseconds(1000);
//        mUploadTest.setIgnoreSocketTimeout(true);
        final UploadTest theTest = mUploadTest;

        long timeStartMilli = System.currentTimeMillis();
        mUploadTest.runBlockingTestToFinishInThisThread();
        long timeEndMilli = System.currentTimeMillis();

        long actualTimeTakenMilli = timeEndMilli - timeStartMilli;
        Log.d("IHT", "STOPPED Running upload test for milliseconds=" + actualTimeTakenMilli + ", completed after " + actualTimeTakenMilli);

        mHandler.post(new Runnable() {
          public void run() {

            // Finished the upload test!

            if (progressUpdate != null) {
              double bytesPerSecond = theTest.getTransferBytesPerSecond();

              // To reach here, we've finished the upload test!
              double mbpsPerSecond1024Based = Conversions.sConvertBytesPerSecondToMbps1024Based(bytesPerSecond);
              progressUpdate.onTestCompleted_OnMainThread(mbpsPerSecond1024Based);
            }
            mUploadTest = null;
          }
        });
      }
    };
    uploadThread.start();

  }

  public void cancel() {
    if (mUploadTest != null) {
      mUploadTest.setShouldCancel();
    }
  }

  public int getProgress0To100() {
    if (mUploadTest == null) {
      SKLogger.sAssert(false);
      return 0;
    }
    return mUploadTest.getProgress0To100();
  }
}
