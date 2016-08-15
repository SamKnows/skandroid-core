package com.samknows.measurement.storage;

import com.samknows.measurement.storage.StorageTestResult.*;

/**
 * All rights reserved SamKnows
 * Written by pablo@samknows.com
 */


/**
 * This class fragment is responsible for:
 * * Store average result
 */

public class SummaryResult
{
	private final DETAIL_TEST_ID testType;
	private final float max;
	private final float min;
	private final float average;
	
	public SummaryResult(DETAIL_TEST_ID pTestType, float pAverage, float pMax, float pMin)
	{
		this.testType = pTestType;
		this.max = pMax;
		this.min = pMin;
		this.average = pAverage;
	}
	
	public DETAIL_TEST_ID getTestType()
	{
		return testType;
	}
	
	public float getMax()
	{
		return max;
	}
	
	public float getMin()
	{
		return min;
	}

	public float getAverage()
	{
		return average;
	}
}
