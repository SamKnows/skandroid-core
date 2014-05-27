package com.samknows.libcore;

import java.util.Locale;

import com.samknows.libcore.*;
import com.samknows.libcore.SKServiceDataCache.CachedValue;
import com.samknows.measurement.*;
import com.samknows.measurement.test.ScheduledTestExecutionQueue;

import org.robolectric.Robolectric;
import org.robolectric.Robolectric.*;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

// Optionally, can use Mockito!
import static org.mockito.Mockito.*;

import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(RobolectricTestRunner.class)
public class CoreTests {

//    public void testStringResource() throws Exception {
////        String hello = new MyActivity().getResources().getString(R.string.hello);
////        assertThat(hello, equalTo("Hello World, MyActivity!"));
//    }
    
    @Test
	public void testCommonVersion() throws Exception{
    	assertThat(SKCommon.getVersion().length() > 0, equalTo(true));
    	assertTrue(Double.valueOf(SKCommon.getVersion()) >= 1.0);
	}
    
    
    @Test
	public void testServiceDataCache() throws Exception{

		SKServiceDataCache serviceDataCache = new SKServiceDataCache();
		CachedValue getValueShouldBeNull = serviceDataCache.get("device",  100);
		assertNull(getValueShouldBeNull);
		
		serviceDataCache.put("device",  99,  "response",  "start");
		serviceDataCache.put("device2",  100,  "response2",  "start");
		
		getValueShouldBeNull = serviceDataCache.get("device",  100);
		assertNull(getValueShouldBeNull);
		
		getValueShouldBeNull = serviceDataCache.get("devicex",  99);
		assertNull(getValueShouldBeNull);
		
		CachedValue getValueShouldNotBeNull = serviceDataCache.get("device",  99);
		assertNotNull(getValueShouldNotBeNull);
		assertTrue(getValueShouldNotBeNull.responce.equals("response"));
		
		getValueShouldNotBeNull = serviceDataCache.get("device2",  100);
		assertNotNull(getValueShouldNotBeNull);
		assertTrue(getValueShouldNotBeNull.responce.equals("response2"));
	}

    @Test
	public void testDebugFlagNotSet() throws Exception{
    	// If set to true, the following flag is used in some UI screens, to show
    	// some debug-only UI elements.
    	// The value must NOT be shipped set to true!
    	
    	assertFalse(SKConstants.DEBUG);
	}
    
    @Test
	public void testNotUsingLocalConfig() throws Exception{
    	// If set to true, the following flag forces BACKGROUND tests to use a built-in
    	// test schedule file.
    	// The value must NOT be shipped set to true!
    	
    	assertFalse(SKConstants.USE_LOCAL_CONFIG);
	}    
    
    @Test
	public void testNotForcingBackgroundTestingToBeFrequent() throws Exception{
    	// If set to true, the following flag forces BACKGROUND tests to use a built-in
    	// test schedule file.
    	// The value must NOT be shipped set to true!
        assertFalse(ScheduledTestExecutionQueue.sGetDebugOnlyForceBackgroundTestingToBeFrequent());
	}
}
