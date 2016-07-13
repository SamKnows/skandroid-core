package com.samknows.tests;

public class Conversions {
  public static double sConvertBytesPerSecondToMbps1000Based(final double bytesPerSecond) {
    double mbps = (bytesPerSecond * 8.0) / 1000000.0;
    return mbps;
  }

  public static double sConvertBytesPerSecondToMbps1024Based(final double bytesPerSecond) {
    double mbps = (bytesPerSecond * 8.0) / (1024.0 * 1024.0);
    return mbps;
  }

  public static double sConvertMbps1024BasedToMBps1000Based(double value1024Based) {
    return value1024Based * (1024.0 * 1024.0) / (1000.0 * 1000.0);
  }

  public static String formatToBytes(long bytes) {
    double data = bytes;
    if (data > 1024*1024) {
      data /= 1024d;
      data /= 1024d;
      return String.format("%.2fMB", data);
    } else if (data > 1024) {
      data /= 1024d;
      return String.format("%.2fKB", data);
    } else {
      return bytes + "B";
    }
  }

  public static double sConvertMbps1024BasedToBytesPerSecond(double bytesPerSecond) {
    return bytesPerSecond * (1024.0 * 1024.0) / 8.0;
  }

  public static String sThroughputBps1000BasedToString(double bps) {
    String ret = "";
    if (bps < 1000) {
      ret = String.format("%.0f bps", bps);
    } else if (bps < 1000000) {
      ret = String.format("%.2f Kbps", (double) (bps / 1000.0));
    } else {
      ret = String.format("%.2f Mbps", (double) (bps / 1000000.0));
    }
    return ret;
  }

//  public static String timeToString(double value){
//    String ret = "";
//    if(value < 1000){
//      ret = String.format("%.0f microseconds", value);
//    }else if(value < 1000000 ){
//      ret = String.format("%.0f ms", value);
//    }else {
//      ret = String.format("%.2f s", value);
//    }
//    return ret;
//  }


  public static String sBitrateMbps1024BasedToString (double bitrateMbps1024Based) {
    double bitrateMbps1000Based = sConvertMbps1024BasedToMBps1000Based(bitrateMbps1024Based);
    double bitrateBitsPerSecond = 1000000.0 * bitrateMbps1000Based;

    return sThroughputBps1000BasedToString(bitrateBitsPerSecond);
  }

  public static String sBitrateMbps1000BasedToString (double bitrateMbps1000Based) {
    double bitrateBitsPerSecond = 1000000.0 * bitrateMbps1000Based;

    return sThroughputBps1000BasedToString(bitrateBitsPerSecond);
  }

  public static String formatToBits(long bytes) {
    double data = bytes;
    data *= 8;
    if (data > 1000*1000) {
      data /= 1000d;
      data /= 1000d;
      return String.format("%.2fMb", data);
    } else if (data > 1000) {
      data /= 1000d;
      return String.format("%.2fKb", data);
    } else {
      return bytes + "b";
    }
  }

}
