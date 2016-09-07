package com.samknows.tests;

import java.util.List;
import java.util.Random;


import com.samknows.libcore.SKPorting;

public abstract class UploadTest extends HttpTest {

  protected double bitrateMpbs1024Based = -1.0;			/* ???? Scale coefficient */
  private byte[] buff;											/* buffer to send values */

  public byte[] getBufferDoNotRandomize() {
    return buff;
  }

  public int getBufferLength() {
    return buff.length;
  }

  public byte[] getBufferWithOptionalRandomize() {
    if (getRandomEnabled()) {
      mRandom.nextBytes(buff);
    }

    return buff;
  }

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

  Random mRandom = new Random();								/* Used for initialisation of upload array */

  private void init() {											/* don't forget to check error state after this method */
																	/* getSocket() is a method from the parent class */
    int maxSendDataChunkSize = 32768;

    // Generate this value in case we need it.
    // It is a random value from [0...2^32-1]

    if (getUploadBufferSize() > 0 && getUploadBufferSize() <= maxSendDataChunkSize) {
      buff = new byte[getUploadBufferSize()];
    } else {
      buff = new byte[maxSendDataChunkSize];
      SKPorting.sAssert(getClass(), false);
    }

    if (getRandomEnabled()) {											/* randomEnabled comes from the parent (HTTPTest) class */
      mRandom = new Random();								/* Used for initialisation of upload array */
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
