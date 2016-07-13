package com.samknows.tests;

public interface ISKVideoTest {
  boolean runTestInCurrentThread();

  double getTestProgress0To1();

  boolean getIsTestComplete();

  double  getCurrentBestBitRate();

  int  getNumberOfStalls();

  double getCurrentDownloadBitrate();

  double getPrebufferingCompletedAfterSeconds();

  String  getCurrentBestBitRateQualityString();
}
