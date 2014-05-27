package com.samknows.libcore;

import java.util.Locale;

import com.samknows.libcore.*;
import com.samknows.libcore.SKServiceDataCache.CachedValue;
import com.samknows.measurement.*;

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
public class LocaleTests {

	@Test
	public void testDateFormatingOfDoubles() throws Exception{

		Locale usLocale = Locale.ENGLISH;
		Locale.setDefault(usLocale);

		String theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1");
		System.out.println("theResult=" + theResult + ", should be 1.00000");
		assertTrue(theResult.equals("1.00000"));

		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1.0");
		System.out.println("theResult=" + theResult + ", should be 1.00000");
		assertTrue(theResult.equals("1.00000"));

		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1.123451");
		System.out.println("theResult=" + theResult + ", should be 1.12345");
		assertTrue(theResult.equals("1.12345"));

		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1.123456");
		System.out.println("theResult=" + theResult + ", should be 1.12346");
		assertTrue(theResult.equals("1.12346"));

		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1,0");
		System.out.println("theResult=" + theResult + ", should be 10.00000");
		assertTrue(theResult.equals("10.00000"));

		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1,123451");
		System.out.println("theResult=" + theResult + ", should be 1,123,451.00000");
		assertTrue(theResult.equals("1,123,451.00000"));

		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1,123456");
		System.out.println("theResult=" + theResult + ", should be 1,123,456.00000");
		assertTrue(theResult.equals("1,123,456.00000"));

		Locale commaBasedLocale = Locale.GERMAN;
		Locale.setDefault(commaBasedLocale);

		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1");
		System.out.println("theResult=" + theResult + ", should be 1,00000");
		assertTrue(theResult.equals("1,00000"));
		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1,0");
		System.out.println("theResult=" + theResult + ", should be 1,00000");
		assertTrue(theResult.equals("1,00000"));
		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1.123451");
		System.out.println("theResult=" + theResult + ", should be 1.123.451,00000");
		assertTrue(theResult.equals("1.123.451,00000"));
		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1.123456");
		assertTrue(theResult.equals("1.123.456,00000"));
		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1,0");
		assertTrue(theResult.equals("1,00000"));
		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1,123451");
		assertTrue(theResult.equals("1,12345"));
		theResult = SKCommon.sGetDecimalStringAnyLocaleAs1Pt5LocalisedString("1,123456");
		assertTrue(theResult.equals("1,12346"));
	}

	@Test
	public void testParsingOfDecimalStrings() throws Exception{

		Locale usLocale = Locale.ENGLISH;
		Locale.setDefault(usLocale);

		double theResult = SKCommon.sGetDecimalStringAnyLocaleAsDouble("1.2345");
		assertTrue(theResult == 1.2345);
		theResult = SKCommon.sGetDecimalStringAnyLocaleAsDouble("1,2345");
		assertTrue(theResult == 12345.0);

		Locale commaBasedLocale = Locale.GERMAN;
		Locale.setDefault(commaBasedLocale);

		theResult = SKCommon.sGetDecimalStringAnyLocaleAsDouble("1.2345");
		assertTrue(theResult == 12345.0);
		theResult = SKCommon.sGetDecimalStringAnyLocaleAsDouble("1,2345");
		assertTrue(theResult == 1.2345);
	}
}
