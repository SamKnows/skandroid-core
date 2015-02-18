package com.samknows.libcore;

import android.app.Activity;
import com.samknows.libcore.SKOperators.ISKQueryCompleted;
import com.samknows.libcore.SKOperators.SKOperators_Return;
import com.samknows.libcore.SKOperators.SKThrottledQueryResult;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class SKOperatorsTests {

	public SKOperatorsTests() {
		// TODO Auto-generated constructor stub
	}

    @Test
    public void testSingleton() throws Exception{
    	
    	// Create a dummy Context!
    	// http://robolectric.org/activity-lifecycle.html
    	Activity activity = (Activity) Robolectric.buildActivity(Activity.class).create().get();
    	
    	SKOperators operator = SKOperators.getInstance(activity);
    	SKOperators operator2 = SKOperators.getInstance(activity);
    	
    	assertTrue(operator != null);
    	assertTrue(operator == operator2);
    }
    
    @Test
    public void testThrottledQueryResultConstructor() throws Exception{
   
    	// Create a dummy Context!
    	// http://robolectric.org/activity-lifecycle.html
    	Activity activity = (Activity) Robolectric.buildActivity(Activity.class).create().get();
    	
    	SKOperators operator = SKOperators.getInstance(activity);
    	
    	SKThrottledQueryResult result = operator.new SKThrottledQueryResult();
		assertTrue(result.returnCode == SKOperators_Return.SKOperators_Return_NoThrottleQuery);
		assertTrue(result.timestamp != null);
		assertTrue(result.datetimeUTCSimple != null);
		assertTrue(result.datetimeUTCSimple.length() > 0);
		assertTrue(result.carrier.equals(""));
    }

    @Test
    public void testThrottleQuery() throws Exception {
    	// So far as we can, test that the query operation works as expected.
    	// It really would be a lot of work to try to fully mock this out, as that would require
    	// several layers of mocking; because the query is actually run asynchronously!

    	// Create a dummy Context!
    	// http://robolectric.org/activity-lifecycle.html
    	Activity activity = (Activity) Robolectric.buildActivity(Activity.class).create().get();
    	
    	SKOperators operators = SKOperators.getInstance(activity);
    	
     	SKThrottledQueryResult result = operators.fireThrottledWebServiceQueryWithCallback(new ISKQueryCompleted() {

			@Override
			public void onQueryCompleted(Exception e, long responseCode,
					String responseDataAsString) {
	    		// In practise, this is never actually reached; as the call is asynchronous!
	    		assertTrue(e == null);
	    		assertTrue(responseCode == 0);
	    		assertTrue(responseDataAsString != null);
	    		assertTrue(responseDataAsString.length() > 0);
			}
		});

    	assertTrue(result.returnCode == SKOperators_Return.SKOperators_Return_NoThrottleQuery);
    	assertTrue(result.timestamp != null);
    	assertTrue(result.datetimeUTCSimple != null);
    	assertTrue(result.datetimeUTCSimple.length() > 0);
    	assertTrue(result.carrier.equals(""));
    }
}
