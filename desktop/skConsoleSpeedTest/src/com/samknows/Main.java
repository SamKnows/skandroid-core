package com.samknows;

import com.samknows.SKKit.SKKitTestDownload;
import com.samknows.SKKit.SKKitTestUpload;
import com.samknows.tests.DownloadTest;
import com.samknows.tests.Param;

import java.util.ArrayList;

public class Main {

  static SKKitTestDownload mDownloadTest = null;
  static SKKitTestUpload mUploadTest = null;

  public static void doDownloadTest() {

    SKKitTestDownload.SKKitTestDescriptor_Download downloadTestDescriptor;
    downloadTestDescriptor = new SKKitTestDownload.SKKitTestDescriptor_Download("speedtestsk1.ofca.gov.hk");
    downloadTestDescriptor.mWarmupMaxTimeSeconds = 2.0;
    downloadTestDescriptor.mTransferMaxTimeSeconds = 5.0;
    downloadTestDescriptor.mNumberOfThreads = 16;
    downloadTestDescriptor.mBufferSizeBytes = 4096*4;
    mDownloadTest = new SKKitTestDownload(downloadTestDescriptor);

    mDownloadTest.start(new SKKitTestDownload.ISKDownloadTestProgressUpdate() {
      @Override
      public void onTestCompleted_OnMainThread(double mbpsPerSecond1024Based) {
        if (mDownloadTest != null) {
          System.out.println("Download test result: " + mbpsPerSecond1024Based + " Mbps");
          mDownloadTest = null;
        }
      }
    });
  }

  public static void doUploadTest() {

    SKKitTestUpload.SKKitTestDescriptor_Upload uploadTestDescriptor;
    uploadTestDescriptor = new SKKitTestUpload.SKKitTestDescriptor_Upload("speedtestsk1.ofca.gov.hk");
    uploadTestDescriptor.mPort = 80;
    uploadTestDescriptor.mWarmupMaxTimeSeconds = 2.0;
    uploadTestDescriptor.mTransferMaxTimeSeconds = 5.0;
    uploadTestDescriptor.mNumberOfThreads = 16;
    uploadTestDescriptor.mSendDataChunkSizeBytes = 512; // 4096*4; // 16K!
    mUploadTest = new SKKitTestUpload(uploadTestDescriptor);

    mUploadTest.start(new SKKitTestUpload.ISKUploadTestProgressUpdate() {
      @Override
      public void onTestCompleted_OnMainThread(double mbpsPerSecond1024Based) {
        if (mUploadTest != null) {
          System.out.println("Upload test result: " + mbpsPerSecond1024Based + " Mbps");
          mUploadTest = null;
        }
      }
    });
  }

  public static void main(String[] args) {
    System.out.println("Hello World!");

    doDownloadTest();
    //doUploadTest();

    try {
      Thread.sleep(1000000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    System.out.println("Hello World 3!");
  }
}
