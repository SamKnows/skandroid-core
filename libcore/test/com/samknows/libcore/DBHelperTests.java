package com.samknows.libcore;

import java.util.Locale;

import android.util.Log;

import org.robolectric.Robolectric;
import org.robolectric.Robolectric.*;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class DBHelperTests {

    @Test
    public void testDateFormatingOfDoubles() throws Exception{
    	
    	//Locale.setDefault(new Locale("ar"));
    	//Locale ar = new Locale("ar");
    	Locale ar = new Locale("ar","SA");
    	System.out.println(ar.getLanguage());
    	System.out.println(ar.getCountry());
    	System.out.println(ar.getISO3Country());
    	System.out.println(ar.getISO3Language());
    	
    	Locale[] allLocales = Locale.getAvailableLocales();
    	for (Locale theLocale : allLocales) {
    		if (theLocale.getLanguage().equals("ar")) {
    			System.out.println("LOCALE\n======");
    			System.out.println(theLocale.getLanguage());
    			System.out.println(theLocale.getCountry());
    			System.out.println(theLocale.getISO3Country());
    			System.out.println(theLocale.getISO3Language());
    		}
    	}
    
    	long start_time = 0;
    	long end_time = 0;
		String selection = String.format(Locale.US, "BETWEEN %d AND %d", start_time, end_time);
		System.out.println("US locale="+Locale.US);
		System.out.println("US selection="+selection);
    	assertTrue(selection.equals("BETWEEN 0 AND 0"));
    	
    	start_time = Long.MAX_VALUE;
    	end_time = Long.MIN_VALUE;
		selection = String.format("BETWEEN %d AND %d", start_time, end_time);
		System.out.println("US selection="+selection);
    	assertTrue(selection.equals("BETWEEN 9223372036854775807 AND -9223372036854775808"));
    
		selection = String.format(ar, "BETWEEN %d AND %d", start_time, end_time);
		System.out.println("ar selection="+selection);
		// This is weird - the numbers SHOULD be seen in Arabic format - but aren't!
    	assertTrue(selection.equals("BETWEEN 9223372036854775807 AND -9223372036854775808"));
		
    	start_time = 25;
    	end_time = 999;
		selection = String.format("BETWEEN %d AND %d", start_time, end_time);
		System.out.println("US selection="+selection);
    	assertTrue(selection.equals("BETWEEN 25 AND 999"));
    }
    
}
