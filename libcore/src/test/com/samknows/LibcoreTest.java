package com.samknows;

import com.samknows.libcore.BuildConfig;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

// NOTE: Run the test EITHER from Android Studio, OR from command line with "./gradlew test"

/**
 * Created by pcole on 17/08/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
//@Config(constants = BuildConfig.class,application = SKMobileApplication.class, sdk = 19)
@Config(constants = BuildConfig.class, sdk = 19)
public class LibcoreTest {

  @org.junit.Before
  public void setUp() throws Exception {
    //mApplication = new SKMobileApplication();
  }

  @org.junit.After
  public void tearDown() throws Exception {

  }

  @org.junit.Test
  public void testDummy() throws Exception {
    org.junit.Assert.assertTrue("Dummy", true);
  }
}
