package com.samknows.tests;

// NOTE: This code is written as Pure Java.
// It is possible to modify it to have Android-specific calls.
// Look at the static methods at the top of the class, for the commented-out Android-specific
// code that can be re-enabled if required in HttpTest.java

import com.samknows.libcore.SKPorting;

import java.util.List;
import java.util.Random;

public abstract class UploadTest extends HttpTest {

  //
  // END: code that you can make behave differently if you so wish on Android...
  //

  protected double bitrateMpbs1024Based = -1.0;			/* ???? Scale coefficient */
  protected byte[] buff;											/* buffer to send values */

  protected UploadTest(List<Param> params) {
    super(_UPSTREAM, params);
    this.init();
  }

  public static UploadTest sCreateUploadTest(List<Param> params) {
    UploadStrategy uploadStrategyServerBased = UploadStrategy.PASSIVE;
    UploadTest result = null;

    for (Param param : params) {
      if (param.contains(TestFactory.UPLOADSTRATEGY)) {
        uploadStrategyServerBased = UploadStrategy.ACTIVE;
        break;
      }
    }

    if (uploadStrategyServerBased == UploadStrategy.ACTIVE) {
      result = ActiveServerUploadTest.sCreateActiveServerUploadTest(params);
    } else {
      result = PassiveServerUploadTest.sCreatePassiveServerUploadTest(params);
    }

    if (result != null) {
      if (result.isReady()) {
        return result;
      } else {
        SKPorting.sAssert(false);
        return null;
      }

    } else {
      SKPorting.sAssert(false);
    }

    return result;
  }

  private String[] formValuesArr() {
    String[] values = new String[1];
    values = new String[1];
    values[0] = String.format("%.2f", (Math.max(0, getTransferBytesPerSecond()) * 8d / 1000000));

    return values;
  }

  private void init() {											/* don't forget to check error state after this method */
																	/* getSocket() is a method from the parent class */
    int maxSendDataChunkSize = 32768;

    // Generate this value in case we need it.
    // It is a random value from [0...2^32-1]
    Random sRandom = new Random();								/* Used for initialisation of upload array */

    if (getUploadBufferSize() > 0 && getUploadBufferSize() <= maxSendDataChunkSize) {
      buff = new byte[getUploadBufferSize()];
    } else {
      buff = new byte[maxSendDataChunkSize];
      SKPorting.sAssert(getClass(), false);
    }

    if (getRandomEnabled()) {											/* randomEnabled comes from the parent (HTTPTest) class */
      sRandom = new Random();									/* Used for initialisation of upload array */
      sRandom.nextBytes(buff);
    }
  }

  @Override
  public String getStringID() {
    String ret = "";
    if (getThreadsNum() == 1) {
      ret = UPSTREAMSINGLE;
    } else {
      ret = UPSTREAMMULTI;
    }
    return ret;
  }

  @Override
  public int getNetUsage() {
    return (int) (getTotalTransferBytes() + getTotalWarmUpBytes());
  }

  @Override
  public boolean isReady() {
    super.isReady();

    if (getUploadBufferSize() == 0 || getPostDataLength() == 0) {
      setError("Upload parameter missing");
      return false;
    }

    return true;
  }
}
