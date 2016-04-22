package com.samknows;

import com.samknows.libcore.BuildConfig;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

// NOTE: Run the test EITHER from Android Studio, OR from command line with "./gradlew test"

@RunWith(RobolectricGradleTestRunner.class)
//@Config(constants = BuildConfig.class,application = SKMobileApplication.class, sdk = 19)
@Config(constants = BuildConfig.class, sdk = 19)
public class LibcoreTests {

  @org.junit.Before
  public void setUp() throws Exception {
    //mApplication = new SKMobileApplication();
  }

  @org.junit.After
  public void tearDown() throws Exception {

  }

  @org.junit.Test
  public void libcoreMain() throws Exception {
    // TODO - this is a dummy test ... extend it!
    org.junit.Assert.assertTrue("Dummy", true);
  }
}
